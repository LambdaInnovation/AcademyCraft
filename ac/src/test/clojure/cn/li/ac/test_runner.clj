(ns cn.li.ac.test-runner
  "Auto-discovers ac unit test namespaces and runs clojure.test.
   Supports -Dac.test.only=ns1,ns2 to run a subset."
  (:require [cn.li.test-support.auto-test-runner :as runner])
  (:gen-class))

(defn -main [& _]
  (runner/run-tests! {:root-segments ["src" "test" "clojure" "cn" "li" "ac"]
                      :base-ns "cn.li.ac"
                      :only-property "ac.test.only"}))
