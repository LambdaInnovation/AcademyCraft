(ns cn.li.ac.command.handlers
  "Command business logic handlers - pure functions returning action maps.

  All handlers take a command context and return action maps that describe
  what should happen (send message, modify player data, etc.). The platform
  layer executes these actions.

  Action map schema:
    {:action :send-message :message string :translate? bool}
    {:action :grant-advancement :advancement-id string :player player-obj}
    {:action :switch-category :category-id keyword :player player-obj}
    {:action :learn-node :node-id keyword :player player-obj}
    {:action :unlearn-node :node-id keyword :player player-obj}
    {:action :set-level :level int :player player-obj}
    {:action :set-node-exp :node-id keyword :exp float :player player-obj}
    {:action :restore-cp :player player-obj}
    {:action :clear-cooldowns :player player-obj}
    {:action :reset-abilities :player player-obj}
    {:action :maxout-progression :player player-obj}
    {:action :enable-cheats :player player-obj}
    {:action :disable-cheats :player player-obj}"
  (:require [cn.li.ac.ability.registry.category :as cat]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.registry.skill :as skill]
            [clojure.string :as str]))

;; ============================================================================
;; Helper Functions
;; ============================================================================

(defn success-message
  "Create a success message action"
  [message-key & args]
  {:action :send-message
   :message message-key
   :args (vec args)
   :translate? true})

(defn error-message
  "Create an error message action"
  [message-key & args]
  {:action :send-message
   :message message-key
   :args (vec args)
   :translate? true
   :error? true})

(defn get-target-player
  "Get the target player from context (either explicit target or executor)"
  [ctx]
  (or (:target-player ctx) (:player ctx)))

(defn- resolve-advancement-id
  [advancement-str]
  (let [raw (str advancement-str)]
    (if (str/includes? raw ":")
      raw
      (modid/namespaced-path (str "achievements/" (str/replace raw "." "/"))))))

;; ============================================================================
;; /acach Command Handler
;; ============================================================================

(defn handle-grant-advancement
  "Grant an advancement to a player.

  Context args:
    :advancement - advancement ID string
    :player - optional target player (defaults to executor)"
  [ctx]
  (let [advancement-id (:advancement (:arguments ctx))
        target-player (get-target-player ctx)]
    (if-not advancement-id
      (error-message "command.academy.acach.missing_advancement")
      {:action :grant-advancement
       :advancement-id (resolve-advancement-id advancement-id)
       :player target-player})))

;; ============================================================================
;; /aim Command Handlers
;; ============================================================================

(defn handle-aim-cat
  "Switch ability category.

  Context args:
    :category - category ID string"
  [ctx]
  (let [category-str (:category (:arguments ctx))
        target-player (get-target-player ctx)]
    ;; Upstream prints the current category when the argument is omitted.
    (if (nil? category-str)
      {:action :show-category :player target-player}
      (let [category-id (keyword category-str)]
        (if-not (cat/get-category category-id)
          (error-message "command.academy.aim.cat.not_found" category-str)
          {:action :switch-category
           :category-id category-id
           :player target-player})))))

(defn handle-aim-catlist
  "List all available ability categories."
  [_ctx]
  (let [categories (cat/get-all-categories)
        cat-names (map #(name (:id %)) categories)]
    (if (empty? cat-names)
      (error-message "command.academy.aim.catlist.empty")
      (success-message "command.academy.aim.catlist.success"
                       (str/join ", " cat-names)))))

(defn handle-aim-reset
  "Reset all abilities for the player."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :reset-abilities
     :player target-player}))

(defn handle-aim-learn
  "Learn a node.

  Context args:
    :skill - skill ID string"
  [ctx]
  (let [skill-str (:skill (:arguments ctx))
        skill-id (keyword skill-str)
        target-player (get-target-player ctx)]
    (if-not (skill/get-skill skill-id)
      (error-message "command.academy.aim.node.learn.not_found" skill-str)
      {:action :learn-node
       :node-id skill-id
       :player target-player})))

(defn handle-aim-unlearn
  "Unlearn a node.

  Context args:
    :skill - skill ID string"
  [ctx]
  (let [skill-str (:skill (:arguments ctx))
        skill-id (keyword skill-str)
        target-player (get-target-player ctx)]
    (if-not (skill/get-skill skill-id)
      (error-message "command.academy.aim.node.unlearn.not_found" skill-str)
      {:action :unlearn-node
       :node-id skill-id
       :player target-player})))

(defn handle-aim-learn-all
  "Learn all nodes in current category."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :learn-all-nodes
     :player target-player}))

(defn handle-aim-learned
  "List all learned nodes."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :list-learned-nodes
     :player target-player}))

(defn handle-aim-skills
  "List all available nodes in current category."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :list-available-nodes
     :player target-player}))

(defn handle-aim-level
  "Set player ability level (1-5).

  Context args:
    :level - integer level"
  [ctx]
  (let [level (:level (:arguments ctx))
        target-player (get-target-player ctx)]
    (cond
      ;; Upstream prints the current level when the argument is omitted.
      (nil? level)
      {:action :show-level :player target-player}

      (not (and (integer? level) (>= level 1) (<= level 5)))
      (error-message "command.academy.aim.level.invalid" level)

      :else
      {:action :set-level
       :level level
       :player target-player})))

(defn handle-aim-exp
  "Set node experience.

  Context args:
    :skill - skill ID string
    :exp - float experience value (0.0-1.0)"
  [ctx]
  (let [skill-str (:skill (:arguments ctx))
        skill-id (keyword skill-str)
        exp (:exp (:arguments ctx))
        target-player (get-target-player ctx)]
    (cond
      (not (skill/get-skill skill-id))
      (error-message "command.academy.aim.node.exp.not_found" skill-str)

      ;; Upstream prints the skill's current exp when the value is omitted.
      (nil? exp)
      {:action :show-node-exp
       :node-id skill-id
       :player target-player}

      (not (and (number? exp) (>= exp 0.0) (<= exp 1.0)))
      (error-message "command.academy.aim.node.exp.invalid" exp)

      :else
      {:action :set-node-exp
       :node-id skill-id
       :exp (float exp)
       :player target-player})))

(defn handle-aim-fullcp
  "Restore CP to full."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :restore-cp
     :player target-player}))

(defn handle-aim-cd-clear
  "Clear all cooldowns."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :clear-cooldowns
     :player target-player}))

(defn handle-aim-maxout
  "Fill the current level's progress bar, matching upstream maxout."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :maxout-progression
     :player target-player}))

(defn handle-aim-max-all
  "Level 5 + every skill learned + every skill at exp 1.0. Not upstream."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :max-all-progression
     :player target-player}))

(defn handle-aim-help
  "Show help message."
  [_ctx]
  (success-message "command.academy.aim.help"))

(defn handle-aim-cheats-on
  "Enable cheat mode."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :enable-cheats
     :player target-player}))

(defn handle-aim-cheats-off
  "Disable cheat mode."
  [ctx]
  (let [target-player (get-target-player ctx)]
    {:action :disable-cheats
     :player target-player}))
