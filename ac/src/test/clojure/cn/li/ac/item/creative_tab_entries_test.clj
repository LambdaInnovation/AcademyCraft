(ns cn.li.ac.item.creative-tab-entries-test
  "Creative tab entry derivation regression — constraint_ingot and the other
  material items must surface as creative-tab entries with their :tab, or the
  platform's BuildCreativeModeTabContentsEvent silently skips them (the event
  handler drops entries whose :tab is nil)."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.item.materials :as materials]
            [cn.li.ac.item.constraint-plate :as constraint-plate]
            [cn.li.mcmod.protocol.metadata :as metadata]))

(use-fixtures :each support-fw/with-fresh-framework)

(deftest material-items-appear-in-creative-tab-entries-test
  (materials/init-materials!)
  (constraint-plate/init-constraint-plate!)
  (let [entries (metadata/get-all-creative-tab-entries)
        by-id (into {} (map (juxt :id identity)) entries)]
    (doseq [id ["imag_silicon_ingot" "imag_silicon_piece" "data_chip" "calc_chip"
                "constraint_ingot" "crystal_low" "crystal_normal" "crystal_pure"
                "constraint_plate"]]
      (is (some? (by-id id)) (str id " must be a creative-tab entry"))
      (is (= :misc (:tab (by-id id))) (str id " must carry :tab :misc")))
    (is (= 9 (count (filter #(= :item (:type %)) entries)))
        "all 9 material items present exactly once")))
