(ns cn.li.ac.block.wind-gen.logic
  "Wind Generator business logic and helpers."
  (:require [clojure.string :as str]
            [cn.li.ac.block.machine.container :as machine-container]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.wind-gen.config :as wind-config]
            [cn.li.ac.block.wind-gen.schema :as wind-schema]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.item :as item]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessGenerator IWirelessNode]))

(def ^:private main-rt (machine-runtime/schema-runtime wind-schema/wind-gen-main-schema :server-only? true))
(def ^:private base-rt (machine-runtime/schema-runtime wind-schema/wind-gen-base-schema :server-only? true))
(def ^:private pillar-rt (machine-runtime/schema-runtime wind-schema/wind-gen-pillar-schema :server-only? true))

(def main-default-state (:default-state main-rt))
(def base-default-state (:default-state base-rt))
(def pillar-default-state (:default-state pillar-rt))

(def main-scripted-load-fn (:load-fn main-rt))
(def main-scripted-save-fn (:save-fn main-rt))
(def base-scripted-load-fn (:load-fn base-rt))
(def base-scripted-save-fn (:save-fn base-rt))
(def pillar-scripted-load-fn (:load-fn pillar-rt))
(def pillar-scripted-save-fn (:save-fn pillar-rt))

;; Block identification
(def ^:private wind-main-ids #{"wind-gen-main" "wind-gen-main-part" "wind_gen_main" "wind_gen_main_part" "windgen_main" "windgen_main_part"})
(def ^:private wind-base-ids #{"wind-gen-base" "wind-gen-base-part" "wind_gen_base" "wind_gen_base_part" "windgen_base" "windgen_base_part"})
(def ^:private wind-pillar-ids #{"wind-gen-pillar" "wind_gen_pillar" "windgen_pillar"})
;; controller/part sets must include the upstream-aligned registry names
;; (windgen_main/windgen_base) — the other id sets gained them, these two were
;; missed, so main-controller-pos-at/base-controller-pos-at resolved nil and
;; valid-pillar-support? rejected every pillar placement.
(def ^:private wind-main-controller-ids #{"wind-gen-main" "wind_gen_main" "windgen_main"})
(def ^:private wind-main-part-ids #{"wind-gen-main-part" "wind_gen_main_part" "windgen_main_part"})
(def ^:private wind-base-controller-ids #{"wind-gen-base" "wind_gen_base" "windgen_base"})
(def ^:private wind-base-part-ids #{"wind-gen-base-part" "wind_gen_base_part" "windgen_base_part"})

(defn- id-path
  [x]
  (let [s (some-> x str str/lower-case)]
    (when (and s (not (str/blank? s)))
      (let [idx (.lastIndexOf s ":")]
        (if (neg? idx) s (subs s (inc idx)))))))

(defn- wind-main-id? [x] (contains? wind-main-ids (id-path x)))
(defn- wind-base-id? [x] (contains? wind-base-ids (id-path x)))
(defn- wind-pillar-id? [x] (contains? wind-pillar-ids (id-path x)))

(defn- sub-id-zero? [be]
  (zero? (long (get (or (platform-be/get-custom-state be) {}) :sub-id 0))))

(defn- fan-item-stack? [stack]
  (when (and stack (not (item/empty? stack)))
    (let [p (-> (try (some-> stack item/object item/registry-name) (catch Exception _ nil))
                id-path)]
      (= p "windgen_fan"))))

(defn- pillar-item-stack? [stack]
  (when (and stack (not (item/empty? stack)))
    (let [p (-> (try (some-> stack item/object item/registry-name) (catch Exception _ nil))
                id-path)]
      (wind-pillar-id? p))))

(defn- rotate-offset [direction [x y z]]
  (case (keyword (name (or direction :north)))
    :east [(- z) y x]
    :south [(- x) y (- z)]
    :west [z y (- x)]
    [x y z]))

(defn- no-obstacle? [level p _direction]
  ;; Upstream TileWindGenMain checks only the six blocks directly above the
  ;; rotor; weather particles and blocks around the tower do not obstruct it.
  (every? (fn [y-offset]
            (world/block-state-is-air
              (world/get-block-state level
                                     (pos/create-block-pos (pos/pos-x p)
                                                           (+ (pos/pos-y p) y-offset)
                                                           (pos/pos-z p)))))
          (range 1 7)))

(defn- find-base-below [level p]
  (loop [y (dec (pos/pos-y p)) pillars 0]
    (let [check-pos (pos/create-block-pos (pos/pos-x p) y (pos/pos-z p))
          be (world/get-tile-entity level check-pos)
          bid (when be (platform-be/get-block-id be))]
      (cond
        (wind-pillar-id? bid)
        (if (< pillars (wind-config/max-pillars)) (recur (dec y) (inc pillars)) nil)

        (wind-base-id? bid)
        {:base-pos check-pos :pillars pillars}

        :else nil))))

(defn- find-main-above-from-base [level base-pos]
  ;; Scan from base+2: the base multiblock occupies two vertical blocks
  ;; (controller + part at y+1), and upstream TileWindGenBase.updateMainTile
  ;; starts its loop at getPos().getY() + 2 — starting at base+1 lands on the
  ;; base part itself, which matches neither pillar nor main and judged every
  ;; tower BASE_ONLY (the base GUI's structure icons stayed dark).
  (loop [y (+ (pos/pos-y base-pos) 2) pillars 0]
    (let [check-pos (pos/create-block-pos (pos/pos-x base-pos) y (pos/pos-z base-pos))
          be (world/get-tile-entity level check-pos)
          bid (when be (platform-be/get-block-id be))]
      (cond
        (wind-pillar-id? bid)
        (if (< pillars (wind-config/max-pillars))
          (recur (inc y) (inc pillars))
          {:completeness :no-top})

        (wind-main-id? bid)
        (if (and be (sub-id-zero? be) (>= pillars (wind-config/min-pillars)))
          {:completeness :complete :main-pos check-pos :pillars pillars}
          {:completeness :no-top})

        :else
        {:completeness (if (< pillars (wind-config/min-pillars)) :base-only :no-top)}))))

(defn- completeness->status [completeness generating?]
  (case completeness
    :complete (if generating? "COMPLETE" "COMPLETE_NOT_WORKING")
    :no-top "NO_TOP"
    "BASE_ONLY"))

(defn- maybe-charge-output-item [state]
  (let [stack (get-in state [:inventory 0])
        cur (double (get state :energy 0.0))]
    (if (and stack (energy/is-energy-item-supported? stack) (pos? cur))
      (let [item-cur (double (energy/get-item-energy stack))
            item-max (double (energy/get-item-max-energy stack))
            need (max 0.0 (- item-max item-cur))
            amount (min cur need)
            leftover (double (energy/charge-energy-to-item stack amount false))
            accepted (max 0.0 (- amount leftover))]
        (if (pos? accepted)
          (assoc state :energy (- cur accepted))
          state))
      state)))

(defn main-tick-state
  [state level pos _block-state _be]
  (let [ticker (machine-runtime/advance-tick! state)
        state1 state]
    (if (zero? (mod ticker (wind-config/structure-update-interval)))
      (let [fan? (boolean (fan-item-stack? (get-in state1 [:inventory 0])))
            base-info (find-base-below level pos)
            complete? (and base-info (>= (:pillars base-info 0) (wind-config/min-pillars)))
            obstacle-free? (and complete? (no-obstacle? level pos (:direction state1 :north)))]
        (assoc state1
          :fan-installed fan?
          :complete (boolean complete?)
          :no-obstacle (boolean obstacle-free?)
          :status (if complete? "COMPLETE" "INCOMPLETE")))
      state1)))

(defn base-tick-state
  [state level pos _block-state _be]
  (let [ticker (machine-runtime/advance-tick! state)
        state1 state
        scan-info (when (zero? (mod ticker (wind-config/structure-update-interval)))
                    (find-main-above-from-base level pos))
        state2 (if scan-info
                 (let [comp (:completeness scan-info :base-only)
                       mpos (:main-pos scan-info)]
                   (cond-> (assoc state1 :completeness (name comp))
                     mpos (assoc :main-pos-x (pos/pos-x mpos)
                                 :main-pos-y (pos/pos-y mpos)
                                 :main-pos-z (pos/pos-z mpos))))
                 state1)
        main-pos (when (= (:completeness state2) "complete")
                   (let [mx (:main-pos-x state2)
                         my (:main-pos-y state2)
                         mz (:main-pos-z state2)]
                     (when (and (number? mx) (number? my) (number? mz))
                       (pos/create-block-pos mx my mz))))
        main-be (when main-pos (world/get-tile-entity level main-pos))
        main-state (when main-be (platform-be/get-custom-state main-be))
        working? (and (= (:completeness state2) "complete")
                      (true? (:no-obstacle main-state))
                      (true? (:fan-installed main-state)))
        gen-speed (if working? (wind-config/calculate-generation-rate (:main-pos-y state2)) 0.0)
        energy-before (double (get state2 :energy 0.0))
        max-energy (double (wind-config/max-energy-base))
        energy-after (min max-energy (+ energy-before gen-speed))
        state3 (assoc state2
                 :energy energy-after
                 :max-energy max-energy
                 :gen-speed (double gen-speed)
                 :status (completeness->status (keyword (:completeness state2 "base-only")) working?))]
    (maybe-charge-output-item state3)))

(defn pillar-tick-state
  [state _level _pos _block-state _be]
  (machine-runtime/advance-tick! state)
  state)

(defn- controller-tick-fn
  [tick-spec]
  (let [inner (machine-runtime/make-tick-fn tick-spec)]
    (fn [level pos block-state be]
      (when (sub-id-zero? be)
        (inner level pos block-state be)))))

(def main-tick-fn
  (controller-tick-fn {:default-state main-default-state
                       :tick-state main-tick-state
                       ;; render.clj reads :fan-installed/:no-obstacle/:complete directly.
                       :sync-client? true}))

(def base-tick-fn
  (controller-tick-fn {:default-state base-default-state
                       :tick-state base-tick-state
                       ;; render.clj reads :status directly (normal vs disabled texture).
                       :sync-client? true}))

(def pillar-tick-fn
  (machine-runtime/make-tick-fn {:default-state pillar-default-state
                                 :tick-state pillar-tick-state
                                 }))

(defn- main-controller-pos-at
  [level p bid]
  (let [be (world/get-tile-entity level p)
        st (when be (or (platform-be/get-custom-state be) {}))]
    (cond
      ;; Main controller block itself
      (and (contains? wind-main-controller-ids (id-path bid)) be (sub-id-zero? be))
      p

      ;; Main part block: resolve linked controller from part state
      (contains? wind-main-part-ids (id-path bid))
      (let [cx (:controller-pos-x st)
            cy (:controller-pos-y st)
            cz (:controller-pos-z st)]
        (when (and (number? cx) (number? cy) (number? cz))
          (let [cp (pos/create-block-pos cx cy cz)
                cbe (world/get-tile-entity level cp)
                cbid (when cbe (platform-be/get-block-id cbe))]
            (when (and (contains? wind-main-controller-ids (id-path cbid)) (sub-id-zero? cbe))
              cp))))

      :else
      nil)))

(defn- base-controller-pos-at
  [level p bid]
  (let [be (world/get-tile-entity level p)
        st (when be (or (platform-be/get-custom-state be) {}))]
    (cond
      ;; Base controller block itself
      (and (contains? wind-base-controller-ids (id-path bid)) be (sub-id-zero? be))
      p

      ;; Base part block: resolve linked controller from part state
      (contains? wind-base-part-ids (id-path bid))
      (let [cx (:controller-pos-x st)
            cy (:controller-pos-y st)
            cz (:controller-pos-z st)]
        (when (and (number? cx) (number? cy) (number? cz))
          (let [cp (pos/create-block-pos cx cy cz)
                cbe (world/get-tile-entity level cp)
                cbid (when cbe (platform-be/get-block-id cbe))]
            (when (and (contains? wind-base-controller-ids (id-path cbid)) (sub-id-zero? cbe))
              cp))))

      :else
      nil)))

(defn- pillar-column-aligned-with-main?
  [pillar-pos main-controller-pos]
  (and (= (pos/pos-x pillar-pos) (pos/pos-x main-controller-pos))
       (= (pos/pos-z pillar-pos) (pos/pos-z main-controller-pos))))

(defn- pillar-column-aligned-with-support?
  [pillar-pos support-pos]
  (and (= (pos/pos-x pillar-pos) (pos/pos-x support-pos))
       (= (pos/pos-z pillar-pos) (pos/pos-z support-pos))))

(defn- valid-pillar-support?
  [level p]
  ;; Rule:
  ;; - Pillar can be stacked on pillar.
  ;; - The bottom of that pillar chain must resolve to a valid wind-gen-base/main controller.
  ;; - Pillar column x/z must match that resolved controller's x/z.
  ;; - Total chain height is capped by wind-config/max-pillars.
  (loop [y (dec (pos/pos-y p))
         pillars 0]
    (let [check-pos (pos/create-block-pos (pos/pos-x p) y (pos/pos-z p))
          be (world/get-tile-entity level check-pos)
          bid (when be (platform-be/get-block-id be))]
      (cond
        (wind-pillar-id? bid)
        (if (< pillars (wind-config/max-pillars))
          (recur (dec y) (inc pillars))
          false)

        (wind-main-id? bid)
        (if-let [controller-pos (main-controller-pos-at level check-pos bid)]
          (pillar-column-aligned-with-main? p controller-pos)
          false)

        (wind-base-id? bid)
        (if-let [controller-pos (base-controller-pos-at level check-pos bid)]
          (pillar-column-aligned-with-main? p controller-pos)
          ;; Fallback for freshly-placed base-part where controller linkage may not
          ;; be persisted yet at this tick; still enforce same x/z column.
          (pillar-column-aligned-with-support? p check-pos))

        :else
        false))))

(defn on-wind-pillar-placed!
  [_player world pos _block-id]
  (when (and world pos (not (world/client-side? world)))
    (when-not (valid-pillar-support? world pos)
      (let [below-pos (pos/create-block-pos (pos/pos-x pos) (dec (pos/pos-y pos)) (pos/pos-z pos))
            below-be (world/get-tile-entity world below-pos)
            below-id (when below-be (platform-be/get-block-id below-be))]
        (log/debug "wind-gen pillar place rejected:"
                  {:pos [(pos/pos-x pos) (pos/pos-y pos) (pos/pos-z pos)]
                   :below-pos [(pos/pos-x below-pos) (pos/pos-y below-pos) (pos/pos-z below-pos)]
                   :below-id below-id
                   :below-id-path (id-path below-id)}))
      {:cancel-place? true
       :messages [{:type :literal
                   :text "wind_gen_pillar must be in the same x/z column above wind_gen_base or wind_gen_main controller."}]})))

(defn on-wind-main-placed!
  "Validate that wind-gen-main is placed on top of a wind-gen-pillar (not beside it).
   Returns {:cancel-place? true} if no pillar is found directly below the controller."
  [_player world pos _block-id]
  (when (and world pos (not (world/client-side? world)))
    (let [below-pos (pos/create-block-pos (pos/pos-x pos) (dec (pos/pos-y pos)) (pos/pos-z pos))
          below-be (world/get-tile-entity world below-pos)
          below-id (when below-be (platform-be/get-block-id below-be))]
      (when-not (wind-pillar-id? below-id)
        (log/debug "wind-gen-main place rejected: no pillar directly below"
                  {:pos [(pos/pos-x pos) (pos/pos-y pos) (pos/pos-z pos)]
                   :below-pos [(pos/pos-x below-pos) (pos/pos-y below-pos) (pos/pos-z below-pos)]
                   :below-id below-id
                   :below-id-path (id-path below-id)})
        {:cancel-place? true
         :messages [{:type :literal
                     :text "wind_gen_main must be placed on top of wind_gen_pillar."}]}))))

(def open-wind-main-gui!
  (machine-runtime/make-open-gui-handler-with-predicate
    :wind-gen-main
    (fn [_player _world _pos _sneaking item-stack]
      (not (pillar-item-stack? item-stack)))))

(def open-wind-base-gui!
  (machine-runtime/make-open-gui-handler-with-predicate
    :wind-gen-base
    (fn [_player _world _pos _sneaking item-stack]
      (not (pillar-item-stack? item-stack)))))

(defn get-linked-node ^IWirelessNode [tile]
  (let [gen-pos (try (pos/block-pos tile) (catch Exception _ nil))
        pos-str (when gen-pos (str (pos/pos-x gen-pos) "," (pos/pos-y gen-pos) "," (pos/pos-z gen-pos)))]
    (if-let [conn (try (wireless-api/get-node-conn-by-generator tile)
                       (catch Exception e
                         (log/debug "[wind-gen get-linked-node] exception:" (ex-message e))
                         nil))]
      (if-let [node (try (node-conn/get-node conn (platform-be/be-get-world-safe tile))
                       (catch Exception e
                         (log/debug "[wind-gen get-linked-node] get-node exception:" (ex-message e))
                         nil))]
        node
        (do
          (log/debug "[wind-gen get-linked-node] connection found but node tile not resolved for gen at" pos-str)
          nil))
      (do
        (log/debug "[wind-gen get-linked-node] no connection found for generator at" pos-str)
        nil))))

;; ============================================================================
;; Container fns — required for inventory drop-on-break (Containers.dropContents)
;; ============================================================================

(def main-container-fns
  "Container for wind-gen-main: single fan/blade slot."
  (machine-container/make-inventory-container-fns
    {:default-state main-default-state
     :slot-count (constantly 1)
     :inventory-key :inventory
     :can-place? (fn [_be _slot item _face]
                   (boolean (fan-item-stack? item)))
     :can-take? (fn [_be _slot _item _face] true)}))

(def base-container-fns
  "Container for wind-gen-base: single energy-item output slot."
  (machine-container/make-inventory-container-fns
    {:default-state base-default-state
     :slot-count (constantly 1)
     :inventory-key :inventory
     :can-place? (fn [_be _slot item _face]
                   (energy/is-energy-item-supported? item))
     :can-take? (fn [_be _slot _item _face] true)}))
