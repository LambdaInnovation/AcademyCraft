(ns cn.li.fabricbase.runtime-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.fabricbase.runtime :as runtime]))

(deftest shared-runtime-installs-callbacks
  (testing "shared lifecycle state does not depend on a Minecraft version"
    (runtime/install! {:on-server-tick identity})
    (is (runtime/installed?))
    (is (= identity (:on-server-tick (runtime/callbacks))))))
