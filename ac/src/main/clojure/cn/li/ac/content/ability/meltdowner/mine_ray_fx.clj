(ns cn.li.ac.content.ability.meltdowner.mine-ray-fx
  "Client FX for all mine-ray variants: composite beam glow (mdray_small
  style, matching upstream EntityMineRayBasic's RendererRayComposite) + loop
  sound + block progress indicator."
  (:require [cn.li.ac.ability.client.effects.ray-composite :as ray-composite]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(def ^:private mine-ray-effect-id :mine-ray)

(defn- start-sound-id
  [variant]
  (case variant
    :expert (modid/namespaced-path "md.mine_expert_startup")
    :luck (modid/namespaced-path "md.mine_luck_startup")
    (modid/namespaced-path "md.mine_basic_startup")))

;; Upstream RendererRayComposite styles per variant:
;;  - basic: mdray_small — inner (216,248,216) w 0.03, outer (106,242,106) w
;;    0.045, glow 0.3 @0.5
;;  - expert: mdray_expert — inner (216,248,216) w 0.045, outer (106,242,106)
;;    w 0.056, glow 0.5 @0.7
;;  - luck: mdray_luck — inner (241,229,247) w 0.04, outer (205,166,232) w
;;    0.05, glow 0.45 @0.6
;; (the glow-board default 1.5 is the barrage fan's wide glow — a single
;; mining ray keeps the original's tight widths).
;; The cylinders are ShaderNotex (untextured solid colour) upstream; the
;; shared beam sprite reads the same at these radii.

(def ^:private mine-ray-styles
  {:basic  {:glow-textures "mdray_small"
            :outer-radius 0.045 :outer-rgb {:r 106 :g 242 :b 106} :outer-alpha 50.0
            :inner-radius 0.03  :inner-rgb {:r 216 :g 248 :b 216} :inner-alpha 230.0
            :glow-width 0.3     :glow-alpha 127.0}
   :expert {:glow-textures "mdray_expert"
            :outer-radius 0.056 :outer-rgb {:r 106 :g 242 :b 106} :outer-alpha 50.0
            ;; EntityMineRayExpert re-sets the inner colour every frame in
            ;; doRender, at alpha 180 rather than the constructor's 230 (and
            ;; the glow at 0.5, not the constructor's 0.7).
            :inner-radius 0.045 :inner-rgb {:r 216 :g 248 :b 216} :inner-alpha 180.0
            :glow-width 0.5     :glow-alpha 127.0}
   :luck   {:glow-textures "mdray_luck"
            :outer-radius 0.05  :outer-rgb {:r 205 :g 166 :b 232} :outer-alpha 50.0
            :inner-radius 0.04  :inner-rgb {:r 241 :g 229 :b 247} :inner-alpha 230.0
            :glow-width 0.45    :glow-alpha 153.0}})

;; EntityRayBase: blendIn 200ms, blendOut 400ms. A mining ray's life is
;; effectively unbounded (233333) so only the blend-in ever runs; the ray is
;; re-queued per tick here, so hold it at full strength.
(defn- mine-ray-ops
  "RendererRayComposite for a mining ray: the two cylinders plus the three
  glow boards, at the variant's own radii and colours.

  The port had a single tube pair with the outer radius 15-40% wide and the
  inner one derived from it by a 0.86 ratio — roughly 1.5x the original bore —
  and tinted the glow board with the outer cylinder's colour.

  The glow alpha stays a flat per-variant value rather than going through
  glow-alpha: upstream's getAlpha() is pinned at 1.0 by the ray's effectively
  unbounded life, so the double multiply is a no-op, and the ±10% glow wiggle
  is skipped along with the other random walks this re-queued ray has no
  state to carry."
  [camera-pos beam variant]
  (let [{:keys [glow-textures outer-radius outer-rgb outer-alpha
                inner-radius inner-rgb inner-alpha glow-width glow-alpha]}
        (get mine-ray-styles (or variant :basic) (:basic mine-ray-styles))]
    (ray-composite/composite-ops (vec3/map->v3 camera-pos) (:start beam) (:end beam)
      {:glow {:textures (ray-composite/glow-textures glow-textures)
              :width glow-width
              :color {:r 255 :g 255 :b 255 :a (int glow-alpha)}}
       :inner {:radius inner-radius :color (assoc inner-rgb :a (int inner-alpha))}
       :outer {:radius outer-radius :color (assoc outer-rgb :a (int outer-alpha))}})))

(def ^:private mine-ray-length 15.0)
(def ^:private loop-sound-id (modid/namespaced-path "md.mine_loop"))

(defn- loop-sound-key [ctx-id] (str "mine-ray/" ctx-id))

(defn- start-loop-sound! [ctx-id source-player-id]
  ;; Upstream c_start: FollowEntitySound(player, "md.mine_loop").setLoop()
  ;; at volume 0.3 — a loop sample, so it needs the loop-sound bridge.
  (client-bridge/run-client-effect!
    :mcmod/start-loop-sound-at-player
    {:key (loop-sound-key ctx-id)
     :sound-id loop-sound-id
     :owner-uuid (str source-player-id)
     :volume 0.3
     :pitch 1.0}))

(defn- stop-loop-sound! [ctx-id]
  (client-bridge/run-client-effect!
    :mcmod/stop-loop-sound
    {:key (loop-sound-key ctx-id)}))

(defn- look-dir-rad
  "Minecraft look vector from yaw/pitch in radians (hand-center-pos units):
  y = -sin(pitch) — xRot is positive looking DOWN (getLookAngle convention)."
  [yaw-rad pitch-rad]
  (let [cp (Math/cos (double pitch-rad))]
    {:dx (* -1.0 (Math/sin (double yaw-rad)) cp)
     :dy (* -1.0 (Math/sin (double pitch-rad)))
     :dz (* (Math/cos (double yaw-rad)) cp)}))

(defn- mine-ray-beam
  "The sightline beam: from the player's eye to 15 blocks along the look
  (upstream EntityMineRayBasic.updatePos), refreshed per frame from the live
  view context."
  [view-pos]
  (when (and (map? view-pos) (number? (:player-y view-pos)))
    (let [eye (vec3/v3 (double (or (:player-x view-pos) 0.0))
                       (+ (double (:player-y view-pos)) 1.62)
                       (double (or (:player-z view-pos) 0.0)))
          dir (look-dir-rad (:player-yaw-rad view-pos)
                            (:player-pitch-rad view-pos))]
      {:start eye
       :end (vec3/v+ eye (vec3/v3 (* mine-ray-length (:dx dir))
                                   (* mine-ray-length (:dy dir))
                                   (* mine-ray-length (:dz dir))))
       :ttl 1 :max-ttl 1})))

(defn default-mine-ray-fx-runtime-state
  []
  {:effect-state {}})

(defn mine-ray-fx-snapshot
  []
  (or (level-effects/effect-state-snapshot mine-ray-effect-id)
      (default-mine-ray-fx-runtime-state)))

(defn reset-mine-ray-fx-for-test!
  []
  (level-effects/reset-level-effect-state-for-test!
    mine-ray-effect-id
    (default-mine-ray-fx-runtime-state))
  nil)

(defn clear-mine-ray-owner!
  [owner-key]
  ;; Externally aborted contexts never get :end — stop the loop sound here
  ;; too (upstream c_end stops it on MSG_TERMINATED).
  (when (and (vector? owner-key) (= :ctx (first owner-key)))
    (stop-loop-sound! (second owner-key)))
  (level-effects/update-effect-state!
    mine-ray-effect-id
    (fn [store]
      (update (or store (default-mine-ray-fx-runtime-state)) :effect-state dissoc owner-key)))
  nil)

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store (default-mine-ray-fx-runtime-state))
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode variant x y z progress source-player-id world-id]} (or payload {})
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
          {:type :sound
           :sound-id (start-sound-id variant)
           :volume 0.4
           :pitch 1.0})
        (start-loop-sound! ctx-id source-player-id)
        (assoc-in store* [:effect-state owner-key*]
                  (merge base-meta {:active? true :ticks 0 :variant (or variant :basic)
                                    :target nil :progress 0.0})))
      :progress
      (if-let [st (get-in store* [:effect-state owner-key*])]
        (assoc-in store* [:effect-state owner-key*]
                  (assoc (merge base-meta st)
                         :owner-key owner-key*
                         :ctx-id ctx-id
                         :channel channel
                         :source-player-id source-player-id
                         :world-id world-id
                         :target {:x (int (or x 0)) :y (int (or y 0)) :z (int (or z 0))}
                         :progress (double (or progress 0.0))))
        store*)
      :end
      (do
        (stop-loop-sound! ctx-id)
        (update store* :effect-state dissoc owner-key*))
      store*)))

(defn- md-particle-type
  "Upstream MdParticleFactory sprite: the luck variant uses the golden
  md_particle_luck texture, basic/expert the standard md_particle."
  [variant]
  (if (= :luck (or variant :basic))
    (modid/namespaced-path "md_particle_luck")
    (modid/namespaced-path "md_particle")))

(defn- tick-state!
  [store]
  (let [store* (or store (default-mine-ray-fx-runtime-state))]
    (update store* :effect-state
      (fn [states]
        (into {}
              (keep (fn [[owner-key st]]
                      (when (:active? st)
                        (let [ticks (inc (long (or (:ticks st) 0)))]
                          ;; Upstream c_spawnParticles runs on EVERY same-block
                          ;; tick (2-3 particles per tick at the block).
                          (when-let [target (:target st)]
                            (client-particles/queue-particle-effect! (:queue-owner st)
                              {:type :particle
                               :particle-type (md-particle-type (:variant st))
                               :x (+ (double (:x target)) 0.5)
                               :y (+ (double (:y target)) 0.5)
                               :z (+ (double (:z target)) 0.5)
                               :count 3 :speed 0.1
                               :offset-x 0.3 :offset-y 0.3 :offset-z 0.3}))
                          [owner-key (assoc st :ticks ticks)]))))
              states)))))

(defn- progress-box-ops
  [target progress ticks variant]
  (let [x (double (:x target))
        y (double (:y target))
        z (double (:z target))
        c (case variant
            :luck {:r 255 :g 215 :b 0 :a 220}
            :expert {:r 100 :g 255 :b 100 :a 200}
            {:r 150 :g 220 :b 255 :a 180})
        alpha (int (* (:a c) (+ 0.5 (* 0.5 (Math/sin (* 0.3 (double ticks)))))))
        col (assoc c :a alpha)
        shrink (* 0.05 (- 1.0 progress))
        x0 (+ x shrink) y0 (+ y shrink) z0 (+ z shrink)
        x1 (- (+ x 1.0) shrink) y1 (- (+ y 1.0) shrink) z1 (- (+ z 1.0) shrink)]
    [(ru/line-op (vec3/v3 x0 y0 z0) (vec3/v3 x1 y0 z0) col)
     (ru/line-op (vec3/v3 x1 y0 z0) (vec3/v3 x1 y0 z1) col)
     (ru/line-op (vec3/v3 x1 y0 z1) (vec3/v3 x0 y0 z1) col)
     (ru/line-op (vec3/v3 x0 y0 z1) (vec3/v3 x0 y0 z0) col)
     (ru/line-op (vec3/v3 x0 y1 z0) (vec3/v3 x1 y1 z0) col)
     (ru/line-op (vec3/v3 x1 y1 z0) (vec3/v3 x1 y1 z1) col)
     (ru/line-op (vec3/v3 x1 y1 z1) (vec3/v3 x0 y1 z1) col)
     (ru/line-op (vec3/v3 x0 y1 z1) (vec3/v3 x0 y1 z0) col)
     (ru/line-op (vec3/v3 x0 y0 z0) (vec3/v3 x0 y1 z0) col)
     (ru/line-op (vec3/v3 x1 y0 z0) (vec3/v3 x1 y1 z0) col)
     (ru/line-op (vec3/v3 x1 y0 z1) (vec3/v3 x1 y1 z1) col)
     (ru/line-op (vec3/v3 x0 y0 z1) (vec3/v3 x0 y1 z1) col)]))

(defn- build-plan
  [camera-pos hand-center-pos _tick _query-fn]
  (let [states (vals (:effect-state (mine-ray-fx-snapshot)))
        ;; Composite beam along the caster's look — refreshed per frame,
        ;; hand-fixed for the caster's own first-person view (upstream's
        ;; ViewOptimize), end kept on the aim axis like the preray. The
        ;; composite follows the variant's renderer (basic/expert green,
        ;; luck purple).
        beam-ops (when (and (seq states) (map? hand-center-pos))
                   (when-let [beam (mine-ray-beam hand-center-pos)]
                     (let [variant (or (:variant (first states)) :basic)
                           fixed (arc-beam/view-fix-rays hand-center-pos [beam]
                                                         {:fix-end? false})]
                       (vec (mapcat #(mine-ray-ops camera-pos % variant) fixed)))))
        box-ops (mapcat (fn [st]
                          (when (and (:active? st) (:target st))
                            (progress-box-ops (:target st)
                                              (double (or (:progress st) 0.0))
                                              (long (or (:ticks st) 0))
                                              (or (:variant st) :basic))))
                        states)]
    (when (or (seq beam-ops) (seq box-ops))
      {:ops (vec (concat beam-ops box-ops))})))

(defn init!
  []
  (fx-spec/register!
    {:id mine-ray-effect-id
     :level {:initial-state (default-mine-ray-fx-runtime-state)
             :enqueue-state-fn enqueue-state!
             :tick-state-fn tick-state!
             :build-plan-fn build-plan
             ;; Externally aborted contexts (overload stun, category change,
             ;; death) never get :end — the clear path must stop the loop
             ;; sound and drop the state or the beam/box/particles linger
             ;; forever (upstream c_end runs on MSG_TERMINATED).
             :clear-owner-fn clear-mine-ray-owner!}
     :channels {:start {:topic :mine-ray/fx-start :mode :start
                        :level-payload (fn [_ _ p] {:variant (:variant p)})}
                :progress {:topic :mine-ray/fx-progress :mode :progress
                           :level-payload (fn [_ _ p]
                                            {:x (:x p) :y (:y p) :z (:z p)
                                             :progress (:progress p)})}
                :end {:topic :mine-ray/fx-end :mode :end}}})
  nil)
