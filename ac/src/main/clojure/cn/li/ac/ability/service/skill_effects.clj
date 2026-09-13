(ns cn.li.ac.ability.service.skill-effects
  "Common side-effect helpers for skills.

  Centralizes the boilerplate:
  - resource consumption via res/perform-resource
  - player-state updates
  - firing ability events"
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.rules.cooldown-rules :as cd-rules]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- resolve-session-id
  []
  (runtime-hooks/require-player-state-session-id "skill-effects"))

(defn- runtime-player-state
  [player-id]
  (store/get-player-state (resolve-session-id)
                           player-id))

(defn- runtime-player-state-in-session
  [session-id player-id]
  (store/get-player-state session-id player-id))

(declare perform-resource!
         perform-resource-in-session!
         add-skill-exp!
         add-skill-exp-in-session!
         set-main-cooldown!
         set-main-cooldown-in-session!
         apply-cost!
         apply-cooldown!
         enforce-overload-floor-in-session!
         enforce-overload-floor!
         mark-railgun-coin-judged!
         mark-railgun-coin-judged-in-session!
         clear-railgun-coin-judged!
         clear-railgun-coin-judged-in-session!
         gain-exp!
         emit-fx!
         get-player-state
         get-player-state-in-session!
         player-path
         player-path-in-session!
         skill-exp
         skill-exp-in-session!
         current-cp
         current-cp-in-session!)


(defn scale-damage
  "Apply global and per-skill damage scaling to a raw damage value.
  spec is the skill spec map (with :damage-scale key, default 1.0)."
  [spec raw-damage]
  (* (double raw-damage)
      (double (cfg/damage-scale))
     (double (or (:damage-scale spec) 1.0))))

(defn perform-resource!
  "Consume overload+cp from player's resource-data.
  Returns {:success? bool, :events events, :data new-resource-data}."
  ([player-id overload cp]
   (perform-resource! player-id overload cp false))
  ([player-id overload cp creative?]
  (perform-resource-in-session! (resolve-session-id)
                                player-id
                                overload
                                cp
                                creative?)))

(defn perform-resource-in-session!
  [session-id player-id overload cp creative?]
  (if (runtime-player-state-in-session session-id player-id)
    (let [{:keys [state success? events]} (command-rt/run-command-in-session! session-id
                                                      player-id
                                                      {:command :consume-resource
                                                      :overload (double overload)
                                                      :cp (double cp)
                                                      :creative? (boolean creative?)})
           data (:resource-data state)]
       {:success? (boolean success?)
        :events events
        :data data})
     {:success? false
      :events []
    :data nil}))

(defn add-skill-exp!
  "Add exp to a skill and fire produced events."
  ([player-id skill-id amount]
   (add-skill-exp! player-id skill-id amount 1.0))
  ([player-id skill-id amount exp-rate]
    (add-skill-exp-in-session! (resolve-session-id)
                      player-id
                      skill-id
                      amount
                      exp-rate)))

(defn add-skill-exp-in-session!
  [session-id player-id skill-id amount exp-rate]
  (when (runtime-player-state-in-session session-id player-id)
    (let [scaled-amount (* (double amount) (double exp-rate))
          {:keys [state events]} (command-rt/run-command-in-session! session-id
                             player-id
                             {:command :add-skill-exp
                              :skill-id skill-id
                              :amount scaled-amount})
          data (:ability-data state)]
         {:data data :events events})))

(defn set-main-cooldown!
  "Set main cooldown for ctrl-id (or skill-id)."
  [player-id ctrl-id cooldown-ticks]
  (set-main-cooldown-in-session! (resolve-session-id)
                                 player-id
                                 ctrl-id
                                 cooldown-ticks)
  true)

(defn set-main-cooldown-in-session!
  [session-id player-id ctrl-id cooldown-ticks]
  (command-rt/run-command-in-session! session-id
                                      player-id
                                      {:command :set-cooldown
                                       :ctrl-id ctrl-id
                                       :sub-id :main
                                       :ticks (max 1 (int cooldown-ticks))})
  true)

(defn- resolve-val
  ([v player-id skill-id exp]
   (cond
     (number? v) (double v)
     (fn? v) (double (or (try
                           (v player-id skill-id exp)
                           (catch clojure.lang.ArityException _
                             (v {:player-id player-id :skill-id skill-id :exp exp})))
                         0.0))
     :else 0.0))
  ([v evt]
   (resolve-val v (:player-id evt) (:skill-id evt) (double (or (:exp evt) 0.0)))))

(defn- runtime-scaled-cost
  [cost-spec]
  (let [cp-speed (double (or (:cp-speed cost-spec) 1.0))
        overload-speed (double (or (:overload-speed cost-spec) 1.0))]
    {:cp (* (cfg/runtime-cp-consume-per-tick) cp-speed)
     :overload (* (cfg/runtime-overload-per-tick) overload-speed)}))

(defn apply-cost!
  "Apply stage cost for a skill spec.
  Scales cp/overload by the skill's per-skill speed multipliers.
  Returns true when cost is paid or no cost is defined."
  ([spec stage player-id skill-id exp]
   (apply-cost! spec stage player-id skill-id exp nil))
  ([spec stage player-id skill-id exp player-ref]
   (let [cost-spec (get-in spec [:cost stage])]
     (if-not (map? cost-spec)
       true
       (let [computed (if (= :runtime-speed (:mode cost-spec))
                        (runtime-scaled-cost cost-spec)
                        cost-spec)
             cp-raw (resolve-val (:cp computed) player-id skill-id exp)
             overload-raw (resolve-val (:overload computed) player-id skill-id exp)
             cp-speed (double (or (:cp-consume-speed spec) 1.0))
             ol-speed (double (or (:overload-consume-speed spec) 1.0))
             cp (* cp-raw cp-speed)
             overload (* overload-raw ol-speed)
             creative-raw (:creative? computed)
             creative? (boolean (if (fn? creative-raw)
                                  (try
                                    (creative-raw player-id skill-id exp player-ref)
                                    (catch clojure.lang.ArityException _
                                      (try
                                        (creative-raw player-id skill-id exp)
                                        (catch clojure.lang.ArityException _
                                          (creative-raw {:player-id player-id
                                                         :skill-id skill-id
                                                         :exp exp
                                                         :player-ref player-ref})))))
                                  creative-raw))]
         (if (and (zero? cp) (zero? overload))
           true
           (let [{:keys [success?]} (perform-resource! player-id overload cp creative?)]
             (boolean success?)))))))
  ;; Legacy evt-map arity for tests and transitional call sites.
  ([spec stage evt]
   (apply-cost! spec
                stage
                (:player-id evt)
                (:skill-id evt)
                (double (or (:exp evt) 0.0))
                (:player-ref evt))))

(defn apply-cooldown!
  "Apply cooldown for skill according to ctrl-id and cooldown-policy."
  [spec evt]
  (let [mode (get-in spec [:cooldown :mode])]
    (when (not= :manual mode)
      (let [player-id (:player-id evt)
            ctrl-id (or (:ctrl-id spec) (:id spec))
            ticks (int (cd-rules/resolve-ticks spec {:player-id player-id
                                                     :skill-id (:skill-id evt)
                                                     :exp (:exp evt)}))]
        (set-main-cooldown! player-id ctrl-id ticks)
        ticks))))

(defn enforce-overload-floor!
  "Ensure player overload is not below floor-value.

  Returns true when player state exists; false otherwise."
  [player-id floor-value]
  (enforce-overload-floor-in-session! (resolve-session-id)
                                      player-id
                                      floor-value))

(defn enforce-overload-floor-in-session!
  [session-id player-id floor-value]
  (if (runtime-player-state-in-session session-id player-id)
    (do
      (command-rt/run-command-in-session!
       session-id
       player-id
       {:command :enforce-overload-floor
        :floor-value floor-value})
      true)
    false))

(defn mark-railgun-coin-judged!
  "Record the currently judged railgun coin UUID in runtime state."
  [player-id coin-uuid]
  (mark-railgun-coin-judged-in-session! (resolve-session-id)
                                        player-id
                                        coin-uuid))

(defn mark-railgun-coin-judged-in-session!
  [session-id player-id coin-uuid]
  (command-rt/run-command-in-session!
   session-id
   player-id
   {:command :set-railgun-coin-judged-uuid
    :coin-uuid coin-uuid})
  true)

(defn clear-railgun-coin-judged!
  "Clear the one-shot judgement lock for railgun coin QTE."
  [player-id]
  (clear-railgun-coin-judged-in-session! (resolve-session-id)
                                         player-id))

(defn clear-railgun-coin-judged-in-session!
  [session-id player-id]
  (command-rt/run-command-in-session!
   session-id
   player-id
   {:command :clear-railgun-coin-judged-uuid})
  true)

(defn skill-destroy-allowed?
  "Skill-level destroy gate — upstream Skill.shouldDestroyBlocks()
   (getOptionalBool(\"destroy_blocks\", true)). The global Settings
   \"Destroy blocks\" toggle is enforced inside block-manip/break-block!;
   this is the per-skill config flag on top of it."
  [skill-id]
  (boolean (get (skill-registry/get-skill skill-id) :destroy-blocks? true)))

(defn gain-exp!
  "Apply exp gain from :exp-policy {:amount n :rate n} when present.
  Multiplies rate by the skill's :exp-incr-speed."
  [spec evt]
  (let [skill-id (:id spec)
        explicit-exp (:exp spec)
        effective? (boolean (:effective? evt))
        amount (cond
                 (map? explicit-exp)
                 (if effective?
                   (:effective explicit-exp)
                   (:ineffective explicit-exp))
                 :else (get-in spec [:exp-policy :amount]))]
    (when (and (some? amount) skill-id)
      (let [base-rate (double (or (get-in spec [:exp-policy :rate]) 1.0))
            skill-rate (double (or (:exp-incr-speed spec) 1.0))
            rate (* base-rate skill-rate)
            value (resolve-val amount evt)]
        (add-skill-exp! (:player-id evt) skill-id value rate)))))

(defn emit-fx!
  "Emit fx stage for a context event.

  By default this reaches only the context owner (fx/send!'s :client
  default). Pass broadcast? true to also fan out to nearby players
  (fx/send-local-and-nearby!) for effects that should be publicly visible,
  not just to the caster."
  ([spec evt stage] (emit-fx! spec evt stage false))
  ([spec evt stage broadcast?]
   (when-let [entry (get-in spec [:fx stage])]
     (if broadcast?
       (fx/send-local-and-nearby! (:ctx-id evt) entry evt nil)
       (fx/send! (:ctx-id evt) entry evt)))))

(defn get-player-state
  "Return full player state map or nil when absent."
  [player-id]
  (get-player-state-in-session! (resolve-session-id)
                                player-id))

(defn get-player-state-in-session!
  [session-id player-id]
  (runtime-player-state-in-session session-id player-id))

(defn player-path
  "Read arbitrary path from player state with optional default value."
  ([player-id path]
    (player-path-in-session! (resolve-session-id) player-id path))
  ([player-id path default]
    (player-path-in-session! (resolve-session-id) player-id path default)))

(defn player-path-in-session!
  ([session-id player-id path]
   (get-in (runtime-player-state-in-session session-id player-id) path))
  ([session-id player-id path default]
   (get-in (runtime-player-state-in-session session-id player-id) path default)))

;; Matches original's Skill.expCustomized: a skill can register a fn that
;; computes its exp synthetically (e.g. RadIntensify's max-CP ratio) instead
;; of reading the normal stored/accumulated value — applied everywhere exp
;; for that skill is read (UI display, prerequisite checks, etc.), not just
;; at the one call site that originally needed it.
(defonce ^:private custom-skill-exp-fns (atom {}))

(defn register-custom-skill-exp!
  [skill-id f]
  (swap! custom-skill-exp-fns assoc skill-id f)
  nil)

(defn skill-exp
  "Read clamped skill exp as double from ability-data."
  [player-id skill-id]
  (skill-exp-in-session! (resolve-session-id)
                         player-id
                         skill-id))

(defn skill-exp-in-session!
  [session-id player-id skill-id]
  (if-let [custom-fn (get @custom-skill-exp-fns skill-id)]
    (double (custom-fn player-id))
    (double (adata/get-skill-exp (:ability-data (runtime-player-state-in-session session-id player-id)) skill-id))))

(defn current-cp
  "Read current CP from resource-data as double."
  [player-id]
  (current-cp-in-session! (resolve-session-id)
                          player-id))

(defn current-cp-in-session!
  [session-id player-id]
  (double (or (player-path-in-session! session-id player-id [:resource-data :cur-cp] 0.0) 0.0)))



