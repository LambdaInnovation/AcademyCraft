(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.blood-retrograde
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.effects.arc-fx :as arc-fx]
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

(def ^:private sound-id (modid/namespaced-path "vecmanip.blood_retro"))
(def ^:private splash-life 10)
(def ^:private spray-life 1200)









(defn- enqueue-state!
	[store ctx-id channel owner-key payload]
	(let [store* (or store {:effect-state {} :splashes {} :sprays {}})
				owner-key* (or owner-key [:ctx ctx-id])
				{:keys [mode ticks charge-ratio performed? sound-pos splashes sprays source-player-id world-id]}
				(or payload {})
				base-meta {:owner-key owner-key*
									 :ctx-id ctx-id
									 :channel channel
									 :source-player-id source-player-id
									 :world-id world-id}]
		(case mode
			:start
			(assoc-in store* [:effect-state owner-key*]
								(merge base-meta {:active? true :ticks 0 :charge-ratio 0.0 :performed? false}))
			:update
			(assoc-in store* [:effect-state owner-key*]
								(assoc (merge base-meta (or (get-in store* [:effect-state owner-key*]) {}))
											 :owner-key owner-key*
											 :ctx-id ctx-id
											 :channel channel
											 :source-player-id source-player-id
											 :world-id world-id
											 :active? true
											 :ticks (long (or ticks 0))
											 :charge-ratio (double (or charge-ratio 0.0))
											 :performed? false))
			:perform
			(let [existing (or (get-in store* [:effect-state owner-key*]) {})
						splash-events (mapv (fn [splash]
																	(merge base-meta splash {:ttl splash-life :max-ttl splash-life}))
																(or splashes (:splashes payload)))
						spray-events (mapv (fn [spray]
																 (merge base-meta spray {:ttl spray-life :max-ttl spray-life}))
															 (or sprays (:sprays payload)))
						updated-store (-> store*
															(update-in [:splashes owner-key*] (fnil into []) splash-events)
															(update-in [:sprays owner-key*] (fnil into []) spray-events))]
				(when (or (seq splash-events) (seq spray-events))
					(client-sounds/queue-current-sound-effect!
						{:type :sound :sound-id sound-id :volume 1.0 :pitch 1.0
						 :x (double (or (some-> sound-pos :x) 0.0))
						 :y (double (or (some-> sound-pos :y) 0.0))
						 :z (double (or (some-> sound-pos :z) 0.0))}))
				(assoc-in updated-store [:effect-state owner-key*]
									(assoc (merge base-meta existing)
												 :owner-key owner-key*
												 :ctx-id ctx-id
												 :channel channel
												 :source-player-id source-player-id
												 :world-id world-id
												 :active? true
												 :performed? true)))
			:end
			(assoc-in store* [:effect-state owner-key*]
								(assoc (merge base-meta (or (get-in store* [:effect-state owner-key*]) {}))
											 :owner-key owner-key*
											 :ctx-id ctx-id
											 :channel channel
											 :source-player-id source-player-id
											 :world-id world-id
											 :active? false
											 :ticks 0
											 :charge-ratio 0.0
											 :performed? (boolean performed?)))
			store*)))

(defn- tick-state!
	[store]
	(let [store* (or store {:effect-state {} :splashes {} :sprays {}})]
		(assoc store*
					 :effect-state (store-tick/keep-active-inc-ticks (:effect-state store*))
					 :splashes (store-tick/tick-ttl-items-by-owner (:splashes store*))
					 :sprays (store-tick/tick-ttl-items-by-owner (:sprays store*)))))

(defn- splash-ops [^V3 cam-pos {:keys [x y z size ttl max-ttl]}]
	(let [center (vec3/v3 (double x) (double y) (double z))
				half-size (* 0.5 (double (or size 1.0)))
				right (ru/camera-facing-right-axis center cam-pos)
				up (ru/billboard-up-axis center cam-pos right)
				side (vec3/v* right half-size)
				lift (vec3/v* up half-size)
				p0 (vec3/v+ (vec3/v- center side) lift)
				p1 (vec3/v+ (vec3/v+ center side) lift)
				p2 (vec3/v- (vec3/v+ center side) lift)
				p3 (vec3/v- (vec3/v- center side) lift)
				age (long (- (long (or max-ttl splash-life)) (long (or ttl splash-life))))
				frame (max 0 (min 9 age))]
		[(ru/quad-op (str (modid/namespaced-path "textures/effects/blood_splash/") frame ".png")
								 p0 p1 p2 p3
								 {:r 213 :g 29 :b 29 :a 200})]))

(defn- spray-face-basis [face]
	(case face
		:up    [(vec3/v3 0.0 1.0 0.0) (vec3/v3 1.0 0.0 0.0) (vec3/v3 0.0 0.0 1.0)]
		:down  [(vec3/v3 0.0 -1.0 0.0) (vec3/v3 1.0 0.0 0.0) (vec3/v3 0.0 0.0 -1.0)]
		:north [(vec3/v3 0.0 0.0 -1.0) (vec3/v3 1.0 0.0 0.0) (vec3/v3 0.0 1.0 0.0)]
		:south [(vec3/v3 0.0 0.0 1.0) (vec3/v3 -1.0 0.0 0.0) (vec3/v3 0.0 1.0 0.0)]
		:west  [(vec3/v3 -1.0 0.0 0.0) (vec3/v3 0.0 0.0 -1.0) (vec3/v3 0.0 1.0 0.0)]
		:east  [(vec3/v3 1.0 0.0 0.0) (vec3/v3 0.0 0.0 1.0) (vec3/v3 0.0 1.0 0.0)]
		[(vec3/v3 0.0 1.0 0.0) (vec3/v3 1.0 0.0 0.0) (vec3/v3 0.0 0.0 1.0)]))

(defn- spray-ops [{:keys [x y z face size rotation offset-u offset-v texture-id ttl]}]
	(let [[normal tangent bitangent] (spray-face-basis face)
				center (vec3/v+
								 (vec3/v3 (+ (double x) 0.5) (+ (double y) 0.5) (+ (double z) 0.5))
								 (vec3/v* normal 0.51))
				tangent' (vec3/rotate-around-axis tangent normal (double (or rotation 0.0)))
				bitangent' (vec3/rotate-around-axis bitangent normal (double (or rotation 0.0)))
				shifted (vec3/v+ center
											 (vec3/v+ (vec3/v* tangent' (double (or offset-u 0.0)))
															(vec3/v* bitangent' (double (or offset-v 0.0)))))
				half-size (* 0.5 (double (or size 1.0)))
				side (vec3/v* tangent' half-size)
				lift (vec3/v* bitangent' half-size)
				p0 (vec3/v+ (vec3/v- shifted side) lift)
				p1 (vec3/v+ (vec3/v+ shifted side) lift)
				p2 (vec3/v- (vec3/v+ shifted side) lift)
				p3 (vec3/v- (vec3/v- shifted side) lift)
				life (if (and ttl (< ttl 60)) (/ (double ttl) 60.0) 1.0)
				tex-folder (if (contains? #{:up :down} face) "wall" "grnd")
				tex-index (max 0 (min 2 (long (or texture-id 0))))]
		[(ru/quad-op (str (modid/namespaced-path "textures/effects/blood_spray/") tex-folder "/" tex-index ".png")
								 p0 p1 p2 p3
								 {:r 255 :g 255 :b 255 :a (int (+ 40 (* 180 life)))})]))

(defn- local-walk-speed [ticks]
	(let [ratio (min 1.0 (/ (double ticks) 20.0))]
		(float (+ 0.1 (* (- 0.007 0.1) ratio)))))

(defn- build-plan
	[camera-pos _hand-center-pos _tick]
	(let [{:keys [effect-state splashes sprays]} (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :blood-retrograde)
				br (some (fn [st]
									 (when (:active? st) st))
								 (vals effect-state))
				current-splashes (mapcat val splashes)
				current-sprays (mapcat val sprays)
				^V3 cam-v (vec3/map->v3 camera-pos)
				splash-plan (mapcat #(splash-ops cam-v %) current-splashes)
				spray-plan (mapcat spray-ops current-sprays)
				ws (when (and br (:active? br))
						 (local-walk-speed (:ticks br)))]
		(when (or (seq splash-plan) (seq spray-plan) ws)
			{:ops (vec (concat splash-plan spray-plan))
			 :local-walk-speed ws})))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:blood-retrograde :level] [_ _] {:effect-state {} :splashes {} :sprays {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:blood-retrograde :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:blood-retrograde :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :blood-retrograde
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :blood-retrograde [_ store owner-key]
  ;; Splashes and sprays are spawned world entities upstream (l_terminate only
  ;; restores walk speed), so they outlive the channel that produced them and
  ;; expire on their own ttl. See railgun_shot.clj's clear-owner.
  (update store :effect-state dissoc owner-key))
