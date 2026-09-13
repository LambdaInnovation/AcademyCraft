(ns cn.li.ac.block.render-runtime-state-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.block.machine.render-runtime :as machine-render-runtime]
            [cn.li.mcmod.client.render.init :as render-init]))

(deftest register-client-renderer-init-resolves-symbol-test
  (let [registered* (atom nil)
        invoked* (atom false)
        target-sym 'cn.li.ac.block.render-runtime-state-test/fake-render-init]
    (with-redefs [clojure.core/requiring-resolve
                  (fn [sym]
                    (when (= sym target-sym)
                      (fn [] (reset! invoked* true))))
                  render-init/register-renderer-init-fn!
                  (fn [f] (reset! registered* f))]
      (machine-render-runtime/register-client-renderer-init! target-sym)
      (is (fn? @registered*))
      (@registered*)
      (is (true? @invoked*)))))
