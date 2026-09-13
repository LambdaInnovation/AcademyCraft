(ns cn.li.mc1201.client.script-render-executor-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcbase.client.render.script-render-executor :as executor]))

(defn recording-executor
  [calls]
  {:draw! (fn [render-ctx draw-plan entity partial-tick]
            (swap! calls conj [render-ctx draw-plan entity partial-tick])
            nil)})

(use-fixtures :each
  (fn [f]
    (executor/call-with-script-render-executor-runtime
      (executor/create-script-render-executor-runtime)
      (fn []
        (f)))))

(deftest execute-draw-plan-uses-registered-executor-test
  (let [calls (atom [])]
    (executor/register-executor! :billboard-cross (recording-executor calls))
    (executor/execute-draw-plan! :ctx {:kind :billboard-cross} :entity 0.25)
    (is (= [[:ctx {:kind :billboard-cross} :entity 0.25]]
           @calls))
    (is (= #{:billboard-cross}
           (set (keys (executor/executors-snapshot)))))))

(deftest script-render-executor-runtime-isolation-test
  (let [runtime-b (executor/create-script-render-executor-runtime)
        calls-a (atom [])
        calls-b (atom [])]
    (executor/register-executor! :billboard-cross (recording-executor calls-a))
    (executor/call-with-script-render-executor-runtime
      runtime-b
      (fn []
        (executor/register-executor! :billboard-cross (recording-executor calls-b))
        (executor/execute-draw-plan! :ctx-b {:kind :billboard-cross} :entity-b 0.5)
        (is (= [[:ctx-b {:kind :billboard-cross} :entity-b 0.5]]
               @calls-b))
        (is (empty? @calls-a))))
    (executor/execute-draw-plan! :ctx-a {:kind :billboard-cross} :entity-a 0.1)
    (is (= [[:ctx-a {:kind :billboard-cross} :entity-a 0.1]]
           @calls-a))
    (is (= [[:ctx-b {:kind :billboard-cross} :entity-b 0.5]]
           @calls-b))))
