(ns cn.li.ac.content.ability.meltdowner.scatter-bomb-fx
  "Client FX for ScatterBomb: ball spawn + scatter beam flashes.

  The release beams render as billboard quads from each ball's position to
  its scattered destination (original SBNetDelegate: EntityMdRaySmall
  setFromTo(ball.getPositionEyes, dest)) — a fixed-direction ray entity
  spawned at the player instead pointed wherever its rotation happened to be."
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.effects.ray-composite :as ray-composite]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]))

(def ^:private scatter-bomb-effect-id :scatter-bomb)

(defn default-scatter-bomb-fx-runtime-state
  []
  {:effect-state {} :beams {}})

(defn scatter-bomb-fx-snapshot
  []
  (or (level-effects/effect-state-snapshot scatter-bomb-effect-id)
      (default-scatter-bomb-fx-runtime-state)))

(defn reset-scatter-bomb-fx-for-test!
  []
  (level-effects/reset-level-effect-state-for-test!
    scatter-bomb-effect-id
    (default-scatter-bomb-fx-runtime-state))
  nil)

(defn clear-scatter-bomb-owner!
  [owner-key]
  (level-effects/update-effect-state!
    scatter-bomb-effect-id
    (fn [store]
      (-> (or store (default-scatter-bomb-fx-runtime-state))
          (update :effect-state dissoc owner-key)
          (update :beams dissoc owner-key))))
  nil)

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

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store (default-scatter-bomb-fx-runtime-state))
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode x y z count start end source-player-id world-id]} (or payload {})
        base-meta {:owner-key owner-key*
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :start
      (do
        (client-sounds/queue-current-sound-effect!
          {:type :sound :sound-id (modid/namespaced-path "md.sb_charge") :volume 0.5 :pitch 1.0})
        (assoc-in store* [:effect-state owner-key*]
                  (merge base-meta {:active? true :ticks 0 :balls 0})))
      :ball
      (do
        (client-particles/queue-current-particle-effect!
          {:type :particle :particle-type :electric-spark
           :x (double (or x 0.0))
           :y (double (or y 0.0))
           :z (double (or z 0.0))
           :count 4 :speed 0.1
           :offset-x 0.3 :offset-y 0.3 :offset-z 0.3})
        (update-in store* [:effect-state owner-key*]
          (fn [st]
            (assoc (merge base-meta (or st {:active? true :ticks 0}))
                   :owner-key owner-key*
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id
                   :balls (int (or count 0))))))
      :beam
      (let [store* (if (and start end)
                     (update-in store* [:beams owner-key*] (fnil conj [])
                                {:start (vec3/map->v3 start)
                                 :end (vec3/map->v3 end)
                                 ;; Original EntityMdRaySmall: life 14 ticks.
                                 :ttl ray-life-ticks
                                 :max-ttl ray-life-ticks
                                 :wiggle-seed (* 2.0 Math/PI (rand))
                                 ;; The tick path (emit-ray-trail!) runs
                                 ;; without an effect-owner binding, so the
                                 ;; trail cannot use the current-owner queue —
                                 ;; capture the owner here, where the channel
                                 ;; push binding exists.
                                 :queue-owner (client-particles/current-effect-owner)})
                     store*)]
        (when (and start end)
          ;; EntityMdRaySmall.onFirstUpdate: md.ray_small at 0.8, at the ray.
          ;; The port played md.eb_explode (the electron bomb's detonation,
          ;; mapped to meltdowner.ogg) at 0.4/1.2, and with no coordinates —
          ;; so it came from the listener rather than from the shot.
          (client-sounds/queue-current-sound-effect!
            {:type :sound :sound-id (modid/namespaced-path "md.ray_small")
             :volume 0.8 :pitch 1.0
             :x (double (or (:x start) 0.0))
             :y (double (or (:y start) 0.0))
             :z (double (or (:z start) 0.0))}))
        store*)
      :end
      ;; The release beams arrive AFTER this :end — the server schedules them
      ;; one tick out of the delayed-projectiles queue, so wiping :beams here
      ;; raced their arrival and deleted every ray the moment it appeared.
      ;; Beams own their ttl and tick out on their own; only the hold state
      ;; dies with the context.
      (update store* :effect-state dissoc owner-key*)
      store*)))

(defn- emit-ray-trail!
  "EntityMdRaySmall.onUpdate spawns ONE MdParticle per tick at a random point
  0-10 blocks along the ray, with a slow random drift — the ray leaves a line
  of green motes behind it. The port only puffed four sparks at the endpoint
  when the ray appeared.

  Runs from tick-state!, which has NO effect-owner binding, so it queues via
  the owner captured at :beam enqueue (see enqueue-state!) rather than the
  current-owner queue — that path throws outside a binding and the whole
  scatter-bomb tick died every frame, freezing every beam's ttl."
  [{:keys [start end queue-owner]}]
  (when (and start end queue-owner)
    (let [t (/ (double (rand 10.0))
               (Math/max 1.0e-5 (vec3/vlen (vec3/v- end start))))
          t (Math/min 1.0 t)
          p (vec3/v+ start (vec3/v* (vec3/v- end start) t))]
      (client-particles/queue-particle-effect!
        queue-owner
        {:type :particle :particle-type (modid/namespaced-path "md_particle")
         :x (.-x ^cn.li.mcmod.math.V3 p)
         :y (.-y ^cn.li.mcmod.math.V3 p)
         :z (.-z ^cn.li.mcmod.math.V3 p)
         ;; A single particle takes offset-* * speed as its velocity verbatim
         ;; (see the mcbase particle bridge); :motion-* is not read at all, so
         ;; with speed 0 these motes hung dead still instead of drifting.
         :count 1 :speed 1.0
         :offset-x (- (rand 0.03) 0.015)
         :offset-y (- (rand 0.03) 0.015)
         :offset-z (- (rand 0.03) 0.015)}))))

(defn- tick-state!
  [store]
  (let [store* (or store (default-scatter-bomb-fx-runtime-state))]
    (doseq [[_owner beams] (:beams store*)
            beam beams]
      (emit-ray-trail! beam))
    (-> store*
        (update :effect-state
          (fn [states]
            (into {}
                  (keep (fn [[owner-key st]]
                          (when (:active? st)
                            [owner-key (assoc st :ticks (inc (long (or (:ticks st) 0))))])))
                  states)))
        (update :beams
          store-tick/tick-ttl-items-by-owner))))

(defn- beam-flash-ops
  "One EntityMdRaySmall: the glow boards plus the two cylinders.

  The port had these as three flat billboards with the layers shuffled — a
  0.3 HALF-width outer quad (twice the glow board, wearing the outer
  cylinder's colour), the outer radius carrying a made-up alpha, and the inner
  cylinder reduced to a one-pixel line."
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

(defn- build-plan
  [camera-pos _hand-center-pos _tick & _query-fn]
  (let [snapshot (or (level-effects/effect-state-snapshot scatter-bomb-effect-id)
                     (default-scatter-bomb-fx-runtime-state))
        beams (get snapshot :beams)]
    (when (seq beams)
      (let [cam-v (vec3/map->v3 camera-pos)
            ops (vec (mapcat (fn [[_owner-key xs]] (beam-flash-ops cam-v xs)) beams))]
        (when (seq ops)
          {:ops ops})))))

(defn init!
  []
  (fx-spec/register!
    {:id scatter-bomb-effect-id
     :level {:initial-state (default-scatter-bomb-fx-runtime-state)
             :enqueue-state-fn enqueue-state!
             :tick-state-fn tick-state!
             :build-plan-fn build-plan}
     :channels {:start {:topic :scatter-bomb/fx-start :mode :start}
                :ball {:topic :scatter-bomb/fx-ball :mode :ball
                       :level-payload (fn [_ _ p]
                                        {:x (:x p) :y (:y p) :z (:z p) :count (:count p)})}
                :beam {:topic :scatter-bomb/fx-beam :mode :beam
                       :level-payload (fn [_ _ p]
                                        {:start (:start p) :end (:end p)})}
                :end {:topic :scatter-bomb/fx-end :mode :end}}})
  nil)
