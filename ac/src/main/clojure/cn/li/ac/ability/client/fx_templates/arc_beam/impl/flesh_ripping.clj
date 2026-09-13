(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.flesh-ripping
  (:require [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.effects.rv3 :as rv3]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

(def ^:private stale-owner-ttl-ticks 80)

;; Upstream FRContextC marker colors: DISABLED (74,74,74,160) with no target,
;; THREATENING (185,25,25,180) when an entity is targeted.
(def ^:private color-disabled {:r 74 :g 74 :b 74 :a 160})
(def ^:private color-threatening {:r 185 :g 25 :b 25 :a 180})

(defn- spawn-blood-splash!
  "Spawn EntityBloodSplash at target (matching original EntityBloodSplash on
  hit — the spec is follow-owner? false with a 10-tick life, so spawnLocalAt
  leaves it where it lands)."
  [x y z]
  (client-bridge/run-client-effect! :mcmod/spawn-local-scripted-effect-at
    {:effect-id "entity_blood_splash" :x x :y y :z z}))

(defn- corner-tick-ops
  "Upstream RenderMarker.renderMark: at each of the 8 box corners draw 3 short
  line segments (vertical + two horizontal ticks rotated per corner so they
  run along the box edges — extending them traces the cube outline). Lines
  are translucent (upstream marker colors carry low alpha)."
  [ox oy oz width height color]
  (let [len (* 0.2 width)
        rots [0.0 -90.0 -180.0 -270.0 0.0 -90.0 -180.0 -270.0]
        axis (fn [theta]
               (let [r (Math/toRadians theta)
                     c (Math/cos r)
                     s (Math/sin r)]
                 {:x1 c :z1 (- s) :x2 s :z2 c}))
        corners [[0 0 0] [1 0 0] [1 0 1] [0 0 1]
                 [0 1 0] [1 1 0] [1 1 1] [0 1 1]]]
    (mapcat (fn [[cx cy cz] theta]
              (let [x (+ ox (* cx width))
                    y (+ oy (* cy height))
                    z (+ oz (* cz width))
                    rev (< cy 0.5)
                    vert (if rev len (- len))
                    {ax1 :x1 az1 :z1 ax2 :x2 az2 :z2} (axis theta)]
                [(assoc (ru/line-op (rv3/v3 x y z) (rv3/v3 x (+ y vert) z) color) :translucent? true)
                 (assoc (ru/line-op (rv3/v3 x y z)
                                    (rv3/v3 (+ x (* ax1 len)) y (+ z (* az1 len))) color) :translucent? true)
                 (assoc (ru/line-op (rv3/v3 x y z)
                                    (rv3/v3 (+ x (* ax2 len)) y (+ z (* az2 len))) color) :translucent? true)]))
            corners
            rots)))

(defn- marker-ops
  "Box bottom sits just ABOVE the aim point (upstream y + 0.05*sin float);
  when targeting an entity the box follows its LIVE client-side position every
  frame and is sized to the target box x1.2 (upstream l_updateEffect +
  EntityMarker follow)."
  [st tick]
  (let [color (if (:hit? st) color-threatening color-disabled)
        ;; McClientAccess.clientEntitySnapshot returns a String-keyed map —
        ;; keywordize so the keyword reads below actually resolve.
        live (when-let [uuid (:target-uuid st)]
               (when-let [raw (client-bridge/run-client-effect!
                               :mcmod/get-entity-position {:entity-uuid uuid})]
                 (into {} (map (fn [[k v]] [(keyword k) v])) raw)))
        aim (:aim st)
        ;; Upstream l_updateEffect pins the marker at 1.0x1.0 without a target;
        ;; only a targeted entity resizes it (target box x1.2). The synced
        ;; :target-height is 0.0 for a block hit — never let it collapse the
        ;; box.
        width (double (if live (* 1.2 (:width live))
                        (if (:hit? st) (:target-width st) 1.0)))
        height (double (if live (* 1.2 (:height live))
                        (if (:hit? st) (:target-height st) 1.0)))
        px (double (if live (:x live) (:x aim)))
        ;; Upstream RenderMarker: y + 0.05 * sin(absTime / 400.0).
        py (+ (double (if live (:y live) (:y aim)))
              (* 0.05 (Math/sin (/ (double tick) 400.0))))
        pz (double (if live (:z live) (:z aim)))]
    (corner-tick-ops (- px (* 0.5 width)) py (- pz (* 0.5 width))
                     width height color)))

(defn- enqueue-state! [state ctx-id channel owner-key payload]
  (let [state* (or state {:fx-state {}})
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [source-player-id world-id]} payload
        base-meta {:owner-key owner-key*
                   :queue-owner (client-particles/current-effect-owner)
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}
        aim (fn [p]
              {:x (double (or (:target-x p) 0.0))
               :y (double (or (:target-y p) 0.0))
               :z (double (or (:target-z p) 0.0))})]
    (case (:mode payload)
      :start
      (update state* :fx-state assoc owner-key*
              (merge base-meta {:active? true :ttl 0
                                :aim (aim payload)
                                :hit? (boolean (:hit? payload))
                                :target-uuid (:target-uuid payload)
                                :target-width (double (or (:target-width payload) 0.6))
                                :target-height (double (or (:target-height payload) 1.8))}))

      :update
      (update state* :fx-state update owner-key*
              (fn [st]
                (assoc (merge base-meta (or st {:active? true :ttl 0}))
                       :active? true
                       :ttl 0
                       :aim (aim payload)
                       :hit? (boolean (:hit? payload))
                       :target-uuid (:target-uuid payload)
                       :target-width (double (or (:target-width payload) 0.6))
                       :target-height (double (or (:target-height payload) 1.8)))))

      :perform
      (do
        (when (:hit? payload)
          (let [x (double (or (:entity-x payload) (:target-x payload) 0.0))
                y (double (or (:entity-y payload) (:target-y payload) 0.0))
                z (double (or (:entity-z payload) (:target-z payload) 0.0))
                width (double (or (:target-width payload) 0.6))
                height (double (or (:target-height payload) 1.8))
                splash-count (+ 4 (rand-int 3))]
            (dotimes [_ splash-count]
              (let [theta (* (rand) Math/PI 2.0)
                    radius (* 0.5 width (+ 0.8 (* 0.2 (rand))))]
                (spawn-blood-splash!
                  (+ x (* radius (Math/sin theta)))
                  (+ y (* height (rand)))
                  (+ z (* radius (Math/cos theta))))))))
        (client-sounds/queue-sound-effect! (:queue-owner base-meta)
          {:type :sound :sound-id (modid/namespaced-path "tp.guts") :volume 0.6 :pitch 1.0})
        ;; Upstream c_endEffect kills the marker on MSG_EFFECT_END.
        (update state* :fx-state dissoc owner-key*))

      :end
      (update state* :fx-state dissoc owner-key*)

      state*)))

(defn- tick-state! [state]
  (let [state* (or state {:fx-state {}})]
    (update state* :fx-state
            (fn [states]
              (reduce-kv
                (fn [acc owner-key st]
                  (let [next-st (update st :ttl (fnil inc 0))]
                    (if (> (long (:ttl next-st)) stale-owner-ttl-ticks)
                      acc
                      (assoc acc owner-key next-st))))
                {}
                states)))))

(defn- build-plan [_camera-pos _hand-center-pos tick]
  (let [states (vals (:fx-state (level-effects/effect-state-snapshot :flesh-ripping)))
        marker-ops
        (vec
         (mapcat (fn [st]
                   (when (and (:active? st) (:aim st))
                     (marker-ops st tick)))
                 states))]
    (when (seq marker-ops)
      {:ops marker-ops})))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:flesh-ripping :level] [_ _] {:fx-state {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:flesh-ripping :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:flesh-ripping :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :flesh-ripping
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :flesh-ripping [_ store owner-key]
  (update store :fx-state dissoc owner-key))
