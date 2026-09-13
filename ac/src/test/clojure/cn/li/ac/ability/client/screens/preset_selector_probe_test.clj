(ns cn.li.ac.ability.client.screens.preset-selector-probe-test
  "Headless probe: build the preset-editor selector off-screen and dump the
  render tape — diagnoses which selector children fail to lay out / bake
  without needing a Minecraft client."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.client.screens.preset-editor-reactive :as per]
            [cn.li.mcmod.ui.layout :as layout]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.dsl :as dsl])
  (:import [cn.li.mcmod.ui.node INode]))

(deftest selector-tape-probe-test
  (let [r (rt/create-runtime)]
    (rt/build! r (dsl/group {:id :root :w 400 :h 300}
                   (dsl/group {:id :selector :w 0 :h 0 :visible? false})))
    (rt/resize! r 400.0 300.0)
    ;; No owner → render-data nil → items = [cancel] only; enough to probe.
    (#'per/build-selector! r 0 0 120.0 150.0)
    (layout/ensure-layout! r)
    (let [tape (layout/ensure-tape! r)
          nodes (filterv #(instance? INode %) (vec tape))]
      (println "=== SELECTOR TAPE DUMP (" (count nodes) "nodes ) ===")
      (doseq [^INode n nodes]
        (println (format "%-22s %-7s vis=%-5s abs=(%.1f,%.1f) size=(%.1f,%.1f) scale=%.2f dslot0=%.3f oslot0=%s"
                         (str (.getId n)) (name (.getKind n)) (.isVisible n)
                         (.getAbsX n) (.getAbsY n)
                         (.getW n) (.getH n) (.getCumScale n)
                         (.getDSlot n 0)
                         (let [o (.getOSlot n 0)]
                           (if (string? o) (subs o 0 (min 40 (count o))) o)))))
      ;; The selector subtree must contain the glow images, the bg box and the
      ;; tooltip — all laid out at non-origin positions.
      (is (some #(= :sel-bg (.getId ^INode %)) nodes) "sel-bg missing from tape")
      (is (some #(= :sel-glow-up (.getId ^INode %)) nodes) "glow missing from tape")
      (is (some #(= :sel-tooltip-text (.getId ^INode %)) nodes) "tooltip text missing from tape"))))
