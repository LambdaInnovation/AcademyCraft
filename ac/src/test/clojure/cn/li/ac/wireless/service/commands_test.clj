(ns cn.li.ac.wireless.service.commands-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.world :as world]
            [cn.li.ac.wireless.service.commands :as commands]))

(use-fixtures :each support-fw/with-fresh-framework)

(defn- test-world
  [world-id]
  {:server-session-id :test-session
   :world-id world-id})

(deftest create-network-registers-indexes-test
  (let [wd (world/create-world-data (test-world :commands-world))
        matrix-vb (vb/create-vmatrix 0 0 0)]
    (is (:success (commands/create-network! wd matrix-vb "svc" "pw")))
    (is (not (:success (commands/create-network! wd matrix-vb "svc" "pw2"))))
    (let [network (lookup/get-network-by-ssid wd "svc")]
      (is (some? network))
      (is (identical? network (lookup/get-network-by-matrix wd matrix-vb))))))

(deftest create-network-returns-reason-on-ssid-exists-test
  (let [wd (world/create-world-data (test-world :commands-ssid-exists))
        matrix-vb (vb/create-vmatrix 0 0 0)]
    (is (:success (commands/create-network! wd matrix-vb "svc" "pw")))
    (let [result (commands/create-network! wd matrix-vb "svc" "pw2")]
      (is (not (:success result)))
      (is (= :ssid-exists (:reason result))))))

(deftest change-network-ssid-updates-index-test
  (let [wd (world/create-world-data (test-world :commands-rename))
        matrix-vb (vb/create-vmatrix 0 0 0)]
    (is (:success (commands/create-network! wd matrix-vb "old" "pw")))
    (let [network (lookup/get-network-by-ssid wd "old")]
      (is (:success (commands/change-network-ssid! network "new")))
      (let [renamed (lookup/get-network-by-ssid wd "new")]
        (is (= "new" (network-state/get-ssid renamed)))
        (is (nil? (lookup/get-network-by-ssid wd "old")))
        (is (some? renamed))))))

(deftest change-network-ssid-returns-reason-on-ssid-taken-test
  (let [wd (world/create-world-data (test-world :commands-ssid-taken))
        matrix-vb (vb/create-vmatrix 0 0 0)]
    (is (:success (commands/create-network! wd matrix-vb "net1" "pw")))
    (is (:success (commands/create-network! wd (vb/create-vmatrix 1 0 0) "net2" "pw")))
    (let [network1 (lookup/get-network-by-ssid wd "net1")]
      (let [result (commands/change-network-ssid! network1 "net2")]
        (is (not (:success result)))
        (is (= :ssid-taken (:reason result)))))))
