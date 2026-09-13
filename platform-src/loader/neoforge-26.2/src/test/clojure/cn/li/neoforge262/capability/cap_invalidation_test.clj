(ns cn.li.neoforge262.capability.cap-invalidation-test
  "Source-level contract: 26.2 uses RegisterCapabilitiesEvent + invalidateCapabilities,
  and adapts handlers to ResourceHandler (no LazyOptional)."
  (:require [clojure.test :refer [deftest is]]))

(defn- read-source [rel-candidates]
  (some (fn [rel]
          (let [f (java.io.File. (str (System/getProperty "user.dir") "/" rel))]
            (when (.exists f) (slurp f))))
        rel-candidates))

(defn- read-neoforge-cap-handler-source []
  (read-source
   ["platform-src/loader/neoforge-26.2/src/main/java/cn/li/neoforge262/capability/ForgeCapabilityHandler.java"
    "../platform-src/loader/neoforge-26.2/src/main/java/cn/li/neoforge262/capability/ForgeCapabilityHandler.java"]))

(defn- read-legacy-boundary-source []
  (read-source
   ["platform-src/loader/neoforge-26.2/src/main/java/cn/li/neoforge262/capability/LegacyCapabilityBoundary.java"
    "../platform-src/loader/neoforge-26.2/src/main/java/cn/li/neoforge262/capability/LegacyCapabilityBoundary.java"]))

(deftest capability-handler-uses-neoforge-invalidation-test
  (let [src (read-neoforge-cap-handler-source)]
    (is (some? src))
    (when src
      (is (re-find #"invalidateCapabilities" src))
      (is (re-find #"RegisterCapabilitiesEvent" src))
      (is (nil? (re-find #"LazyOptional" src))))))

(deftest capability-boundary-uses-resource-handler-test
  (let [src (read-legacy-boundary-source)]
    (is (some? src))
    (when src
      (is (re-find #"ResourceHandler" src))
      (is (re-find #"adaptBlock" src))
      (is (re-find #"adaptItem" src))
      (is (nil? (re-find #"LazyOptional" src))))))
