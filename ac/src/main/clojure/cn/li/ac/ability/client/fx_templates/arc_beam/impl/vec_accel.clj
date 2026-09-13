(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.vec-accel
  (:require [cn.li.ac.ability.client.effects.arc-fx :as arc-fx]
            [cn.li.ac.ability.client.effects.beam-ops :as fx-beam]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.ability.client.runtime :as client-runtime]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [clojure.string :as str]
            [cn.li.ac.ability.client.fx-templates.arc-beam])
  (:import [cn.li.mcmod.math V3]))

(def ^:private sound-id (modid/namespaced-path "vecmanip.vec_accel"))









(defn- integrate-trajectory-positions
	"Physics integration (drag 0.98/step, gravity -1.9*dt) is purely a function
	of init-vel — it does not depend on camera-pos/look-dir, so it is
	precomputed once here (enqueue time, on :start/:update) instead of once
	per frame in build-plan. Returns 100 positions (idx 0..99) relative to a
	(0,0,0) origin; build-plan translates by the live start position."
	[^V3 init-vel]
	(let [dt 0.02]
		(loop [^V3 pos (vec3/v3 0.0 0.0 0.0)
					 ^V3 vel init-vel
					 positions (transient [pos])
					 idx 0]
			(if (>= idx 99)
				(persistent! positions)
				(let [vel2 (vec3/v3 (* (.-x vel) 0.98) (* (.-y vel) 0.98) (* (.-z vel) 0.98))
							pos2 (vec3/v3 (+ (.-x pos) (* (.-x vel2) dt))
														(+ (.-y pos) (* (.-y vel2) dt))
														(+ (.-z pos) (* (.-z vel2) dt)))
							vel3 (vec3/v3 (.-x vel2) (- (.-y vel2) (* dt 1.9)) (.-z vel2))]
					(recur pos2 vel3 (conj! positions pos2) (inc idx)))))))

(defn- start-offset
	"First-person weapon-relative start offset, derived from look-dir alone —
	independent of the live camera position, so it is precomputed alongside
	the trajectory instead of every frame. Folds in the constant eye-height
	offset (-1.62) that build-plan previously applied to camera-pos.-y."
	[^V3 look-dir]
	(let [lx (.-x look-dir) ly (.-y look-dir) lz (.-z look-dir)
				horiz-len (Math/sqrt (+ (* lx lx) (* lz lz)))
				safe-h (max 1.0e-8 horiz-len)
				rot-x (* (/ lz safe-h) -0.08)
				rot-z (* (/ (- lx) safe-h) -0.08)]
		(vec3/v3 (- rot-x (* lx 0.12))
						 (- 1.56 (* ly 0.12) 1.62)
						 (- rot-z (* lz 0.12)))))

(defn- precompute-trajectory
	[st]
	(let [look-dir-v (vec3/map->v3 (or (:look-dir st) {:x 0.0 :y 0.0 :z 1.0}))
				init-vel-v (vec3/map->v3 (or (:init-vel st) {:x 0.0 :y 0.0 :z 1.0}))]
		(assoc st
					 :start-offset (start-offset look-dir-v)
					 :trajectory-positions (integrate-trajectory-positions init-vel-v))))

(defn- enqueue-state!
	[store ctx-id channel owner-key payload]
	(let [store* (or store {:effect-state {}})
				owner-key* (or owner-key [:ctx ctx-id])
				{:keys [mode charge-ticks can-perform? look-dir init-vel source-player-id world-id]} (or payload {})
				base-meta {:owner-key owner-key*
									 :ctx-id ctx-id
									 :channel channel
									 :source-player-id source-player-id
									 :world-id world-id}]
		(case mode
			:start
			(assoc-in store* [:effect-state owner-key*]
								(precompute-trajectory
									(merge base-meta
												 {:active? true :charge-ticks 0
													:can-perform? false
													:look-dir {:x 0.0 :y 0.0 :z 1.0}
													:init-vel {:x 0.0 :y 0.0 :z 1.0}})))
			:update
			(update-in store* [:effect-state owner-key*]
				(fn [st]
					(precompute-trajectory
						(assoc (merge base-meta (or st {:active? true}))
									 :owner-key owner-key*
									 :ctx-id ctx-id
									 :channel channel
									 :source-player-id source-player-id
									 :world-id world-id
									 :active? true
									 :charge-ticks (long (or charge-ticks 0))
									 :can-perform? (boolean can-perform?)
									 :look-dir (or look-dir {:x 0.0 :y 0.0 :z 1.0})
									 :init-vel (or init-vel {:x 0.0 :y 0.0 :z 1.0})))))
			:perform
			(do
				(client-sounds/queue-current-sound-effect!
					{:type :sound :sound-id sound-id :volume 0.35 :pitch 1.0})
				store*)
			:end
			(update store* :effect-state dissoc owner-key*)
			store*)))

(defn- tick-state!
	[store]
	(or store {:effect-state {}}))

(defn- trajectory-ops
	[^V3 camera-pos effect]
	(when (and effect (:active? effect) (:start-offset effect) (seq (:trajectory-positions effect)))
		(let [can-do?   (boolean (:can-perform? effect))
					^V3 start-pos (vec3/v+ camera-pos (:start-offset effect))
					positions (:trajectory-positions effect)
					h         0.02]
			(loop [idx 1
						 ops (transient [])]
				(if (>= idx 99)
					(persistent! ops)
					(recur (inc idx)
								 (let [a-int (fx-beam/fade-alpha idx)]
									 (if (> a-int 0)
										 (let [^V3 prev (vec3/v+ start-pos (nth positions (dec idx)))
													 ^V3 pos  (vec3/v+ start-pos (nth positions idx))
													 color (fx-beam/rgba
																	 (if can-do?
																		 {:r 255 :g 255 :b 255}
																		 {:r 255 :g 51 :b 51})
																	 a-int)
													 ;; Original ParabolaRenderer GL_QUADS texcoords: u runs
													 ;; prev→cur along the trajectory, v across the ribbon
													 ;; height. The glow_line texture's soft vertical
													 ;; gradient (alpha 0→255→0) then falls off across the
													 ;; ribbon's top/bottom edges, and its constant-u axis
													 ;; keeps every segment uniform along the path. The
													 ;; earlier corner order swapped u/v: the gradient was
													 ;; compressed into each 0.02s segment (every quad read
													 ;; as its own fading vertical bar) and the ribbon edges
													 ;; were hard — the "flat line of dashes" look.
													 p0 (vec3/v3 (.-x prev) (+ (.-y prev) h) (.-z prev))
													 p1 (vec3/v3 (.-x prev) (- (.-y prev) h) (.-z prev))
													 p2 (vec3/v3 (.-x pos) (- (.-y pos) h) (.-z pos))
													 p3 (vec3/v3 (.-x pos) (+ (.-y pos) h) (.-z pos))]
											 (conj! ops (fx-beam/glow-line-quad-op p0 p1 p2 p3 color)))
										 ops))))))))

(defn- build-plan
	[camera-pos hand-center-pos _tick]
	;; Original ParabolaRenderer only draws in first person
	;; (thirdPersonView == 0) — in F5 the line's camera-anchored start
	;; would sit detached from the player.
	(if-not (:first-person? hand-center-pos true)
		nil
		(let [^V3 cam-v (vec3/map->v3 camera-pos)
					ops (mapcat #(trajectory-ops cam-v %)
											(filter #(get % :active?) (vals (:effect-state (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :vec-accel)))))]
			(when (seq ops)
				{:ops (vec ops)}))))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:vec-accel :level] [_ _] {:effect-state {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:vec-accel :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:vec-accel :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :vec-accel
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :vec-accel [_ store owner-key]
  (update store :effect-state dissoc owner-key))
