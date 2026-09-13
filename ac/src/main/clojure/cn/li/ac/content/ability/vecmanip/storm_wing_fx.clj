(ns cn.li.ac.content.ability.vecmanip.storm-wing-fx
  "Client FX for Storm Wing: 4 tornado columns behind the caster (port of the
  original StormWingEffect, on top of the shared TornadoEffect/TornadoRenderer
  port in cn.li.ac.ability.client.effects.tornado), dirt dust particles, and an
  entity-following loop sound (original FollowEntitySound).

  Per frame build-plan applies the player-yaw/pitch + back-tilt transform
  (StormWingEffectRender) and hands the resulting linear map to the shared
  ring-quad emitter."
  (:require [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.tornado :as tornado]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(def ^:private storm-wing-effect-id :storm-wing)
(def ^:private loop-sound (modid/namespaced-path "vecmanip.storm_wing"))
(defn- loop-sound-key [ctx-id] (str "storm-wing/" ctx-id))

;; ---------------------------------------------------------------------------
;; Tornado constants (original TornadoEffect(2, 0.16, dscale=2.0))
;; ---------------------------------------------------------------------------

(def ^:private tornado-params {:ht 2.0 :sz 0.16 :dscale 2.0})
(def ^:private terminate-ticks 15) ;; original StormWingEffect TERMINATE_TICK

;; Original StormWingEffect tornadoList: setTransform +
;; setRotation(0, sepY, sepZ) — the Y and Z angles are independent per column.
(def ^:private tornado-transforms
  [{:ox -0.1 :oy -0.3 :oz 0.1 :sep-y 45 :sep-z 45}
   {:ox 0.1 :oy -0.3 :oz 0.1 :sep-y -45 :sep-z -45}
   {:ox -0.1 :oy -0.5 :oz -0.1 :sep-y -45 :sep-z 45}
   {:ox 0.1 :oy -0.5 :oz -0.1 :sep-y 45 :sep-z -45}])

;; Original StormWingEffectRender: glRotated(-70, 1, 0, 0) — the columns
;; lean back toward the player's back. Stored as a positive magnitude and
;; SUBTRACTED in phi so the effective rotation is exactly -70deg.
(def ^:private tornado-back-tilt-degrees 70.0)

;; ---------------------------------------------------------------------------
;; Enqueue / tick state
;; ---------------------------------------------------------------------------

(defn default-storm-wing-fx-runtime-state
  []
  {:effect-state {}})

(defn storm-wing-fx-snapshot
  []
  (or (level-effects/effect-state-snapshot storm-wing-effect-id)
      (default-storm-wing-fx-runtime-state)))

(defn reset-storm-wing-fx-for-test!
  []
  (level-effects/reset-level-effect-state-for-test!
    storm-wing-effect-id
    (default-storm-wing-fx-runtime-state))
  nil)

(defn clear-storm-wing-owner!
  [owner-key]
  (level-effects/update-effect-state!
    storm-wing-effect-id
    (fn [store]
      (update (or store (default-storm-wing-fx-runtime-state)) :effect-state dissoc owner-key)))
  nil)

(defn- stop-loop-sound! [ctx-id]
  (client-bridge/run-client-effect!
   :mcmod/stop-loop-sound
   {:key (loop-sound-key ctx-id)})
  nil)

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store (default-storm-wing-fx-runtime-state))
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode phase charge-ticks charge-ratio source-player-id world-id]} (or payload {})
        base-meta {:owner-key owner-key*
                   :queue-owner (client-particles/current-effect-owner)
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :start
      (do
        ;; Original c_makealive: FollowEntitySound loop attached to the caster.
        (client-bridge/run-client-effect!
         :mcmod/start-loop-sound-at-player
         {:key (loop-sound-key ctx-id)
          :sound-id loop-sound
          :owner-uuid (str source-player-id)
          :volume 0.5
          :pitch 1.0})
        (assoc-in store* [:effect-state owner-key*]
                  (merge base-meta
                         {:active? true :phase :charging :charge-ticks 0
                          :charge-ticks-needed (long (or charge-ticks 70))
                          :ticks 0
                          :tornadoes (mapv #(tornado/new-column tornado-params %)
                                           tornado-transforms)})))
      :update
      (assoc-in store* [:effect-state owner-key*]
                (assoc (merge base-meta (get-in store* [:effect-state owner-key*] {}))
                       :owner-key owner-key*
                       :queue-owner (or (get-in store* [:effect-state owner-key* :queue-owner])
                                        (:queue-owner base-meta))
                       :ctx-id ctx-id
                       :channel channel
                       :source-player-id source-player-id
                       :world-id world-id
                       :active? true
                       :phase (or phase :charging)
                       :charge-ticks (long (or charge-ticks 0))
                       :charge-ratio (double (or charge-ratio 0.0))))
      :end
      (do
        ;; Original c_terminate stops the loop sound at once, but
        ;; StormWingEffect keeps rendering for TERMINATE_TICK more ticks while
        ;; alpha fades to 0 before setDead().
        (stop-loop-sound! ctx-id)
        (if-let [st (get-in store* [:effect-state owner-key*])]
          (assoc-in store* [:effect-state owner-key*]
                    (assoc st :terminating? true :terminate-tick 0))
          (assoc-in store* [:effect-state owner-key*]
                    (merge base-meta {:active? false :ticks 0}))))
      store*)))

(defn- tick-state!
  [store]
  (let [store* (or store (default-storm-wing-fx-runtime-state))]
    (update store* :effect-state
      (fn [states]
        (into {}
              (keep (fn [[owner-key st]]
                      (when (:active? st)
                        (let [terminating? (boolean (:terminating? st))
                              term-tick (inc (long (or (:terminate-tick st) 0)))]
                          (when-not (and terminating? (> term-tick terminate-ticks))
                            (let [ticks (inc (long (or (:ticks st) 0)))
                                  phase (or (:phase st) :charging)
                                  ;; StormWingEffect.onUpdate's eff.alpha, kept
                                  ;; as the original 0..1 double — the extra
                                  ;; *0.7 for the vertex colour is applied once,
                                  ;; at render time.
                                  alpha (cond
                                          (= phase :charging)
                                          (* 0.7 (double (or (:charge-ratio st) 0.0)))
                                          (not terminating?) 0.7
                                          :else
                                          (* 0.7 (- 1.0 (/ (double term-tick)
                                                           (double terminate-ticks)))))]
                              [owner-key (cond-> (assoc st
                                                   :ticks ticks
                                                   :ring-alpha alpha
                                                   :tornado-rings
                                                   (mapv #(tornado/ring-states
                                                           (tornado/effect-time ticks)
                                                           tornado-params %)
                                                         (:tornadoes st)))
                                           terminating? (assoc :terminate-tick term-tick))]))))))
              states)))))

(defn- matching-active-state
  [effect-state hand-center-pos]
  (some (fn [st]
          (when (and (:active? st)
                     (or (nil? (:source-player-id st))
                         (nil? (:player-uuid hand-center-pos))
                         (= (str (:source-player-id st))
                            (str (:player-uuid hand-center-pos)))))
            st))
        (vals effect-state)))

(defn- maybe-spawn-particles!
  "Original StormWingContextC.c_tick: 12 dirt-dust particles per tick in a
  shell of radius 3..8 around the player's feet. It runs from make-alive to
  terminate, i.e. during the charge phase too — not only while flying — and
  gives each particle a tangential swirl velocity (the platform particle op
  multiplies :offset-* by :speed to get the spawn velocity)."
  [owner-key sw tick px py pz]
  (when (not= tick (:last-particle-tick sw))
    (level-effects/update-effect-state!
      storm-wing-effect-id
      (fn [store]
        (update-in (or store (default-storm-wing-fx-runtime-state))
          [:effect-state owner-key] assoc :last-particle-tick tick)))
    (dotimes [_ 12]
      (let [theta (* (rand) 2.0 Math/PI)
            phi (- (* (rand) 2.0 Math/PI) Math/PI)
            r (+ 3.0 (* (rand) 5.0))
            rzx (* r (Math/sin phi))
            cth (Math/cos theta)
            sth (Math/sin theta)]
        (client-particles/queue-particle-effect! (:queue-owner sw)
          {:type :particle
           :particle-type :block-crack
           :block-id "minecraft:dirt"
           :x (+ px (* rzx cth))
           :y (+ py (* r (Math/cos phi)))
           :z (+ pz (* rzx sth))
           :count 1
           :speed 1.0
           :offset-x (* sth 0.7)
           :offset-y (+ -0.01 (* (rand) 0.06))
           :offset-z (* (- cth) 0.7)}))))
  nil)

;; ---------------------------------------------------------------------------
;; Build plan - StormWingEffectRender's transform + TornadoRenderer's quads
;; ---------------------------------------------------------------------------

(defn- tornado-quad-ops
  "One tornado's rings transformed to world space.

  Original render chain: translate(player + (0,1.6,0)) -> rotY(-yaw) ->
  rotX(pitch*0.2 - 70deg) -> translate(0, 0.2, -0.5) -> CompTransform
  [translate(ox,oy,oz) -> rotY(sepY) -> rotZ(sepZ)] -> ring-local coords.

  That is affine, so it folds into an origin O and a linear map
  M = rotY(-yaw).rotX(phi).rotY(sepY).rotZ(sepZ): a ring corner is
  O + M*(x,y,z). Both are computed once per tornado per frame. The ring band's
  half-width runs along the column axis M*(0,1,0), which the tilt makes very
  much not world-up."
  [px py pz yaw-rad phi-rad alpha {:keys [ox oy oz sep-y sep-z]} rings]
  (let [cy (Math/cos (- yaw-rad)) sy (Math/sin (- yaw-rad))
        cp (Math/cos phi-rad) sp (Math/sin phi-rad)
        cys (Math/cos (Math/toRadians sep-y)) sys (Math/sin (Math/toRadians sep-y))
        czs (Math/cos (Math/toRadians sep-z)) szs (Math/sin (Math/toRadians sep-z))
        ;; rotY(-yaw) . rotX(phi) — the part outside the CompTransform.
        outer (fn [^double x ^double y ^double z]
                (let [qy (- (* y cp) (* z sp))
                      qz (+ (* y sp) (* z cp))]
                  [(+ (* x cy) (* qz sy)) qy (- (* qz cy) (* x sy))]))
        ;; M = outer . rotY(sepY) . rotZ(sepZ)
        m (fn [^double x ^double y ^double z]
            (let [a (- (* x czs) (* y szs))
                  b (+ (* x szs) (* y czs))]
              (outer (+ (* a cys) (* z sys)) b (- (* z cys) (* a sys)))))
        ;; O = player + (0,1.6,0) + outer*(0,0.2,-0.5) + outer*(ox,oy,oz).
        ;; oy/oz are what stagger the four columns in height and depth; the
        ;; previous port applied only ox, collapsing them onto one line.
        [hx hy hz] (outer 0.0 0.2 -0.5)
        [tx ty tz] (outer (double ox) (double oy) (double oz))
        origin-x (+ px hx tx)
        origin-y (+ py 1.6 hy ty)
        origin-z (+ pz hz tz)]
    (tornado/ring-quad-ops origin-x origin-y origin-z m alpha rings)))

(defn- build-plan
  [_camera-pos hand-center-pos tick _query-fn]
  (let [{:keys [effect-state]} (storm-wing-fx-snapshot)
        sw (matching-active-state effect-state hand-center-pos)]
    (when (and hand-center-pos sw (:active? sw))
      (let [px (double (or (:player-x hand-center-pos) 0.0))
            py (double (or (:player-y hand-center-pos) 0.0))
            pz (double (or (:player-z hand-center-pos) 0.0))
            ;; StormWingEffect tracks setRotation(player.renderYawOffset, ...) —
            ;; BODY yaw, so turning your head does not swing the columns.
            yaw (double (or (:player-body-yaw-rad hand-center-pos)
                            (:player-yaw-rad hand-center-pos)
                            0.0))
            pitch (double (or (:player-pitch-rad hand-center-pos) 0.0))
            phi (- (* 0.2 pitch) (Math/toRadians tornado-back-tilt-degrees))]
        (maybe-spawn-particles! (:owner-key sw) sw tick px py pz)
        (let [ops (into []
                        (mapcat (fn [[tf rings]]
                                  (tornado-quad-ops
                                   px py pz yaw phi (:ring-alpha sw) tf rings)))
                        (map vector (:tornadoes sw) (:tornado-rings sw)))]
          (when (seq ops)
            {:ops ops}))))))

(defn init!
  []
  (fx-spec/register!
    {:id storm-wing-effect-id
     :level {:initial-state (default-storm-wing-fx-runtime-state)
             :enqueue-state-fn enqueue-state!
             :tick-state-fn tick-state!
             :build-plan-fn build-plan
             :clear-owner-fn (fn [store owner-key]
                               (stop-loop-sound! (second owner-key))
                               (update (or store (default-storm-wing-fx-runtime-state))
                                       :effect-state dissoc owner-key))}
     :channels {:start {:topic :storm-wing/fx-start :mode :start
                        :level-payload (fn [_ _ p]
                                         {:charge-ticks (long (or (:charge-ticks p) 70))})}
                :update {:topic :storm-wing/fx-update :mode :update
                         :level-payload (fn [_ _ p]
                                          {:phase (or (:phase p) :charging)
                                           :charge-ticks (long (or (:charge-ticks p) 0))
                                           :charge-ratio (double (or (:charge-ratio p) 0.0))})}
                :end {:topic :storm-wing/fx-end :mode :end}}})
  nil)
