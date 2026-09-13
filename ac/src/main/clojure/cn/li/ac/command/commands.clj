(ns cn.li.ac.command.commands
  (:require [cn.li.ac.ability.registry.category :as cat]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.command.dsl :as cmd]
            [cn.li.ac.command.handlers :as handlers]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.runtime.install :as install]))

(defn- resolve-handler [handler-name]
  (or (ns-resolve 'cn.li.ac.command.handlers (symbol handler-name))
      (throw (ex-info "Command handler not found" {:handler handler-name}))))

(defn- category-id-suggestions
  "Tab-completion values for category arguments. Reads the category registry
  at completion time — categories registered later appear automatically."
  [_ctx]
  (map #(name (:id %)) (cat/get-all-categories)))

(defn- skill-id-suggestions
  "Tab-completion values for skill arguments. Prefers the requesting player's
  current category (that is what learn/unlearn operate on); falls back to
  every registered category when there is no player (console) or no category
  yet. Registry + player state are read at completion time — nothing is
  hardcoded."
  [{:keys [player-uuid]}]
  (let [session-id (runtime-hooks/player-state-session-id)
        current-cat (when (and player-uuid session-id)
                      (get-in (store/get-player-state session-id player-uuid)
                              [:ability-data :category-id]))
        cat-ids (if current-cat
                  [current-cat]
                  (map :id (cat/get-all-categories)))]
    (->> cat-ids
         (mapcat skill-query/get-skills-for-category)
         (keep :id)
         (map name)
         sort)))

(defn- build-common-aim-subcommands []
  ;; Upstream runs cat / level / exp with the value omitted as a query that
  ;; prints the current one, so those arguments are optional here.
  {:cat {:arguments [{:name "category"
                      :type :string
                      :optional? true
                      :description "Category ID to switch to; omit to print the current one"
                      :suggestions category-id-suggestions}]
         :executor-fn (resolve-handler "handle-aim-cat")
         :description "Show or switch ability category"}
   :catlist {:arguments []
             :executor-fn (resolve-handler "handle-aim-catlist")
             :description "List all ability categories"}
   :reset {:arguments []
           :executor-fn (resolve-handler "handle-aim-reset")
           :description "Reset all abilities"}
   :learn {:arguments [{:name "skill"
                        :type :string
                        :description "Skill ID to learn"
                        :suggestions skill-id-suggestions}]
           :executor-fn (resolve-handler "handle-aim-learn")
           :description "Learn a skill"}
   :unlearn {:arguments [{:name "skill"
                          :type :string
                          :description "Skill ID to unlearn"
                          :suggestions skill-id-suggestions}]
             :executor-fn (resolve-handler "handle-aim-unlearn")
             :description "Unlearn a skill"}
   :learn_all {:arguments []
               :executor-fn (resolve-handler "handle-aim-learn-all")
               :description "Learn all skills in current category"}
   :learned {:arguments []
             :executor-fn (resolve-handler "handle-aim-learned")
             :description "List learned skills"}
   :skills {:arguments []
            :executor-fn (resolve-handler "handle-aim-skills")
            :description "List available skills"}
   :level {:arguments [{:name "level"
                        :type :integer
                        :optional? true
                        :description "Level to set (1-5); omit to print the current one"}]
           :executor-fn (resolve-handler "handle-aim-level")
           :description "Show or set ability level"}
   :exp {:arguments [{:name "skill"
                      :type :string
                      :description "Skill ID"
                      :suggestions skill-id-suggestions}
                     {:name "exp"
                      :type :float
                      :optional? true
                      :description "Experience value (0.0-1.0); omit to print the current one"}]
         :executor-fn (resolve-handler "handle-aim-exp")
         :description "Show or set skill experience"}
   :fullcp {:arguments []
            :executor-fn (resolve-handler "handle-aim-fullcp")
            :description "Restore CP to full"}
   :cd_clear {:arguments []
              :executor-fn (resolve-handler "handle-aim-cd-clear")
              :description "Clear all cooldowns"}
   :maxout {:arguments []
            :executor-fn (resolve-handler "handle-aim-maxout")
            :description "Fill the current level's progress bar"}
   :max_all {:arguments []
             :executor-fn (resolve-handler "handle-aim-max-all")
             :description "Level 5, learn every skill, set every skill to full exp"}
   :help {:arguments []
          :executor-fn (resolve-handler "handle-aim-help")
          :description "Show help message"}})

;; Upstream requires a chosen category for every subcommand that reads or
;; writes category-scoped data, answering `nonecathint` otherwise. cat,
;; catlist, reset, learned and level are deliberately absent: upstream runs
;; those with no category too.
(def ^:private category-required-subcommands
  #{:learn :unlearn :learn_all :skills :fullcp :exp :cd_clear :maxout :max_all})

(defn context-player-uuid
  "UUID of the player a subcommand acts on.

  `cmd-ctx/create-context` carries the platform player object and a
  :player-uuid-fn to resolve it — there is no :player-uuid key on the context
  itself, so reading one directly always yielded nil and every gate below it
  failed open or closed regardless of the real player. Targets before
  executor, matching handlers/get-target-player, so /aimp gates on the player
  being operated on."
  [{:keys [player target-player metadata] :as ctx}]
  (let [subject (or target-player player)]
    (or (:player-uuid ctx)
        (cond
          (string? subject) subject
          (keyword? subject) (name subject)
          subject (when-let [resolve-fn (:player-uuid-fn metadata)]
                    (resolve-fn subject))))))

(defn cheats-enabled?
  "Whether /aim's cheat switch is on for this context's player.

  Creative mode counts as on, matching upstream's
  `!isActive(player) && !player.capabilities.isCreativeMode` gate."
  [{:keys [player] :as ctx}]
  (boolean
   (or (and player
            (entity/available?)
            (entity/player-creative? player))
       (let [session-id (runtime-hooks/player-state-session-id)
             player-uuid (context-player-uuid ctx)]
         (when (and player-uuid session-id)
           (get-in (store/get-player-state session-id player-uuid)
                   [:cheats-data :enabled?]))))))

(defn has-category?
  "Whether this context's player has chosen an ability category."
  [ctx]
  (let [session-id (runtime-hooks/player-state-session-id)
        player-uuid (context-player-uuid ctx)]
    (boolean
     (when (and player-uuid session-id)
       (get-in (store/get-player-state session-id player-uuid)
               [:ability-data :category-id])))))

(defn- gated-by-category
  "Wrap a subcommand executor with upstream's `aData.hasCategory()` check."
  [executor-fn]
  (fn [ctx]
    (if (has-category? ctx)
      (executor-fn ctx)
      (handlers/error-message "command.academy.aim.nonecathint"))))

(defn- gated-by-cheats
  "Wrap an /aim subcommand executor with upstream CommandAIM's cheat gate.

  Upstream refuses every subcommand until the player switches cheats on —
  that refusal is the entire observable effect of cheats_on, and without it
  the switch does nothing at all. cheats_on/cheats_off/help stay ungated so
  the switch remains reachable. The gate is defense in depth now that the
  command itself is permission-level 4 like upstream's CommandAIM; /aimp
  carries no cheat gate because upstream's CommandAIMP has no such check —
  it is already level 4."
  [executor-fn]
  (fn [ctx]
    (if (cheats-enabled? ctx)
      (executor-fn ctx)
      (handlers/error-message "command.academy.aim.notactive"))))

(defn- with-category-gate
  [subcommands]
  (reduce-kv (fn [acc subcommand spec]
               (assoc acc subcommand
                      (cond-> spec
                        (contains? category-required-subcommands subcommand)
                        (update :executor-fn gated-by-category))))
             {}
             subcommands))

(defn- build-aim-subcommands []
  (-> (reduce-kv (fn [acc subcommand spec]
                   (assoc acc subcommand
                          (if (= :help subcommand)
                            spec
                            (update spec :executor-fn gated-by-cheats))))
                 {}
                 (with-category-gate (build-common-aim-subcommands)))
      (assoc :cheats_on {:arguments []
                         :executor-fn (resolve-handler "handle-aim-cheats-on")
                         :description "Enable cheat mode"}
             :cheats_off {:arguments []
                          :executor-fn (resolve-handler "handle-aim-cheats-off")
                          :description "Disable cheat mode"})))

(defn- build-aimp-subcommands []
  ;; The category check is in CommandAIMBase.matchCommands upstream, so it
  ;; applies to /aimp too — unlike the cheat gate, which lives in CommandAIM.
  (with-category-gate (build-common-aim-subcommands)))

(defn init-commands!
  []
  (install/framework-once! ::commands-installed?
  (fn []
    (cmd/register-command!
      (cmd/create-command-spec
        "acach"
        ;; Upstream CommandACACH extends ACCommand which does not override
        ;; getRequiredPermissionLevel() — vanilla 1.12.2 defaults to 4.
        {:permission-level 4
         :arguments [{:name "advancement"
                      :type :string
                      :description "Advancement ID to grant"}
                     {:name "player"
                      :type :player
                      :optional? true
                      :description "Target player (defaults to self)"}]
         :executor-fn (resolve-handler "handle-grant-advancement")
         :description "Grant an advancement to a player"}))

    (cmd/register-command!
      (cmd/create-command-spec
        "aim"
        ;; Upstream CommandAIM extends ACCommand which does not override
        ;; getRequiredPermissionLevel() — vanilla 1.12.2 defaults to 4, so
        ;; /aim is an OP-level-4 command there. Keep the same level.
        {:permission-level 4
         :description "Manage your abilities"
         :subcommands (build-aim-subcommands)}))

    (cmd/register-command!
      (cmd/create-command-spec
        "aimp"
        ;; Same: upstream CommandAIMP inherits the vanilla default of 4.
        {:permission-level 4
         :description "Manage abilities for a player"
         :arguments [{:name "player"
                      :type :player
                      :description "Target player"}]
         :subcommands (build-aimp-subcommands)})))))
