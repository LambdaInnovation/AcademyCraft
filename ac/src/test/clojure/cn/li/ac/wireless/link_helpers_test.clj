(ns cn.li.ac.wireless.link-helpers-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.wireless.link-helpers :as link-helpers]
            [cn.li.mcmod.platform.position :as pos])
  (:import [cn.li.acapi.wireless IWirelessNode]))

(use-fixtures :each
  (fn [f]
    (with-redefs [pos/pos-x (fn [p] (:x p))
                  pos/pos-y (fn [p] (:y p))
                  pos/pos-z (fn [p] (:z p))]
      (f))))

(defn- mock-node
  [{:keys [name password pos]}]
  (reify IWirelessNode
    (getEnergy [_] 0.0)
    (setEnergy [_ _] nil)
    (getMaxEnergy [_] 0.0)
    (getBandwidth [_] 0.0)
    (getCapacity [_] 0)
    (getRange [_] 0.0)
    (getNodeName [_] name)
    (getPassword [_] password)
    (getBlockPos [_] pos)))

(deftest wireless-node->info-test
  (let [node (mock-node {:name "Alpha"
                         :password "secret"
                         :pos {:x 1 :y 2 :z 3}})]
    (is (= {:node-name "Alpha"
            :pos-x 1 :pos-y 2 :pos-z 3
            :is-encrypted? true}
           (link-helpers/wireless-node->info node)))))

(deftest available-node-infos-excludes-linked-position-test
  (let [linked (mock-node {:name "Linked"
                          :password ""
                          :pos {:x 0 :y 0 :z 0}})
        nearby (mock-node {:name "Nearby"
                           :password "pw"
                           :pos {:x 4 :y 0 :z 0}})
        duplicate (mock-node {:name "Duplicate"
                              :password ""
                              :pos {:x 0 :y 0 :z 0}})]
    (is (= [{:node-name "Nearby"
             :pos-x 4 :pos-y 0 :pos-z 0
             :is-encrypted? true}]
           (link-helpers/available-node-infos [linked nearby duplicate] linked)))))

(deftest link-panel-state-test
  (let [linked (mock-node {:name "Current"
                           :password ""
                           :pos {:x 1 :y 1 :z 1}})
        avail-node (mock-node {:name "Other"
                               :password ""
                               :pos {:x 2 :y 2 :z 2}})]
    (is (= {:linked {:node-name "Current"
                     :pos-x 1 :pos-y 1 :pos-z 1
                     :is-encrypted? false}
            :avail [{:node-name "Other"
                     :pos-x 2 :pos-y 2 :pos-z 2
                     :is-encrypted? false}]}
           (link-helpers/link-panel-state linked [linked avail-node])))))

(deftest payload-node-position-test
  (testing "valid node position"
    (is (true? (link-helpers/valid-node-position?
                 (link-helpers/payload-node-position
                   {:node-x 1 :node-y 2 :node-z 3 :password "x"})))))
  (testing "invalid node position"
    (is (false? (link-helpers/valid-node-position?
                  (link-helpers/payload-node-position {:node-x 1 :node-z 3}))))))
