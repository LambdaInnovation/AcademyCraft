(ns cn.li.ac.content.ability.meltdowner.electron-missile-fx
  "Client FX for ElectronMissile: orbiting sparks while channelling, plus one
  EntityMdRaySmall per fired ball.

  Original EMContextC does two things and nothing else: on MSG_EFFECT_UPDATE it
  puffs 1-2 MdParticles around the caster's waist, and on MSG_EFFECT_SPAWN it
  spawns an EntityMdRaySmall from the ball to the victim's eyes. That ray is the
  very same entity ScatterBomb fires, so it renders through the shared
  ray-composite with SmallMdRayRender's numbers."
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.ray-composite :as ray-composite]
            [cn.li.ac.ability.client.effects.rv3 :as rv3]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.level-effects :as level-effects]))

(def ^:private electron-missile-effect-id :electron-missile)

;; SmallMdRayRender (RendererRayComposite "mdray_small"):
;;   glow        width 0.3, white at alpha 0.5
;;   cylinderOut radius 0.045, rgba(106, 242, 106, 50)
;;   cylinderIn  radius 0.03,  rgba(216, 248, 216, 230)
(def ^:private ray-glow-textures (ray-composite/glow-textures "mdray_small"))
(def ^:private ray-glow-width 0.3)
(def ^:private ray-outer-radius 0.045)
(def ^:private ray-inner-radius 0.03)

;; EntityMdRaySmall: life 14 ticks, blendIn 200ms, blendOut 400ms, and
;; getWidth() ramps 1 -> 0 over the last 500ms.
(def ^:private ray-life-ticks 14)
(def ^:private ray-blend-in-ticks 4.0)
(def ^:private ray-blend-out-ticks 8.0)
(def ^:private ray-width-shrink-ticks 10.0)

;; ACRenderingHelper.getHeightFix is a flat 1.6 — the orbit particles sit on the
;; caster's body, in the same band the balls occupy.
(def ^:private charge-height-fix 1.6)

(defn default-electron-missile-fx-runtime-state
  []
  {:charge-state {}
   :beams {}})

(defn electron-missile-fx-snapshot
  []
  (or (level-effects/effect-state-snapshot electron-missile-effect-id)
      (default-electron-missile-fx-runtime-state)))

(defn reset-electron-missile-fx-for-test!
  []
  (level-effects/reset-level-effect-state-for-test!
    electron-missile-effect-id
    (default-electron-missile-fx-runtime-state))
  nil)

(defn clear-electron-missile-owner!
  [owner-key]
  (level-effects/update-effect-state!
    electron-missile-effect-id
    (fn [store]
      (-> (or store (default-electron-missile-fx-runtime-state))
          (update :charge-state dissoc owner-key)
          (update :beams dissoc owner-key))))
  nil)

(defn- all-beams
  []
  (mapcat val (:beams (electron-missile-fx-snapshot))))

(defn- ranged
  ^double [^double from ^double to]
  (+ from (rand (- to from))))

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store (default-electron-missile-fx-runtime-state))
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode source-player-id world-id ticks balls start end x y z]} (or payload {})
        base-meta {:owner-key owner-key*
                   :queue-owner (client-particles/current-effect-owner)
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :start
      (do
        (client-sounds/queue-sound-effect! (:queue-owner base-meta)
          {:type :sound :sound-id (modid/namespaced-path "md.em_start") :volume 0.5 :pitch 1.0})
        (assoc-in store* [:charge-state owner-key*]
                  (merge base-meta {:active? true :ticks 0 :balls 0})))
      :update
      (assoc-in store* [:charge-state owner-key*]
                (merge base-meta
                       {:active? true
                        :ticks (long (or ticks 0))
                        :balls (long (or balls 0))
                        ;; The caster's feet, refreshed every server tick: the
                        ;; orbit particles are world-space, so without this they
                        ;; all land at the world origin.
                        :owner-x (when (some? x) (double x))
                        :owner-y (when (some? y) (double y))
                        :owner-z (when (some? z) (double z))}))
      :fire
      (let [store* (if (and start end)
                     (update-in store* [:beams owner-key*] (fnil conj [])
                                (merge base-meta
                                       {:start (rv3/map->v3 start)
                                        :end (rv3/map->v3 end)
                                        :ttl ray-life-ticks
                                        :max-ttl ray-life-ticks
                                        :wiggle-seed (* 2.0 Math/PI (rand))}))
                     store*)]
        (when (and start end)
          ;; EntityMdRaySmall.onFirstUpdate: md.ray_small at 0.8, at the ray.
          (client-sounds/queue-sound-effect! (:queue-owner base-meta)
            {:type :sound :sound-id (modid/namespaced-path "md.ray_small")
             :volume 0.8 :pitch 1.0
             :x (double (or (:x start) 0.0))
             :y (double (or (:y start) 0.0))
             :z (double (or (:z start) 0.0))}))
        store*)
      :end
      ;; In-flight shot beams must survive the release: upstream's rays are
      ;; world entities with their own lives and MSG_TERMINATED only kills
      ;; the orbiting balls. Wiping :beams here cut every beam fired in the
      ;; last 0.7s of the hold short the moment the key came up.
      (update store* :charge-state dissoc owner-key*)
      store*)))

(defn- emit-charge-particles!
  "EMContextC.c_updateEffect: rangei(1, 3) particles per update at
  player.pos + (r*sin(theta), heightFix + h, r*cos(theta)), r in [0.5, 1),
  h in [-1.2, 0), drifting slowly upwards."
  [{:keys [queue-owner owner-x owner-y owner-z]}]
  (when (and owner-x owner-y owner-z)
    (dotimes [_ (inc (rand-int 2))]
      (let [r (ranged 0.5 1.0)
            theta (ranged 0.0 (* 2.0 Math/PI))
            h (ranged -1.2 0.0)]
        (client-particles/queue-particle-effect! queue-owner
          {:type :particle :particle-type (modid/namespaced-path "md_particle")
           :x (+ (double owner-x) (* r (Math/sin theta)))
           :y (+ (double owner-y) charge-height-fix h)
           :z (+ (double owner-z) (* r (Math/cos theta)))
           ;; A single particle takes offset-* * speed as its velocity verbatim
           ;; (see mcbase particle bridge), so speed 1 makes these the original's
           ;; ranged(-.02,.02) / ranged(.01,.05) / ranged(-.02,.02) motion.
           :count 1 :speed 1.0
           :offset-x (ranged -0.02 0.02)
           :offset-y (ranged 0.01 0.05)
           :offset-z (ranged -0.02 0.02)})))))

(defn- emit-ray-trail!
  "EntityMdRaySmall.onUpdate spawns ONE MdParticle per tick at a random point
  0-10 blocks along the ray, with a slow random drift."
  [{:keys [queue-owner start end]}]
  (when (and start end)
    (let [span (rv3/v- end start)
          t (min 1.0 (/ (rand 10.0) (Math/max 1.0e-5 (rv3/vlen span))))
          p (rv3/v+ start (rv3/v* span t))]
      (client-particles/queue-particle-effect! queue-owner
        {:type :particle :particle-type (modid/namespaced-path "md_particle")
         :x (.-x ^cn.li.mcmod.math.V3 p)
         :y (.-y ^cn.li.mcmod.math.V3 p)
         :z (.-z ^cn.li.mcmod.math.V3 p)
         :count 1 :speed 1.0
         :offset-x (ranged -0.015 0.015)
         :offset-y (ranged -0.015 0.015)
         :offset-z (ranged -0.015 0.015)}))))

(defn- tick-state!
  [store]
  (let [store* (or store (default-electron-missile-fx-runtime-state))]
    ;; Original MSG_EFFECT_UPDATE is sent every server tick, with or without
    ;; stored balls, for as long as the channel runs.
    (doseq [[_ st] (:charge-state store*)
            :when (:active? st)]
      (emit-charge-particles! st))
    (doseq [beam (all-beams)]
      (emit-ray-trail! beam))
    (-> store*
        (update :charge-state
                (fn [states]
                  (into {}
                        (keep (fn [[owner-key st]]
                                (when (:active? st)
                                  [owner-key st])))
                        states)))
        (update :beams store-tick/tick-ttl-items-by-owner))))

(defn- beam-ops
  "One EntityMdRaySmall: the glow boards plus the two cylinders."
  [camera-pos beams]
  (mapcat (fn [{:keys [start end ttl max-ttl wiggle-seed]}]
            (let [max-ttl (double (max 1 (or max-ttl ray-life-ticks)))
                  ttl (double (or ttl 0))
                  age (- max-ttl ttl)
                  blend-in (min 1.0 (/ age ray-blend-in-ticks))
                  blend-out (min 1.0 (/ ttl ray-blend-out-ticks))
                  am (* blend-in blend-out)
                  ;; getWidth(): full width until the last 500ms, then linear
                  ;; down to zero.
                  w (if (> ttl ray-width-shrink-ticks)
                      1.0
                      (max 0.0 (/ ttl ray-width-shrink-ticks)))
                  alpha (fn [^double base] (int (* base am)))]
              (when (pos? w)
                (ray-composite/composite-ops camera-pos start end
                  {:glow {:textures ray-glow-textures
                          :width (* ray-glow-width w)
                          :color {:r 255 :g 255 :b 255
                                  :a (int (ray-composite/glow-alpha 127.0 am
                                                                    (double (or wiggle-seed 0.0))
                                                                    (/ ttl max-ttl)))}}
                   :inner {:radius (* ray-inner-radius w)
                           :color {:r 216 :g 248 :b 216 :a (alpha 230.0)}}
                   :outer {:radius (* ray-outer-radius w)
                           :color {:r 106 :g 242 :b 106 :a (alpha 50.0)}}}))))
          beams))

(defn- build-plan
  [camera-pos _hand-center-pos _tick _query-fn]
  (let [{:keys [beams]} (electron-missile-fx-snapshot)]
    (when (seq beams)
      (let [cam-v3 (rv3/map->v3 camera-pos)
            ops (vec (mapcat (fn [[_owner-key xs]] (beam-ops cam-v3 xs)) beams))]
        (when (seq ops)
          {:ops ops})))))

(defn init!
  []
  (fx-spec/register!
    {:id electron-missile-effect-id
     :level {:initial-state (default-electron-missile-fx-runtime-state)
             :enqueue-state-fn enqueue-state!
             :tick-state-fn tick-state!
             :build-plan-fn build-plan}
     :channels {:start {:topic :electron-missile/fx-start :mode :start}
                :update {:topic :electron-missile/fx-update :mode :update
                         :level-payload (fn [_ _ p]
                                          {:ticks (long (or (:ticks p) 0))
                                           :balls (long (or (:balls p) 0))
                                           :x (:x p)
                                           :y (:y p)
                                           :z (:z p)})}
                :fire {:topic :electron-missile/fx-fire :mode :fire
                       :level-payload (fn [_ _ p]
                                        {:start (:start p)
                                         :end (:end p)})}
                :end {:topic :electron-missile/fx-end :mode :end}}})
  nil)
