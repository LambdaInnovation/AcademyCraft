(ns cn.li.ac.block.imag-fusor.place-test
  "Imaginary Fusor placement tests: on-place writes the opposite of the
  placer's horizontal facing into the tile :facing field (upstream
  BlockImagFusor.onBlockPlacedBy parity), and passes the blockstate updater so
  the per-facing world BlockState follows. Client-side placements (prediction)
  are skipped entirely."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.block.imag-fusor.logic :as logic]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.world :as world]))

(defn- capture-place!
  "Run handle-fusor-place with stubbed platform ops; returns a map with the
  commit-transform! args plus the facing the transform would write (nil when
  the handler did not commit). The transform is applied INSIDE the with-redefs
  scope — the platform stubs only live for the duration of the call."
  [player-facing client-side?]
  (let [calls (atom nil)]
    (with-redefs [world/client-side? (constantly (boolean client-side?))
                  world/get-tile-entity (constantly :fake-be)
                  entity/player-get-horizontal-facing (constantly player-facing)
                  machine-runtime/commit-transform! (fn [tile default-state transform & commit-opts]
                                                      (let [committed {:tile tile
                                                                       :default-state default-state
                                                                       :transform transform
                                                                       :opts (apply hash-map commit-opts)}
                                                            facing (:facing (transform default-state))]
                                                        (reset! calls (assoc committed :facing facing))))]
      (logic/handle-fusor-place :player :world :pos "imag-fusor"))
    @calls))

(deftest fusor-place-writes-opposite-facing-test
  (let [{:keys [tile facing opts]} (capture-place! "east" false)]
    (is (= :fake-be tile) "commits against the placed tile entity")
    (is (= "west" facing) "facing is the opposite of the placer's horizontal facing")
    (is (contains? opts :blockstate-updater)
        "blockstate updater passed so the world BlockState follows"))
  (is (= "south" (:facing (capture-place! "north" false)))
      "every horizontal facing maps to its opposite"))

(deftest fusor-place-skips-client-side-test
  (is (nil? (capture-place! "east" true))
      "client-side placement (prediction) never commits"))
