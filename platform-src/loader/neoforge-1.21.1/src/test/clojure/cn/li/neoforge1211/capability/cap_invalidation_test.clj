(ns cn.li.neoforge1211.capability.cap-invalidation-test
  (:require [clojure.test :refer [deftest is]]))

(defn- read-neoforge-cap-handler-source []
  (let [candidates ["platform-src/loader/neoforge-1.21.1/src/main/java/cn/li/neoforge1211/capability/ForgeCapabilityHandler.java"
                    "../platform-src/loader/neoforge-1.21.1/src/main/java/cn/li/neoforge1211/capability/ForgeCapabilityHandler.java"]]
    (some (fn [rel]
            (let [f (java.io.File. (str (System/getProperty "user.dir") "/" rel))]
              (when (.exists f) (slurp f))))
          candidates)))

(deftest capability-handler-uses-neoforge-invalidation-test
  (let [src (read-neoforge-cap-handler-source)]
    (is (some? src))
    (when src
      (is (re-find #"invalidateCapabilities" src))
      (is (re-find #"RegisterCapabilitiesEvent" src))
      (is (nil? (re-find #"LazyOptional" src))))))
