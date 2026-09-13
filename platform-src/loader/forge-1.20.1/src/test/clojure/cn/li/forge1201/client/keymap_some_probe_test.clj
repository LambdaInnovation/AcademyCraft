(ns cn.li.forge1201.client.keymap-some-probe-test
  "Verify some over (seq KeyMapping[]) yields KeyMapping elements."
  (:require [clojure.test :refer [deftest is]])
  (:import [net.minecraft.client KeyMapping]))

(deftest some-over-keymapping-array-test
  (let [km1 (KeyMapping. "key.probe.1" 0 "probe.category")
        km2 (KeyMapping. "key.probe.2" 0 "probe.category")
        arr (into-array KeyMapping [km1 km2])
        items (seq arr)
        seen (atom [])]
    (is (= "net.minecraft.client.KeyMapping" (.getName (.getComponentType (class arr)))))
    (is (= 2 (count items)))
    (let [r (some (fn [^KeyMapping x]
                    (swap! seen conj (.getName (class x)))
                    (identical? x km2))
                  items)]
      (is (true? r))
      (is (= ["net.minecraft.client.KeyMapping" "net.minecraft.client.KeyMapping"] @seen)))))
