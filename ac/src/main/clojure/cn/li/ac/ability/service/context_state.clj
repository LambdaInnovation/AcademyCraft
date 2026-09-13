(ns cn.li.ac.ability.service.context-state
  "Server-side handlers for context key input lifecycle.

  This namespace executes skill runtime callbacks behind context messages:
  - MSG-SLOT-KEY-DOWN
  - MSG-SLOT-KEY-TICK
  - MSG-SLOT-KEY-UP
  - MSG-SLOT-KEY-ABORT

  It keeps input state transitions strict to avoid duplicated lifecycle calls."
  (:require [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.registry.skill :as skill]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.rules.cooldown-rules :as cd-rules]
            [cn.li.ac.ability.service.skill-callback :as skill-cb]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as owner]))

(def INPUT-IDLE :idle)
(def INPUT-ACTIVE :active)
(def INPUT-RELEASED :released)
(def INPUT-ABORTED :aborted)

(def ^:private supported-patterns
  #{:instant :hold-charge-release :toggle :hold-channel :release-cast :charge-window :passive})

(defn- pattern-owned-spec?
  [spec]
  (and (keyword? (:pattern spec))
       (contains? supported-patterns (:pattern spec))))

(defn- resolved-session-id
  [owner]
  (or (some-> owner owner/store-session-id)
      (runtime-hooks/require-player-state-session-id "context-state")))

(defn- runtime-player-state
  ([player-id]
   (runtime-player-state nil player-id))
  ([owner player-id]
   (store/get-player-state (resolved-session-id owner) player-id)))

(defn- with-context-owner-binding
  [owner f]
  (let [owner* (or owner (ctx/context-owner)
                   (runtime-hooks/current-player-state-owner))]
    (when-not owner*
      (throw (ex-info "Skill callback requires bound context owner"
                      {:context-owner (ctx/context-owner)})))
    (ctx/with-context-owner-fn owner* f)))

(defn- dispatch-skill-callback!
  ([ctx-map cb-key event-type payload]
   (dispatch-skill-callback! nil ctx-map cb-key event-type payload))
  ([owner ctx-map cb-key event-type payload]
   (with-context-owner-binding owner
     (fn []
       (when-let [spec (skill/get-skill (:skill-id ctx-map))]
         (let [pattern (:pattern spec)
               action-key (skill-cb/resolve-action-key pattern cb-key)
               callback-fn (when action-key (get-in spec [:actions action-key]))
               stage (skill-cb/cost-stage-for-cb-key cb-key)
               player-id (:player-uuid ctx-map)
               skill-id (:skill-id ctx-map)
               exp (skill-cb/skill-exp-for owner player-id skill-id)
               player-ref (:player payload)
               cost-ok? (if stage
                          (skill-effects/apply-cost! spec stage player-id skill-id exp player-ref)
                          true)
               invoke-args (skill-cb/extract-invoke-args
                            owner ctx-map payload
                            {:cost-ok? cost-ok? :cost-stage stage})]
           (when (fn? callback-fn)
             (try
               (apply skill-cb/invoke-action! callback-fn invoke-args)
               (when (and stage (not cost-ok?))
                 (skill-cb/invoke-cost-fail! spec (:id ctx-map) player-id skill-id exp stage player-ref))
               (catch Exception e
                 (log/warn "Skill callback failed" (:id spec) cb-key (ex-message e)))))))
       (evt/fire-ability-event!
        {:event/type event-type
         :event/side :server
         :ctx-id (:id ctx-map)
         :server-id (:server-id ctx-map)
         :player-id (:player-uuid ctx-map)
         :skill-id (:skill-id ctx-map)
         :payload payload})))))

(defn- set-input-state!
  ([ctx-id state]
   (set-input-state! nil ctx-id state))
  ([owner ctx-id state]
   (let [ctx-map (if owner (ctx/get-context owner ctx-id) (ctx/get-context ctx-id))
         player-id (:player-uuid ctx-map)
         session-id (resolved-session-id owner)
         _ (when (and player-id session-id)
             (command-rt/run-command-in-session!
              session-id
              player-id
              {:command :context-set-input-state
               :ctx-id (:id ctx-map)
               :input-state state}))
         result (if owner
                  (ctx/get-context owner ctx-id)
                  (ctx/get-context ctx-id))]
     result)))

(defn- apply-main-cooldown!
  ([ctx-map]
   (apply-main-cooldown! nil ctx-map))
  ([owner ctx-map]
   (let [uuid (:player-uuid ctx-map)
         skill-id (:skill-id ctx-map)
         spec (skill/get-skill skill-id)
         ctrl-id (or (:ctrl-id spec) skill-id)
         ;; Same resolution the pattern path uses (skill-effects/apply-cooldown!):
         ;; this used to read :cooldown-ticks raw, which ignored
         ;; :cooldown-policy/:ticks and would have thrown on a fn declaration.
         exp (some-> (runtime-player-state owner uuid) :ability-data
                     (adata/get-skill-exp skill-id))
         cooldown-ticks (int (cd-rules/resolve-ticks spec {:player-id uuid
                                                           :skill-id skill-id
                                                           :exp exp}))
         session-id (resolved-session-id owner)]
     (command-rt/run-command-in-session! session-id
                                         uuid
                                         {:command :set-cooldown
                                          :ctrl-id ctrl-id
                                          :ticks cooldown-ticks
                                          :sub-id :main}))))

(defn- in-main-cooldown?
  ([ctx-map]
   (in-main-cooldown? nil ctx-map))
  ([owner ctx-map]
   (let [uuid (:player-uuid ctx-map)
         state (runtime-player-state owner uuid)
         spec (skill/get-skill (:skill-id ctx-map))
         ctrl-id (or (:ctrl-id spec) (:skill-id ctx-map))]
     (and state
          (cd-rules/in-cooldown? (:cooldown-data state) ctrl-id :main)))))

(defn should-apply-main-cooldown?
  "Pure policy helper: only skip cooldown when skill cooldown mode is explicit :manual."
  [skill-spec]
  (not= :manual (get-in skill-spec [:cooldown :mode])))

(defn should-terminate-context-on-key-up?
  "Pure policy helper: by default key-up terminates context.
  Skills can opt out via {:input-policy {:terminate-on-key-up? false}}."
  [skill-spec]
  (not (false? (get-in skill-spec [:input-policy :terminate-on-key-up?]))))

(defn should-keep-active-on-key-up?
  "Pure policy helper: when true, key-up keeps context in ACTIVE state
  after on-key-up callback settlement.

  This is primarily used by skills that have a post-release triggering phase
  driven by server tick.

  Note: this policy is only effective when context is not terminated on key-up."
  [skill-spec]
  (true? (get-in skill-spec [:input-policy :keep-active-on-key-up?])))

(defn handle-key-down!
  ([ctx-id payload]
   (handle-key-down! ctx-id payload nil))
  ([ctx-id payload terminate-fn]
   (handle-key-down! nil ctx-id payload terminate-fn))
  ([owner ctx-id payload terminate-fn]
   (with-context-owner-binding owner
     (fn []
       (when-let [ctx-map (ctx/get-context ctx-id)]
         (when (and (= (:status ctx-map) ctx/STATUS-ALIVE)
                    (or (nil? (:input-state ctx-map))
                        (= (:input-state ctx-map) INPUT-IDLE)))
           (if (in-main-cooldown? owner ctx-map)
             (do
               (ctx/terminate-context! ctx-id terminate-fn)
               false)
             (let [new-ctx (set-input-state! owner ctx-id INPUT-ACTIVE)]
               (log/debug "Context key-down dispatched"
                         {:ctx-id ctx-id
                          :skill-id (:skill-id ctx-map)
                          :input-state (get-in new-ctx [:input-state])})
               (dispatch-skill-callback! owner new-ctx :on-key-down evt/EVT-CONTEXT-KEY-DOWN payload)
               true))))))))

(defn handle-key-tick!
  ([ctx-id payload]
   (handle-key-tick! ctx-id payload nil))
  ([ctx-id payload terminate-fn]
   (handle-key-tick! nil ctx-id payload terminate-fn))
  ([owner ctx-id payload terminate-fn]
   (with-context-owner-binding owner
     (fn []
       (when-let [ctx-map (ctx/get-context ctx-id)]
         (when (and (= (:status ctx-map) ctx/STATUS-ALIVE)
                    (= (:input-state ctx-map) INPUT-ACTIVE))
           (dispatch-skill-callback! owner ctx-map :on-key-tick evt/EVT-CONTEXT-KEY-TICK payload)
           true))))))

(defn handle-key-up!
  ([ctx-id payload]
   (handle-key-up! ctx-id payload nil))
  ([ctx-id payload terminate-fn]
   (handle-key-up! nil ctx-id payload terminate-fn))
  ([owner ctx-id payload terminate-fn]
   (with-context-owner-binding owner
     (fn []
       (when-let [ctx-map (ctx/get-context ctx-id)]
         (when (and (= (:status ctx-map) ctx/STATUS-ALIVE)
                    (= (:input-state ctx-map) INPUT-ACTIVE))
           (let [released-ctx (set-input-state! owner ctx-id INPUT-RELEASED)
                 spec (skill/get-skill (:skill-id released-ctx))]
             (log/debug "Context key-up dispatched"
                       {:ctx-id ctx-id
                        :skill-id (:skill-id released-ctx)
                        :input-state (get-in released-ctx [:input-state])})
             (dispatch-skill-callback! owner released-ctx :on-key-up evt/EVT-CONTEXT-KEY-UP payload)
             (when-not (boolean (and spec (pattern-owned-spec? spec)))
               (evt/fire-ability-event!
                (evt/make-skill-perform-event (:player-uuid released-ctx) (:skill-id released-ctx)))
               (when (should-apply-main-cooldown? spec)
                 (apply-main-cooldown! owner released-ctx)))
             (if (should-terminate-context-on-key-up? spec)
               (ctx/terminate-context! ctx-id terminate-fn)
               (if (should-keep-active-on-key-up? spec)
                 (set-input-state! owner ctx-id INPUT-ACTIVE)
                 (set-input-state! owner ctx-id INPUT-IDLE)))
             true)))))))

(defn handle-key-abort!
  ([ctx-id payload]
   (handle-key-abort! ctx-id payload nil))
  ([ctx-id payload terminate-fn]
   (handle-key-abort! nil ctx-id payload terminate-fn))
  ([owner ctx-id payload terminate-fn]
   (with-context-owner-binding owner
     (fn []
       (when-let [ctx-map (ctx/get-context ctx-id)]
         (when (not= (:status ctx-map) ctx/STATUS-TERMINATED)
           (let [aborted-ctx (set-input-state! owner ctx-id INPUT-ABORTED)]
             (do
               (dispatch-skill-callback! owner aborted-ctx :on-key-abort evt/EVT-CONTEXT-KEY-ABORT payload)
               (ctx/terminate-context! ctx-id terminate-fn)
               true))))))))
