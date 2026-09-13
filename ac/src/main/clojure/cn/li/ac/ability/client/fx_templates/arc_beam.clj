(ns cn.li.ac.ability.client.fx-templates.arc-beam
  "Shared FX template: arc-beam defaults + defmulti registry for custom effects.

  Ability FX namespaces register via `build-spec` and expose only `init!`
  plus test helpers delegating to this template."
  (:require [cn.li.ac.ability.client.arc-patterns :as arc-patterns]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.effects.rv3 :as rv3]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcmod.math V3]))

;; ---------------------------------------------------------------------------
;; Effect registry (populated by build-spec)
;; ---------------------------------------------------------------------------

(def ^:private effect-registry (atom {}))

(defn register-effect!
  "Public: def-arc-beam-fx expands calls to this into each skill FX
  namespace's init!, so it must be visible cross-namespace."
  [effect-id entry]
  (swap! effect-registry assoc effect-id entry))

(defn- effect-entry
  [effect-id]
  (get @effect-registry effect-id))

(defn- runtime-kind
  [effect-id]
  (or (:runtime (effect-entry effect-id)) :level))

(defn- snapshot-runtime
  [effect-id runtime]
  (case runtime
    :hand (hand-effects/effect-state-snapshot effect-id)
    :level (level-effects/effect-state-snapshot effect-id)
    nil))

(defn- update-runtime-state!
  [effect-id runtime f]
  (case runtime
    :hand (hand-effects/update-effect-state! effect-id f)
    :level (level-effects/update-effect-state! effect-id f)))

(defn- reset-runtime-state!
  [effect-id runtime initial-state]
  (case runtime
    :hand (hand-effects/reset-hand-effect-state-for-test! effect-id initial-state)
    :level (level-effects/reset-level-effect-state-for-test! effect-id initial-state)))

;; ---------------------------------------------------------------------------
;; defmulti effect implementations (custom + default arc)
;; ---------------------------------------------------------------------------

(defmulti effect-initial-state
  (fn [effect-id runtime] [effect-id runtime]))

(defmulti effect-enqueue-state!
  (fn [runtime effect-id _store _ctx-id _channel _owner-key _payload]
    [effect-id runtime]))

(defmulti effect-tick-state!
  (fn [runtime effect-id _store] [effect-id runtime]))

(defmulti effect-build-plan
  (fn [effect-id _camera-pos _hand-center-pos _tick & _rest]
    effect-id))

(defmulti effect-clear-owner!
  (fn [effect-id _store _owner-key] effect-id))

(defmulti effect-transform-fn
  (fn [effect-id] effect-id))

(defn validate-multimethod-arity!
  "Check every NON-VARIADIC defmethod of `mf` against `dispatch-arity`.
  Variadic defmethods (with `& args`) accept extra args from the dispatch
  fn beyond their named params, so a lower required-arity is legitimate.
  Only flag methods that are FIXED-ARITY and don't match — those are the
  ones that corrupt the dispatch table."
  [label mf dispatch-arity]
  (let [mis (keep (fn [[dv method]]
                    (let [ma (if (instance? clojure.lang.RestFn method)
                               (.getRequiredArity ^clojure.lang.RestFn method)
                               dispatch-arity)]  ;; non-RestFn → skip check
                      (when (< ma (dec dispatch-arity))
                        {:dispatch-value dv :method-arity ma :min-acceptable (dec dispatch-arity)})))
                  (.getMethodTable ^clojure.lang.MultiFn mf))]
    (when (seq mis)
      (throw (ex-info (str label ": defmethod arity mismatch — "
                           (count mis) " FIXED-ARITY method(s) have wrong arity "
                           "(the MULTIMETHOD dispatch table is CORRUPTED, "
                           "ALL calls silently return nil)")
                      {:multimethod label :mismatches mis})))))

(defn validate-fx-multimethods!
  "Call after all impl namespaces are loaded (post init-discovered-fx!).
  Catches the groundshock-class bug where a defmethod with wrong arity
  silently breaks every effect's build-plan / clear / transform."
  []
  (validate-multimethod-arity! "effect-build-plan"   effect-build-plan   4)
  (validate-multimethod-arity! "effect-clear-owner!"  effect-clear-owner! 3)
  (validate-multimethod-arity! "effect-transform-fn"  effect-transform-fn  1))

;; ---------------------------------------------------------------------------
;; Default arc state / arc implementation
;; ---------------------------------------------------------------------------

(defn default-arc-state
  []
  {:arcs {}})

(defn- ensure-arc-store
  [store]
  (if (contains? (or store {}) :arcs)
    (or store (default-arc-state))
    (default-arc-state)))

(defn- base-meta
  [owner-key ctx-id channel payload]
  {:owner-key owner-key
   :ctx-id ctx-id
   :channel channel
   :source-player-id (:source-player-id payload)
   :world-id (:world-id payload)})

;; ---------------------------------------------------------------------------
;; Caster-hand origin (original LambdaLib2 ViewOptimize.fix)
;; ---------------------------------------------------------------------------

;; The original spawns EntityArc at the caster's eye, then translates the whole
;; arc in its own local frame before drawing — [forward up right], applied
;; after the GL matrix has been rotated to the arc's yaw/pitch. Which of the
;; two sets applies is the original's isFirstPerson(): the caster's own arc
;; seen through their own eyes gets the small one, everything else — the caster
;; in F5, or anyone watching someone else cast, i.e. whenever a player model is
;; on screen — gets the one that drops it to the model's hand.
(def first-person-view-offset [-0.05 -0.25 0.2])
(def third-person-view-offset [0.15 -0.8 0.23])

(defn local-frame-offset
  "Resolve one [forward up right] offset triple against the arc's/beam's own
  axes. Accepts either map positions (crossing from network state) or
  already-converted V3s (the beam impls' ray endpoints)."
  [start end [forward-o up-o right-o]]
  (let [start-v3 (if (instance? V3 start) start (rv3/map->v3 start))
        end-v3 (if (instance? V3 end) end (rv3/map->v3 end))
        forward (rv3/vnorm (rv3/v- end-v3 start-v3))
        right-raw (rv3/vcross forward rv3/unit-y)
        ;; Straight up/down leaves "right" undefined; any perpendicular does.
        right (if (> (rv3/vlen right-raw) 1.0e-5)
                (rv3/vnorm right-raw)
                rv3/unit-x)
        up (rv3/vnorm (rv3/vcross right forward))]
    (rv3/v+ (rv3/v+ (rv3/v* forward forward-o)
                    (rv3/v* up up-o))
            (rv3/v* right right-o))))

(defn view-fix-rays
  "Original ViewOptimize fix applied to ray endpoints: translate each ray by
  the viewer-dependent hand offset so the ray issues from the hand, not the
  eye. The viewer's own first-person view gets the small eye-adjacent offset,
  everyone else (F5, remote viewers) the model-hand offset — the same pairing
  the original's isFirstPerson() uses. Rays whose :source-player-id matches
  the viewer match first-person; opaque rays (no :source-player-id) always
  get the third-person offset.

  `:fix-end?` defaults true (translate the whole ray, meltdowner's beam);
  pass false to keep the END anchored on the aim point — the original's
  \"Don't fix end to get accurate pointing direction\" (preray rays must
  terminate exactly where the crosshair points, or the ray visibly misses
  its target from the side)."
  ([view-ctx rays]
   (view-fix-rays view-ctx rays {}))
  ([view-ctx rays {:keys [fix-end?] :or {fix-end? true}}]
   (mapv (fn [beam]
           (let [own? (and (:first-person? view-ctx)
                           (= (str (:player-uuid view-ctx))
                              (str (:source-player-id beam))))
                 offset (if own? first-person-view-offset third-person-view-offset)
                 fix (local-frame-offset (:start beam) (:end beam) offset)]
             (cond-> beam
               true (update :start rv3/v+ fix)
               fix-end? (update :end rv3/v+ fix))))
         rays)))

(defn- arc-item
  "Precompute the zigzag vertex path once per arc, at enqueue time."
  [base-meta start end arc-life pattern-key & {:keys [is-aoe? hit-type hand-origin?]}]
  (let [pattern-key* (if is-aoe? :aoe pattern-key)
        pattern (arc-patterns/get-pattern pattern-key*)
        start-v3 (rv3/map->v3 start)
        end-v3 (rv3/map->v3 end)
        seed (rand-int 100000)
        vertices (arc-patterns/generate-zigzag-segments start-v3 end-v3
                   {:segments (:segments pattern)
                    :amplitude (:amplitude pattern)
                    :seed seed})]
    (merge base-meta
           {:ttl arc-life
            :max-ttl arc-life
            :pattern-key pattern-key*
            :hit-type hit-type
            ;; :start/:end/:seed let build-plan regenerate the zigzag per
            ;; tick — EntityArc re-rolls its template with texWiggle
            ;; probability every tick, so the bolt must reshape, not hold one
            ;; frozen path for its whole life. :vertices stays for
            ;; diagnostics/tests.
            :start start-v3
            :end end-v3
            :seed seed
            :vertices vertices}
           ;; Both candidates are resolved here because they depend only on the
           ;; arc's direction; the viewer-dependent pick happens per frame.
           (when hand-origin?
             {:view-offset-own (local-frame-offset start end first-person-view-offset)
              :view-offset-other (local-frame-offset start end third-person-view-offset)}))))

(defn- play-sound!
  [{:keys [sound-id sound-source sound-volume sound-pitch]} payload]
  (when sound-id
    (let [{:keys [x y z]} (:sound-pos payload)]
      (client-sounds/queue-current-sound-effect!
        (cond-> {:type :sound
                 :sound-id sound-id
                 :volume (double (or sound-volume 0.5))
                 :pitch (double (or sound-pitch 1.0))}
          sound-source (assoc :source sound-source)
          (every? number? [x y z])
          (assoc :x (double x)
                 :y (double y)
                 :z (double z)))))))

(defn- enqueue-arc-state!
  [opts store ctx-id channel owner-key payload]
  (let [store* (ensure-arc-store store)
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode start end hit-type aoe-origin aoe-points]} (or payload {})
        base (base-meta owner-key* ctx-id channel payload)
        arc-life (long (or (:arc-life opts) 10))
        arc-pattern (:arc-pattern opts :weak)]
    (case mode
      :perform
      (cond
        (and (:aoe-points? opts) start end)
        (let [aoe-start (if (map? aoe-origin) aoe-origin end)
              ;; repeat would evaluate arc-item ONCE and hand back the same
              ;; item three times — identical seed, identical vertices, three
              ;; bolts drawn exactly on top of each other, reading as one.
              ;; Upstream spawns three independent EntityArcs, each picking its
              ;; own template out of the pattern's bank.
              main-arcs (vec (repeatedly 3 #(arc-item base start end arc-life arc-pattern
                                                     :hand-origin? (:hand-origin? opts))))
              aoe-arcs (->> aoe-points
                            (keep (fn [pt]
                                    (when (map? pt)
                                      (let [life (+ 15 (rand-int 11))]
                                        (arc-item base aoe-start pt life arc-pattern :is-aoe? true)))))
                            vec)
              store** (update-in store* [:arcs owner-key*] (fnil into []) (into main-arcs aoe-arcs))]
          (play-sound! opts payload)
          store**)

        (and (map? start) (map? end))
        (do
          (play-sound! opts payload)
          (update-in store* [:arcs owner-key*] (fnil conj [])
                     (arc-item base start end arc-life arc-pattern
                               :hit-type hit-type
                               :hand-origin? (:hand-origin? opts))))

        :else store*)

      :end
      (update store* :arcs dissoc owner-key*)

      store*)))

(defn- tick-arc-state!
  [store]
  (update (ensure-arc-store store) :arcs store-tick/tick-ttl-items-by-owner))

(defn- view-origin-offset
  "Pick this viewer's ViewOptimize offset for one arc, or nil when the effect
  didn't opt into hand origins.

  `view-ctx` is hand-center-pos, which carries both halves of the original's
  isFirstPerson() test: `:player-uuid` (the same signal the other fx-template
  impls use to recognise their own player's effects) and `:first-person?`. Only
  the caster looking through their own eyes gets the small offset — as soon as
  a player model is on screen, theirs or someone else's, the arc has to drop to
  its hand instead. A `view-ctx` with no `:first-person?` key is read as first
  person, the vanilla default camera."
  [view-ctx {:keys [source-player-id view-offset-own view-offset-other]}]
  (when view-offset-own
    (let [{:keys [player-uuid]} view-ctx]
      (if (and (:first-person? view-ctx true)
               player-uuid
               source-player-id
               (= (str player-uuid) (str source-player-id)))
        view-offset-own
        view-offset-other))))

(defn- arc-visible?
  "EntityArc.onUpdate's show/hide Markov chain: a visible arc hides with
  showWiggle probability per tick, a hidden one reshows with hideWiggle
  (defaults 0.2/0.2 — a ~50% duty flicker, which is the upstream bolt's
  crackle). Rolled deterministically from the arc's seed so the same chain
  prefix is replayed for every ttl — stable within a tick, stable across
  viewers."
  [pattern seed ttl]
  (let [show-w (double (or (:show-wiggle pattern) 0.2))
        hide-w (double (or (:hide-wiggle pattern) 0.2))
        rng (java.util.Random. (long seed))]
    (loop [t 0 visible true]
      (if (>= t (long ttl))
        visible
        (recur (inc t)
               (if visible
                 (not (< (.nextDouble rng) show-w))
                 (< (.nextDouble rng) hide-w)))))))

(defn- arc-ops
  [cam-v3 view-ctx wiggle-phase {:keys [start end pattern-key ttl max-ttl seed] :as item}]
  (let [pattern (arc-patterns/get-pattern pattern-key)
        life-ratio (- 1.0 (/ (double ttl) (double (max 1 max-ttl))))]
    (when (arc-visible? pattern seed ttl)
      (let [;; EntityArc.onUpdate re-rolls its template with texWiggle
            ;; probability per tick — reseed the zigzag from the ttl so the
            ;; bolt reshapes once per tick and stays stable within it.
            seed* (hash [seed ttl])
            vertices (arc-patterns/generate-zigzag-segments
                      start end
                      {:segments (:segments pattern)
                       :amplitude (:amplitude pattern)
                       :seed seed*})]
        (ru/zigzag-arc-ops cam-v3 vertices pattern
          {:life-ratio life-ratio
           :origin-offset (view-origin-offset view-ctx item)
           :seed seed})))))

(defn- build-arc-plan
  [opts camera-pos hand-center-pos _tick]
  (let [effect-id (:effect-id opts)
        by-owner (:arcs (level-effects/effect-state-snapshot effect-id))]
    (when (seq by-owner)
      (let [cam-v3 (rv3/map->v3 camera-pos)
            wiggle-phase (arc-patterns/wiggle-phase)
            items (mapcat val by-owner)
            ops (into [] (mapcat #(arc-ops cam-v3 hand-center-pos wiggle-phase %)) items)]
        (when (seq ops)
          {:ops ops})))))

(defmethod effect-initial-state :default
  [effect-id runtime]
  (if-let [entry (effect-entry effect-id)]
    (let [init (case runtime
                 :hand (or (:hand-initial-state entry) (:initial-state entry))
                 :level (or (:level-initial-state entry) (:initial-state entry)))]
      (cond
        (fn? init) (init)
        (some? init) init
        :else (default-arc-state)))
    (default-arc-state)))

(defmethod effect-enqueue-state! :default
  [runtime effect-id store ctx-id channel owner-key payload]
  (when (= runtime :level)
    (enqueue-arc-state! (:arc-opts (effect-entry effect-id))
                        store ctx-id channel owner-key payload)))

(defmethod effect-tick-state! :default
  [runtime effect-id store]
  (when (= runtime :level)
    (tick-arc-state! store)))

(defmethod effect-build-plan :default
  [effect-id camera-pos hand-center-pos tick & _args]
  (when-let [entry (effect-entry effect-id)]
    (when-let [arc-opts (:arc-opts entry)]
      (build-arc-plan arc-opts camera-pos hand-center-pos tick))))

(defmethod effect-clear-owner! :default
  [_effect-id store _owner-key]
  ;; One-shot arcs (arc-gen, thunder-bolt) are TTL-lived world visuals: their
  ;; :instant context terminates on the same tick as perform, so
  ;; MSG-CTX-TERMINATED reaches clear-effect-owner! immediately — clearing
  ;; here deleted the arc a frame after it appeared, leaving only the cast
  ;; sound. The impl skills hit the same trap and each overrides this method
  ;; (see ray-barrage / mine-detect / railgun-shot: "clearing the queue here
  ;; deleted every ray a frame after it appeared... They expire on their own
  ;; ttl instead"). The default arc state only ever holds TTL-bounded items,
  ;; which expire on their own via tick-arc-state!; channeled effects clear
  ;; through their :end channel. Effects with context-bound persistent state
  ;; supply their own defmethod.
  store)

(defmethod effect-transform-fn :default
  [_effect-id]
  nil)

;; ---------------------------------------------------------------------------
;; Runtime dispatch wrappers (used by build-spec)
;; ---------------------------------------------------------------------------

(defn- dispatch-enqueue!
  [runtime effect-id store ctx-id channel owner-key payload]
  (effect-enqueue-state! runtime effect-id store ctx-id channel owner-key payload))

(defn- dispatch-tick!
  [runtime effect-id store]
  (effect-tick-state! runtime effect-id store))

(defn- dispatch-clear-owner!
  [effect-id store owner-key]
  (effect-clear-owner! effect-id store owner-key))

;; ---------------------------------------------------------------------------
;; Channel normalization
;; ---------------------------------------------------------------------------

(defn- normalize-channels
  [channels]
  (cond
    (map? channels) channels

    (sequential? channels)
    (into {}
          (map-indexed
            (fn [idx {:keys [topic mode level-payload-fn targets handler immediate-fn]
                      :or {mode :perform targets [:level]}}]
              [(keyword (str "ch" idx))
               (cond-> {:topic topic :mode mode :targets targets}
                 level-payload-fn (assoc :level-payload level-payload-fn)
                 handler (assoc :handler handler)
                 immediate-fn (assoc :immediate-fn immediate-fn))])
            channels))

    :else
    (throw (IllegalArgumentException. "build-spec :channels must be vector or map"))))

(defn- resolve-initial-state
  [opts runtime]
  (let [specific (case runtime
                   :hand (:hand-initial-state opts)
                   :level (:level-initial-state opts))
        shared (:initial-state opts)
        resolved (or specific shared (effect-initial-state (:effect-id opts) runtime))]
    (if (fn? resolved) (resolved) resolved)))

;; ---------------------------------------------------------------------------
;; Public test / owner API
;; ---------------------------------------------------------------------------

(defn initial-state
  "Default runtime state for an effect (from registry or defmethod)."
  [effect-id & {:keys [runtime]}]
  (let [kind (runtime-kind effect-id)
        rt (or runtime (case kind :both :hand kind))]
    (effect-initial-state effect-id rt)))

(defn snapshot
  ([effect-id]
   (let [kind (runtime-kind effect-id)
         default-runtime (case kind :both :hand kind)]
     (snapshot effect-id {:runtime default-runtime})))
  ([effect-id {:keys [runtime] :as _opts}]
   (let [rt (or runtime (runtime-kind effect-id))
         runtimes (case rt
                    :both [:level :hand]
                    :none []
                    [rt])]
     (if (= 1 (count runtimes))
       (or (snapshot-runtime effect-id (first runtimes))
           (effect-initial-state effect-id (first runtimes)))
       (into {}
             (keep (fn [r]
                     (let [s (or (snapshot-runtime effect-id r)
                                 (effect-initial-state effect-id r))]
                       (when s [r s]))))
             runtimes)))))

(defn reset-for-test!
  [effect-id & {:keys [runtime]}]
  (let [rt (or runtime (runtime-kind effect-id))
        runtimes (case rt
                   :both [:level :hand]
                   :none []
                   [rt])]
    (doseq [r runtimes]
      (reset-runtime-state! effect-id r (effect-initial-state effect-id r)))
    nil))

(defn clear-owner!
  [effect-id owner-key & {:keys [runtime]}]
  (let [rt (or runtime (runtime-kind effect-id))
        runtimes (case rt
                   :both [:level :hand]
                   :none []
                   [rt])]
    (doseq [r runtimes]
      (update-runtime-state!
        effect-id r
        (fn [store]
          (dispatch-clear-owner! effect-id (or store (effect-initial-state effect-id r)) owner-key))))
    nil))

(defn enqueue-for-test!
  [effect-id ctx-id channel payload & {:keys [owner-key runtime]}]
  (let [rt (or runtime (runtime-kind effect-id))
        runtimes (case rt
                   :both [:level :hand]
                   :none []
                   [rt])]
    (doseq [r runtimes]
      (update-runtime-state!
        effect-id r
        (fn [store]
          (dispatch-enqueue! r effect-id
                             (or store (effect-initial-state effect-id r))
                             ctx-id channel
                             (or owner-key [:ctx ctx-id]) payload))))
    nil))

;; ---------------------------------------------------------------------------
;; build-spec
;; ---------------------------------------------------------------------------

(defn build-spec
  "Build a complete `fx-spec/register!` map, pure — no registration side
  effect. The registry entry that `init!` (from def-arc-beam-fx) later
  passes to `register-effect!` travels in the returned map under the
  private key `::arc-entry`; `init!` strips it before calling
  `fx-spec/register!`.

  Required: `:effect-id`, `:channels`
  Runtime: `:runtime` — :level (default), :hand, :both, :none
  State: `:initial-state`, or `:level-initial-state` / `:hand-initial-state` for :both
  FOV: `:fov-offset-fn` — optional (fn [player-uuid] -> number|nil), queried
  per frame by level-effects/current-fov-offset for the camera zoom
  Arc opts (default impl): `:sound-id`, `:arc-life`, `:arc-pattern`, `:aoe-points?`,
  `:hand-origin?` (arc is cast from the player — shift it out of their eye into
  their hand, as the original's ViewOptimize.fix does)
  Hand: `:transform-fn` — static fn or keyword dispatching effect-transform-fn

  Every :level/:both effect gets a :build-plan-fn wired to the
  effect-build-plan multimethod — effects with arc-opts render the default
  arc via the :default method; effects with a custom `defmethod
  effect-build-plan :<id>` render that; effects with neither get nil every
  call (idle-gated by level-effects, so this costs one multimethod dispatch
  only while the effect has live state, never while idle)."
  [opts]
  (let [effect-id (:effect-id opts)
        runtime (or (:runtime opts) :level)
        arc-opts (when (some opts [:sound-id :sound-source :sound-volume :sound-pitch :arc-life :arc-pattern
                                   :aoe-points? :hand-origin?])
                   (merge {:effect-id effect-id :arc-pattern :weak :arc-life 10}
                          (select-keys opts [:effect-id :sound-id :sound-source :sound-volume :sound-pitch
                                             :arc-life :arc-pattern :aoe-points? :hand-origin?])))
        channels (normalize-channels (:channels opts))
        transform (or (:transform-fn opts)
                        (when (= runtime :hand)
                          #(effect-transform-fn effect-id)))]
    (when-not (keyword? effect-id)
      (throw (IllegalArgumentException. "build-spec requires :effect-id keyword")))
    (-> (cond-> {:id effect-id :channels channels}
          (not= runtime :none)
          (as-> spec spec
                (if (contains? #{:level :both} runtime)
                  (let [level-handler (cond-> {:initial-state (resolve-initial-state opts :level)
                                               :enqueue-state-fn #(dispatch-enqueue! :level effect-id %1 %2 %3 %4 %5)
                                               :tick-state-fn #(dispatch-tick! :level effect-id %1)
                                               :build-plan-fn (fn [cam pos tick query-fn]
                                                               (effect-build-plan effect-id cam pos tick query-fn))
                                               :clear-owner-fn #(dispatch-clear-owner! effect-id %1 %2)}
                                        (:fov-offset-fn opts)
                                        (assoc :fov-offset-fn (:fov-offset-fn opts)))]
                    (assoc spec :level level-handler))
                  spec)
                (if (contains? #{:hand :both} runtime)
                  (assoc spec :hand
                         {:initial-state (resolve-initial-state opts :hand)
                          :enqueue-state-fn #(dispatch-enqueue! :hand effect-id %1 %2 %3 %4 %5)
                          :tick-state-fn #(dispatch-tick! :hand effect-id %1)
                          :transform-fn transform})
                  spec)))
        (assoc ::arc-entry
               {:runtime runtime
                :arc-opts arc-opts
                :initial-state (:initial-state opts)
                :level-initial-state (:level-initial-state opts)
                :hand-initial-state (:hand-initial-state opts)
                :after-register (:after-register opts)}))))

;; ---------------------------------------------------------------------------
;; Per-skill FX boilerplate
;; ---------------------------------------------------------------------------

(defmacro def-arc-beam-fx
  "Declare the standard `init!` / `fx-snapshot` / `reset-fx-for-test!` /
  `clear-fx-owner!` quartet for one skill's FX namespace, given its
  (already-defined, via build-spec) `spec` var and :effect-id keyword.
  Expands in the calling (per-skill FX) namespace, so every skill FX file
  collapses these four near-identical forms into one macro invocation.

  init! does all load-time-deferred registration for this effect:
  - process-once! loads the shared defmethod impl namespace (JVM-level
    dispatch table, must not redo on Framework reinjection) — was a bare
    top-level (require ...) at the bottom of this file; each effect's
    init! now triggers it exactly once per process instead.
  - register-effect! populates the (still process-local, P3-pending)
    effect-registry from the ::arc-entry build-spec attached.
  - fx-spec/register! registers the public fx-spec, stripped of the
    private ::arc-entry key."
  [effect-kw]
  `(do
     (defn ~'init! []
       (install/process-once! ::arc-beam-impls
         #(require 'cn.li.ac.ability.client.fx-templates.arc-beam.impl.load))
       (register-effect! ~effect-kw (get ~'spec ::arc-entry))
       (fx-spec/register! (dissoc ~'spec ::arc-entry))
       nil)
     (defn ~'fx-snapshot [] (snapshot ~effect-kw))
     (defn ~'reset-fx-for-test! [] (reset-for-test! ~effect-kw))
     (defn ~'clear-fx-owner! [owner-key#] (clear-owner! ~effect-kw owner-key#))))
