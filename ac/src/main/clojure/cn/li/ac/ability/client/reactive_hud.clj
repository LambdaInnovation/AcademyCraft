(ns cn.li.ac.ability.client.reactive-hud
  "Reactive HUD snapshot — independent of build-client-overlay-plan.
   Reads player projection + hud.clj builders; no element-vector plan."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.ac.ability.client.debug-overlay :as debug-overlay]
            [cn.li.ac.ability.client.hud :as hud]
            [cn.li.ac.ability.client.keybinds :as keybinds]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.model.preset :as preset-data]
            [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.client.toast :as toast]
            [cn.li.ac.content.ability.meltdowner.jet-engine-fx :as jet-engine-fx]
            [cn.li.ac.tutorial.client.notification :as tutorial-notification]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks])
  (:import [java.util ArrayList HashMap]))

(def ^:private vm-wave-glow (modid/asset-path "textures" "effects/glow_circle.png"))
(def ^:private coin-dot-glow vm-wave-glow)
(def ^:private coin-dot-count 36)

(defn- owner-key [player-uuid]
  (read-model/owner-key {:player-uuid player-uuid} nil))

(defn- body-intensify-charge-state
  [player-uuid]
  ;; AC adapter hooks live in hooks-core, not the platform bridge — the
  ;; bridge has no :client-visual-state op, so call-adapter silently returned
  ;; nil and this layer never rendered.
  (or (runtime-hooks/client-visual-state :ac/body-intensify-charge
                                         {:player-uuid player-uuid})
      {:active? false :charge-ticks 0 :charge-ratio 0.0}))

(defonce ^:private ^HashMap vm-waves-by-owner (HashMap.))
(defonce ^:private ^HashMap vm-wave-spawn-by-owner (HashMap.))

;; ============================================================================
;; Charging arc particles — port of upstream CurrentChargingHUD/SubArc2D
;; (electromaster BodyIntensify "current charging" full-screen release cue):
;; a small ring of flickering arc sprites around the screen center, growing
;; into a denser burst the instant the charge is released.
;; ============================================================================

(def ^:private arc-frame-count 10)

(defn- arc-frame-src [idx]
  (modid/asset-path "textures" (str "effects/arcs/" idx ".png")))

(def ^:private arc-frame-srcs (mapv arc-frame-src (range arc-frame-count)))

(defonce ^:private ^HashMap arc-particles-by-owner (HashMap.))
;; {:started-ms ms} per owner, set by start-charging-blend! — upstream's
;; CurrentChargingHUD.blendTime field, which also gates disposal (1s).
(defonce ^:private ^HashMap charging-blend-by-owner (HashMap.))
;; started-ms/blend-out-ms per owner, driving the mask fade (BLEND_TIME/
;; BLEND_OUT_TIME below) — separate map/concern from the particle list above.
(defonce ^:private ^HashMap charging-fade-session-by-owner (HashMap.))

;; Upstream disposes the HUD one second after startBlend.
(def ^:private blend-dispose-ms 1000)

(defn- blending?
  "True while this owner is inside an active startBlend window. Read-only —
   eviction happens in tick-charging-arcs! so the render path stays pure."
  [ok now-ms]
  (when-let [blend (.get charging-blend-by-owner ok)]
    (<= (- (long now-ms) (long (:started-ms blend))) blend-dispose-ms)))

(defn- gen-arc
  "One SubArc2D: fixed (x,y) in [-1,1] screen-fraction coords (phi·sin/cosθ),
   fixed size, ticked texture-frame flicker + on/off flicker."
  [phi-lo phi-hi size-lo size-hi life frame-rate switch-rate]
  (let [phi (+ phi-lo (* (rand) (- phi-hi phi-lo)))
        theta (* (rand) 2.0 Math/PI)]
    {:x (* phi (Math/sin theta))
     :y (* phi (Math/cos theta))
     :size (+ size-lo (* (rand) (- size-hi size-lo)))
     :tex-idx (rand-int arc-frame-count)
     :tick 0
     :life (long life)
     :frame-rate (double frame-rate)
     :switch-rate (double switch-rate)
     :draw? true
     :dead? false}))

(defn- tick-arc
  "Port of SubArc2D.tick(): stochastic frame reroll, tick-with-90%-chance
   life countdown, and (for switch-rate>0 burst arcs) on/off flicker."
  [{:keys [tick life frame-rate switch-rate draw?] :as arc}]
  (let [tex-idx' (if (< (rand) (* 0.5 frame-rate)) (rand-int arc-frame-count) (:tex-idx arc))
        tick' (if (< (rand) 0.9) (inc (long tick)) (long tick))
        dead? (>= tick' (long life))
        draw?' (cond
                 (and draw? (< (rand) (* 0.4 switch-rate))) false
                 (and (not draw?) (< (rand) (* 0.3 switch-rate))) true
                 :else draw?)]
    (assoc arc :tex-idx tex-idx' :tick tick' :dead? dead? :draw? draw?')))

(defn start-charging-blend!
  "Port of CurrentChargingHUD.startBlend(regen), driven by body-intensify's
   MSG_EFFECT_END. Upstream clears the ambient charging ring and, only when
   the release actually performed, replaces it with a denser one-shot burst
   (10-15 arcs, phi 0.6-1, life 25 ticks); the mask then fades out over
   BLEND_OUT_TIME. Upstream's hud is isLocal-only, so a fanned-out FX message
   about somebody else's release must not touch this client's HUD."
  [source-player-id performed?]
  (when-let [local-uuid (uuid/player-uuid (bridge/get-client-player))]
    (when (or (nil? source-player-id)
              (= (str source-player-id) (str local-uuid)))
      (let [ok (owner-key local-uuid)
            burst (ArrayList.)]
        (when performed?
          (dotimes [_ (+ 10 (rand-int 6))]
            (.add burst (gen-arc 0.6 1.0 35.0 40.0 25 0.3 0.2))))
        (.put arc-particles-by-owner ok burst)
        (.put charging-blend-by-owner ok {:started-ms (bridge/game-time-ms)}))))
  nil)

(defn tick-charging-arcs!
  "Advance the arc-particle lifecycle for one player (client tick hook).
   Mirrors upstream SubArcHandler2D.tick() plus CurrentChargingHUD's spawn
   rules: an idle ring (5-7 arcs, phi 0.84-0.96) while charging, then whatever
   start-charging-blend! left behind (the release burst, or nothing on a failed
   release) ticked out to the end of the blend window."
  [player-uuid]
  (let [ok (owner-key player-uuid)
        now-ms (bridge/game-time-ms)
        {:keys [active? charge-ticks]} (body-intensify-charge-state player-uuid)
        blend-out? (boolean (blending? ok now-ms))
        charging? (boolean (or active? (pos? (long (or charge-ticks 0)))))
        ^ArrayList particles (.get arc-particles-by-owner ok)]
    (when (and (not blend-out?) (.containsKey charging-blend-by-owner ok))
      (.remove charging-blend-by-owner ok))
    (cond
      (and charging? (not blend-out?) (nil? particles))
      (let [n (+ 5 (rand-int 3))
            fresh (ArrayList.)]
        (dotimes [_ n]
          (.add fresh (gen-arc 0.84 0.96 25.0 30.0 233333 0.3 0.0)))
        (.put arc-particles-by-owner ok fresh))

      (and (not charging?) (not blend-out?) particles)
      (.remove arc-particles-by-owner ok)

      particles
      (let [pending (ArrayList. ^java.util.Collection particles)]
        (.clear particles)
        (doseq [arc pending]
          (let [arc' (tick-arc arc)]
            (when-not (:dead? arc') (.add particles arc')))))))
  nil)

(defn clear-charging-arcs-for-owner!
  [owner-key]
  (.remove arc-particles-by-owner owner-key)
  (.remove charging-blend-by-owner owner-key)
  (.remove charging-fade-session-by-owner owner-key)
  nil)

(defn- charging-mask-alpha
  "mAlpha: fade in over BLEND_TIME (500ms) since session start, held at 1.0
   while charging, then fade out over BLEND_OUT_TIME (200ms) once blending."
  ^double [session ^long now-ms]
  (if (nil? session)
    0.0
    (let [{:keys [started-ms blend-out-ms]} session]
      (if blend-out-ms
        (Math/max 0.0 (- 1.0 (/ (- now-ms (long blend-out-ms)) 200.0)))
        (Math/min 1.0 (/ (- now-ms (long started-ms)) 500.0))))))

(defn- update-charging-fade-session!
  "Upstream getTimeActive()/blendTime state machine: track when this charging
   session started (drives the 500ms fade-in) and when it began blending out
   (drives the 200ms fade-out), matching CurrentChargingHUD field semantics.
   The session lingers past charging?→false until its own fade-out finishes
   (started immediately if the game state never reports an explicit blending?
   phase), so the mask/arcs always get their full local fade-out."
  [ok charging? blending? now-ms]
  (when-let [session (.get charging-fade-session-by-owner ok)]
    (when (and (or blending? (not charging?)) (nil? (:blend-out-ms session)))
      (.put charging-fade-session-by-owner ok (assoc session :blend-out-ms now-ms))))
  (when (and charging? (nil? (.get charging-fade-session-by-owner ok)))
    (.put charging-fade-session-by-owner ok {:started-ms now-ms :blend-out-ms nil}))
  (let [session' (.get charging-fade-session-by-owner ok)]
    (when (and session' (not charging?) (<= (charging-mask-alpha session' now-ms) 0.0))
      (.remove charging-fade-session-by-owner ok))
    (.get charging-fade-session-by-owner ok)))

(defn- build-arc-particle-items
  "Reactive arc-particle sprites for one frame — screen position derived from
   each arc's fixed [-1,1] fraction, matching upstream's
   width/2 + xScale*x - size/2 (xScale = width/2)."
  [player-uuid screen-w screen-h now-ms]
  (let [ok (owner-key player-uuid)
        ^ArrayList particles (.get arc-particles-by-owner ok)
        alpha (if (blending? ok now-ms) 0.4 0.3)
        hw (/ (double screen-w) 2.0)
        hh (/ (double screen-h) 2.0)]
    (when particles
      (->> particles
           (filter :draw?)
           (mapv (fn [{:keys [x y size tex-idx]}]
                   {:src (nth arc-frame-srcs tex-idx)
                    :tint 0xFFFFFFFF
                    :x (int (- (+ hw (* hw (double x))) (/ size 2.0)))
                    :y (int (- (+ hh (* hh (double y))) (/ size 2.0)))
                    :w (int size)
                    :h (int size)
                    :alpha alpha}))))))

(defn- railgun-coin-active-threshold []
  (skill-config/tunable-double :railgun :qte.coin-active-threshold))

(defn- coin-qte-visual-state [player-uuid now-ms]
  ;; Same as body-intensify-charge-state: hooks-core adapter, not the bridge.
  (or (runtime-hooks/client-visual-state :ac/charge-coin
                                         {:player-uuid player-uuid :now-ms now-ms})
      {:active? false :coin-active? false :coin-progress 0.0}))

(defn- build-charging-layer [player-uuid _screen-w _screen-h now-ms]
  (let [ok (owner-key player-uuid)
        {:keys [active? charge-ticks]}
        (body-intensify-charge-state player-uuid)
        blend-out? (boolean (blending? ok now-ms))
        charging? (or active? (pos? (long (or charge-ticks 0))))
        session (update-charging-fade-session! ok charging? blend-out? now-ms)
        visible? (or charging? blend-out? session)]
    (when visible?
      (let [mask-alpha (charging-mask-alpha session now-ms)]
        {;; Upstream CurrentChargingHUD: black mask alpha = 0.1*mAlpha.
         :dim-a (int (* 0.1 mask-alpha 255.0))
         :mask-alpha mask-alpha}))))

(defn- build-coin-qte-layer [player-uuid screen-w screen-h now-ms]
  (let [coin-state (coin-qte-visual-state player-uuid now-ms)]
    (when (and (:active? coin-state) (pos? (:coin-progress coin-state)))
      (let [cx (int (/ screen-w 2))
            cy (int (/ screen-h 2))
            progress (double (:coin-progress coin-state))
            coin-active? (boolean (:coin-active? coin-state))
            threshold (double (railgun-coin-active-threshold))
            ring-radius 34
            dot-size 4
            threshold-color {:r 255 :g 235 :b 120 :a 255}
            bg-color {:r 20 :g 18 :b 10 :a 150}
            dots (for [i (range coin-dot-count)
                       :let [angle (* 2.0 Math/PI (/ i coin-dot-count))
                             dot-active? (< (/ i coin-dot-count) progress)
                             dx (int (* ring-radius (Math/cos angle)))
                             dy (int (* ring-radius (Math/sin angle)))]]
                   {:x (+ cx dx (- dot-size))
                    :y (+ cy dy (- dot-size))
                    :w (* 2 dot-size)
                    :h (* 2 dot-size)
                    :src coin-dot-glow
                    :tint (if coin-active?
                            (if dot-active? [255 215 0] [255 230 150])
                            (if dot-active? [230 190 70] [180 150 60]))
                    :alpha (if dot-active? 1.0 0.22)})
            threshold-angle (* 2.0 Math/PI threshold)
            tx (int (* ring-radius (Math/cos threshold-angle)))
            ty (int (* ring-radius (Math/sin threshold-angle)))
            marker-size 3]
        {:cx cx :cy cy
         :bg-disc {:x (- cx ring-radius) :y (- cy ring-radius)
                   :w (* 2 ring-radius) :h (* 2 ring-radius) :color bg-color}
         :dots dots
         :marker {:x (+ cx tx (- marker-size)) :y (+ cy ty (- marker-size))
                  :w (* 2 marker-size) :h (* 2 marker-size) :color threshold-color}
         :pct-text {:x (- cx 14) :y (- cy 4)
                    :text (str (int (* 100.0 progress)) "%")
                    :color (if coin-active?
                             {:r 255 :g 215 :b 0 :a 255}
                             {:r 230 :g 190 :b 70 :a 230})}}))))

;; WaveEffectUI: ripples appear ANYWHERE on the screen, avgSize * 0.8-1.2 px
;; across, growing 20 px a second, living 1.5-2.5 seconds, arriving on a
;; `nextFloat < deltaTime * intensity` roll. The two skills build their own with
;; DIFFERENT settings, so the parameters travel with the ripple rather than
;; being one shared constant.
(def ^:private vm-wave-params
  {:vec-reflection {:max-alpha 0.4 :avg-size 110.0 :intensity 1.6}
   :vec-deviation {:max-alpha 0.2 :avg-size 100.0 :intensity 1.4}})

(def ^:private vm-wave-growth-px-per-second 20.0)

(defn- vm-wave-param
  [skills k]
  (let [vs (keep #(get-in vm-wave-params [% k]) skills)]
    (if (seq vs) (apply max vs) (get-in vm-wave-params [:vec-reflection k]))))

(defn- spawn-vm-wave-circle [skills screen-w screen-h now-ms]
  (let [size (* (+ 0.8 (* (rand) 0.4)) (vm-wave-param skills :avg-size))
        life-ms (long (* 1000.0 (+ 1.5 (* (rand) 1.0))))]
    {:x (* (rand) (double screen-w))
     :y (* (rand) (double screen-h))
     :born-ms now-ms
     :life-ms life-ms
     :start-size size
     ;; realSize = size + timeAlive * 20
     :end-size (+ size (* vm-wave-growth-px-per-second (/ (double life-ms) 1000.0)))
     :max-alpha (vm-wave-param skills :max-alpha)
     :seed (rand)}))

(defn tick-vm-wave!
  "Advance VM wave circle lifecycle (client tick hook). `skills` is the set of
  VM skills currently drawing one."
  [player-uuid skills screen-w screen-h now-ms]
  (let [ok (owner-key player-uuid)
        active? (boolean (seq skills))
        ^ArrayList circles (.get vm-waves-by-owner ok)]
    (if-not active?
      ;; Upstream VecDeviationContextC.l_terminate unregisters the overlay
      ;; listener on MSG_TERMINATED, so the ripples vanish the same frame the
      ;; toggle ends — there is no 1.5-2.5s decay tail. Drop the whole circle
      ;; list on the first inactive tick instead of letting circles age out.
      (do
        (when circles (.clear circles))
        (.remove vm-waves-by-owner ok)
        (.remove vm-wave-spawn-by-owner ok))
      (let [circles* (or circles
                        (let [created (ArrayList.)]
                          (.put vm-waves-by-owner ok created)
                          created))]
        (loop [i (dec (.size circles*))]
          (when (>= i 0)
            (let [{:keys [born-ms life-ms]} (.get circles* i)]
              (when (>= (- now-ms (long born-ms)) (long life-ms))
                (.remove circles* (int i))))
            (recur (dec i))))
        ;; update(): `if (RandUtils.nextFloat < deltaTime * intensity)` -- a
        ;; Poisson-ish roll averaging `intensity` ripples a second, not a fixed
        ;; cadence. The port fired one every 90 ms, about seven times too many.
        (let [last-ms (long (or (.get vm-wave-spawn-by-owner ok) now-ms))
              delta-s (/ (double (max 0 (- now-ms last-ms))) 1000.0)]
          (when (< (rand) (* delta-s (vm-wave-param skills :intensity)))
            (.add circles* (spawn-vm-wave-circle skills screen-w screen-h now-ms)))
          (.put vm-wave-spawn-by-owner ok (long now-ms)))))
    nil))

(defn seed-vm-wave-state-for-test!
  ([owner circles]
   (seed-vm-wave-state-for-test! owner circles 0))
  ([owner circles last-spawn-ms]
   (let [ok (read-model/owner-key owner nil)]
     (.put vm-waves-by-owner ok (ArrayList. ^java.util.Collection circles))
     (.put vm-wave-spawn-by-owner ok (long last-spawn-ms))
     nil)))

(defn clear-vm-wave-for-owner!
  [owner-key]
  (.remove vm-waves-by-owner owner-key)
  (.remove vm-wave-spawn-by-owner owner-key)
  nil)

(defn build-vm-wave-items
  "Reactive VM wave circle items for one frame."
  [player-uuid now-ms tint]
  (when tint
    (->> (or (.get vm-waves-by-owner (owner-key player-uuid)) [])
         (map (fn [{:keys [x y born-ms life-ms start-size end-size max-alpha]}]
                (let [elapsed (double (max 0 (- now-ms (long born-ms))))
                      life (double (max 1 life-ms))
                      t (min 1.0 (/ elapsed life))
                      s (+ start-size (* (- end-size start-size) t))
                      ;; Ripple.alpha: up over the first fifth, held to the
                      ;; half, then straight down. Times maxAlpha.
                      alpha (* (double (or max-alpha 0.4))
                               (cond
                                 (< t 0.2) (/ t 0.2)
                                 (< t 0.5) 1.0
                                 :else (- 1.0 (/ (- t 0.5) 0.5))))
                      hs (/ s 2.0)]
                  {:src vm-wave-glow
                   :tint tint
                   :x (int (- x hs))
                   :y (int (- y hs))
                   :w (int s)
                   :h (int s)
                   :alpha (double (max 0.0 (min 1.0 alpha)))
                   })))
         (filter #(pos? (:alpha %)))
         vec)))

(defn build-vm-wave-overlay-elements
  "Plan-path bridge for tests — returns :blit-texture element maps."
  [player-uuid now-ms tint]
  (mapv (fn [item]
          (-> item
              (assoc :kind :blit-texture :texture (:src item))
              (dissoc :src)))
        (or (build-vm-wave-items player-uuid now-ms tint) [])))

(defn- build-overlay-app-ui [app screen-w screen-h]
  (case app
    :freq-tx {:panel {:x 0 :y 0 :w 640 :h 480 :color {:r 32 :g 32 :b 32 :a 192}}
              :title {:x 200 :y 10 :text "Frequency Transmitter (Overlay)" :color 0xFFFFFFFF}
              :subtitle {:x 200 :y 30 :text "Press ESC to close" :color 0xFF888888}}
    :install-fx (let [cx (quot screen-w 2) cy (quot screen-h 2)]
                  {:panel {:x (- cx 150) :y (- cy 20) :w 300 :h 40 :color {:r 32 :g 32 :b 32 :a 192}}
                   :title {:x (- cx 60) :y (- cy 5) :text "Installing terminal..." :color 0xFFFFFFFF}})
    nil))

(defn- build-hud-model [player-state activated?]
  (when player-state
    (let [resource-data (:resource-data player-state)
          ability-data (:ability-data player-state)
          preset-data-map (:preset-data player-state)
          category-id (:category-id ability-data)
          cat (when category-id (category/get-category category-id))]
      {:cp {:cur (double (or (:cur-cp resource-data) 0.0))
            :max (double (or (:max-cp resource-data) 1.0))}
       :overload (let [fine? (boolean (get resource-data :overload-fine true))
                       until-recover (long (or (:until-overload-recover resource-data) 0))]
                   {:cur (double (or (:cur-overload resource-data) 0.0))
                    :max (double (or (:max-overload resource-data) 1.0))
                    :fine fine?
                    ;; Upstream CPData.isOverloaded() = !overloadFine && untilOverloadRecover>0
                    ;; (the dramatic post-cap phase: drawOverload visual, hidden numbers).
                    :overloaded (and (not fine?) (pos? until-recover))
                    ;; Upstream CPData.isOverloadRecovering() = !overloadFine — stays true
                    ;; through the subsequent decay phase (drawNormal visual, dimmed CP fill).
                    :recovering (not fine?)})
       :active-slots (vec (preset-data/get-active-slots preset-data-map))
       :activated activated?
       :category-id category-id
       :category-color (:color cat)
       :category-icon (:icon cat)
       :interfered? (boolean (seq (:interferences resource-data)))})))

(defn- consumption-hint [contexts]
  (some
    (fn [ctx-data]
      (let [skill-id (:skill-id ctx-data)
            exp (double (or (:exp ctx-data) 0.0))]
        (some
          (fn [cost-path]
            (try
              (let [cost (skill-config/lerp-double skill-id cost-path exp)]
                (when (pos? cost) (double cost)))
              (catch Throwable _ nil)))
          [:cost.tick.cp :cost.down.cp :cost.up.cp :cost.release.cp :cost.attack.cp])))
    (filter ctx/active-context? contexts)))

(defn- background-mask [resource-data ability-data activated?]
  (let [category-id (:category-id ability-data)
        cat (when category-id (category/get-category category-id))
        cat-color (:color cat)
        ;; Match CPBar.isOverloaded() — the red overload cue belongs to the
        ;; post-cap phase only, not the whole !overloadFine recovery tail.
        overloaded? (and (not (get resource-data :overload-fine true))
                         (pos? (long (or (:until-overload-recover resource-data) 0))))]
    (cond
      overloaded? {:r 0.82 :g 0.08 :b 0.08 :a 0.65}
      (and activated? cat-color) {:r (double (nth cat-color 0))
                                  :g (double (nth cat-color 1))
                                  :b (double (nth cat-color 2))
                                  :a 0.35}
      :else {:r 0.0 :g 0.0 :b 0.0 :a 0.0})))

(defonce ^:private ^HashMap snapshot-cache-by-owner (HashMap.))

(defn- cached-frame-inputs
  "Recompute contexts/hud-model/background-mask/skill-slot-shape only when
  their pure inputs actually changed instead of every render frame.

  Two independent sub-keys, matching the split already proven correct for
  the (now-superseded) client-ui-hooks overlay-plan cache:
  - contexts/hud-model/background-mask key off whole player-state identity —
    resource-data (cp/overload) can legitimately change every server tick via
    continuous regen, and hud-model must reflect that, so this cache mostly
    saves the several render frames between two server ticks, not ticks
    themselves.
  - skill-slot-shape (registry/skill lookups) keys off preset-data identity
    alone, per build-skill-slot-shape's own documented contract: it depends
    only on active-slots (preset-data), not cooldown/context/resource data,
    so it must not be invalidated by cp/overload ticking or cooldown countdown.

  Server sync applies a whole-map replaceState on any player-state change
  (see runtime-store/set-player-state!), so `identical?` on player-state (and
  on the preset-data value nested within it) is a valid, zero-cost change
  token: guaranteed stale exactly when the underlying value would differ.
  Returns [contexts hud-model background-mask skill-slot-shape]."
  [ok player-uuid player-state activated? screen-w screen-h]
  (let [^objects prev (.get snapshot-cache-by-owner ok)
        preset-data (:preset-data player-state)
        state-fresh? (and prev (identical? (aget prev 0) player-state) (= (aget prev 1) activated?))
        [contexts hud-model bg-mask]
        (if state-fresh?
          [(aget prev 2) (aget prev 3) (aget prev 4)]
          (let [contexts (read-model/get-player-contexts-for-player player-uuid)
                hint (consumption-hint contexts)
                hud-model (cond-> (build-hud-model player-state activated?)
                            hint (assoc :consumption-hint hint))]
            [contexts hud-model
             (background-mask (:resource-data player-state) (:ability-data player-state) activated?)]))
        slots-fresh? (and prev (identical? (aget prev 5) preset-data)
                         (= (aget prev 6) screen-w) (= (aget prev 7) screen-h)
                         (= (boolean (:activated hud-model)) (boolean (aget prev 8))))
        skill-slot-shape (if slots-fresh?
                           (aget prev 9)
                           (when (:activated hud-model)
                             (hud/build-skill-slot-shape hud-model screen-w screen-h)))
        entry (object-array [player-state activated? contexts hud-model bg-mask
                             preset-data screen-w screen-h (boolean (:activated hud-model)) skill-slot-shape])]
    (.put snapshot-cache-by-owner ok entry)
    [contexts hud-model bg-mask skill-slot-shape]))

(defn clear-snapshot-cache-for-owner!
  [owner-key]
  (.remove snapshot-cache-by-owner owner-key)
  nil)

(defn fx-effect-active?
  "True when effect-id's client-side fx state carries an active entry — set by
  its fx-start channel, cleared by fx-end/terminate. Message-driven, so it
  lights HUD feedback on ANY connection topology, unlike the server-context
  projection, which only sees server contexts when client and server share
  one runtime (single-player). Matches build-movement-hints-data's 'Driven by
  the LEVEL FX state, not the client context mirror' precedent."
  [effect-id]
  (boolean
    (some (fn [[_k v]] (true? (:active? v)))
          (:effect-state (level-effects/effect-state-snapshot effect-id)))))

(defn fx-effect-ticks
  "Tick counter of effect-id's active client fx entry (incremented per fx
  tick), or 0. Fallback source for the reflection crosshair intensity when the
  server-context skill-state (total-ticks) is not visible on this client."
  [effect-id]
  (long
   (or (some (fn [[_k v]] (:ticks v))
             (:effect-state (level-effects/effect-state-snapshot effect-id)))
       0)))

(defn- scan-vm-state
  "Reduce over an already-fetched contexts list (see cached-frame-inputs) —
  callers must not re-fetch via read-model here, since build-snapshot's caller
  already pays for that fetch once per underlying player-state change.

  Active when the server context carries the toggle OR the client-side fx
  state says the effect is up (fx-start received) — the latter keeps the
  feedback working on dedicated/remote servers where the server context is
  never visible to the client render thread."
  [contexts]
  (let [fx-reflection (fx-effect-active? :vec-reflection)
        fx-deviation (fx-effect-active? :vec-deviation)]
    (reduce
      (fn [acc ctx-data]
        (if (ctx/active-context? ctx-data)
          (cond-> acc
            (or (toggle/is-toggle-active? ctx-data :vec-reflection) fx-reflection)
            (-> (assoc :reflection-active? true)
                (assoc :reflection-intensity
                       (let [ticks (long (or (get-in ctx-data [:skill-state :toggle :vec-reflection :total-ticks]) 0))
                             fx-ticks (fx-effect-ticks :vec-reflection)]
                         (double (min 1.0 (/ (max ticks fx-ticks) 20.0))))))
            (or (toggle/is-toggle-active? ctx-data :vec-deviation) fx-deviation)
            (assoc :deviation-active? true))
          acc))
      {:reflection-active? fx-reflection
       :deviation-active? fx-deviation
       :reflection-intensity 0.0}
      contexts)))

(defn with-fx-active-contexts
  "Append synthetic alive/active contexts for fx-active skills so skill-slot
  delegate state (context-to-delegate-state reads :input-state, which the
  server projection only writes in a shared single-player runtime) lights up
  on any connection topology. The synthetic maps carry exactly the fields
  delegate-state-for-slot matches on: skill-id and status, plus input-state."
  [contexts]
  (let [extra (keep (fn [effect-id]
                      (when (fx-effect-active? effect-id)
                        {:skill-id effect-id :status :alive :input-state :active}))
                    [:vec-reflection :vec-deviation])]
    (if (seq extra) (concat contexts extra) contexts)))

(def ^:private movement-hint-keys
  "Upstream StormWing/Flashing key-group order: forward/back/left/right with
  the vanilla WASD keycap labels."
  [[:forward "W" "前"]
   [:back "S" "后"]
   [:left "A" "左"]
   [:right "D" "右"]])

(def ^:private flashing-sub-key-icons
  "Upstream Flashing KEY_GROUP delegates: getIcon() returns
  abilities/teleporter/flashing/{w,s,a,d}.png per movement sub-key."
  {:forward "textures/abilities/teleporter/flashing/w.png"
   :back "textures/abilities/teleporter/flashing/s.png"
   :left "textures/abilities/teleporter/flashing/a.png"
   :right "textures/abilities/teleporter/flashing/d.png"})

(defn build-movement-hints-data
  "Upstream KeyHintUI's key-group column: while storm-wing is charging or
  flying, or flashing is active, show the four WASD hints in a column to the
  LEFT of the skill-slot hints, highlighting the key currently held.

  Driven by the LEVEL FX state (the same state that renders the wings /
  tp-marking), not the client context mirror — the mirror is not reliably
  synced with the skill-state phase."
  [player-uuid _contexts screen-w screen-h]
  (let [fx-state (fn [effect-id]
                   ;; Storm-wing stores its state under :effect-state, flashing
                   ;; under :fx-state — read whichever key the effect uses.
                   (vals (or (:effect-state
                              (level-effects/effect-state-snapshot effect-id))
                             (:fx-state
                              (level-effects/effect-state-snapshot effect-id)))))
        sw-storm
        (some (fn [st]
                (when (and (:active? st)
                           (contains? #{:charging :flying} (:phase st))
                           (or (nil? (:source-player-id st))
                               (= (str player-uuid)
                                  (str (:source-player-id st)))))
                  :storm-wing))
              (fx-state :storm-wing))
        flashing-active? (boolean (seq (fx-state :flashing)))
        key-state (keybinds/key-state-snapshot player-uuid)
        hint-item (fn [[movement-key key-label dir-label] icon-src]
                    {:key-label key-label
                     :label dir-label
                     :skill-icon icon-src
                     :active? (boolean (get-in key-state
                                               [:movement-keys movement-key]))})]
    (cond
      sw-storm
      (let [skill-icon (skill-query/get-skill-icon-path sw-storm)]
        {:kind :movement-hints
         :x (- screen-w 165)
         :y (- screen-h 100)
         :skill-icon skill-icon
         :items (mapv #(hint-item % skill-icon) movement-hint-keys)})

      flashing-active?
      {:kind :movement-hints
       :x (- screen-w 165)
       :y (- screen-h 100)
       :skill-icon (skill-query/get-skill-icon-path :flashing)
       :items (mapv (fn [[movement-key :as hint]]
                      (hint-item hint (modid/namespaced-path
                                       (flashing-sub-key-icons movement-key))))
                    movement-hint-keys)})))

(defn build-snapshot
  "Reactive HUD snapshot for one frame.
   opts: {:activated-override :showing-numbers? :last-show-value-change-ms :active-overlay-app :now-ms}"
  [player-uuid screen-w screen-h opts]
  (let [now-ms (long (or (:now-ms opts) (System/currentTimeMillis)))
        ;; VM-wave lifecycle runs on GAME time (upstream GameTimer.getTime =
        ;; worldTime + partialTick): tick-vm-wave! stamps circles with it and
        ;; the render-side alpha/expansion math must use the same clock. The
        ;; wall-clock now-ms (HUD animations) would make elapsed enormous and
        ;; every ripple's alpha collapse to 0.
        vm-wave-now-ms (long (bridge/game-time-ms))
        ok (owner-key player-uuid)
        player-state (read-model/get-player-state ok)
        resource-data (:resource-data player-state)
        activated? (if (some? (:activated-override opts))
                     (boolean (:activated-override opts))
                     (boolean (:activated resource-data)))
        [contexts hud-model bg-mask skill-slot-shape]
        (cached-frame-inputs ok player-uuid player-state activated? screen-w screen-h)
        cooldown-data (:cooldown-data player-state)
        skill-exps (get-in player-state [:ability-data :skill-exps])
        showing-numbers? (boolean (:showing-numbers? opts false))
        last-show-ms (long (or (:last-show-value-change-ms opts) 0))
        preset-state (keybinds/get-preset-switch-state player-uuid)
        activate-hint (keybinds/get-activate-hint player-uuid)
        cp-bar (when (:activated hud-model) (hud/build-cp-bar-render-data hud-model))
        overload-bar (when (:activated hud-model)
                       (hud/build-overload-bar-render-data hud-model now-ms))
        skill-slots (when skill-slot-shape
                      (-> skill-slot-shape
                  (hud/patch-skill-slot-cooldown cooldown-data {:player-id player-uuid
                                        :skill-exps skill-exps})
                          (hud/patch-skill-slot-visual
                           (with-fx-active-contexts contexts) player-uuid now-ms)))
        preset-indicators (hud/build-preset-indicators-data preset-state now-ms)
        numbers-texts (hud/build-numbers-texts-data hud-model showing-numbers? last-show-ms now-ms)
        vm (scan-vm-state contexts)
        vm-tint (cond
                  (:reflection-active? vm) [70 179 255]
                  (:deviation-active? vm) [90 255 120]
                  :else nil)
        phase (double (/ (mod now-ms 1200) 1200.0))
        ol-pct (double (or (:percent overload-bar) 0.0))
        overlay-app (:active-overlay-app opts)]
    {:overlay-app overlay-app
     :overlay-app-ui (when overlay-app (build-overlay-app-ui overlay-app screen-w screen-h))
     :background-mask bg-mask
     :interfered? (boolean (seq (:interferences resource-data)))
     :activated? activated?
     :cp-bar cp-bar
     :overload-bar overload-bar
     :cp-full-glow? (boolean (:full-glow? cp-bar))
     :skill-slots (or skill-slots [])
     :movement-hints (build-movement-hints-data player-uuid contexts screen-w screen-h)
     :activation-indicator (when (:activated hud-model)
                             (hud/build-activation-indicator-data hud-model activate-hint))
     :preset-indicators (or preset-indicators [])
     :numbers-texts (or numbers-texts [])
     :crosshair (when (:reflection-active? vm)
                  {:phase phase
                   :intensity (double (or (:reflection-intensity vm) 1.0))
                   :x (int (/ screen-w 2))
                   :y (int (/ screen-h 2))})
     :vm-waves (build-vm-wave-items player-uuid vm-wave-now-ms vm-tint)
     :charging (build-charging-layer player-uuid screen-w screen-h now-ms)
     :charging-arcs (or (build-arc-particle-items player-uuid screen-w screen-h now-ms) [])
     :coin-qte (build-coin-qte-layer player-uuid screen-w screen-h now-ms)
     :toasts (toast/build-toast-layouts screen-w screen-h now-ms)
     :tutorial-notification (tutorial-notification/build-notification-layout screen-w screen-h now-ms)
     :debug-lines (or (debug-overlay/build-debug-line-items player-state) [])
     :screen-flash-alpha (jet-engine-fx/flash-alpha player-uuid)
     :overload-pulse-intensity (when (> ol-pct 0.8) (* (- ol-pct 0.8) 5.0))
     :screen-w screen-w
     :screen-h screen-h}))
