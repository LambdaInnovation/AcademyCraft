(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.mine-detect
  (:require [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.effects.rv3 :as vec3]
            [clojure.string :as str]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

(def ^:private mineview-texture
  (modid/namespaced-path "textures/effects/mineview.png"))

(def ^:private default-life-ticks 100)
(def ^:private default-rescan-interval 5)
(def ^:private max-client-range 28.0)
(def ^:private max-ore-results 8400)

(def ^:private default-ore-color
  {:r 220 :g 235 :b 255 :a 185})

(def ^:private advanced-tier-colors
  ;; Matching original MineDetect colors array (5 tiers):
  ;;   default(0) → harvest-level 0-3 mapped via Math.min(3, harvest+1)
  {0 {:r 161 :g 181 :b 188 :a 165}    ;; harvest level 0 → tier 1 (original index 1)
   1 {:r 87  :g 231 :b 248 :a 190}    ;; harvest level 1 → tier 2 (original index 2)
   2 {:r 97  :g 204 :b 94  :a 210}    ;; harvest level 2 → tier 3 (original index 3)
   3 {:r 235 :g 109 :b 84  :a 225}})   ;; harvest level 3 → tier 4 (original index 4)

(defn- ore-block?
  ([block-id]
   (ore-block? block-id nil))
  ([block-id {:keys [ore-tagged?]}]
   (and (string? block-id)
        (or ore-tagged?
            (str/includes? block-id "_ore")
            ;; Modern equivalent of a rare mineral block; retained as an
            ;; enhanced 1.20-era extension.
            (str/includes? block-id "ancient_debris")))))

(defn- clamped-range
  [range]
  (double (max 1.0 (min max-client-range (double (or range 20.0))))))

(defn- fallback-advanced-tier
  [block-id]
  (cond
    (or (str/includes? block-id "diamond")
        (str/includes? block-id "emerald")
        (str/includes? block-id "ancient_debris")) 2

    (or (str/includes? block-id "gold")
        (str/includes? block-id "redstone")
        (str/includes? block-id "lapis")) 1

    :else 0))

(defn- advanced-tier
  "Map ore harvest-level to color tier, matching original MineDetect:
   colors[Math.min(3, harvestLevel+1)], re-indexed to this 0-based 4-entry
   map (advanced-tier-colors key K == original colors[K+1]) — so the tier
   here is Math.min(2, harvestLevel), one less than the original index."
  [{:keys [harvest-level block-id]}]
  (if (number? harvest-level)
    (min 2 (long harvest-level))
    (fallback-advanced-tier (str block-id))))

(defn- ore-color
  [ore advanced?]
  (if advanced?
    (get advanced-tier-colors (advanced-tier ore) default-ore-color)
    default-ore-color))

(defn- faded-color
  "Alpha based on distance from player, matching original calcAlpha:
   alpha = 0.3 + (1 - (dist/range) * 2.2) * 0.7, clamped to [0.0, 1.0]"
  [color player-pos ore-x ore-y ore-z range]
  (let [dx (- (double ore-x) (double (:x player-pos 0.0)))
        dy (- (double ore-y) (double (:y player-pos 0.0)))
        dz (- (double ore-z) (double (:z player-pos 0.0)))
        dist (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
        jdg (max 0.0 (- 1.0 (* 2.2 (/ dist (double range)))))
        alpha-factor (+ 0.3 (* jdg 0.7))
        scaled-alpha (int (* (double (:a color)) (max 0.0 (min 1.0 alpha-factor))))]
    (ru/with-alpha color scaled-alpha)))

(defn- highlight-quad
  [texture p0 p1 p2 p3 color]
  ;; Upstream HandlerRender disables BOTH the depth test and the fog for the
  ;; ore-highlight pass (glDisable(GL_DEPTH_TEST) + glDisable(GL_FOG)): the
  ;; ores sit underground so a depth-tested quad would be hidden by the
  ;; terrain, and the blindness the skill itself applies would fog the boxes
  ;; out at range. The no-fog render path is no-depth, no-cull as well.
  (-> (ru/quad-op texture p0 p1 p2 p3 color)
      (assoc :no-depth-test? true)
      (assoc :no-fog? true)))

(defn- block-highlight-ops
  [x y z color]
  (let [eps 0.02
        x0 (- (double x) eps)
        y0 (- (double y) eps)
        z0 (- (double z) eps)
        x1 (+ (double x) 1.0 eps)
        y1 (+ (double y) 1.0 eps)
        z1 (+ (double z) 1.0 eps)
        v3 vec3/v3
        p000 (v3 x0 y0 z0) p100 (v3 x1 y0 z0) p110 (v3 x1 y1 z0) p010 (v3 x0 y1 z0)
        p101 (v3 x1 y0 z1) p001 (v3 x0 y0 z1) p011 (v3 x0 y1 z1) p111 (v3 x1 y1 z1)]
    [(highlight-quad mineview-texture p000 p100 p110 p010 color)
     (highlight-quad mineview-texture p101 p001 p011 p111 color)
     (highlight-quad mineview-texture p001 p000 p010 p011 color)
     (highlight-quad mineview-texture p100 p101 p111 p110 color)
     (highlight-quad mineview-texture p010 p110 p111 p011 color)
     (highlight-quad mineview-texture p001 p101 p100 p000 color)]))

(defn- should-rescan?
  [{:keys [ticks rescan-interval last-rescan-tick]}]
  (or (nil? last-rescan-tick)
      (>= (- (long ticks) (long last-rescan-tick))
          (long (max 1 (or rescan-interval default-rescan-interval))))))

(defn- player-position
  "Use the local player's feet position, matching HandlerEntity.posX/Y/Z.
  The :x/:y/:z fallback keeps the renderer contract compatible with callers
  that only provide the historic hand-center coordinates."
  [view-pos]
  {:x (double (or (:player-x view-pos) (:x view-pos) 0.0))
   :y (double (or (:player-y view-pos) (:y view-pos) 64.0))
   :z (double (or (:player-z view-pos) (:z view-pos) 0.0))})

(defn- rescan-ores
  [{:keys [range]} view-pos query-fn]
  (if (and (fn? query-fn) (map? view-pos))
      (let [{origin-x :x origin-y :y origin-z :z} (player-position view-pos)
            r (clamped-range range)]
        (->> (query-fn origin-x origin-y origin-z r ore-block?)
             (take max-ore-results)
             (map (fn [block]
                    {:x (int (:x block))
                     :y (int (:y block))
                     :z (int (:z block))
                     :block-id (:block-id block)
                     :harvest-level (:harvest-level block)}))
             distinct
             vec))
      []))

(defn- apply-perform!
  [owner-key ctx-id channel {:keys [range advanced? life-ticks rescan-interval source-player-id world-id]}]
  (client-sounds/queue-current-sound-effect!
    {:type :sound
     :sound-id (modid/namespaced-path "em.minedetect")
     :source :ambient
     :volume 0.5
     :pitch 1.0})
  {:owner-key owner-key
   :ctx-id ctx-id
   :channel channel
   :source-player-id source-player-id
   :world-id world-id
   :active? true
   :ticks 0
   :life-ticks (long (max 1 (or life-ticks default-life-ticks)))
   :rescan-interval (long (max 1 (or rescan-interval default-rescan-interval)))
   :last-rescan-tick nil
   :range (clamped-range range)
   :advanced? (boolean advanced?)
   :ores []})

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (if (contains? (or store {}) :effect-state)
                 (or store {:effect-state {}})
                 {:effect-state {}})
        owner-key* (or owner-key [:ctx ctx-id])]
    (case (:mode payload)
      :perform
      (assoc-in store* [:effect-state owner-key*]
                (apply-perform! owner-key* ctx-id channel payload))

      :end
      (update store* :effect-state dissoc owner-key*)

      store*)))

(defn- tick-state!
  [store]
  (let [store* (if (contains? (or store {}) :effect-state)
                 (or store {:effect-state {}})
                 {:effect-state {}})]
    (update store* :effect-state
      (fn [states]
        (reduce-kv (fn [acc owner-key st]
                     (if (:active? st)
                       (let [next-ticks (inc (long (:ticks st)))
                             life-ticks (long (:life-ticks st))]
                         (if (< next-ticks life-ticks)
                           (assoc acc owner-key (assoc st :ticks next-ticks))
                           acc))
                       acc))
                   {}
                   states)))))

(defn- maybe-refresh-ores!
  [owner-key hand-center-pos query-fn]
  (level-effects/update-effect-state!
    :mine-detect
    (fn [store]
      (let [store* (if (contains? (or store {}) :effect-state)
                     (or store {:effect-state {}})
                     {:effect-state {}})]
        (update-in store* [:effect-state owner-key]
                   (fn [st]
                     (if (and st (should-rescan? st))
                       (assoc st
                              :ores (rescan-ores st hand-center-pos query-fn)
                              :last-rescan-tick (:ticks st))
                       st)))))))

(defn- build-plan
  [_camera-pos hand-center-pos _tick query-fn]
  (when-let [[owner-key _] (some (fn [[owner-key st]]
                                   (when (:active? st)
                                     [owner-key st]))
                                 (:effect-state (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :mine-detect)))]
    (maybe-refresh-ores! owner-key hand-center-pos query-fn)
    (let [player-pos (player-position hand-center-pos)
          {:keys [ores advanced? range]} (get (:effect-state (cn.li.ac.ability.client.fx-templates.arc-beam/snapshot :mine-detect)) owner-key)
          ops (into []
                    (mapcat (fn [{:keys [x y z] :as ore}]
                              (let [base-color (ore-color ore advanced?)
                                    color (faded-color base-color player-pos
                                                       x y z range)]
                                (block-highlight-ops x y z color))))
                    ores)]
      (when (seq ops)
        {:ops ops}))))

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:mine-detect :level] [_ _] {:effect-state {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:mine-detect :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:mine-detect :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :mine-detect
  [_effect-id camera-pos hand-center-pos tick & [query-fn]]
  (build-plan camera-pos hand-center-pos tick query-fn))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :mine-detect [_ store _owner-key]
  ;; The ore-highlight visual is a one-shot world effect (upstream's
  ;; HandlerEntity) with its own 100-tick life. The :instant context ends
  ;; right after perform, so MSG-CTX-TERMINATED reaches clear-effect-owner!
  ;; on the same tick — clearing the owner here deleted the highlights a
  ;; frame after they appeared. They expire on their own life-ticks instead.
  store)
