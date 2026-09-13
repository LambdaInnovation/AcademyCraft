(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.teleporter-crit
  "Teleporter critical-hit effect — upstream AcademyCraft alignment
  (CriticalHitEffect + FormulaParticleFactory + Particle).

  On :crit-hit, spawn 5-8 formula particles around the target: random angle,
  radius width*(0.5..0.7), height random within the target box; each drifts
  with velocity random*0.03, size 1.0-1.7, one of the 10 formula textures,
  alpha 152..384 (clamped to 255), fade-in 2 ticks, then a 10-15 tick life
  and a 20-tick fade-out (upstream Particle.onUpdate curve).

  The target box is resolved client-side from the live entity snapshot
  (payload :target-uuid) — upstream fires TPCritHitEvent with the Entity and
  the effect reads t.posX/posY/width/height at event time, so the same live
  source is used here; the payload position is the fallback when the entity
  is gone. No sound, no vanilla particles: the combat notice (upstream chat
  message) + this burst are the entire upstream effect."
  (:require [cn.li.ac.ability.client.effects.billboard-particles :as bp]
            [cn.li.ac.ability.client.effects.rv3 :as rv3]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

(def ^:private formula-frame-count 10)

(defn- formula-texture
  [frame]
  (modid/asset-path "textures/effects/formula"
                    (str (mod (long frame) formula-frame-count) ".png")))

;; Upstream FormulaParticleFactory template: color 220,220,220,255, hasLight=false.
(def ^:private particle-color {:r 220 :g 220 :b 220})

(defn- rand-range
  [a b]
  (+ a (rand (- b a))))

(defn- fallback-box
  "Payload position used when no live entity is available: :x/:y/:z is the
  target feet position (threatening/shift send feet; the payload is always
  feet when the live lookup fails, so the burst stays on the target)."
  [payload]
  {:cx (double (or (:x payload) 0.0))
   :feet-y (double (or (:y payload) 0.0))
   :cz (double (or (:z payload) 0.0))
   :width (double (or (:target-width payload) 0.6))
   :height (double (or (:target-height payload) 1.8))})

(defn- resolve-target-box
  "Live entity box via the platform snapshot (McClientAccess.clientEntitySnapshot
  returns a String-keyed map — keywordize before keyword reads), mirroring
  the marker impl."
  [payload]
  (if-let [uuid (:target-uuid payload)]
    (or (when-let [raw (client-bridge/run-client-effect!
                        :mcmod/get-entity-position {:entity-uuid uuid})]
          (let [live (into {} (map (fn [[k v]] [(keyword k) v])) raw)]
            (when (and (:x live) (:y live) (:z live))
              {:cx (double (:x live))
               :feet-y (double (:y live))
               :cz (double (:z live))
               :width (double (or (:width live) 0.6))
               :height (double (or (:height live) 1.8))})))
        (fallback-box payload))
    (fallback-box payload)))

(defn- make-particles
  "Upstream CriticalHitEffect.onTPCritHit: 5-8 particles at random angle,
  radius [width*0.5, width*0.7], height [0, height]; each drifts with
  VecUtils.multiply(VecUtils.random(), 0.03). FormulaParticleFactory
  decorator: size 1.0-1.7, alpha 152-384, random formula texture, fade-in 2,
  fadeAfter(10-15, 20)."
  [{:keys [cx feet-y cz width height]}]
  (let [n (long (+ 5 (rand-int 3)))]
    (vec
     (for [_ (range n)]
       (let [angle (rand-range 0.0 (* 2.0 Math/PI))
             r (rand-range (* 0.5 width) (* 0.7 width))
             h (rand-range 0.0 height)]
         {:x (+ cx (* r (Math/sin angle)))
          :y (+ feet-y h)
          :z (+ cz (* r (Math/cos angle)))
          :vx (rand-range -0.03 0.03)
          :vy (rand-range -0.03 0.03)
          :vz (rand-range -0.03 0.03)
          :size (rand-range 1.0 1.7)
          :texture (formula-texture (rand-int formula-frame-count))
          :color particle-color
          :start-alpha (long (min 255 (rand-range 152.0 384.0)))
          :age 0
          :life (long (+ 10 (rand-int 6)))
          :fade-in 2
          :fade-out 20})))))

(defn- enqueue!
  [store ctx-id channel owner-key payload]
  (let [state* (or store {:bursts []})]
    (case (:mode payload)
      :crit-hit
      (let [box (resolve-target-box payload)
            particles (make-particles box)]
        (when (:message-key payload)
          (runtime-hooks/client-show-combat-notice!
            :teleporter-crit
            {:message-key (:message-key payload)
             :args (:message-args payload)
             :duration-ms 1500
             :color [255 226 120]}))
        (update state* :bursts conj {:particles particles}))
      state*)))

(defn- tick!
  [store]
  (let [state* (or store {:bursts []})]
    (update state* :bursts
            (fn [bursts]
              (into []
                    (keep (fn [burst]
                            (let [alive (bp/tick-particles! (:particles burst))]
                              (when (seq alive)
                                (assoc burst :particles alive)))))
                    bursts)))))

(defn- build-plan [camera-pos _hand-center-pos _tick]
  (let [store (level-effects/effect-state-snapshot :teleporter-crit)
        cam (rv3/map->v3 camera-pos)
        ops (vec (mapcat (fn [burst]
                           (bp/particle-ops cam (:particles burst)))
                         (:bursts store)))]
    (when (seq ops)
      {:ops ops})))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:teleporter-crit :level] [_ _] {:bursts []})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:teleporter-crit :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:teleporter-crit :level] [_ _ store] (tick! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :teleporter-crit
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :teleporter-crit [_ store _owner-key]
  ;; Bursts are transient and owner-independent — upstream spawns the crit
  ;; particles as entities that live out their life after the event, so
  ;; context termination (MSG-CTX-TERMINATED right after up!) must NOT wipe
  ;; the just-created burst.
  store)
