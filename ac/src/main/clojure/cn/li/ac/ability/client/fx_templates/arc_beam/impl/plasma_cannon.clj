(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.plasma-cannon
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.effects.arc-fx :as arc-fx]
            [cn.li.ac.ability.client.effects.beam-ops :as fx-beam]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.tornado :as tornado]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.ability.client.runtime :as client-runtime]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.util.log :as log]
            [clojure.string :as str]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

(def ^:private loop-sound (modid/namespaced-path "vecmanip.plasma_cannon"))
(def ^:private charged-sound (modid/namespaced-path "vecmanip.plasma_cannon_t"))
(defn- loop-sound-key [ctx-id] (str "plasma-cannon/" ctx-id))

(defn- local-player?
  "Upstream guards the charged cue with `isLocal` — only the caster's own
  client plays it, even though every nearby client runs the context."
  [source-player-id]
  (boolean (when source-player-id
             (= (str source-player-id) (str (client-bridge/local-player-uuid))))))

;; ---------------------------------------------------------------------------
;; Charge tornado (original PlasmaCannon's private Tornado LocalEntity)
;; ---------------------------------------------------------------------------

;; Original: new TornadoEffect(12, 8, 1, 0.3) — one column, far bigger than
;; Storm Wing's four, standing on the ground under the charge position.
(def ^:private tornado-params {:ht 12.0 :sz 8.0 :dscale 0.3})

;; Tornado.alpha: fade in over the first 20 ticks, and once dead fade back out
;; over 20 more (setDead at 30). onUpdate then halves it: alpha * 0.5.
(def ^:private tornado-fade-ticks 20.0)
(def ^:private tornado-dead-ticks 30)

;; ---------------------------------------------------------------------------
;; Plasma body (original PlasmaBodyEffect + PlasmaBodyRenderer)
;; ---------------------------------------------------------------------------

(defn- ranged ^double [^double lower ^double upper]
  (+ lower (* (rand) (- upper lower))))

(defn- trig-par
  "PlasmaBodyEffect.nextTrigPar(size): an orbit amplitude scaled by `size`, a
  slow angular speed in rad/SECOND, and a fixed phase offset."
  [^double size]
  {:amp (* (ranged 1.4 2.0) size)
   :speed (ranged 0.5 0.7)
   :dphase (ranged 0.0 (* 2.0 Math/PI))})

(defn- new-balls
  "PlasmaBodyEffect's constructor: 4 core balls (size 1..1.5, centre ±1.5,
  orbit amplitude 1.4..2) plus rangei(4,6) → 4-5 satellites (size 0.1..0.3,
  centre ±3, orbit amplitude ×2.5). Fixed per effect instance, like upstream."
  []
  (letfn [(ball [^double size-lo ^double size-hi ^double spread ^double amp-scale]
            {:size (ranged size-lo size-hi)
             :center [(ranged (- spread) spread)
                      (ranged (- spread) spread)
                      (ranged (- spread) spread)]
             :hmove (trig-par amp-scale)
             :vmove (trig-par amp-scale)})]
    (vec (concat (repeatedly 4 #(ball 1.0 1.5 1.5 1.0))
                 (repeatedly (+ 4 (rand-int 2)) #(ball 0.1 0.3 3.0 2.5))))))

(defn- ball-positions
  "PlasmaBodyRenderer's per-ball offset: dx/dz trace a circle of radius
  hmove.amp, dy a sine of amplitude vmove.amp.

  `seconds` is the time since the effect spawned. Upstream passes its
  `deltaTime` here, but `updateAlpha` resets `initTime` every frame, so what it
  actually passes is the frame delta — the cluster is frozen at its dphase
  offsets. Feeding the accumulated time instead is what the trig parameters
  (speed in rad/s, i.e. a ~10s period) are clearly written for."
  [{:keys [x y z]} balls ^double seconds]
  (mapv (fn [{:keys [size center hmove vmove]}]
          (let [[cx cy cz] center
                hp (- (* (double (:speed hmove)) seconds) (double (:dphase hmove)))
                vp (- (* (double (:speed vmove)) seconds) (double (:dphase vmove)))
                ha (double (:amp hmove))]
            {:x (+ (double x) (double cx) (* ha (Math/sin hp)))
             :y (+ (double y) (double cy) (* (double (:amp vmove)) (Math/sin vp)))
             :z (+ (double z) (double cz) (* ha (Math/cos hp)))
             :size (double size)}))
        balls))

;; PlasmaBodyEffect.updateAlpha: moveTowards(alpha, terminated ? 0 : 1,
;; dt * (terminated ? 1 : 0.3)) — real seconds, so 0.3/s in and 1.0/s out.
;; One client tick is 0.05s.
(def ^:private plasma-fade-in-per-tick (* 0.05 0.3))
(def ^:private plasma-fade-out-per-tick (* 0.05 1.0))
;; onUpdate: setDead once terminated and |alpha| <= 1e-3.
(def ^:private plasma-dead-alpha 1.0e-3)

(defn- move-towards ^double [^double from ^double to ^double max-step]
  (let [delta (- to from)]
    (+ from (* (Math/min (Math/abs delta) max-step) (Math/signum delta)))))

(defn- tick-plasma-alpha
  [st]
  (let [terminated? (boolean (:terminated? st))
        alpha (double (or (:plasma-alpha st) 0.0))]
    (assoc st :plasma-alpha (move-towards alpha
                                          (if terminated? 0.0 1.0)
                                          (if terminated?
                                            plasma-fade-out-per-tick
                                            plasma-fade-in-per-tick)))))

;; One client tick, in wall-clock milliseconds.
(def ^:private tick-ms 50.0)

(defn- move-charge-pos
  "Record a new authoritative position and keep the one it replaced, so the
  render can interpolate across the tick it happened in."
  [st pos]
  (assoc st
         :prev-charge-pos (:charge-pos st)
         :charge-pos pos
         :moved-ms (System/currentTimeMillis)))

(defn- rendered-charge-pos
  "Vanilla renders an entity at lastTickPos + (pos - lastTickPos) * partialTick,
  which is the only reason upstream's body glides: it moves a whole block per
  tick, 20 times a second. The level-effect plan gets whole game ticks, no
  partial, so interpolate over the 50ms since the move landed instead — same
  curve, at most one tick behind."
  ([st] (rendered-charge-pos st (System/currentTimeMillis)))
  ([st now-ms]
  (let [prev (:prev-charge-pos st)
        cur (:charge-pos st)]
    (if-not (and (map? prev) (map? cur))
      cur
      (let [t (Math/max 0.0 (Math/min 1.0 (/ (double (- (long now-ms)
                                                        (long (or (:moved-ms st) 0))))
                                             tick-ms)))]
        {:x (+ (double (:x prev)) (* t (- (double (:x cur)) (double (:x prev)))))
         :y (+ (double (:y prev)) (* t (- (double (:y cur)) (double (:y prev)))))
         :z (+ (double (:z prev)) (* t (- (double (:z cur)) (double (:z prev)))))})))))

;; Reconciliation budget: the error below which the prediction is simply
;; correct, how much of a real error may be absorbed per tick, and the error
;; beyond which this is not an error but a desync (missed packets, a re-aimed
;; shot) and the body snaps.
;;
;; The deadband matters as much as the rate. Client and server advance the same
;; block per tick, and the client starts exactly as late as the sync it
;; receives is stale, so in steady state the two agree to within the ±1 tick of
;; message timing jitter. Chasing that jitter is what remained of the stutter:
;; a 25% speed surge every 250ms, periodic, always forward.
(def ^:private sync-deadband 1.5)
(def ^:private max-correction-per-tick 0.15)
(def ^:private desync-snap-distance 8.0)

(defn- vsub [a b]
  {:x (- (double (:x a)) (double (:x b)))
   :y (- (double (:y a)) (double (:y b)))
   :z (- (double (:z a)) (double (:z b)))})

(defn- vlen ^double [v]
  (Math/sqrt (+ (* (double (:x v)) (double (:x v)))
                (* (double (:y v)) (double (:y v)))
                (* (double (:z v)) (double (:z v))))))

(defn- reconcile-sync
  "Fold an authoritative position into the client's own prediction.

  Both sides walk the same deterministic straight line at one block per tick,
  but the client only starts when the fire message lands, so it runs a constant
  latency behind and every sync used to yank that gap shut — a lurch four times
  a second, which is what the flight stuttered on. Keep the predicted position
  and bleed the error off a quarter block per tick instead; the body converges
  within half a second without ever changing apparent speed by more than 25%."
  [st pos]
  (let [cur (:charge-pos st)]
    (if-not (and (= :go (:state st)) (map? cur))
      (move-charge-pos st pos)
      (let [error (vsub pos cur)
            len (vlen error)]
        (cond
          (> len desync-snap-distance) (move-charge-pos st pos)
          (<= len sync-deadband) (dissoc st :sync-error)
          :else (assoc st :sync-error error))))))

(defn- step-toward
  "One tryMove step plus at most `max-correction-per-tick` of the standing sync
  error. Returns [next-pos remaining-error]."
  [pos destination error]
  (let [d (vsub destination pos)
        len (vlen d)
        step (if (< len 1.0)
               {:x 0.0 :y 0.0 :z 0.0}
               {:x (/ (double (:x d)) len)
                :y (/ (double (:y d)) len)
                :z (/ (double (:z d)) len)})
        err-len (vlen error)
        corr (if (<= err-len max-correction-per-tick)
               error
               (let [k (/ max-correction-per-tick err-len)]
                 {:x (* k (double (:x error)))
                  :y (* k (double (:y error)))
                  :z (* k (double (:z error)))}))]
    [{:x (+ (double (:x pos)) (double (:x step)) (double (:x corr)))
      :y (+ (double (:y pos)) (double (:y step)) (double (:y corr)))
      :z (+ (double (:z pos)) (double (:z step)) (double (:z corr)))}
     (vsub error corr)]))

(defn- advance-flight
  "PlasmaCannonContextC.c_tick: while the shot is flying the client runs the
  same tryMove as the server (1 block/tick toward the destination) instead of
  waiting for the 5-tick position sync, which would otherwise leave the body a
  quarter second behind."
  [st]
  (let [{:keys [charge-pos destination]} st
        error (or (:sync-error st) {:x 0.0 :y 0.0 :z 0.0})]
    (if-not (and (= :go (:state st))
                 (not (:terminated? st))
                 (map? charge-pos) (map? destination))
      st
      (let [[next-pos remaining] (step-toward charge-pos destination error)]
        (if (and (= next-pos charge-pos) (zero? (vlen error)))
          st
          (assoc (move-charge-pos st next-pos) :sync-error remaining))))))

(defn- finished?
  "Both halves have played out their death: the body's alpha has reached 0
  (upstream setDead) and the tornado has been dropped 30 ticks after dying."
  [st]
  (and (:terminated? st)
       (<= (double (or (:plasma-alpha st) 0.0)) plasma-dead-alpha)
       (nil? (:tornado st))))


(defn- tornado-base-fallback
	"Used only when the start payload carried no ground point: the original's
	miss case puts the column at the bottom of the 20-block downward ray."
	[charge-pos]
	(when (map? charge-pos)
		{:x (double (:x charge-pos))
		 :y (- (double (:y charge-pos)) 20.0)
		 :z (double (:z charge-pos))}))

(defn- tornado-alpha
	"Tornado.alpha times the 0.5 applied in onUpdate. Fades in over the first 20
	ticks; once dead (original: `ctx.state == STATE_GO`) it fades back out over
	20 more."
	^double [^long ticks ^long dead-ticks dead?]
	(* 0.5
		 (if dead?
			 (max 0.0 (- 1.0 (/ (double dead-ticks) tornado-fade-ticks)))
			 (min 1.0 (/ (double ticks) tornado-fade-ticks)))))

(defn- tick-tornado
	"Advance one owner's charge tornado: age it, sample its rings for this tick,
	and drop it 30 ticks after it died (original setDead)."
	[st ^long ticks]
	(if-not (:tornado st)
		st
		;; Original: dead = (ctx.state == STATE_GO || ctx.getStatus == TERMINATED).
		(let [dead? (boolean (or (:tornado-dead? st) (= :go (:state st)) (:terminated? st)))
					dead-ticks (if dead? (inc (long (or (:tornado-dead-ticks st) 0))) 0)]
			(if (>= dead-ticks tornado-dead-ticks)
				(dissoc st :tornado :tornado-rings :tornado-alpha :tornado-base)
				(assoc st
							 :tornado-dead? dead?
							 :tornado-dead-ticks dead-ticks
							 :tornado-alpha (tornado-alpha ticks dead-ticks dead?)
							 :tornado-rings (tornado/ring-states (tornado/effect-time ticks)
																									 tornado-params
																									 (:tornado st)))))))

(defn- enqueue-state!
	[store ctx-id channel owner-key payload]
	(let [store* (or store {:effect-state {}})
				owner-key* (or owner-key [:ctx ctx-id])
				{:keys [mode charge-ticks fully-charged? release-ready? charge-pos tornado-base
								flight-ticks state destination pos performed? source-player-id world-id]} (or payload {})
				base-meta {:owner-key owner-key*
									 :queue-owner (client-particles/current-effect-owner)
									 :ctx-id ctx-id
									 :channel channel
									 :source-player-id source-player-id
									 :world-id world-id}]
		(case mode
			:start
			(do
				;; Original c_begin: `new FollowEntitySound(player, ..., AMBIENT)`
				;; without setLoop() — one playback of the 5.9s charge clip that
				;; follows the caster and is cut short by stop() on terminate.
				(client-bridge/run-client-effect!
				 :mcmod/start-loop-sound-at-player
				 {:key (loop-sound-key ctx-id)
				  :sound-id loop-sound
				  :owner-uuid (str source-player-id)
				  :volume 1.0
				  :pitch 1.0
				  :loop? false})
				(assoc-in store* [:effect-state owner-key*]
									(merge base-meta
												 {:active? true :charge-ticks 0 :charge-pos (:charge-pos payload)
												:flight-ticks 0 :state :charging :destination nil
												:performed? false :terminated? false
												;; PlasmaBodyEffect: fixed ball cluster, alpha ramping from 0.
												:balls (new-balls)
												:plasma-alpha 0.0
												;; Original c_begin also spawns the Tornado entity, seated on
												;; the ground under the charge position and fading in from 0.
												:tornado (tornado/new-column tornado-params)
												:tornado-base (or tornado-base (tornado-base-fallback charge-pos))
												:tornado-dead-ticks 0})))
			:update
			(let [prev-st (or (get-in store* [:effect-state owner-key*]) {})
						;; Only overwrite what this payload actually carries. The 5-tick
						;; flight sync sends :charge-pos and :flight-ticks alone, so
						;; defaulting the rest reset :state to :charging — which switched
						;; off the client's own tryMove and left the body jumping five
						;; blocks per sync instead of gliding one per tick.
						synced (cond-> prev-st
										 (some? charge-ticks) (assoc :charge-ticks (long charge-ticks))
										 (some? flight-ticks) (assoc :flight-ticks (long flight-ticks))
										 (some? state) (assoc :state state)
										 (some? destination) (assoc :destination destination)
										 (some? release-ready?) (assoc :release-ready? (boolean release-ready?)))
						;; Position last, so the fields above (notably :state) already
						;; describe this update when the sync is reconciled against the
						;; client's prediction.
						synced (if (map? charge-pos) (reconcile-sync synced charge-pos) synced)
						updated (assoc-in store* [:effect-state owner-key*]
												(assoc (merge base-meta synced)
													 :owner-key owner-key*
													 :ctx-id ctx-id
													 :channel channel
													 :source-player-id source-player-id
													 :world-id world-id
													 :active? true
													 :state (or (:state synced) :charging)))]
					;; Original plays the charged cue from l_tick under `if (isLocal)`
					;; — the caster hears it, bystanders do not.
					(when (and (boolean fully-charged?)
										 (local-player? source-player-id))
						(client-sounds/queue-sound-effect! (:queue-owner base-meta)
							{:type :sound :sound-id charged-sound :volume 0.5 :pitch 1.0}))
					updated)
			:perform
			(do
				(when (map? pos)
					(let [tx (double (:x pos)) ty (double (:y pos)) tz (double (:z pos))]
						(client-particles/queue-particle-effect! (:queue-owner base-meta)
							{:type :particle :particle-type :explosion-large
							 :x tx :y ty :z tz :count 1 :speed 0.0
							 :offset-x 0.0 :offset-y 0.0 :offset-z 0.0})
						(dotimes [_ 12]
							(client-particles/queue-particle-effect! (:queue-owner base-meta)
								{:type :particle :particle-type :smoke-large
								 :x (+ tx (- (* (rand) 10.0) 5.0))
								 :y (+ ty (- (* (rand) 5.0) 2.5))
								 :z (+ tz (- (* (rand) 10.0) 5.0))
								 :count 1 :speed (+ 0.1 (* (rand) 0.3))
								 :offset-x 0.5 :offset-y 0.5 :offset-z 0.5}))
						;; No explosion sound here: the server-side Explosion already
						;; plays vanilla's, and queuing another only doubled it up.
						))
				store*)
			:end
			(do
				;; Original c_terminate: sound.stop()
				(client-bridge/run-client-effect!
				 :mcmod/stop-loop-sound
				 {:key (loop-sound-key ctx-id)})
				;; Upstream's two entities outlive the context: the body fades to 0
				;; and only then setDead, the tornado fades for 20 ticks and is
				;; removed at 30. Keep the state (and keep it :active? so it goes on
				;; ticking) and let tick-state! drop it once both have played out.
				(if-let [st (get-in store* [:effect-state owner-key*])]
					(assoc-in store* [:effect-state owner-key*]
										(assoc st :terminated? true :performed? (boolean performed?)))
					store*))
			store*)))

(defn- tick-state!
	[store]
	(let [store* (or store {:effect-state {}})]
		(update store* :effect-state
			(fn [states]
				(store-tick/map-active-states
				 states
				 (fn [_owner-key st]
					 (let [ticks (inc (long (or (:ticks st) 0)))
								 st (-> (assoc st :ticks ticks)
												advance-flight
												tick-plasma-alpha
												(tick-tornado ticks))]
						 ;; The charge sound is one FollowEntitySound started on :start
						 ;; and stopped on :end — no re-queue.
						 (let [cp (:charge-pos st)]
							 (when (and cp (= :go (:state st)) (not (:terminated? st)))
								 (client-particles/queue-particle-effect! (:queue-owner st)
									 {:type :particle :particle-type :flame
										:x (double (:x cp)) :y (double (:y cp)) :z (double (:z cp))
										:count 4 :speed 0.2
										:offset-x 0.5 :offset-y 0.5 :offset-z 0.5})))
						 (when-not (finished? st) st))))))))

(defn- tornado-ops
	"The charge tornado's ring quads. TornadoEntityRenderer only translates to
	the entity position before TornadoRenderer.doRender, so the column stands
	upright in world space — no rotation to fold in, unlike Storm Wing's."
	[st]
	(let [base (:tornado-base st)
				rings (:tornado-rings st)]
		(when (and (map? base) (seq rings))
			(tornado/ring-quad-ops (double (:x base)) (double (:y base)) (double (:z base))
														 tornado/identity-linear
														 (double (or (:tornado-alpha st) 0.0))
														 rings))))

(defn- plasma-state-ops
	"One PlasmaBodyEffect draw. The renderer's `alpha` uniform is upstream's
	`math.pow(eff.alpha, 2)`, not the raw fade value."
	[st]
	(let [cp (rendered-charge-pos st)
				alpha (double (or (:plasma-alpha st) 0.0))]
		(when (and (map? cp) (> alpha plasma-dead-alpha))
			[{:kind :plasma-body
				:center {:x (double (:x cp)) :y (double (:y cp)) :z (double (:z cp))}
				:alpha (* alpha alpha)
				:balls (ball-positions cp (:balls st) (* 0.05 (double (or (:ticks st) 0))))}])))

(defn- build-plan
	[_camera-pos _hand-center-pos _tick]
	(let [state (vals (:effect-state (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :plasma-cannon)))
				ops (mapcat (fn [st] (concat (tornado-ops st) (plasma-state-ops st))) state)]
		(when (seq ops)
			{:ops (vec ops)})))


(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:plasma-cannon :level] [_ _] {:effect-state {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:plasma-cannon :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:plasma-cannon :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :plasma-cannon
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :plasma-cannon [_ store owner-key]
  (client-bridge/run-client-effect!
   :mcmod/stop-loop-sound
   {:key (loop-sound-key (second owner-key))})
  ;; Context termination is exactly upstream's TERMINATED signal, not a reason
  ;; to delete the visuals: both entities outlive it while they fade. Mark and
  ;; let tick-state! drop the entry once they are done (:end does the same, and
  ;; whichever arrives first wins).
  (if-let [st (get-in store [:effect-state owner-key])]
    (assoc-in store [:effect-state owner-key] (assoc st :terminated? true))
    store))

;; ---------------------------------------------------------------------------
;; Slot delegate state (upstream PlasmaCannonContext.getState)
;; ---------------------------------------------------------------------------

(defn charge-visual-state
  "CHARGE until the charge completes, ACTIVE from then on (including flight) —
  upstream's IStateProvider.getState. nil when this player has no plasma cannon
  context, so the slot falls back to :idle."
  [player-uuid]
  (when player-uuid
    (let [states (vals (:effect-state (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :plasma-cannon)))]
      (when-let [st (first (filter (fn [st]
                                     (and (not (:terminated? st))
                                          (= (str (:source-player-id st)) (str player-uuid))))
                                   states))]
        (if (or (:release-ready? st) (= :go (:state st))) :active :charge)))))
