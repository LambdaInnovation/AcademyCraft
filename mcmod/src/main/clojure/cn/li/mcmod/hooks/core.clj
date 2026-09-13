(ns cn.li.mcmod.hooks.core
  "Platform-neutral bridge for power runtime lifecycle hooks.

  forge/fabric adapters invoke these functions from platform events.
  content modules register concrete handlers during core initialization."
  (:require [cn.li.mcmod.content.registry :as content-registry]
            [cn.li.mcmod.schema.core :as schema]
            [cn.li.mcmod.runtime.owner :as runtime-owner]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(def ^:private noop
  (fn [& _] nil))

(def ^:private hooks-map-schema
  [:map-of keyword? fn?])

(def ^:private hooks-map-validator (schema/lazy-validator hooks-map-schema))
(defn- valid-hooks-map? [x]
  (schema/valid? (hooks-map-validator) x))

(defn- default-runtime-hooks-state []
  {:on-player-login! noop
   :on-player-logout! noop
   :on-server-stop! noop
   :on-player-clone! noop
   :on-player-death! noop
   :on-player-dimension-change! noop
   :on-server-tick-start! noop
   :on-player-tick! noop
   :on-server-tick-end! noop
   :init-damage-handlers! noop
   :list-player-uuids (fn [] [])
   :build-sync-payload (fn ([_] nil) ([_ _] nil))
   ;; Default true: unknown/unregistered player state is always considered
   ;; dirty (safe fallback — matches pre-dirty-tracking behavior of syncing
   ;; every player every tick).
   :player-state-dirty? (fn [_] true)
   :mark-player-clean! noop
   :get-player-state (fn [_] nil)
  :sync-player-state! (fn [_ _] nil)
  :ensure-player-state! (fn [_] nil)
   :fresh-player-state (fn [] nil)
   :runtime-activated? (fn [_] false)
   :register-network-handlers! noop
   :subscribe-achievement-trigger! (fn [_] nil)
   :register-context-route-fns! noop
   :register-context-send-fns! noop
   :get-context-player-uuid (fn [_] nil)
   :register-damage-handler! (fn [_ _ _] false)
   :unregister-damage-handler! (fn [_] false)
   :get-active-damage-handlers (fn [] [])
   :process-damage-interception (fn [_ _ damage _] damage)
   :should-cancel-attack-interception? (fn [_ _ _ _] false)
   :run-attack-precheck-side-effects! (fn [_ _ _ _] false)
   :resolve-item-use-action (fn [_] nil)
   :on-runtime-item-action! noop
   :build-item-use-plan (fn [_ _ _ _] nil)
   :compute-aoe-damage (fn [_ _ _ damage _] damage)
   :select-reflection-target (fn [_ _ _ _] nil)
   :compute-reflected-damage identity
   :reflection-search-radius (fn [] 0.0)
   :client-new-context (fn [_ _] nil)
   :client-register-context! noop
   :client-get-context (fn [_] nil)
   :client-terminate-context! noop
   :client-transition-to-alive! noop
   :client-send-context-local! noop
   :client-build-overlay-plan (fn [_ _ _ _] nil)
   :set-client-overlay-activated! (fn [_ _] nil)
   :client-poll-particle-effects (fn [_owner] [])
   :client-poll-sound-effects (fn [_owner] [])
   :client-tick-start! noop
   :client-font-init! noop
   :client-tick-keys! noop
   :client-active-contexts (fn [] {})
   :client-latest-sync (fn [_] nil)
   :client-register-push-handlers! noop
   :client-notify-visual-event! noop
   :client-show-combat-notice! noop
   :client-enqueue-level-effect! noop
   :client-build-level-effect-plan (fn [_ _ _ _] nil)
   :client-level-effects-active? (fn [] false)
   :client-tick-level-effects! noop
   :client-level-effect-fov-offset (fn [_player-uuid] 0.0)
   :client-slot-visual-state (fn [_ _] :idle)
   :client-visual-state (fn [_ _] nil)
   :client-on-slot-key-down! noop
   :client-on-slot-key-tick! noop
   :client-on-slot-key-up! noop
   :client-on-slot-key-abort! noop
   :client-on-movement-key-down! noop
   :client-on-movement-key-tick! noop
   :client-on-movement-key-up! noop
   :client-on-slot-wheel! noop
   :client-clear-owner-state! noop
   :client-abort-all! noop
   :client-tick! noop
   :client-tick-hand-effects! noop
   :client-drain-camera-pitch-deltas! (fn [_owner] [])
   :client-current-hand-transform (fn [] nil)
   :toggle-debug-overlay-state! noop})

;; ============================================================================
;; Runtime Container — stored in Framework [:registry :hooks]
;; ============================================================================

(def ^:private hooks-path [:registry :hooks :runtime-hooks])

;; These hooks are entered from client tick/input/render callbacks.  Publish
;; their actual IFns at registration time so the callback does not dereference
;; Framework or look up an operation in the runtime-hooks map.
(def ^:private client-tick-start-fn noop)
(def ^:private client-tick-keys-fn noop)
(def ^:private client-tick-fn noop)
(def ^:private client-tick-hand-effects-fn noop)
(def ^:private client-tick-level-effects-fn noop)
(def ^:private client-level-effects-active-fn (fn [] false))
(def ^:private client-build-level-effect-plan-fn (fn [_ _ _ _] nil))
(def ^:private client-level-effect-fov-offset-fn (fn [_] 0.0))
(def ^:private client-slot-visual-state-fn (fn [_ _] :idle))
(def ^:private client-visual-state-fn (fn [_ _] nil))
(def ^:private client-on-slot-key-down-fn noop)
(def ^:private client-on-slot-key-tick-fn noop)
(def ^:private client-on-slot-key-up-fn noop)
(def ^:private client-on-slot-key-abort-fn noop)
(def ^:private client-on-movement-key-down-fn noop)
(def ^:private client-on-movement-key-tick-fn noop)
(def ^:private client-on-movement-key-up-fn noop)
(def ^:private client-on-slot-wheel-fn noop)
(def ^:private client-build-overlay-plan-fn (fn [_ _ _ _] nil))
(def ^:private client-poll-particle-effects-fn (fn [_] []))
(def ^:private client-poll-sound-effects-fn (fn [_] []))
(def ^:private client-current-hand-transform-fn (fn [] nil))
(def ^:private client-drain-camera-pitch-deltas-fn (fn [_] []))

(def ^:private hot-client-hook-vars
  {:client-tick-start! #'client-tick-start-fn
   :client-tick-keys! #'client-tick-keys-fn
   :client-tick! #'client-tick-fn
   :client-tick-hand-effects! #'client-tick-hand-effects-fn
   :client-tick-level-effects! #'client-tick-level-effects-fn
   :client-level-effects-active? #'client-level-effects-active-fn
   :client-build-level-effect-plan #'client-build-level-effect-plan-fn
   :client-level-effect-fov-offset #'client-level-effect-fov-offset-fn
   :client-slot-visual-state #'client-slot-visual-state-fn
   :client-visual-state #'client-visual-state-fn
   :client-on-slot-key-down! #'client-on-slot-key-down-fn
   :client-on-slot-key-tick! #'client-on-slot-key-tick-fn
   :client-on-slot-key-up! #'client-on-slot-key-up-fn
   :client-on-slot-key-abort! #'client-on-slot-key-abort-fn
   :client-on-movement-key-down! #'client-on-movement-key-down-fn
   :client-on-movement-key-tick! #'client-on-movement-key-tick-fn
   :client-on-movement-key-up! #'client-on-movement-key-up-fn
   :client-on-slot-wheel! #'client-on-slot-wheel-fn
   :client-build-overlay-plan #'client-build-overlay-plan-fn
   :client-poll-particle-effects #'client-poll-particle-effects-fn
   :client-poll-sound-effects #'client-poll-sound-effects-fn
   :client-current-hand-transform #'client-current-hand-transform-fn
   :client-drain-camera-pitch-deltas! #'client-drain-camera-pitch-deltas-fn})

(defn- publish-hot-client-hooks! [hooks]
  (doseq [[hook target-var] hot-client-hook-vars]
    (when (contains? hooks hook)
      (alter-var-root target-var (constantly (get hooks hook)))))
  nil)

(defn- hooks-core-state-snapshot []
  ;; P3: no write-through cache — would go stale across with-fresh-framework re-injection with no invalidation hook to catch it; get-in here is already a lock-free HAMT lookup.
  (if-let [fw-atom (fw/fw-atom)]
    (or (get-in @fw-atom hooks-path)
        (default-runtime-hooks-state))
    (default-runtime-hooks-state)))

(defn freeze-runtime-hooks
  "Capture the installed runtime VTable after bootstrap.

  ServerRuntime stores this immutable map and calls it directly for the
  lifetime of a server.  This removes Framework atom derefs and nested HAMT
  lookups from player/server tick paths."
  []
  (hooks-core-state-snapshot))

(defn- update-hooks-core-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom
           (fn [state]
             (update-in state hooks-path
                        (fn [current]
                          (apply f (or current (default-runtime-hooks-state)) args))))))
  nil)

(def ^:private allowed-hooks-keys-path [:registry :hooks :allowed-keys])

(defn- allowed-hook-keys
  []
  (if-let [fw-atom (fw/fw-atom)]
    (or (get-in @fw-atom allowed-hooks-keys-path)
        (set (keys (default-runtime-hooks-state))))
    (set (keys (default-runtime-hooks-state)))))


(defn register-runtime-hook-keys!
  "Extend the set of allowed runtime hook keys before registration.

  This is used by content modules to opt into content-owned hook/request
  entrypoints while keeping the neutral contract validation fail-fast.
  No-op during AOT compilation when framework is nil."
  [hook-keys]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom
           (fn [state]
             (update-in state allowed-hooks-keys-path
                        (fn [current]
                          (into (or current (set (keys (default-runtime-hooks-state))))
                                (set hook-keys)))))))
  nil)

(defn- validate-hooks!
  [hooks]
  (when-not (valid-hooks-map? hooks)
    (throw (schema/contract-ex-info :runtime-hooks
                                    hooks
                                    (schema/explain hooks-map-schema hooks))))
  (let [allowed (allowed-hook-keys)
        unknown-keys (seq (remove allowed (keys hooks)))]
    (when unknown-keys
      (throw (ex-info "Unknown runtime hook keys"
                      {:contract :runtime-hooks
                       :unknown-hook-keys (vec unknown-keys)
                       :allowed-hook-keys allowed}))))
  hooks)

;; ============================================================================
;; Client session / player-state-owner — per-thread via ThreadLocal.
;; NOT ^:dynamic (铁律十一) and NOT in Framework (governance exception:
;; "客户端 session 状态 — 闭包工厂管理，不入 Framework").
;; ThreadLocal provides per-thread isolation without the ^:dynamic prohibition.
;; ============================================================================

(def ^:private ^ThreadLocal client-ctx-thread-local (ThreadLocal.))

(defn- get-client-ctx [] (.get ^ThreadLocal client-ctx-thread-local))
(defn- set-client-ctx! [ctx] (.set ^ThreadLocal client-ctx-thread-local ctx))

(defn client-session-id
  "Read client session id from ThreadLocal."
  [] (:session-id (get-client-ctx)))

(defn player-state-owner
  "Read player state owner from ThreadLocal."
  [] (or (:player-owner (get-client-ctx)) (:context-owner (get-client-ctx))))

(defn current-player-state-owner
  "Return the currently bound runtime player-state owner map (or nil)."
  [] (player-state-owner))

(defn current-context-owner
  "Return the explicitly scoped context-routing owner.

  Context routing remains stable when command execution temporarily installs a
  different player-state owner in the same thread."
  []
  (or (:context-owner (get-client-ctx))
      (:player-owner (get-client-ctx))))

(defn player-state-session-id
  "Resolve store session-id from a canonical owner map (server > client)."
  ([owner]
   (when owner
     (runtime-owner/store-session-id owner)))
  ([]
   (player-state-session-id (current-player-state-owner))))

(defn require-player-state-session-id
  "Resolve session-id from current owner, throwing with component context when absent."
  [component]
  (let [owner (current-player-state-owner)
        session-id (player-state-session-id owner)]
    (or session-id
        (throw (ex-info (str component " requires bound session-id")
                        {:player-state-owner owner})))))

(defn player-state-server-session-id
  "Resolve server-session-id from an owner map (or nil)."
  ([owner]
   (:server-session-id owner))
  ([]
   (player-state-server-session-id (current-player-state-owner))))

(defn require-player-state-server-session-id
  "Resolve server-session-id from current owner, throwing with component context when absent."
  [component]
  (let [owner (current-player-state-owner)
        session-id (player-state-server-session-id owner)]
    (or session-id
        (throw (ex-info (str component " requires bound :server-session-id")
                        {:player-state-owner owner})))))

(defn player-state-client-session-id
  "Resolve client session-id from a canonical owner map."
  ([owner]
   (:client-session-id owner))
  ([]
   (player-state-client-session-id (current-player-state-owner))))

(defn context-player-state-session-id
  "Resolve store session-id from command/action context metadata owner, with runtime owner fallback."
  [context]
  (or (some-> context :metadata :player-state-owner runtime-owner/store-session-id)
      (player-state-session-id)))

(defn require-context-player-state-session-id
  "Resolve session-id from context metadata/runtime owner, throwing when absent."
  [component context]
  (let [session-id (context-player-state-session-id context)]
    (or session-id
        (throw (ex-info (str component " requires bound session-id")
                        {:context context
                         :player-state-owner (current-player-state-owner)})))))

(defn with-client-ctx-fn
  "ThreadLocal-based context HOF. Sets ctx-map in current thread's ThreadLocal
  for duration of thunk, restores on exit. LazySeq-safe: auto-doall on return value.

  === 调用规范（强制）===
  1. 唯一入口：设置/恢复必须通过本函数或 with-player-state-owner-fn，禁止直接操作 ThreadLocal
  2. 网络重建：Packet Handler 必须在分派前重建上下文（见 forge/fabric gui/network）
  3. 异步边界：传给 enqueueWork / future / CompletableFuture 的闭包必须在内部重新建立上下文
  4. 读取规范：client-session-id 和 player-state-owner 是函数，必须加括号调用

  ⚠️ 铁律六：上下文不跨越异步边界。Minecraft 的 enqueueWork / future 启动新调用链时，
  必须在新线程上重新调用 with-client-ctx-fn 建立上下文。

  ⚠️ 铁律四：返回值如果是 LazySeq，会在 finally 恢复上下文后才求值 → 读到 nil。
  本函数自动对顶层 LazySeq 执行 doall 截断。"
  [ctx-map thunk]
  (let [old (.get ^ThreadLocal client-ctx-thread-local)]
    (.set ^ThreadLocal client-ctx-thread-local (merge (or old {}) ctx-map))
    (try
      (let [result (thunk)]
        (if (instance? clojure.lang.LazySeq result) (doall result) result))
      (finally
        (if (nil? old)
          (.remove ^ThreadLocal client-ctx-thread-local)
          (.set ^ThreadLocal client-ctx-thread-local old))))))

(defn with-player-state-owner-fn
  "Use with-client-ctx-fn to bind player-owner for the duration of thunk."
  [owner thunk]
  (with-client-ctx-fn {:player-owner owner} thunk))

;; ============================================================================
;; Session context push/pop/clear — 零分配 ThreadLocal 管理
;; 供 DelegatingScreen.java 调用，替代 per-frame with-client-ctx
;; ============================================================================

(defn push-session-context!
  "Push :session-id onto client context ThreadLocal.
   Optimized: when (:session-id old) == session-id, no allocation & no ThreadLocal write.
   Returns previous context (nil if none) — must be passed to pop-session-context!."
  [^String session-id]
  (let [old (get-client-ctx)]
    (when (not= (:session-id old) session-id)
      ;; first frame or session changed: allocate new map + write ThreadLocal
      (set-client-ctx! (assoc (or old {}) :session-id session-id)))
    old))

(defn pop-session-context!
  "Restore ThreadLocal context to old value.
   No-op when old and current are value-equal (steady state)."
  [old]
  (let [current (get-client-ctx)]
    (when (not= current old)
      (if old
        (set-client-ctx! old)
        (.remove ^ThreadLocal client-ctx-thread-local)))))

(defn clear-session-context!
  "Explicitly remove client context from ThreadLocal.
   Called by DelegatingScreen.removed() for final cleanup."
  []
  (.remove ^ThreadLocal client-ctx-thread-local))

(defn push-player-state-owner!
  "Install a player-state owner without allocating a per-call thunk.

  This entrypoint is reserved for the server tick coordinator, which pushes
  once around the complete start/player/end pipeline. Returns the previous
  context for pop-client-context!."
  [owner]
  (let [old (get-client-ctx)]
    (set-client-ctx! (assoc (or old {}) :player-owner owner))
    old))

(defn pop-client-context!
  "Restore a context returned by push-player-state-owner!."
  [old]
  (if old
    (set-client-ctx! old)
    (.remove ^ThreadLocal client-ctx-thread-local))
  nil)


(defn register-power-runtime-hooks!
  "Register/replace power runtime hook fns."
  [hooks]
  (let [validated (validate-hooks! hooks)]
    (update-hooks-core-state! merge validated)
    (publish-hot-client-hooks! validated))
  nil)

(defn register-action!
  "Register a content-owned action descriptor through the neutral registry."
  [descriptor]
  (content-registry/register-action! descriptor))

(defn dispatch-action!
  "Dispatch a content action by opaque id with host-provided context/payload."
  [action-id context payload]
  (content-registry/dispatch-action! action-id context payload))

(defn register-sync-descriptor!
  "Register a content-owned sync descriptor through the neutral registry."
  [descriptor]
  (content-registry/register-sync-descriptor! descriptor))

(defn list-sync-descriptors
  "List content-owned sync descriptors known to the neutral registry."
  []
  (content-registry/list-sync-descriptors))

(defn apply-sync!
  "Apply a sync descriptor by opaque id with host-provided context/payload."
  [sync-id context payload]
  (content-registry/apply-sync! sync-id context payload))

(defn register-player-persistence-descriptor!
  "Register a content-owned player persistence descriptor."
  [descriptor]
  (content-registry/register-player-persistence-descriptor! descriptor))

(defn list-player-persistence-descriptors
  "List player persistence descriptors known to the neutral registry."
  []
  (content-registry/list-player-persistence-descriptors))

;; ============================================================================
;; Player state domain registry
;; Maps domain-key (keyword) → nbt-key (string) for per-domain state persistence.
;; Registered by content modules during init; read by nbt_core at load/save time.
;; Stored in Framework [:registry :hooks :player-state-domains].
;; ============================================================================

(def ^:private player-state-domains-path [:registry :hooks :player-state-domains])

(defn register-player-state-domain!
  "Register a player state domain mapping: {:domain-key kw :nbt-key str}.
  Called by content modules during init. Platform nbt layer reads this at runtime.
  No-op during AOT compilation when framework is nil."
  [{:keys [domain-key nbt-key]}]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in player-state-domains-path
           (fn [current] (assoc (or current {}) domain-key nbt-key)))))

(defn list-player-state-domains
  "Return map of {domain-key nbt-key} for all registered player state domains."
  []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom player-state-domains-path {})
    {}))

;; ============================================================================
;; Server-side player login side-effect hooks
;; Content modules register callbacks here to run on server player login.
;; The player argument is passed as-is (opaque to this layer).
;; Stored in Framework [:registry :hooks :server-player-login-hooks].
;; ============================================================================

(def ^:private server-player-login-hooks-path [:registry :hooks :server-player-login-hooks])

(defn register-server-player-login-hook!
  "Register a content-owned server player login hook fn.
  fn receives the raw ServerPlayer object. Called by content modules during init.
  No-op during AOT compilation when framework is nil."
  [f]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in server-player-login-hooks-path
           (fn [current] (conj (or current []) f)))))

(defn run-server-player-login-hooks!
  "Run all registered server player login hooks with the given player.
  Called by platform lifecycle adapters on player login, after core login handling."
  [player]
  (doseq [f (if-let [fw-atom (fw/fw-atom)]
              (get-in @fw-atom server-player-login-hooks-path [])
              [])]
    (try (f player) (catch Throwable e (log/stacktrace "Error running server player login hook" e)))))

;; Client input is now dispatched solely through mcmod.protocol.keyboard-input.
;; register-client-input-descriptor! / emit-client-input! (former System B) were dead
;; code — emit-client-input! was never called from any platform event loop.
;; Use mcmod.protocol.keyboard-input/register-input-id! and emit-keyboard-input! instead.

(defn on-player-login!
  [player-uuid]
  ((:on-player-login! (hooks-core-state-snapshot)) player-uuid))

(defn on-player-logout!
  [player-uuid]
  ((:on-player-logout! (hooks-core-state-snapshot)) player-uuid))

(defn on-server-stop!
  [session-id]
  ((:on-server-stop! (hooks-core-state-snapshot)) session-id))

(defn on-player-clone!
  [old-player-uuid new-player-uuid]
  ((:on-player-clone! (hooks-core-state-snapshot)) old-player-uuid new-player-uuid))

(defn on-player-death!
  [player-uuid]
  ((:on-player-death! (hooks-core-state-snapshot)) player-uuid))

(defn on-player-dimension-change!
  [player-uuid from-dim to-dim]
  ((:on-player-dimension-change! (hooks-core-state-snapshot)) player-uuid from-dim to-dim))

(defn on-server-tick-start!
  [tick-id]
  ((:on-server-tick-start! (hooks-core-state-snapshot)) tick-id))

(defn on-player-tick!
  [player-uuid]
  ((:on-player-tick! (hooks-core-state-snapshot)) player-uuid))

(defn on-server-tick-end!
  [tick-id]
  ((:on-server-tick-end! (hooks-core-state-snapshot)) tick-id))

(defn init-damage-handlers!
  []
  ((:init-damage-handlers! (hooks-core-state-snapshot))))

(defn list-player-uuids
  []
  ((:list-player-uuids (hooks-core-state-snapshot))))

(defn build-sync-payload
  "Build the runtime sync payload for player-uuid. When full? is true (or
  omitted), every synced domain is included; when false, content layers may
  return only domains that changed since the last clean mark (delta sync)."
  ([player-uuid]
   (build-sync-payload player-uuid true))
  ([player-uuid full?]
   ((:build-sync-payload (hooks-core-state-snapshot)) player-uuid full?)))

(defn player-state-dirty?
  "Return true when player-uuid's runtime state has unsynced changes.
  Content layer implements this by reading the per-player :dirty? flag; the
  default hook conservatively returns true so platforms without a registered
  implementation keep syncing every tick (safe fallback)."
  [player-uuid]
  (boolean ((:player-state-dirty? (hooks-core-state-snapshot)) player-uuid)))

(defn mark-player-clean!
  [player-uuid]
  ((:mark-player-clean! (hooks-core-state-snapshot)) player-uuid))

(defn get-player-state
  [player-uuid]
  ((:get-player-state (hooks-core-state-snapshot)) player-uuid))

(defn runtime-activated?
  "Returns true if the installed runtime marks `player-uuid` active."
  [player-uuid]
  (boolean ((:runtime-activated? (hooks-core-state-snapshot)) player-uuid)))

(defn sync-player-state!
  [player-uuid state]
  ((:sync-player-state! (hooks-core-state-snapshot)) player-uuid state))

(defn ensure-player-state!
  [player-uuid]
  ((:ensure-player-state! (hooks-core-state-snapshot)) player-uuid))

(defn fresh-player-state
  []
  ((:fresh-player-state (hooks-core-state-snapshot))))

(defn register-network-handlers!
  []
  ((:register-network-handlers! (hooks-core-state-snapshot))))

(defn subscribe-achievement-trigger!
  [handler]
  ((:subscribe-achievement-trigger! (hooks-core-state-snapshot)) handler))

(defn register-context-route-fns!
  [fns-map]
  ((:register-context-route-fns! (hooks-core-state-snapshot)) fns-map))

(defn register-context-send-fns!
  [fns-map]
  ((:register-context-send-fns! (hooks-core-state-snapshot)) fns-map))

(defn get-context-player-uuid
  [ctx-id]
  ((:get-context-player-uuid (hooks-core-state-snapshot)) ctx-id))

(defn register-damage-handler!
  [handler-id handler-fn priority]
  ((:register-damage-handler! (hooks-core-state-snapshot)) handler-id handler-fn priority))

(defn unregister-damage-handler!
  [handler-id]
  ((:unregister-damage-handler! (hooks-core-state-snapshot)) handler-id))

(defn get-active-damage-handlers
  []
  ((:get-active-damage-handlers (hooks-core-state-snapshot))))

(defn process-damage-interception
  [player-id attacker-id damage damage-source]
  ((:process-damage-interception (hooks-core-state-snapshot)) player-id attacker-id damage damage-source))

(defn should-cancel-attack-interception?
  [player-id attacker-id damage damage-source]
  ((:should-cancel-attack-interception? (hooks-core-state-snapshot)) player-id attacker-id damage damage-source))

(defn run-attack-precheck-side-effects!
  [player-id attacker-id damage damage-source]
  ((:run-attack-precheck-side-effects! (hooks-core-state-snapshot)) player-id attacker-id damage damage-source))

(defn resolve-item-use-action
  [item-id]
  ((:resolve-item-use-action (hooks-core-state-snapshot)) item-id))

(defn on-runtime-item-action!
  [action player-uuid payload]
  ((:on-runtime-item-action! (hooks-core-state-snapshot)) action player-uuid payload))

(defn build-item-use-plan
  [player-uuid item-id activated? side]
  ((:build-item-use-plan (hooks-core-state-snapshot)) player-uuid item-id activated? side))

(defn compute-aoe-damage
  [origin-pos target-pos radius damage falloff?]
  ((:compute-aoe-damage (hooks-core-state-snapshot)) origin-pos target-pos radius damage falloff?))

(defn select-reflection-target
  [current-entity-uuid current-pos candidates max-radius]
  ((:select-reflection-target (hooks-core-state-snapshot)) current-entity-uuid current-pos candidates max-radius))

(defn compute-reflected-damage
  [current-damage]
  ((:compute-reflected-damage (hooks-core-state-snapshot)) current-damage))

(defn get-reflection-search-radius
  []
  ((:reflection-search-radius (hooks-core-state-snapshot))))

(defn client-new-context
  [player-uuid context-id]
  ((:client-new-context (hooks-core-state-snapshot)) player-uuid context-id))

(defn client-register-context!
  [ctx]
  ((:client-register-context! (hooks-core-state-snapshot)) ctx))

(defn client-get-context
  [ctx-id]
  ((:client-get-context (hooks-core-state-snapshot)) ctx-id))

(defn client-terminate-context!
  [ctx-id reason]
  ((:client-terminate-context! (hooks-core-state-snapshot)) ctx-id reason))

(defn client-transition-to-alive!
  [ctx-id server-id payload]
  ((:client-transition-to-alive! (hooks-core-state-snapshot)) ctx-id server-id payload))

(defn client-send-context-local!
  [ctx-id channel payload]
  ((:client-send-context-local! (hooks-core-state-snapshot)) ctx-id channel payload))

(defn client-build-overlay-plan
  [player-uuid screen-width screen-height overlay-state]
  (client-build-overlay-plan-fn player-uuid screen-width screen-height overlay-state))

(defn set-client-overlay-activated!
  "Notify the overlay layer that activation state has changed for a player.
  Called by the content layer after the activate handler stack resolves.
  player-uuid is a string, activated is a boolean."
  [player-uuid activated]
  ((:set-client-overlay-activated! (hooks-core-state-snapshot)) player-uuid activated))

(defn client-poll-particle-effects
  ([]
   (client-poll-particle-effects nil))
  ([owner]
   (client-poll-particle-effects-fn owner)))

(defn client-poll-sound-effects
  ([]
   (client-poll-sound-effects nil))
  ([owner]
   (client-poll-sound-effects-fn owner)))

(defn client-tick-start!
  "Per-frame hook fired at the START of the client tick — before vanilla
  handleKeybinds reads the KeyMappings. Used for vanilla-input suppression of
  skill-owned keys (flashing's WASD sub-keys) that the END-phase hook cannot
  stop: KeyboardHandler re-reads movement keys from GLFW every tick."
  [get-player-uuid-fn]
  (client-tick-start-fn get-player-uuid-fn))

(defn client-font-init!
  "Register AC MSDF font keywords. Called by the platform AFTER the client
  bridge is fully installed (the content client-init hook may fire earlier,
  on RegisterRenderers, before the bridge ops exist)."
  []
  ((:client-font-init! (hooks-core-state-snapshot))))

(defn client-tick-keys!
  "Per-frame key polling hook. key-state-fn reports raw physical key state;
  screen-open? gates event dispatch (a Screen is open — see keybinds/tick-keys!)."
  [key-state-fn get-player-uuid-fn screen-open?]
  (client-tick-keys-fn key-state-fn get-player-uuid-fn screen-open?))

(defn client-active-contexts
  []
  ((:client-active-contexts (hooks-core-state-snapshot))))

(defn client-latest-sync
  [player-uuid]
  ((:client-latest-sync (hooks-core-state-snapshot)) player-uuid))

(defn client-register-push-handlers!
  []
  ((:client-register-push-handlers! (hooks-core-state-snapshot))))

(defn client-notify-visual-event!
  [event-key payload]
  ((:client-notify-visual-event! (hooks-core-state-snapshot)) event-key payload))

(defn client-show-combat-notice!
  [notice-id payload]
  ((:client-show-combat-notice! (hooks-core-state-snapshot)) notice-id payload))

(defn client-enqueue-level-effect!
  [effect-id ctx-id channel payload & opts]
  (apply (:client-enqueue-level-effect! (hooks-core-state-snapshot)) effect-id ctx-id channel payload opts))

(defn client-build-level-effect-plan
  [camera-pos hand-center-pos tick query-nearby-blocks-fn]
  (client-build-level-effect-plan-fn
   camera-pos hand-center-pos tick query-nearby-blocks-fn))

(defn client-level-effects-active?
  []
  (client-level-effects-active-fn))

(defn client-tick-level-effects!
  []
  (client-tick-level-effects-fn))

(defn client-level-effect-fov-offset
  "Per-frame camera FOV offset (degrees) from active level effects, for the
  local player (ComputeFov event handler)."
  [player-uuid]
  (client-level-effect-fov-offset-fn player-uuid))

(defn client-slot-visual-state
  [player-uuid key-idx]
  (client-slot-visual-state-fn player-uuid key-idx))

(defn client-visual-state
  [state-key payload]
  (client-visual-state-fn state-key payload))

(defn client-on-slot-key-down!
  [player-uuid key-idx]
  (client-on-slot-key-down-fn player-uuid key-idx))

(defn client-on-slot-key-tick!
  [player-uuid key-idx]
  (client-on-slot-key-tick-fn player-uuid key-idx))

(defn client-on-slot-key-up!
  [player-uuid key-idx]
  (client-on-slot-key-up-fn player-uuid key-idx))

(defn client-on-slot-key-abort!
  [player-uuid key-idx]
  (client-on-slot-key-abort-fn player-uuid key-idx))

(defn client-on-movement-key-down!
  [player-uuid movement-key]
  (client-on-movement-key-down-fn player-uuid movement-key))

(defn client-on-movement-key-tick!
  [player-uuid movement-key]
  (client-on-movement-key-tick-fn player-uuid movement-key))

(defn client-on-movement-key-up!
  [player-uuid movement-key]
  (client-on-movement-key-up-fn player-uuid movement-key))

(defn client-on-slot-wheel!
  [player-uuid key-idx delta]
  (client-on-slot-wheel-fn player-uuid key-idx delta))

(defn client-clear-owner-state!
  [owner]
  ((:client-clear-owner-state! (hooks-core-state-snapshot)) owner))

(defn client-abort-all!
  []
  ((:client-abort-all! (hooks-core-state-snapshot))))

(defn client-tick!
  []
  (client-tick-fn))

(defn client-tick-hand-effects!
  []
  (client-tick-hand-effects-fn))

(defn client-drain-camera-pitch-deltas!
  ([]
   (client-drain-camera-pitch-deltas! nil))
  ([owner]
   (client-drain-camera-pitch-deltas-fn owner)))

(defn client-current-hand-transform
  []
  (client-current-hand-transform-fn))

(defn toggle-debug-overlay-state! []
  ((:toggle-debug-overlay-state! (hooks-core-state-snapshot))))

;; ============================================================================
;; Default Client Owner Hook
;; Platform Minecraft component registers a function that returns a complete client
;; owner map. This allows platform-agnostic modules (ac) to resolve a client owner
;; without importing Minecraft classes directly.
;; ============================================================================

(def ^:private default-client-owner-fn nil)

(defn set-default-client-owner-fn!
  "Register a zero-arg function that returns a canonical client owner map
  {:logical-side :client :client-session-id ... :player-uuid ...}.
  Called by the platform layer during client initialization."
  [f]
  (install/install-root! #'default-client-owner-fn f)
  nil)

(defn default-client-owner
  "Return the current client owner map via the platform-registered hook.
  Returns nil if the platform layer hasn't registered a hook yet."
  []
  (when-let [f default-client-owner-fn]
    (f)))
