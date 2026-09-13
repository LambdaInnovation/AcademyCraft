(ns cn.li.mcmod.ui.kinds-lint-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcmod.ui.kinds-lint :as kinds-lint]
            [cn.li.mcmod.ui.node :as node]))

(deftest kinds-table-passes-lint
  (is (empty? (kinds-lint/lint-kinds node/kinds))))

(deftest all-documented-kinds-present
  (doseq [k [:group :box :image :text :progress :crosshair
             :shader-quad :shader-ring :shader-progress
             :gradient :line :list]]
    (is (contains? node/kinds k) (str "missing kind: " k))))

(deftest lint-catches-bad-writer-index
  (let [bad (assoc-in node/kinds [:text :prop-writers :color :idx] 99)
        errors (kinds-lint/lint-kinds bad)]
    (is (pos? (count errors)))
    (is (some #(re-find #"color" %) errors))))
