(ns cn.li.ac.ability.service.reducer
  "Pure command reducer for ability system.
  
  Takes a player-state map and a command map, returns:
    {:state   updated-player-state  (pure data)
     :events  [event-maps]          (to be fired by shell)
     :effects [effect-plans]}       (to be executed by shell)
  
  Sub-reducers are private and focused on a single concern.
  apply-command is the top-level dispatcher.
  
  No side effects, no atoms, no dynamic bindings, no requiring-resolve.
  All business logic delegates to the rules layer (rules/*.clj)."
  (:require [cn.li.ac.ability.rules.learning-rules :as learning]
            [cn.li.ac.ability.rules.develop-rules :as develop-rules]
            [cn.li.ac.ability.rules.resource-rules :as resource]
            [cn.li.ac.ability.rules.cooldown-rules :as cooldown]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.model.cooldown :as cdata]
            [cn.li.ac.ability.model.preset :as pdata]
            [cn.li.ac.ability.model.develop :as ddata]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.config :as cfg]
            [cn.li.mcmod.util.log :as log]))

;; Duplicated from runtime-store/sync-domains rather than required: this ns is
;; the pure reducer layer (no atoms, no imperative shell deps) and runtime-store
;; is the imperative shell built on top of it.
(def ^:private sync-domains
  #{:ability-data :resource-data :cooldown-data :preset-data :develop-data})

;; ============================================================================
;; Outcome Builder Helpers
;; ============================================================================

(defn- ok
  "Build a successful reducer result."
  ([state]
   {:state state :events [] :effects []})
  ([state events]
   {:state state :events events :effects []})
  ([state events effects]
   {:state state :events events :effects effects}))

(defn- rejected
  "Build a no-op reducer result (command rejected)."
  ([state reason]
   {:state state :events [] :effects [] :rejected-reason reason}))

;; ============================================================================
;; Sub-Reducer: learn-skill
;; ============================================================================

(defn- cmd-learn-skill
  "Learn a skill for a player.
  
  Command fields:
    :skill-id       keyword
    :player-uuid    UUID-string
    :player-level   int
    :developer-type keyword :portable|:normal|:advanced
    :check-conditions? bool (default true)"
  [player-state {:keys [skill-id player-uuid player-level developer-type check-conditions?]
                 :or {check-conditions? true}}]
  (let [ability-data (:ability-data player-state)]
    
    ;; Already learned?
    (if (adata/is-learned? ability-data skill-id)
      (rejected player-state :already-learned)
      
      (let [skill-spec (skill-registry/get-skill skill-id)]
        
        ;; Skill exists?
        (if-not skill-spec
          (rejected player-state :unknown-skill)
          
          ;; Check conditions if requested
          (let [conditions-ok? (if check-conditions?
                                 (learning/can-learn? skill-spec ability-data
                                                      player-level developer-type)
                                 true)]
            (if-not conditions-ok?
              (rejected player-state :conditions-not-met)
              
              (let [new-ability-data (adata/learn-skill ability-data skill-id)
                  events [(evt/make-skill-learn-event player-uuid skill-id)]
                    effects [{:effect/type :network-send
                        :player-uuid player-uuid
                              :channel :ability/skill-learned
                              :payload {:player-uuid player-uuid :skill-id skill-id}}
                             {:effect/type :persist-state
                        :player-uuid player-uuid
                              :domain :ability-data}]]
                (ok (assoc player-state :ability-data new-ability-data)
                    events
                    effects)))))))))

;; ============================================================================
;; Sub-Reducer: level-up
;; ============================================================================

(defn- cmd-level-up
  "Attempt to level up based on current ability progress.
  
  Command fields:
    :player-uuid  UUID-string
    :force?       bool (default false) — skip threshold check"
  [player-state {:keys [player-uuid force?] :or {force? false}}]
  (let [ability-data (:ability-data player-state)
        resource-data (:resource-data player-state)
        cat-id (:category-id ability-data)
        current-level (:level ability-data)]
    
    (if (>= current-level (cfg/max-level))
      (rejected player-state :at-max-level)
      
      (let [skills (skill-query/get-controllable-skills-at-level cat-id current-level)
            cat-rate (category/get-prog-incr-rate cat-id)
            can-level? (or force?
                           (learning/can-level-up? ability-data skills cat-rate
                                                   (cfg/prog-incr-rate) (cfg/max-level)))]
        (if-not can-level?
          (rejected player-state :not-enough-progress)
          
          (let [{:keys [data old-level new-level]} (learning/perform-level-up ability-data)
                new-res (-> resource-data
                            (rdata/reset-add-max)
                            (rdata/recalc-max-values new-level))
                events [(evt/make-level-change-event player-uuid old-level new-level)]
                effects [{:effect/type :network-send
                          :channel :ability/level-up
                          :payload {:player-uuid player-uuid
                                    :old-level old-level
                                    :new-level new-level}}
                         {:effect/type :persist-state
                          :domain :ability-data}]]
            (ok (assoc player-state :ability-data data :resource-data new-res)
                events
                effects)))))))

;; ============================================================================
;; Sub-Reducer: add-skill-exp
;; ============================================================================

(defn- cmd-add-skill-exp
  "Add exp to a skill and accumulate level progress.

  Command fields:
    :player-uuid  UUID-string
    :skill-id     keyword
    :amount       float
    :source       keyword (optional, for audit events)

  Upstream AbilityData.addSkillExp:
  - learnSkill(skill) — exp gained by using a skill auto-learns it, so the
    port must not lose exp on an unlearned skill (fire the learn event too,
    mirroring cmd-learn-skill)
  - events carry the PRE-clamp amount and fire even when the skill is already
    mastered (exp clamp at 1.0 must not silence level progress / events)"
  [player-state {:keys [player-uuid skill-id amount source]}]
  (let [ability-data (:ability-data player-state)
        skill-spec (skill-registry/get-skill skill-id)
        exp-incr-speed (or (:exp-incr-speed skill-spec) 1.0)
        global-rate (cfg/prog-incr-rate)
        was-learned? (adata/is-learned? ability-data skill-id)
        ability-data* (adata/learn-skill ability-data skill-id)

        {:keys [data exp-scaled]} (learning/add-skill-exp
                                    ability-data* skill-id amount
                                    exp-incr-speed global-rate)]

    (if (<= (double (or exp-scaled 0.0)) 0.0)
      (ok player-state)

      (let [events (cond-> [{:event/type evt/EVT-SKILL-EXP-ADDED
                             :event/side :server
                             :player-uuid player-uuid
                             :skill-id skill-id
                             :amount exp-scaled
                             :source source}
                            {:event/type evt/EVT-SKILL-EXP-CHANGED
                             :event/side :server
                             :player-uuid player-uuid
                             :skill-id skill-id
                             :new-exp (adata/get-skill-exp data skill-id)}]
                     (not was-learned?)
                     (conj (evt/make-skill-learn-event player-uuid skill-id)))
            effects (when-not was-learned?
                      [{:effect/type :network-send
                        :player-uuid player-uuid
                        :channel :ability/skill-learned
                        :payload {:player-uuid player-uuid :skill-id skill-id}}])]
        (ok (assoc player-state :ability-data data) events effects)))))

;; ============================================================================
;; Sub-Reducer: consume-resource
;; ============================================================================

(defn- cmd-consume-resource
  "Attempt to consume CP and/or overload.
  
  Command fields:
    :player-uuid  UUID-string
    :cp           float
    :overload     float
    :creative?    bool (default false)"
  [player-state {:keys [player-uuid cp overload creative?]
                 :or {cp 0.0 overload 0.0 creative? false}}]
  (let [ability-data (:ability-data player-state)
        resource-data (:resource-data player-state)
        level (:level ability-data 1)
        
        {:keys [data success? events-needed]}
        (resource/perform-resource resource-data
                                   (double overload)
                                   (double cp)
                                   creative?
                                   level
                                   (cfg/maxcp-incr-rate)
                                   (cfg/maxo-incr-rate))]
    
    (if-not success?
      {:state player-state :events [] :effects [] :success? false}
      
      ;; Must go through make-overload-event: server-hooks answers this with
      ;; (abort-player-contexts! uuid) — upstream's __onOverload ->
      ;; disposePlayer — and destructures :uuid, like every other event
      ;; constructor here produces. Emitting :player-uuid instead made the
      ;; subscriber read nil and skip the abort entirely, so an overloaded
      ;; player kept running the very contexts the overload should have killed.
      (let [events (cond-> []
                     (some #{:overload-cap-hit} events-needed)
                     (conj (evt/make-overload-event player-uuid)))]
        (assoc (ok (assoc player-state :resource-data data) events [])
               :success? true)))))

;; ============================================================================
;; Sub-Reducer: set-cooldown
;; ============================================================================

(defn- cmd-set-cooldown
  "Set a cooldown after a skill use.
  
  Command fields:
    :player-uuid  UUID-string
    :ctrl-id      keyword
    :ticks        int
    :sub-id       keyword (default :main)"
  [player-state {:keys [ctrl-id ticks sub-id] :or {sub-id :main}}]
  (let [cd-data (:cooldown-data player-state)
        {:keys [data]} (cooldown/set-cooldown cd-data ctrl-id ticks sub-id)]
    (ok (assoc player-state :cooldown-data data))))

;; ============================================================================
;; Sub-Reducer: set-activated
;; ============================================================================

(defn- cmd-set-activated
  "Toggle activation state.
  
  Command fields:
    :player-uuid  UUID-string
    :activated    bool"
  [player-state {:keys [player-uuid activated]}]
  (let [res-data (:resource-data player-state)
        {:keys [data events-needed]} (resource/set-activated res-data activated)
        events (mapv (fn [e]
                       (case e
                         :activate (evt/make-activate-event player-uuid)
                         :deactivate (evt/make-deactivate-event player-uuid)))
                     events-needed)]
    (ok (assoc player-state :resource-data data) events [])))

;; ============================================================================
;; Sub-Reducer: manage-preset
;; ============================================================================

(defn- cmd-set-preset-slot
  "Set a skill in a preset slot.
  
  Command fields:
    :preset-idx   int (0-3)
    :key-idx      int (0-3)
    :controllable [cat-id ctrl-id] or nil"
  [player-state {:keys [player-uuid preset-idx key-idx controllable]}]
  (let [
      preset-data (:preset-data player-state)
        new-preset-data (pdata/set-slot preset-data preset-idx key-idx controllable)]
    (ok (assoc player-state :preset-data new-preset-data)
      [(evt/make-preset-update-event player-uuid preset-idx key-idx controllable)]
        [{:effect/type :persist-state :domain :preset-data}])))

(defn- cmd-switch-preset
  "Switch to a different preset.
  
  Command fields:
    :player-uuid  UUID-string
    :preset-idx   int (0-3)"
  [player-state {:keys [player-uuid preset-idx]}]
    (let [preset-data (:preset-data player-state)
      old-preset (:active-preset preset-data 0)
        new-preset-data (pdata/set-active-preset preset-data preset-idx)
      events [(evt/make-preset-switch-event player-uuid old-preset preset-idx)]
        effects [{:effect/type :network-send
                  :channel :ability/preset-switched
                  :payload {:player-uuid player-uuid :preset-idx preset-idx}}]]
    (ok (assoc player-state :preset-data new-preset-data) events effects)))

;; ============================================================================
;; Sub-Reducer: server-tick
;; ============================================================================

(defn- assoc-if-changed
  "assoc k v only when v differs from the current value at k — idle-tick
  reducer branches then return the same top-level map object, which lets
  callers short-circuit on identical? instead of paying for a full not=."
  [m k v]
  (if (= (get m k) v) m (assoc m k v)))

;; ============================================================================
;; Sub-Reducer: timed development on player-state (:develop-data)
;; ============================================================================
;; The portable developer's session host (upstream PortableDevData +
;; DevelopData): same state machine as the block developer's tile session,
;; with per-tick energy pulled from the held item by the shell (state-tick).

(defn- cmd-develop-start
  "Start a timed development session in player-state's :develop-data.

  Command fields:
    :action          :learn-skill | :level-up | :awaken
    :skill-id        keyword (learn-skill)
    :target-category keyword (awaken — decided by the shell, which consumes
                     an induction factor or picks a random category)
    :developer-type  keyword (default :portable)"
  [player-state {:keys [action skill-id target-category developer-type]}]
  (let [dev-type (or (some-> developer-type keyword) :portable)
        ad (:ability-data player-state)
        dd (or (:develop-data player-state) (ddata/new-develop-data))]
    (if (ddata/developing? dd)
      (rejected player-state :already-developing)
      (case (keyword action)
        :learn-skill
        (let [spec (skill-registry/get-skill (keyword skill-id))]
          (cond
            (nil? spec) (rejected player-state :unknown-skill)
            (not (:pass? (learning/check-all-conditions spec ad (int (:level ad 1)) dev-type)))
            (rejected player-state :conditions-not-met)
            :else
            (let [{:keys [develop-data error]}
                  (develop-rules/start-skill-learning dd dev-type (:id spec))]
              (if error
                (rejected player-state error)
                (ok (assoc player-state :develop-data develop-data))))))

        :level-up
        (if-not (:category-id ad)
          (rejected player-state :no-category)   ;; shell issues :awaken instead
          (let [{:keys [develop-data error]}
                (develop-rules/start-level-up dd dev-type (int (:level ad 1)))]
            (if error
              (rejected player-state error)
              (ok (assoc player-state :develop-data develop-data)))))

        :awaken
        (if-not target-category
          (rejected player-state :no-target-category)
          (ok (assoc player-state :develop-data
                     (ddata/start-develop dd dev-type :awaken
                                          {:target-category (keyword target-category)
                                           :target-level 1}
                                          (ddata/level-up-stims 0)))))

        (rejected player-state :unknown-develop-action)))))

(defn- cmd-develop-fail
  "Mark the current player-state development as failed — energy shortfall or
   device gone (upstream DevelopData.resetProgress(true))."
  [player-state _cmd]
  (if (some-> (:develop-data player-state) ddata/developing?)
    (ok (update player-state :develop-data ddata/fail))
    (ok player-state)))

(defn- develop-completion-valid?
  "Re-validate a completed development against CURRENT ability data before
   applying it — upstream DevelopData.tick calls type.validate() on the last
   stim tick. Mirrors the block developer session's completion check."
  [player-state dd]
  (let [ad (:ability-data player-state)]
    (case (:action-type dd)
      :learn-skill
      (let [skill-id (:skill-id (:action-data dd))
            spec (when skill-id (skill-registry/get-skill skill-id))]
        (boolean (and spec
                      (:pass? (learning/check-all-conditions
                               spec ad (int (:level ad 1)) (:developer-type dd))))))
      :level-up
      (let [cat-id (:category-id ad)
            skills (when cat-id
                     (skill-query/get-controllable-skills-at-level cat-id (int (:level ad 1))))
            cat-rate (when cat-id (category/get-prog-incr-rate cat-id))]
        (boolean (or (not cat-id)
                     (learning/can-level-up? ad skills cat-rate
                                             (cfg/prog-incr-rate) (cfg/max-level)))))
      ;; :awaken validated at start; anything else applies as-is.
      true)))

(defn- cmd-server-tick
  "Advance one server tick: resource/cooldown recovery, develop tick, completion.

  Command fields:
    :cp-speed       float (optional override)
    :ol-speed       float (optional override)
    :uuid           UUID-string (optional, for develop completion events)
    :player-uuid    UUID-string (optional alias)"
  [player-state {:keys [cp-speed ol-speed uuid player-uuid]}]
  (let [uuid-str (or uuid player-uuid (:player-uuid player-state))
        res-data (:resource-data player-state)
        cd-data (:cooldown-data player-state)
        eff-cp-speed (or cp-speed (cfg/cp-recover-speed))
        eff-ol-speed (or ol-speed (cfg/overload-recover-speed))
        res-result (resource/server-tick-recovery res-data eff-cp-speed eff-ol-speed)
        {:keys [data]} (cooldown/server-tick cd-data)
        ticked-state (-> player-state
                        (assoc-if-changed :resource-data (:data res-result))
                        (assoc-if-changed :cooldown-data data))
        develop-data (:develop-data ticked-state)
        dev-result (when (and develop-data (ddata/developing? develop-data))
                     (develop-rules/tick-develop develop-data))
        completed? (boolean (and dev-result (:completed? dev-result)))
        valid? (when completed?
                 (develop-completion-valid? ticked-state (:develop-data dev-result)))
        new-dev (cond
                  (and completed? (not valid?)) (ddata/fail (:develop-data dev-result))
                  dev-result (:develop-data dev-result)
                  :else develop-data)
        completion (when (and completed? valid? uuid-str)
                     (develop-rules/apply-completion
                      new-dev
                      (:ability-data ticked-state)
                      (:resource-data ticked-state)
                      uuid-str))
        ;; Keep :develop-data in its terminal :done/:failed state rather than
        ;; completion's reset — upstream DevelopData stays DONE until the next
        ;; startDeveloping, and the client reads that terminal state to show
        ;; success/failure. cmd-develop-start overwrites it on the next session.
        final-state (cond-> ticked-state
                      (some? new-dev) (assoc :develop-data new-dev)
                      completion (assoc :ability-data (:ability-data completion)
                                        :resource-data (:resource-data completion)))
        completion-events (or (:events completion) [])]
    (ok final-state completion-events)))

(defn server-tick-noop?
  "True iff cmd-server-tick would return player-state unchanged with no
  events for it — i.e. the player is fully idle (CP/overload at rest, no
  cooldowns, not developing). Used by the imperative shell to skip the
  :server-tick command entirely on idle ticks.

  Must mirror cmd-server-tick's three sub-steps branch-for-branch; guarded
  by an equivalence test so drift is caught immediately."
  [player-state]
  (and (rdata/recovery-idle? (:resource-data player-state))
       (empty? (:cooldown-data player-state))
       (let [dd (:develop-data player-state)]
         (or (nil? dd) (not (ddata/developing? dd))))))

;; ============================================================================
;; Sub-Reducer: context lifecycle
;; ============================================================================

(defn- cmd-register-context
  "Register a new context in the player's context-registry.
  
  Command fields:
    :ctx-id    keyword/uuid
    :skill-id  keyword
    :status    keyword (default :constructed)"
  [player-state {:keys [ctx-id skill-id status] :or {status :constructed}}]
    (let [ctx {:id ctx-id :skill-id skill-id :status status}
      new-state (assoc-in player-state [:context-registry ctx-id] ctx)
      uuid (:player-uuid player-state)]
    (ok new-state
      [(evt/make-context-registered-event uuid ctx-id skill-id status)]
      [])))

(defn- cmd-update-context-status
  "Update status of a context in the context-registry.
  
  Command fields:
    :ctx-id  keyword/uuid
    :status  keyword :constructed|:alive|:terminated
    :reason  keyword (termination reason)"
  [player-state {:keys [ctx-id status reason]}]
  (if-not (get-in player-state [:context-registry ctx-id])
    (rejected player-state :context-not-found)
    (let [old-status (get-in player-state [:context-registry ctx-id :status])
          new-state (update-in player-state [:context-registry ctx-id]
                               merge {:status status :terminated-reason reason})
          uuid (:player-uuid player-state)]
      (ok new-state
          [(evt/make-context-status-changed-event uuid ctx-id old-status status reason)]
          []))))

(defn- cmd-touch-context-keepalive
  "Update keepalive timestamp of a context in the context-registry.

  Command fields:
    :ctx-id      keyword/uuid
    :timestamp-ms long (optional, default now)"
  [player-state {:keys [ctx-id timestamp-ms]}]
  (if-not (get-in player-state [:context-registry ctx-id])
    (rejected player-state :context-not-found)
    (let [ts (long (or timestamp-ms (System/currentTimeMillis)))
          new-state (assoc-in player-state [:context-registry ctx-id :last-keepalive-ms] ts)]
      (ok new-state))))

(defn- cmd-purge-terminated-contexts
  "Remove :terminated contexts from the context-registry."
  [player-state _command]
  (let [registry (:context-registry player-state)
        live (into {}
                   (filter (fn [[_ ctx]]
                             (not= :terminated (:status ctx)))
                           registry))]
    (if (= (count registry) (count live))
      (ok (assoc player-state :context-registry live))
      (ok (assoc player-state :context-registry live)
          [(evt/make-context-purged-event (:player-uuid player-state)
                                          (- (count registry) (count live)))]
          []))))

;; ============================================================================
;; Sub-Reducer: resource recovery override
;; ============================================================================

(defn- cmd-restore-resource
  "Fully restore a player's resources (e.g. on death/respawn).
  
  No command fields required."
  [player-state _command]
  (let [res-data (:resource-data player-state)
        restored (rdata/recover-all res-data)]
    (ok (assoc player-state :resource-data restored))))

(defn- cmd-clear-all-cooldowns
  "Remove all cooldowns (e.g. on admin request)."
  [player-state _command]
  (ok (assoc player-state :cooldown-data (cdata/new-cooldown-data))))

;; ============================================================================
;; Sub-Reducer: admin/maintenance commands
;; ============================================================================

(defn- cmd-set-level
  "Set level directly (admin path) and fire level-change event when changed."
  [player-state {:keys [player-uuid level]}]
  (let [old-level (int (get-in player-state [:ability-data :level] 1))
        new-level (int level)]
    (if (= old-level new-level)
      (ok player-state)
      (ok (update player-state :ability-data adata/set-level new-level)
          [(evt/make-level-change-event player-uuid old-level new-level)]
          [{:effect/type :persist-state
            :player-uuid player-uuid
            :domain :ability-data}]))))

(defn- cmd-set-skill-exp
  "Set one skill exp directly (admin path).

  Upstream AbilityData.setSkillExp is guarded by `if (isSkillLearned(skill))`
  — exp on an unlearned skill is meaningless and would resurrect as soon as
  the skill was learned. Rejecting keeps :skill-exps consistent with
  :learned-skills, which unlearn-skill also maintains by dissoc'ing the exp."
  [player-state {:keys [player-uuid skill-id amount]}]
  (if-not (contains? (get-in player-state [:ability-data :learned-skills] #{}) skill-id)
    (rejected player-state :skill-not-learned)
    (let [new-state (update player-state :ability-data adata/set-skill-exp skill-id amount)]
      (ok new-state
          []
          [{:effect/type :persist-state
            :player-uuid player-uuid
            :domain :ability-data}]))))

(defn- cmd-maxout-level-progress
  "Fill the current level's progress bar so the player can level up now.

  Upstream AbilityData.maxOutLevelProgress is `expAddedThisLevel = 100`, a
  sentinel comfortably above any threshold (getLevelProgress caps the ratio
  at 1). The equivalent here is the actual threshold can-level-up? compares
  against, computed from the same inputs cmd-level-up uses.

  This is all upstream's maxout does: no levels granted, no skills learned,
  no exp handed out."
  [player-state {:keys [player-uuid]}]
  (let [ability-data (:ability-data player-state)
        cat-id (:category-id ability-data)
        skills (skill-query/get-controllable-skills-at-level cat-id (:level ability-data))
        threshold (learning/level-up-threshold ability-data
                                               skills
                                               (category/get-prog-incr-rate cat-id)
                                               (cfg/prog-incr-rate))]
    (ok (update player-state :ability-data adata/set-level-progress threshold)
        []
        [{:effect/type :persist-state
          :player-uuid player-uuid
          :domain :ability-data}])))

(defn- cmd-unlearn-skill
  "Remove a skill from learned set and clear related preset slots."
  [player-state {:keys [player-uuid skill-id]}]
  (let [ability-before (:ability-data player-state)
        preset-before (:preset-data player-state)
        controllable (skill-query/controllable-key skill-id)
        new-ability (-> ability-before
                        (update :learned-skills disj skill-id)
                        (update :skill-exps dissoc skill-id))
        new-preset (if controllable
                     (reduce-kv (fn [data [preset-idx key-idx] slot]
                                  (if (= slot controllable)
                                    (pdata/set-slot data preset-idx key-idx nil)
                                    data))
                                preset-before
                                (:slots preset-before))
                     preset-before)
        changed? (not= [ability-before preset-before] [new-ability new-preset])]
    (if-not changed?
      (ok player-state)
      (ok (-> player-state
              (assoc :ability-data new-ability)
              (assoc :preset-data new-preset))
          []
          [{:effect/type :persist-state
            :player-uuid player-uuid
            :domain :ability-data}
           {:effect/type :persist-state
            :player-uuid player-uuid
            :domain :preset-data}]))))

(defn- cmd-recover-all
  "Fully recover CP/overload values."
  [player-state {:keys [player-uuid]}]
  (ok (update player-state :resource-data rdata/recover-all)
      []
      [{:effect/type :persist-state
        :player-uuid player-uuid
        :domain :resource-data}]))

(defn- cmd-reset-abilities
  "Reset ability/runtime-facing ability domains to defaults."
  [player-state {:keys [player-uuid]}]
  (let [old-category (get-in player-state [:ability-data :category-id])
        next-state (assoc player-state
                          :ability-data (adata/new-ability-data)
                          :resource-data (rdata/new-resource-data)
                          :cooldown-data (cdata/new-cooldown-data)
                          :preset-data (pdata/new-preset-data)
                          :develop-data (ddata/new-develop-data))
        events (if old-category
                 [(evt/make-category-change-event player-uuid old-category nil)]
                 [])]
    (ok next-state
        events
        [{:effect/type :persist-state
          :player-uuid player-uuid
          :domain :ability-data}
         {:effect/type :persist-state
          :player-uuid player-uuid
          :domain :resource-data}
         {:effect/type :persist-state
          :player-uuid player-uuid
          :domain :cooldown-data}
         {:effect/type :persist-state
          :player-uuid player-uuid
          :domain :preset-data}
         {:effect/type :persist-state
          :player-uuid player-uuid
          :domain :develop-data}])) )

(defn- cmd-change-category
  "Set category and clear preset slots.

  This command intentionally uses the default category transition behaviour.
  Special transitions that need custom level handling should remain in explicit
  application-level flows until they are modeled as first-class commands."
  [player-state {:keys [player-uuid new-category]}]
  (let [old-category (get-in player-state [:ability-data :category-id])]
    (if (= old-category new-category)
      (ok player-state)
      (ok (-> player-state
              (update :ability-data adata/set-category new-category)
              (update :preset-data pdata/clear-slots))
          [(evt/make-category-change-event player-uuid old-category new-category)]
          [{:effect/type :persist-state
            :player-uuid player-uuid
            :domain :ability-data}
           {:effect/type :persist-state
            :player-uuid player-uuid
            :domain :preset-data}]))))

(defn- cmd-change-category-with-level
  "Set category, force a target level, and clear preset slots.

  This models explicit category transforms previously handled by callback-style
  state mutation in item workflows."
  [player-state {:keys [player-uuid new-category new-level]}]
  (let [old-category (get-in player-state [:ability-data :category-id])
        target-level (int new-level)]
    (if (= old-category new-category)
      (ok player-state)
      (ok (-> player-state
              (update :ability-data
                      (fn [ability-data]
                        (-> ability-data
                            (adata/set-category new-category)
                            (adata/set-level target-level))))
              (update :preset-data pdata/clear-slots))
          [(evt/make-category-change-event player-uuid old-category new-category)]
          [{:effect/type :persist-state
            :player-uuid player-uuid
            :domain :ability-data}
           {:effect/type :persist-state
            :player-uuid player-uuid
            :domain :preset-data}]))))

  (defn- cmd-enforce-overload-floor
    "Ensure resource overload is not below floor.

    Command fields:
      :floor-value number"
    [player-state {:keys [floor-value]}]
    (let [floor (double (or floor-value 0.0))
          current (double (or (get-in player-state [:resource-data :cur-overload]) 0.0))]
      (if (< current floor)
        (ok (-> player-state
                (assoc-in [:resource-data :cur-overload] floor)
                (assoc-in [:resource-data :overload-fine] true)))
        (ok player-state))))

  (def ^:private radiation-marks-path [:runtime :meltdowner :radiation-marks])

  (defn- radiation-index-effect
    "Effect telling the radiation-mark-index shell to fully replace one
    player's outgoing marks (see service/radiation-mark-index.clj)."
    [player-uuid marks]
    {:effect/type :radiation-index-sync
     :player-uuid (str player-uuid)
     :marks marks})

  (defn- cmd-hydrate-player-state
    "Replace player-state slices from persistence or lifecycle hydration.

    Only keys present on the command are applied. Intended for adapters and
    server hooks — not arbitrary gameplay callers."
    [player-state cmd]
    (if (and (contains? cmd :context-registry)
             (not (map? (:context-registry cmd))))
      (rejected player-state :invalid-context-registry-sync)
      (let [hydrated (cond-> player-state
                       (contains? cmd :ability-data) (assoc :ability-data (:ability-data cmd))
                       (contains? cmd :resource-data) (assoc :resource-data (:resource-data cmd))
                       (contains? cmd :cooldown-data) (assoc :cooldown-data (:cooldown-data cmd))
                       (contains? cmd :preset-data) (assoc :preset-data (:preset-data cmd))
                       (contains? cmd :develop-data) (assoc :develop-data (:develop-data cmd))
                       (contains? cmd :sync-revision) (assoc :sync-revision (:sync-revision cmd))
                       (contains? cmd :context-registry) (assoc :context-registry (:context-registry cmd))
                       (contains? cmd :runtime-data) (assoc :runtime (:runtime-data cmd))
                       (contains? cmd :dirty?) (assoc :dirty-domains (if (:dirty? cmd) sync-domains #{})))]
        ;; :runtime-data can replace [:runtime] wholesale (including
        ;; radiation-marks) — resync the derived index so it doesn't drift.
        (ok hydrated
            []
            (if (contains? cmd :runtime-data)
              [(radiation-index-effect (:player-uuid cmd) (get-in hydrated radiation-marks-path))]
              [])))))

  (defn- cmd-set-dirty-flag
    [player-state {:keys [dirty?] :or {dirty? false}}]
    (ok (assoc player-state :dirty-domains (if dirty? sync-domains #{}))))

  (defn- cmd-set-cheats-enabled
    [player-state {:keys [enabled?]}]
    (ok (assoc-in player-state [:cheats-data :enabled?] (boolean enabled?))))

  (defn- cmd-context-assoc-skill-state
    [player-state {:keys [ctx-id k v]}]
    (if-not (get-in player-state [:context-registry ctx-id])
      (rejected player-state :context-not-found)
      (let [key-path (if (vector? k) k [k])]
        (ok (assoc-in player-state (into [:context-registry ctx-id :skill-state] key-path) v)))))

  (defn- cmd-context-increment-skill-state
    [player-state {:keys [ctx-id k max]}]
    (if-not (get-in player-state [:context-registry ctx-id])
      (rejected player-state :context-not-found)
    (let [state-key (or k :charge-ticks)
      key-path (if (vector? state-key) state-key [state-key])
            max-v (long (or max Long/MAX_VALUE))
      current (long (or (get-in player-state (into [:context-registry ctx-id :skill-state] key-path)) 0))
            next-v (min max-v (inc current))]
      (ok (assoc-in player-state (into [:context-registry ctx-id :skill-state] key-path) next-v)))))

  (defn- cmd-context-set-toggle-state
    [player-state {:keys [ctx-id skill-id toggle-state]}]
    (if-not (get-in player-state [:context-registry ctx-id])
      (rejected player-state :context-not-found)
      (ok (assoc-in player-state [:context-registry ctx-id :skill-state :toggle skill-id]
                    toggle-state))))

  (defn- cmd-context-set-toggle-active
    [player-state {:keys [ctx-id skill-id active]}]
    (if-not (get-in player-state [:context-registry ctx-id])
      (rejected player-state :context-not-found)
      (ok (assoc-in player-state [:context-registry ctx-id :skill-state :toggle skill-id :active]
                    (boolean active)))))

  (defn- cmd-context-remove-toggle-state
    [player-state {:keys [ctx-id skill-id]}]
    (if-not (get-in player-state [:context-registry ctx-id])
      (rejected player-state :context-not-found)
      (ok (update-in player-state [:context-registry ctx-id :skill-state :toggle]
                     (fn [toggle-map]
                       (if (map? toggle-map)
                         (dissoc toggle-map skill-id)
                         toggle-map))))))

  (defn- cmd-context-clear-skill-state
    [player-state {:keys [ctx-id]}]
    (if-not (get-in player-state [:context-registry ctx-id])
      (rejected player-state :context-not-found)
      (ok (update-in player-state [:context-registry ctx-id] dissoc :skill-state))))

  (defn- cmd-context-set-input-state
    [player-state {:keys [ctx-id input-state]}]
    (if-not (get-in player-state [:context-registry ctx-id])
      (rejected player-state :context-not-found)
      (ok (assoc-in player-state [:context-registry ctx-id :input-state] input-state))))

  (defn- cmd-set-railgun-coin-judged-uuid
    [player-state {:keys [coin-uuid]}]
    (if (some? coin-uuid)
      (ok (assoc-in player-state [:runtime :railgun :coin-judged-uuid] coin-uuid))
      (rejected player-state :invalid-coin-uuid)))

  (defn- cmd-clear-railgun-coin-judged-uuid
    [player-state _]
    (let [railgun (get-in player-state [:runtime :railgun])]
      (if (map? railgun)
        (ok (assoc-in player-state [:runtime :railgun] (dissoc railgun :coin-judged-uuid)))
        (ok player-state))))

;; ============================================================================
;; Sub-Reducer: player runtime (delayed projectiles, radiation marks, vecmanip)
;; ============================================================================

(defn- cmd-mark-radiation-target
  [player-state {:keys [player-uuid target-id mark]}]
  (if (and target-id mark)
    (let [next-state (assoc-in player-state (conj radiation-marks-path (str target-id)) mark)]
      (ok next-state
          []
          [(radiation-index-effect player-uuid (get-in next-state radiation-marks-path))]))
    (rejected player-state :invalid-radiation-mark)))

(defn- cmd-clear-radiation-marks
  [player-state {:keys [player-uuid target-id source-player-id clear-all? clear-expired?]}]
  (let [marks (or (get-in player-state radiation-marks-path) {})
        next-marks
        (cond
          clear-all?
          {}

          clear-expired?
          (into {}
                (remove (fn [[_k {:keys [ticks-left]}]]
                          (not (pos? (long (or ticks-left 0))))))
                marks)

          target-id
          (dissoc marks (str target-id))

          source-player-id
          (let [source-key (str source-player-id)]
            (into {}
                  (remove (fn [[_k {:keys [source-player-id]}]]
                            (= source-key (str source-player-id))))
                  marks))

          :else
          marks)]
    (if (= marks next-marks)
      (ok player-state)
      (ok (assoc-in player-state radiation-marks-path next-marks)
          []
          [(radiation-index-effect player-uuid next-marks)]))))

(defn- cmd-tick-radiation-marks
  [player-state {:keys [player-uuid]}]
  (let [marks (or (get-in player-state radiation-marks-path) {})
        next-marks
        (reduce-kv
          (fn [acc target-id mark]
            (let [ticks-left (dec (long (or (:ticks-left mark) 0)))]
              (if (pos? ticks-left)
                (assoc acc target-id (assoc mark :ticks-left ticks-left))
                acc)))
          {}
          marks)]
    ;; Unconditional emit (even when next-marks == marks or empty): this is
    ;; the index's per-tick self-healing channel — any drift from a swallowed
    ;; effect exception is corrected on the very next tick.
    (ok (assoc-in player-state radiation-marks-path next-marks)
        []
        [(radiation-index-effect player-uuid next-marks)])))

(defn- projectile-claims-path
  []
  [:runtime :vecmanip :projectile-claims])

(defn- cmd-claim-projectile
  [player-state {:keys [player-uuid skill-id projectile-id tick]}]
  (if (and player-uuid projectile-id skill-id)
    (let [now-tick (long tick)
          lock-key [(str player-uuid) (str projectile-id)]
          claims (or (get-in player-state (projectile-claims-path))
                     {:tick -1 :owners {}})
          current (if (= (:tick claims) now-tick)
                    claims
                    {:tick now-tick :owners {}})
          owner (get-in current [:owners lock-key])
          next-claims
          (cond
            (nil? owner)
            (assoc-in current [:owners lock-key] skill-id)

            (= owner skill-id)
            current

            :else
            current)
          granted? (= (get-in next-claims [:owners lock-key]) skill-id)
          next-state (assoc-in player-state (projectile-claims-path) next-claims)]
      (assoc (ok next-state) :granted? granted?))
    (assoc (ok player-state) :granted? false)))

(defn- cmd-replace-projectile-claims
  [player-state {:keys [claims]}]
  (ok (assoc-in player-state (projectile-claims-path) (or claims {:tick -1 :owners {}}))))

(defn- cmd-clear-player-projectile-claims
  [player-state {:keys [player-uuid]}]
  (let [claims (or (get-in player-state (projectile-claims-path))
                   {:tick -1 :owners {}})
        next-owners
        (into {}
              (remove (fn [[[owner-player-id _] _]]
                        (= (str owner-player-id) (str player-uuid))))
              (or (:owners claims) {}))]
    (ok (assoc-in player-state (projectile-claims-path)
                (assoc claims :owners next-owners)))))

(defn- reflection-path
  []
  [:runtime :vecmanip :reflection])

(defn- reflecting-pairs-set
  [player-state]
  (set (or (get-in player-state (into (reflection-path) [:reflecting-pairs])) [])))

(defn- cmd-enter-vec-reflection
  [player-state {:keys [owner-key]}]
  (let [pairs (reflecting-pairs-set player-state)]
    (if (contains? pairs owner-key)
      (assoc (ok player-state) :granted? false)
      (assoc (ok (update-in player-state (into (reflection-path) [:reflecting-pairs])
                    (fnil conj []) owner-key))
             :granted? true))))

(defn- cmd-leave-vec-reflection
  [player-state {:keys [owner-key]}]
  (let [pairs (reflecting-pairs-set player-state)
        depths (or (get-in player-state (into (reflection-path) [:reflection-depths])) {})
        next-depths
        (if-let [v (get depths owner-key)]
          (let [nv (dec (long v))]
            (if (pos? nv)
              (assoc depths owner-key nv)
              (dissoc depths owner-key)))
          depths)
        next-pairs (disj pairs owner-key)]
    (ok (-> player-state
            (assoc-in (into (reflection-path) [:reflecting-pairs]) (vec next-pairs))
            (assoc-in (into (reflection-path) [:reflection-depths]) next-depths)))))

(defn- cmd-set-vec-reflection-depth
  [player-state {:keys [owner-key depth]}]
  (ok (assoc-in player-state (into (reflection-path) [:reflection-depths owner-key])
              (long depth))))

(defn- cmd-reset-vec-reflection-runtime
  [player-state _]
  (ok (assoc-in player-state (reflection-path)
              {:reflecting-pairs [] :reflection-depths {}})))

;; ============================================================================
;; Public Dispatcher
;; ============================================================================

(defn apply-command
  "Apply a command to player-state.
  
  Returns: {:state updated-state :events [event-maps] :effects [effect-plans]}
  
  Optional extra keys:
    :success?        – bool, present on resource-check commands
    :rejected-reason – keyword, present on rejected commands"
  [player-state command]
  (case (:command command)
    :learn-skill (cmd-learn-skill player-state command)
    :level-up (cmd-level-up player-state command)
    :add-skill-exp (cmd-add-skill-exp player-state command)
    :consume-resource (cmd-consume-resource player-state command)
    :set-cooldown (cmd-set-cooldown player-state command)
    :set-activated (cmd-set-activated player-state command)
    :set-preset-slot (cmd-set-preset-slot player-state command)
    :switch-preset (cmd-switch-preset player-state command)
    :server-tick (cmd-server-tick player-state command)
    :develop-start (cmd-develop-start player-state command)
    :develop-fail (cmd-develop-fail player-state command)
    :register-context (cmd-register-context player-state command)
    :update-context-status (cmd-update-context-status player-state command)
    :touch-context-keepalive (cmd-touch-context-keepalive player-state command)
    :purge-terminated-contexts (cmd-purge-terminated-contexts player-state command)
    :restore-resource (cmd-restore-resource player-state command)
    :clear-all-cooldowns (cmd-clear-all-cooldowns player-state command)
    :set-level (cmd-set-level player-state command)
    :set-skill-exp (cmd-set-skill-exp player-state command)
    :maxout-level-progress (cmd-maxout-level-progress player-state command)
    :unlearn-skill (cmd-unlearn-skill player-state command)
    :recover-all (cmd-recover-all player-state command)
    :reset-abilities (cmd-reset-abilities player-state command)
    :change-category (cmd-change-category player-state command)
    :change-category-with-level (cmd-change-category-with-level player-state command)
    :enforce-overload-floor (cmd-enforce-overload-floor player-state command)
    :hydrate-player-state (cmd-hydrate-player-state player-state command)
    :set-dirty-flag (cmd-set-dirty-flag player-state command)
    :set-cheats-enabled (cmd-set-cheats-enabled player-state command)
    :context-assoc-skill-state (cmd-context-assoc-skill-state player-state command)
    :context-increment-skill-state (cmd-context-increment-skill-state player-state command)
    :context-set-toggle-state (cmd-context-set-toggle-state player-state command)
    :context-set-toggle-active (cmd-context-set-toggle-active player-state command)
    :context-remove-toggle-state (cmd-context-remove-toggle-state player-state command)
    :context-clear-skill-state (cmd-context-clear-skill-state player-state command)
    :context-set-input-state (cmd-context-set-input-state player-state command)
    :set-railgun-coin-judged-uuid (cmd-set-railgun-coin-judged-uuid player-state command)
    :clear-railgun-coin-judged-uuid (cmd-clear-railgun-coin-judged-uuid player-state command)
    :mark-radiation-target (cmd-mark-radiation-target player-state command)
    :clear-radiation-marks (cmd-clear-radiation-marks player-state command)
    :tick-radiation-marks (cmd-tick-radiation-marks player-state command)
    :claim-projectile (cmd-claim-projectile player-state command)
    :replace-projectile-claims (cmd-replace-projectile-claims player-state command)
    :clear-player-projectile-claims (cmd-clear-player-projectile-claims player-state command)
    :enter-vec-reflection (cmd-enter-vec-reflection player-state command)
    :leave-vec-reflection (cmd-leave-vec-reflection player-state command)
    :set-vec-reflection-depth (cmd-set-vec-reflection-depth player-state command)
    :reset-vec-reflection-runtime (cmd-reset-vec-reflection-runtime player-state command)
    (do
      (log/warn "Unknown ability command" (:command command))
      (ok player-state))))

;; ============================================================================
;; Batch Dispatcher
;; ============================================================================

(defn apply-commands
  "Apply a sequence of commands to player-state, accumulating events and effects.
  
  Returns: {:state final-state :events [all-events] :effects [all-effects]}"
  [player-state commands]
  (reduce (fn [{:keys [state events effects]} command]
            (let [result (apply-command state command)]
              {:state   (:state result)
               :events  (into events (:events result))
               :effects (into effects (:effects result))}))
          {:state player-state :events [] :effects []}
          commands))
