(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.ray-barrage
  "Ray-barrage client FX: green tube rays (RendererRayComposite style).

  Original visuals:
  - MSG_EFFECT_PRERAY spawns EntityBarrageRayPre — ONE small green ray from
    the caster's eye to the aim target (life 30, or 50 when it hit a
    silbarn).
  - MSG_EFFECT_BARRAGE spawns EntityMdRayBarrage at the silbarn position —
    25~30 small green rays scattered around the caster's CURRENT aim
    (yaw ±50~60deg, pitch ±25~30deg), life 50.

  Both render with the same mdray_small composite (inner 216,248,216 /
  outer 106,242,106 / soft glow); the port renders them as tube + glow
  board ops (see ray-ops) so they are visible from any camera, including
  the caster's own on-axis first-person view. The preray applies the
  ViewOptimize hand fix like the original; the barrage does not
  (EntityMdRayBarrage.needsViewOptimize() == false)."
  (:require [cn.li.ac.ability.client.fx-templates.store-tick :as store-tick]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.effects.ray-composite :as ray-composite]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcmod.math V3]))

;; Two different renderers upstream, and the port used one for both:
;;
;;   EntityBarrageRayPre        inner 0.045, outer 0.052, glow 0.4 @0.5
;;   EntityMdRayBarrage         extends SmallMdRayRender, so the mdray_small
;;                              numbers: inner 0.03, outer 0.045, glow 0.3 @0.5
;;
;; The port drew both at the pre-ray's widths with the glow board left at its
;; 1.5 default — nearly four times the pre-ray's board and five times the
;; fan's.
(def ^:private preray-style
  {:textures "mdray_small"
   :outer-radius 0.052 :inner-radius 0.045
   :glow-width 0.4 :glow-alpha 127.0})

(def ^:private barrage-style
  {:textures "mdray_small"
   :outer-radius 0.045 :inner-radius 0.03
   :glow-width 0.3 :glow-alpha 127.0})

(def ^:private outer-rgb {:r 106 :g 242 :b 106})
(def ^:private inner-rgb {:r 216 :g 248 :b 216})

;; EntityRayBase: blendIn 200ms (4 ticks), blendOut 400ms (8 ticks).
(def ^:private blend-in-ticks 4.0)
(def ^:private blend-out-ticks 8.0)

(defn- ray-alpha
  ^double [beam ^double base]
  (let [max-ttl (double (max 1 (or (:max-ttl beam) 1)))
        ttl (double (or (:ttl beam) 0))
        age (- max-ttl ttl)]
    (* base
       (min 1.0 (/ age blend-in-ticks))
       (min 1.0 (/ ttl blend-out-ticks)))))

(defn- ray-ops
  [^V3 cam-v beam]
  (let [{:keys [textures outer-radius inner-radius glow-width glow-alpha]}
        (if (:barrage? beam) barrage-style preray-style)
        ga (ray-alpha beam 1.0)
        seed (double (or (:wiggle-seed beam) 0.0))
        life (/ (double (:ttl beam)) (double (:max-ttl beam)))]
    (ray-composite/composite-ops cam-v (:start beam) (:end beam)
      {:glow {:textures (ray-composite/glow-textures textures)
              :width glow-width
              :color {:r 255 :g 255 :b 255
                      :a (int (ray-composite/glow-alpha glow-alpha ga seed life))}}
       :inner {:radius inner-radius :color (assoc inner-rgb :a (int (ray-alpha beam 230.0)))}
       :outer {:radius outer-radius :color (assoc outer-rgb :a (int (ray-alpha beam 50.0)))}})))

(defn- all-rays
  []
  (mapcat val (:beam-queue (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :ray-barrage))))

(defn- look-dir-from-yaw-pitch
  "Minecraft look vector from yaw/pitch in degrees."
  [yaw-deg pitch-deg]
  (let [yaw (Math/toRadians (double yaw-deg))
        pitch (Math/toRadians (double pitch-deg))
        cp (Math/cos pitch)]
    {:dx (* -1.0 (Math/sin yaw) cp)
     :dy (Math/sin pitch)
     :dz (* (Math/cos yaw) cp)}))

(defn- barrage-sub-rays
  "25~30 sub rays from the silbarn position, scattered around the caster's
  aim by the original's SubRay offsets (yaw ±uniform(50,60)deg,
  pitch ±uniform(25,30)deg), length 15."
  [silbarn-pos yaw pitch]
  (let [count (long (+ 25 (rand-int 6)))
        max-angle (+ 50.0 (rand 10.0))
        base (vec3/map->v3 silbarn-pos)
        length 15.0]
    (vec
      (for [_ (range count)]
        (let [yaw-offset (- (rand (* 2.0 max-angle)) max-angle)
              pitch-offset (- (rand max-angle) (/ max-angle 2.0))
              dir (look-dir-from-yaw-pitch (+ (double (or yaw 0.0)) yaw-offset)
                                           (+ (double (or pitch 0.0)) pitch-offset))]
          {:start base
           :end (vec3/v+ base (vec3/v3 (* length (:dx dir))
                                       (* length (:dy dir))
                                       (* length (:dz dir))))
           :ttl 50 :max-ttl 50
           :wiggle-seed (* 2.0 Math/PI (rand))
           :barrage? true})))))

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store {:beam-queue {}})
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode source-player-id world-id]} (or payload {})
        base-meta {:owner-key owner-key*
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}
        rays (case mode
               ;; Original c_spawnPreRay: one ray from the caster's eye
               ;; (y + 1.6) to the aim point; life 50 when it hit a silbarn.
               :preray
               (when (and (map? (:start payload)) (map? (:end payload)))
                 (let [life (if (true? (:hit? payload)) 50 30)]
                   [(merge base-meta
                           {:start (vec3/map->v3 (:start payload))
                            :end (vec3/map->v3 (:end payload))
                            :ttl life :max-ttl life
                            :wiggle-seed (* 2.0 Math/PI (rand))})]))
               ;; Original c_spawnBarrage: sub rays from the silbarn position
               ;; around the caster's aim, no view optimization.
               :barrage
               (barrage-sub-rays (:silbarn payload)
                                 (:yaw payload) (:pitch payload))
               nil)]
    (if (seq rays)
      (update store* :beam-queue
        (fn [by-owner]
          (assoc by-owner owner-key* (vec (concat (get by-owner owner-key*) rays)))))
      store*)))

(defn- tick-state!
  [store]
  (let [store* (or store {:beam-queue {}})]
    (update store* :beam-queue
      store-tick/tick-ttl-items-by-owner)))

(defn- expanding-barrage-beam
  "Barrage sub rays grow OUT of the silbarn: the endpoint starts at the
  silbarn and extends to full length over the first 4 ticks (upstream's
  200ms blend-in), so the burst reads as rays SHOOTING from the silbarn
  instead of a full-length fan popping in instantly."
  [beam]
  (if-not (:barrage? beam)
    beam
    (let [ttl (long (:ttl beam))
          max-ttl (long (:max-ttl beam))
          scale (max 0.0 (min 1.0 (/ (double (- max-ttl ttl)) blend-in-ticks)))
          start (:start beam)
          end (:end beam)]
      (if (>= scale 1.0)
        beam
        (assoc beam :end (vec3/v+ start (vec3/v* (vec3/v- end start) scale)))))))

(defn- build-plan
  [camera-pos hand-center-pos _tick]
  (when-let [rays (seq (all-rays))]
    (let [cam-v (vec3/map->v3 camera-pos)
          ;; Preray follows the original's needsViewOptimize: hand-fix the
          ;; START only, keeping the END anchored on the aim point ("Don't
          ;; fix end to get accurate pointing direction"); barrage rays are
          ;; not view-optimized (they issue from the silbarn, off the
          ;; caster's view axis already).
          preray-fixed (arc-beam/view-fix-rays hand-center-pos
                                               (filterv #(not (:barrage? %)) rays)
                                               {:fix-end? false})
          barrage (map expanding-barrage-beam (filterv :barrage? rays))
          fixed (into preray-fixed barrage)]
      {:ops (vec (mapcat #(ray-ops cam-v %) fixed))})))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:ray-barrage :level] [_ _] {:beam-queue {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:ray-barrage :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:ray-barrage :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :ray-barrage
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :ray-barrage [_ store _owner-key]
  ;; The rays are one-shot world visuals (upstream EntityBarrageRayPre /
  ;; EntityMdRayBarrage carry their own lives). The :instant context ends on
  ;; the same tick as perform, so MSG-CTX-TERMINATED reaches
  ;; clear-effect-owner! immediately — clearing the queue here deleted every
  ;; ray a frame after it appeared. They expire on their own ttl instead.
  store)
