(ns cn.li.ac.test.support.framework
  "Shared clojure.test fixture installing a fresh Framework atom.

  Wireless (and other Framework-backed) state lives under the injected
  `cn.li.mcmod.framework/framework` atom; tests that mutate world state
  must install one per test for isolation."
  (:require [cn.li.mcmod.framework :as fw]))

(defn with-fresh-framework
  [f]
  (let [prev fw/framework]
    (try
      (when-let [fw-inst (fw/create-framework)]
        (alter-var-root #'fw/framework (constantly fw-inst)))
      (f)
      (finally
        (alter-var-root #'fw/framework (constantly prev))))))
