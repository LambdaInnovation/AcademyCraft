(ns cn.li.ac.block.wind-gen-logic-test
  "Wind generator structure-scan and client-sync regression tests.

  Two bugs fixed here:
  1. find-main-above-from-base scanned from base.y+1, but the base multiblock
     occupies two vertical blocks (controller + part at y+1) - the scan hit the
     part, matched neither pillar nor main, and judged every tower BASE_ONLY
     (base GUI structure icons stayed dark). Upstream starts at y+2.
  2. wind-gen-main-schema fields the TESR reads (:complete/:no-obstacle/
     :fan-installed) lacked :client-sync?, so the client BE custom-state never
     updated and the fan never rendered."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.wind-gen.config :as wind-config]
            [cn.li.ac.block.wind-gen.logic :as logic]
            [cn.li.ac.block.wind-gen.schema :as wind-schema]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.item :as item]
            [cn.li.mcmod.platform.world :as world]))

(def ^:private find-main-above-from-base
  (var-get #'cn.li.ac.block.wind-gen.logic/find-main-above-from-base))

(def ^:private no-obstacle?
  (var-get #'cn.li.ac.block.wind-gen.logic/no-obstacle?))

;; ============================================================================
;; Position + world mocks
;; ============================================================================

(defn- install-pos-mocks!
  "BlockPos -> [x y z] vectors."
  []
  (with-redefs [pos/create-block-pos (fn [x y z] [x y z])
                pos/pos-x (fn [p] (nth p 0))
                pos/pos-y (fn [p] (nth p 1))
                pos/pos-z (fn [p] (nth p 2))]))

(defn- tower-blocks
  "Full tower: base controller at y0, base part at y0+1, `n` pillars
   y0+2..y0+1+n, main on top. Returns map [x y z] -> block id."
  [x y0 z n]
  (merge {[x y0 z] "wind-gen-base"
          [x (inc y0) z] "wind-gen-base-part"}
         (into {} (map (fn [i] [[x (+ y0 2 i) z] "wind-gen-pillar"]) (range n)))
         {[x (+ y0 2 n) z] "wind-gen-main"}))

(defn- scan-completeness
  "Run find-main-above-from-base against `blocks` from base at (x,y0,z)."
  [blocks x y0 z]
  (install-pos-mocks!)
  (with-redefs [world/get-tile-entity (fn [_ p] (when-let [id (get blocks p)] {:id id}))
                platform-be/get-block-id (fn [be] (:id be))
                ;; the main block must read as the multiblock controller (sub-id 0)
                platform-be/get-custom-state (fn [_] {:sub-id 0})]
    (find-main-above-from-base nil [x y0 z])))

;; ============================================================================
;; find-main-above-from-base - scan starts at base+2 (above the base part)
;; ============================================================================

(deftest full-8-pillar-tower-is-complete
  (testing "scan must skip the base part at y+1: an 8-pillar tower is COMPLETE"
    (let [result (scan-completeness (tower-blocks 0 64 0 8) 0 64 0)]
      (is (= :complete (:completeness result)))
      (is (= 8 (:pillars result)))
      (is (= [0 74 0] (:main-pos result))))))

(deftest seven-pillar-tower-is-no-top
  (testing "fewer than min-pillars under the main is NO_TOP"
    (let [result (scan-completeness (tower-blocks 0 64 0 7) 0 64 0)]
      (is (= :no-top (:completeness result))))))

(deftest tower-without-main-is-no-top
  (testing "a pillar stack with no main on top is NO_TOP once past min-pillars"
    (let [blocks (-> (tower-blocks 0 64 0 8)
                     (dissoc [0 74 0]))  ;; remove the main
          result (scan-completeness blocks 0 64 0)]
      (is (= :no-top (:completeness result))))))

(deftest bare-base-is-base-only
  (testing "no pillars at all stays BASE_ONLY"
    (let [blocks (tower-blocks 0 64 0 0)  ;; main directly on the part
          result (scan-completeness blocks 0 64 0)]
      (is (= :base-only (:completeness result))))))

(deftest obstacle-check-only-scans-six-blocks-above-the-main
  (install-pos-mocks!)
  (with-redefs [world/get-block-state (fn [_ block-pos]
                                        (if (= block-pos [0 72 0]) :stone :air))
                world/block-state-is-air (fn [state] (= state :air))]
    (is (true? (no-obstacle? :level [0 64 0] :north))
        "a side block must not obstruct the rotor")
    (with-redefs [world/get-block-state (fn [_ block-pos]
                                          (if (= block-pos [0 67 0]) :stone :air))]
      (is (false? (no-obstacle? :level [0 64 0] :north))
          "a block within the six cells directly above must obstruct the rotor"))))

;; ============================================================================
;; main-tick-fn - structure fields must sync to the client (fan render)
;; ============================================================================

(deftest main-tick-syncs-client-state-on-structure-change
  (testing "placing the tower flips :complete/:no-obstacle, which must reach the
            client BE (render.clj reads custom-state to draw the fan)"
    (let [blocks (tower-blocks 5 64 5 8)
          sync-calls (atom 0)
          state-ref (atom nil)
          ;; inventory empty -> fan-installed stays false; complete flips
          ;; false->true on the first scan, so a sync must fire.
          with-mocks (fn [f]
                       (install-pos-mocks!)
                       (with-redefs [world/client-side? (fn [_] false)
                                     world/get-tile-entity (fn [_ p]
                                                             (when-let [id (get blocks p)]
                                                               {:id id}))
                                     world/get-block-state (fn [_ _] :air)
                                     world/block-state-is-air (fn [_] true)
                                     platform-be/get-block-id (fn [be] (:id be))
                                     platform-be/get-custom-state (fn [_]
                                                                    (or @state-ref {:sub-id 0}))
                                     platform-be/set-custom-state! (fn [_ st] (reset! state-ref st))
                                     platform-be/set-changed! (fn [_] nil)
                                     platform-be/sync-to-client! (fn [_] (swap! sync-calls inc))
                                     item/empty? (fn [_] true)
                                     wind-config/structure-update-interval (fn [] 1)
                                     wind-config/min-pillars (fn [] 8)
                                     wind-config/max-pillars (fn [] 40)]
                         (f)))]
      (with-mocks
        (fn []
          ;; The main block sits at the top of the 8-pillar tower (y0+10).
          (logic/main-tick-fn :level [5 74 5] nil {:id "wind-gen-main"})
          (is (pos? @sync-calls)
              "structure fields changed server-side must sync-to-client for the TESR")
          (let [committed @state-ref]
            (is (true? (get committed :complete)))
            (is (true? (get committed :no-obstacle)))))))))

(deftest main-schema-fields-carry-client-sync-flag
  (testing "the schema marks the TESR-read fields :client-sync? so MachineState
            field masks include the sync bit"
    (let [field-flags (reduce (fn [m spec] (assoc m (:key spec) spec))
                              {}
                              wind-schema/wind-gen-main-schema)
          sync-fields (keep (fn [[k spec]] (when (:client-sync? spec) k))
                            field-flags)]
      (is (every? sync-fields [:complete :no-obstacle :fan-installed])
          "render.clj reads these from client custom-state; without the flag
          the client BE never updates and the fan never draws"))))

(deftest base-schema-structure-fields-ride-the-data-slot-path
  (testing "the base GUI reads :completeness/:status from client container atoms,
            which only update through vanilla DataSlots - the fields must be
            encodable (gui-coerce + status codes) and not explicitly excluded"
    (let [fields (into {} (map (fn [s] [(:key s) s]))
                       wind-schema/wind-gen-base-schema)
          completeness (:completeness fields)
          status (:status fields)]
      (is (= str (:gui-coerce completeness))
          "without a coercion the codec is nil and the field never syncs")
      (is (seq (:gui-data-slot-status-codes completeness))
          "completeness needs a string-status codec to ride the DataSlot")
      (is (= (set ["base-only" "no-top" "complete" "complete-not-working"])
             (set (:gui-data-slot-status-codes completeness)))
          "codes must be the lower-case hyphenated values base-tick-state
          stores ((name comp)), and cover every scan outcome")
      (is (not (contains? completeness :gui-data-slot?))
          "an explicit :gui-data-slot? false permanently excludes the field")
      (is (= str (:gui-coerce status)))
      (is (every? (set (:gui-data-slot-status-codes status))
                  ["IDLE" "BASE_ONLY" "NO_TOP" "COMPLETE" "COMPLETE_NOT_WORKING"])
          "status codes must cover every value base-tick-state can produce"))))
