(ns cn.li.forge1201.capability.cap-invalidation-test
  (:require [clojure.test :refer [deftest is]]))

(defn- read-forge-cap-handler-source []
  (let [candidates ["platform-src/loader/forge-1.20.1/src/main/java/cn/li/forge1201/capability/ForgeCapabilityHandler.java"
                    "../platform-src/loader/forge-1.20.1/src/main/java/cn/li/forge1201/capability/ForgeCapabilityHandler.java"]]
    (some (fn [rel]
            (let [f (java.io.File. (str (System/getProperty "user.dir") "/" rel))]
              (when (.exists f) (slurp f))))
          candidates)))

(deftest capability-handler-matches-neoforge-orchestration-shape-test
  (let [src (read-forge-cap-handler-source)]
    (is (some? src))
    (when src
      (is (re-find #"static .* resolve\(" src))
      (is (re-find #"invalidateAt" src))
      (is (re-find #"LazyOptional" src))
      (is (nil? (re-find #"ForgeCapabilityCache" src))))))
