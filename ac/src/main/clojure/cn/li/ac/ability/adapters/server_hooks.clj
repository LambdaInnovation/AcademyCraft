(ns cn.li.ac.ability.adapters.server-hooks
  "Server/runtime hook composition for AC ability platform bridge."
  (:require 
            [cn.li.ac.ability.service.state-tick :as ps-tick]
[cn.li.ac.ability.service.command-runtime :as command-rt]
[cn.li.ac.ability.service.runtime-store :as store]
[cn.li.ac.ability.config :as ability-config]
            [cn.li.ac.ability.item-actions :as item-actions]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.server.network :as network]
            [cn.li.ac.ability.server.damage.entity :as entity-damage-runtime]
            [cn.li.ac.ability.server.damage.handler :as damage-handler]
            [cn.li.ac.ability.server.damage.runtime :as damage-runtime]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.delayed-projectiles :as delayed-projectiles]
            [cn.li.ac.ability.service.reflection-damage :as reflection-damage]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.gui.registry-verify :as gui-registry-verify]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
            [cn.li.ac.ability.service.platform-hooks :as platform-hooks]            [cn.li.ac.block.developer.logic :as developer-logic]
            [cn.li.ac.block.developer.session :as dev-session]
            [cn.li.ac.item.developer-portable-energy :as portable-energy]
            [cn.li.ac.ability.service.player-runtime-commands :as player-runtime-cmd]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.util.log :as log]))

(def ^:private fn-reset-server-runtimes :ability/reset-server-runtimes!)
(def ^:private fn-register-network-handlers :ability/register-network-handlers!)
(def ^:private fn-try-pull-developer-energy :ability/try-pull-developer-energy!)
(def ^:private fn-held-portable-dev-energy :ability/held-portable-dev-energy)
(def ^:private fn-pull-portable-dev-energy :ability/pull-portable-dev-energy!)
(def ^:private fn-resolve-awaken-category :ability/resolve-awaken-category!)

(defn- unique-context-by-id
  [ctx-id]
  (let [matches (->> (ctx/snapshot-context-registry)
                     vals
                     (filter #(= ctx-id (:id %)))
                     vec)]
    (when (= 1 (count matches))
      (first matches))))

(defn- runtime-get-player-state
  [player-uuid]
  (store/get-player-state (runtime-hooks/require-player-state-session-id "Server hooks runtime state access") player-uuid))

(defn- runtime-sync-player-state!
  [player-uuid state]
  (let [state* (or state {})
        session-id (runtime-hooks/require-player-state-session-id "Server hooks runtime state access")
        hydrate-cmd (cond-> {:command :hydrate-player-state}
                      (contains? state* :ability-data) (assoc :ability-data (:ability-data state*))
                      (contains? state* :resource-data) (assoc :resource-data (:resource-data state*))
                      (contains? state* :cooldown-data) (assoc :cooldown-data (:cooldown-data state*))
                      (contains? state* :preset-data) (assoc :preset-data (:preset-data state*))
                      (contains? state* :develop-data) (assoc :develop-data (:develop-data state*))
                      (contains? state* :context-registry) (assoc :context-registry (:context-registry state*))
                      (contains? state* :runtime) (assoc :runtime-data (:runtime state*))
                      (contains? state* :dirty?) (assoc :dirty? (:dirty? state*)))]
    (if (> (count hydrate-cmd) 1)
      (command-rt/run-command-in-session!
       session-id
       player-uuid
       hydrate-cmd
       {:mark-dirty? (if (contains? state* :dirty?)
                       (boolean (:dirty? state*))
                       true)})
      (command-rt/run-command-in-session! session-id player-uuid {:command :set-dirty-flag :dirty? false}))))

(defn- runtime-get-or-create-player-state!
  [player-uuid]
  (or (runtime-get-player-state player-uuid)
      (do
        (runtime-sync-player-state! player-uuid (store/fresh-player-state))
        (or (runtime-get-player-state player-uuid)
            (store/fresh-player-state)))))

(defn- runtime-set-player-state!
  [player-uuid state]
  (runtime-sync-player-state! player-uuid state))

(defn- runtime-mark-clean!
  [player-uuid]
  (store/clear-dirty!
   (runtime-hooks/require-player-state-session-id "Server hooks runtime state access")
   player-uuid)
  nil)

(defn- build-sync-payload-impl
  "Build the client sync payload for player-uuid.

  full? true: every sync domain included (login/respawn/dimension-change
  request this; there is no periodic full-sync fallback — see sync-core).
  full? false: only domains present in :dirty-domains are included — absent
  keys tell sync-message-payload (network-core) to skip that domain's wire
  message entirely rather than send a stale/nil value over it.

  Domain values must be present whenever their dirty bit is set. Sync codecs
  reject nil/missing domains instead of encoding {}."
  [player-uuid full?]
  (when-let [state (runtime-get-player-state player-uuid)]
    (let [session-id (runtime-hooks/require-player-state-session-id "Server hooks runtime state access")
          mask (if full? store/all-sync-mask (store/dirty-mask session-id player-uuid))
          payload {:version 2
                   :opcode (if full? 1 2)
                   :uuid player-uuid
                   :revision (store/player-revision session-id player-uuid)
                   :dirty-mask mask}]
      (reduce (fn [acc [bit domain]]
                (if (zero? (bit-and mask bit))
                  acc
                  (let [value (get state domain)]
                    (when-not (some? value)
                      (throw (ex-info "Player-state sync domain missing for dirty bit"
                                      {:player-uuid player-uuid
                                       :domain domain
                                       :full? full?
                                       :dirty-mask mask})))
                    (assoc acc domain value))))
              payload
              [[store/ability-data-mask :ability-data]
               [store/resource-data-mask :resource-data]
               [store/cooldown-data-mask :cooldown-data]
               [store/preset-data-mask :preset-data]
               [store/develop-data-mask :develop-data]]))))

(defn- runtime-list-player-uuids
  []
  (store/list-players
   (runtime-hooks/require-player-state-session-id "Server hooks runtime state access")))

(defn- run-runtime-command!
  [player-uuid command]
  (command-rt/run-command-in-session!
   (runtime-hooks/require-player-state-session-id "Server hooks runtime state access")
   player-uuid
   command))

(defn- recalc-max-for-level-with-calc
  [res-data level uuid]
  (let [base (rdata/recalc-max-values res-data level)
        calc-extra {:uuid uuid}
        max-cp (evt/fire-calc-event! evt/CALC-MAX-CP (:max-cp base) calc-extra)
        max-ol (evt/fire-calc-event! evt/CALC-MAX-OVERLOAD (:max-overload base) calc-extra)]
    ;; recalc-max-values already refilled to its own maximum; redo it against
    ;; the post-CALC one so passive bonuses are part of the refill rather than
    ;; headroom left empty.
    (assoc base
           :max-cp max-cp
           :max-overload max-ol
           :cur-cp max-cp
           :cur-overload 0.0)))

(defn lifecycle-subscriptions-registered-snapshot
  []
  (install/framework-once-done? ::register-lifecycle-subscriptions))

(defn reset-lifecycle-subscriptions-registered-for-test!
  ([]
   (install/reset-framework-once-flag-for-test! ::register-lifecycle-subscriptions))
  ([registered?]
   (if registered?
     (install/framework-once! ::register-lifecycle-subscriptions (fn []))
     (install/reset-framework-once-flag-for-test! ::register-lifecycle-subscriptions))))

(defn register-platform-functions!
  "Register platform-facing callbacks used by reducer/effects/network shells."
  []
  (platform-hooks/register-platform-fn! fn-reset-server-runtimes
                                       player-runtime-cmd/reset-all-content-runtimes!)
  (platform-hooks/register-platform-fn! fn-register-network-handlers
                                       network/register-handlers!)
  (platform-hooks/register-platform-fn! fn-try-pull-developer-energy
                                       developer-logic/try-pull-energy!)
  (platform-hooks/register-platform-fn! fn-held-portable-dev-energy
                                       portable-energy/held-portable-energy)
  (platform-hooks/register-platform-fn! fn-pull-portable-dev-energy
                                       portable-energy/try-pull-portable-energy!)
  (platform-hooks/register-platform-fn! fn-resolve-awaken-category
                                       dev-session/resolve-awaken-category!)
  nil)

(defn register-lifecycle-subscriptions!
  []
  (install/framework-once! ::register-lifecycle-subscriptions
    (fn []
    (evt/subscribe-ability-event!
     evt/EVT-LEVEL-CHANGE
     (fn [{:keys [uuid new-level]}]
       (when (and uuid new-level)
         (when-let [state (runtime-get-player-state uuid)]
           (let [next-resource-data (recalc-max-for-level-with-calc
                                     (rdata/reset-add-max (:resource-data state))
                                     new-level
                                     uuid)]
             (run-runtime-command! uuid
                                   {:command :hydrate-player-state
                                    :resource-data next-resource-data}))))))
    (evt/subscribe-ability-event!
     evt/EVT-SKILL-LEARN
     (fn [{:keys [uuid]}]
       (when uuid
         (when-let [state (runtime-get-player-state uuid)]
           (let [level (get-in state [:ability-data :level] 1)]
             (run-runtime-command! uuid
                                   {:command :hydrate-player-state
                                    :resource-data (recalc-max-for-level-with-calc
                                                    (:resource-data state)
                                                    level
                                                    uuid)}))))))
    (evt/subscribe-ability-event!
     evt/EVT-CATEGORY-CHANGE
     (fn [{:keys [uuid]}]
       (when uuid
         (ctx-mgr/abort-player-contexts! uuid)
         (run-runtime-command! uuid {:command :clear-all-cooldowns})
         (run-runtime-command! uuid {:command :set-activated :activated false})
         (when-let [state (runtime-get-player-state uuid)]
           (let [level (get-in state [:ability-data :level] 1)]
             (run-runtime-command! uuid
                                   {:command :hydrate-player-state
                                    :resource-data (recalc-max-for-level-with-calc
                                                    (:resource-data state)
                                                    level
                                                    uuid)}))))))
    (evt/subscribe-ability-event!
     evt/EVT-OVERLOAD
     (fn [{:keys [uuid]}]
       (when uuid
         (ctx-mgr/abort-player-contexts! uuid))))
    (log/debug "Ability lifecycle event subscriptions registered"))))

(defn runtime-server-hooks
  []
  {:on-player-login!
   (fn [player-uuid]
     (runtime-get-or-create-player-state! player-uuid))

   :on-player-logout!
   (fn [player-uuid]
     (ctx-mgr/abort-player-contexts! player-uuid)
     (delayed-projectiles/clear-player-tasks! player-uuid)
     (reflection-damage/clear-player-tasks! player-uuid)
     (md-damage/clear-target-mark! player-uuid)
     (md-damage/clear-source-marks! player-uuid)
     (store/remove-player-state! (runtime-hooks/require-player-state-session-id "Server hooks runtime state access")
                                  player-uuid))

   :on-server-stop!
   (fn [session-id]
    (ctx/clear-store-session-contexts! session-id)
     (store/remove-session! session-id)
     (world-registry/clear-session-world-data! session-id)
     (when (platform-hooks/platform-fn-registered? fn-reset-server-runtimes)
       ((platform-hooks/get-platform-fn fn-reset-server-runtimes)))
     (delayed-projectiles/clear-all-tasks!)
     (reflection-damage/clear-all-tasks!)
     (md-damage/on-server-stop! session-id))

   :on-player-clone!
   (fn [_old-player-uuid _new-player-uuid]
     nil)

   :on-player-death!
   (fn [player-uuid]
     (delayed-projectiles/clear-player-tasks! player-uuid)
     (reflection-damage/clear-player-tasks! player-uuid)
     (md-damage/clear-target-mark! player-uuid)
     (md-damage/clear-source-marks! player-uuid)
     (ctx-mgr/abort-player-contexts! player-uuid))

   :on-player-dimension-change!
   (fn [player-uuid _from-dim _to-dim]
     (delayed-projectiles/clear-player-tasks! player-uuid)
     (reflection-damage/clear-player-tasks! player-uuid)
     (md-damage/clear-target-mark! player-uuid)
     (md-damage/clear-source-marks! player-uuid)
     (ctx-mgr/abort-player-contexts! player-uuid))

   :get-skills-for-category
   (fn [cat-id]
     (vec (skill-query/get-skills-for-category cat-id)))

   :on-server-tick-start!
   (fn [_tick-id]
     ;; Global work is driven once per server tick, before the player phase.
     (md-damage/tick-marks!)
     (ctx-mgr/tick-context-manager!))

   :on-player-tick!
   (fn [player-uuid player]
     (runtime-get-or-create-player-state! player-uuid)
     (ps-tick/server-tick-player-in-session! (runtime-hooks/require-player-state-session-id "Server hooks runtime state access")
                                             player-uuid
                                             nil)
     (ctx-mgr/tick-player-contexts! player-uuid player)
     (delayed-projectiles/tick-player! player-uuid))

   :on-server-tick-end!
   (fn [_tick-id]
     (reflection-damage/drain!))

   :list-player-uuids
   (fn []
     (runtime-list-player-uuids))

   :build-sync-payload
   (fn
     ([player-uuid] (build-sync-payload-impl player-uuid true))
     ([player-uuid full?] (build-sync-payload-impl player-uuid full?)))

   :player-state-dirty?
   (fn [player-uuid]
     (not (zero? (store/dirty-mask
                  (runtime-hooks/require-player-state-session-id "Server hooks runtime state access")
                  player-uuid))))

   :mark-player-clean!
   (fn [player-uuid]
     (runtime-mark-clean! player-uuid))

   :get-player-state
   (fn [player-uuid]
     (runtime-get-player-state player-uuid))

   :sync-player-state!
   (fn [player-uuid state]
     (runtime-sync-player-state! player-uuid state))

   :ensure-player-state!
   (fn [player-uuid]
     (runtime-get-or-create-player-state! player-uuid))

   :fresh-player-state
   (fn []
     (store/fresh-player-state))

   :runtime-activated?
   (fn [player-uuid]
     (boolean (some-> (runtime-get-player-state player-uuid)
                      :resource-data
                      rdata/is-activated?)))

   :register-network-handlers!
   (fn []
     (if (platform-hooks/platform-fn-registered? fn-register-network-handlers)
       (do
         ((platform-hooks/get-platform-fn fn-register-network-handlers))
         (gui-registry-verify/finalize-gui-network-registration!))
       (log/warn "Ability network register-handlers! not available")))

   :subscribe-achievement-trigger!
   (fn [handler]
     (evt/subscribe-ability-event! evt/EVT-ACHIEVEMENT-TRIGGER handler))

   :register-context-route-fns!
   (fn [fns-map]
     (ctx/register-route-fns! fns-map))

   :register-context-send-fns!
   (fn [fns-map]
     (ctx-mgr/register-send-fns! fns-map))

   :get-context-player-uuid
   (fn [ctx-id]
     (when-let [ctx-map (or (when-let [owner (ctx/context-owner)]
                               (ctx/get-context owner ctx-id))
                            (unique-context-by-id ctx-id))]
       (:player-uuid ctx-map)))

   :register-damage-handler!
   (fn [handler-id handler-fn priority]
     (damage-runtime/register-damage-handler! handler-id handler-fn priority))

   :unregister-damage-handler!
   (fn [handler-id]
     (damage-runtime/unregister-damage-handler! handler-id))

   :get-active-damage-handlers
   (fn []
     (damage-runtime/get-active-handlers))

   :process-damage-interception
   (fn [player-id attacker-id damage damage-source]
     (damage-runtime/process-damage! player-id attacker-id damage damage-source))

   :should-cancel-attack-interception?
   (fn [player-id attacker-id damage damage-source]
     (damage-handler/should-cancel-attack? player-id attacker-id damage damage-source))

   :run-attack-precheck-side-effects!
   (fn [player-id attacker-id damage damage-source]
     (damage-handler/run-attack-precheck-side-effects! player-id attacker-id damage damage-source))

   :resolve-item-use-action
   (fn [item-id]
     (item-actions/resolve-item-action item-id))

   :on-runtime-item-action!
   (fn [action player-uuid payload]
     (item-actions/on-item-action! action player-uuid payload))

   :build-item-use-plan
   (fn [_player-uuid item-id _activated? _side]
     (when-let [action (item-actions/resolve-item-action item-id)]
       (let [railgun-coin? (= action :railgun-coin-throw)
             entity-spawn (item-actions/get-item-entity-spawn item-id)
             domain-payload (if railgun-coin?
                              {:timestamp-ms (System/currentTimeMillis)}
                              {})
             ;; Upstream ItemCoin#onItemRightClick throws unconditionally — no
             ;; ability-activation requirement. A coin use always consumes,
             ;; spawns, dispatches the domain action (CoinThrowEvent analog)
             ;; and notifies the client QTE.
             server-actions (cond-> [{:kind :consume-item :count 1 :unless-instabuild? true}
                                     {:kind :domain-action :action action :payload domain-payload}]
                              entity-spawn (conj {:kind :spawn-scripted-effect
                                                  :entity-id (:entity-id entity-spawn)
                                                  :unique-per-owner?
                                                  (boolean (:unique-per-owner? entity-spawn))
                                                  :speed (double (or (:speed entity-spawn) 0.0))}))]
         {:server-actions server-actions
          :client-actions [{:kind :notify-local-effect
                            :event-key :ac/charge-coin-throw}]
          :consume? true})))

   :compute-aoe-damage
   (fn [origin-pos target-pos radius damage falloff?]
     (entity-damage-runtime/compute-aoe-damage origin-pos target-pos radius damage falloff?))

   :select-reflection-target
   (fn [current-entity-uuid current-pos candidates max-radius]
     (entity-damage-runtime/select-next-reflection-target current-entity-uuid current-pos candidates max-radius))

   :reflection-search-radius
   (fn []
     (ability-config/reflection-search-radius))

   :compute-reflected-damage
   (fn [current-damage]
     (entity-damage-runtime/compute-reflected-damage current-damage))})
