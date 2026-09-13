(ns cn.li.mcmod.test-runner
  "Auto-discovers mcmod unit test namespaces and runs clojure.test.
   Supports -Dmcmod.test.only=ns1,ns2 to run a subset."
  (:require [cn.li.test-support.auto-test-runner :as runner])
  (:gen-class))

(defn -main [& _]
  (runner/run-tests! {:root-segments ["src" "test" "clojure" "cn" "li" "mcmod"]
                      :base-ns "cn.li.mcmod"
                      :only-property "mcmod.test.only"}))
