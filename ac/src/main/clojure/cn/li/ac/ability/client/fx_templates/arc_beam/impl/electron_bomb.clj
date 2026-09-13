(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.electron-bomb
  "Client FX for ElectronBomb.

  Upstream has exactly two visible pieces: the EntityMdBall the skill spawns
  (which renders itself) and, when its callback fires, one EntityMdRaySmall
  from the ball to the aim point. That ray is the same entity ScatterBomb and
  ElectronMissile fire, so it goes through the shared ray-composite with
  SmallMdRayRender's numbers."
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.ray-composite :as ray-composite]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

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

(defn- ranged
  ^double [^double from ^double to]
  (+ from (rand (- to from))))

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store {:effect-state {} :beams {}})
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode start end source-player-id world-id]} (or payload {})
        base-meta {:owner-key owner-key*
                   :queue-owner (client-particles/current-effect-owner)
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :spawn
      ;; The ball itself is the EntityMdBall the skill spawned — this used to
      ;; also draw a spinning line and a spark every third tick at the caster's
      ;; EYE, a second ball's worth of visuals in the wrong place. Only the
      ;; port's cast cue survives; upstream is silent here.
      (do
        (client-sounds/queue-sound-effect! (:queue-owner base-meta)
          {:type :sound :sound-id (modid/namespaced-path "md.eb_spawn") :volume 0.6 :pitch 1.2})
        (assoc-in store* [:effect-state owner-key*]
                  (merge base-meta {:active? true})))

      :beam
      (let [store* (if (and start end)
                     (update-in store* [:beams owner-key*] (fnil conj [])
                                (merge base-meta
                                       {:start (vec3/map->v3 start)
                                        :end (vec3/map->v3 end)
                                        :ttl ray-life-ticks
                                        :max-ttl ray-life-ticks
                                        :wiggle-seed (* 2.0 Math/PI (rand))}))
                     store*)]
        (when (and start end)
          ;; EntityMdRaySmall.onFirstUpdate: md.ray_small at 0.8, at the ray.
          ;; The port played md.eb_explode — a made-up detonation cue — with no
          ;; coordinates, so it came from the listener rather than the shot.
          (client-sounds/queue-sound-effect! (:queue-owner base-meta)
            {:type :sound :sound-id (modid/namespaced-path "md.ray_small")
             :volume 0.8 :pitch 1.0
             :x (double (or (:x start) 0.0))
             :y (double (or (:y start) 0.0))
             :z (double (or (:z start) 0.0))}))
        (update store* :effect-state dissoc owner-key*))

      :end
      (-> store*
          (update :effect-state dissoc owner-key*)
          (update :beams dissoc owner-key*))

      store*)))

(defn- emit-ray-trail!
  "EntityMdRaySmall.onUpdate spawns ONE MdParticle per tick at a random point
  0-10 blocks along the ray, with a slow random drift. The port puffed eight
  vanilla sparks at the endpoint the moment the ray appeared and nothing after."
  [{:keys [queue-owner start end]}]
  (when (and start end)
    (let [span (vec3/v- end start)
          t (min 1.0 (/ (rand 10.0) (Math/max 1.0e-5 (vec3/vlen span))))
          p (vec3/v+ start (vec3/v* span t))]
      (client-particles/queue-particle-effect! queue-owner
        {:type :particle :particle-type (modid/namespaced-path "md_particle")
         :x (.-x ^cn.li.mcmod.math.V3 p)
         :y (.-y ^cn.li.mcmod.math.V3 p)
         :z (.-z ^cn.li.mcmod.math.V3 p)
         ;; A single particle takes offset-* * speed as its velocity verbatim.
         :count 1 :speed 1.0
         :offset-x (ranged -0.015 0.015)
         :offset-y (ranged -0.015 0.015)
         :offset-z (ranged -0.015 0.015)}))))

(defn- tick-state!
  [store]
  (let [store* (or store {:effect-state {} :beams {}})]
    (doseq [[_owner beams] (:beams store*)
            beam beams]
      (emit-ray-trail! beam))
    (update store* :beams store-tick/tick-ttl-items-by-owner)))

(defn- beam-ops
  "One EntityMdRaySmall: the glow boards plus the two cylinders. The port drew
  three flat billboards whose layers had been shuffled — the glow board's white
  alpha wearing the outer cylinder's colour, and the inner cylinder's colour
  demoted to a one-pixel line."
  [camera-pos beams]
  (mapcat (fn [{:keys [start end ttl max-ttl wiggle-seed]}]
            (let [max-ttl (double (max 1 (or max-ttl ray-life-ticks)))
                  ttl (double (or ttl 0))
                  age (- max-ttl ttl)
                  blend-in (min 1.0 (/ age ray-blend-in-ticks))
                  blend-out (min 1.0 (/ ttl ray-blend-out-ticks))
                  am (* blend-in blend-out)
                  ;; getWidth(): full width until the last 500ms, then linear
                  ;; to zero — not the port's hard cut with 210ms left.
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

(defn- build-plan [camera-pos _hand-center-pos _tick]
  (let [{:keys [beams]} (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :electron-bomb)]
    (when (seq beams)
      (let [cam-v (vec3/map->v3 camera-pos)
            ops (vec (mapcat (fn [[_owner-key xs]] (beam-ops cam-v xs)) beams))]
        (when (seq ops)
          {:ops ops})))))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:electron-bomb :level] [_ _] {:effect-state {} :beams {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:electron-bomb :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:electron-bomb :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :electron-bomb
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :electron-bomb [_ store owner-key]
  ;; The ball state is context-bound; the settlement beam is upstream's
  ;; EntityMdRaySmall — a world entity with its own 14-tick life. This skill is
  ;; :instant, so the context is usually gone before the delayed beam even
  ;; arrives. See railgun_shot.clj's clear-owner.
  (update store :effect-state dissoc owner-key))
