(ns cn.li.ac.wireless.api-event-regression-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.test.support.wireless-stubs :as stubs]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.ac.wireless.service.commands :as commands]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.mcmod.platform.be :as platform-be]))

(deftest create-network-fires-topology-map-when-created
  (testing "create-network! posts map event when create succeeds"
    (let [events (atom [])
          matrix (stubs/fake-matrix)]
      (with-redefs [platform-be/be-get-world-safe (fn [_] :world)
                    world-registry/get-world-data (fn [_] :world-data)
                    vb/create-vmatrix (fn [_] :matrix-vb)
                    cn.li.ac.wireless.data.network-lookup/get-network-by-matrix (constantly nil)
                    commands/create-network! (fn [_ _ _ _] {:success true})
                    resolver/matrix-capability (fn [tile]
                                                 (when (= tile :matrix-tile) matrix))
                    wireless-api/fire-event! (fn [evt] (swap! events conj evt))]
        (is (:success (wireless-api/create-network! :matrix-tile "ssid-a" "pw")))
        (is (= 1 (count @events)))
        (let [event (first @events)]
          (is (= :topology/network (:kind event)))
          (is (= :created (:action event)))
          (is (= "ssid-a" (:ssid event)))
          (is (= matrix (:matrix event))))))))

(deftest create-network-no-event-when-not-created
  (let [events (atom [])]
    (with-redefs [platform-be/be-get-world-safe (fn [_] :world)
                  world-registry/get-world-data (fn [_] :world-data)
                  vb/create-vmatrix (fn [_] :matrix-vb)
                  cn.li.ac.wireless.data.network-lookup/get-network-by-matrix (constantly nil)
                  commands/create-network! (fn [_ _ _ _] {:success false})
                  resolver/matrix-capability (constantly (stubs/fake-matrix))
                  wireless-api/fire-event! (fn [evt] (swap! events conj evt))]
      (is (not (:success (wireless-api/create-network! :matrix-tile "ssid-a" "pw"))))
      (is (empty? @events)))))

(deftest destroy-network-fires-when-destroyed
  (let [events (atom [])
        matrix (stubs/fake-matrix)
      net (network-state/create-wireless-net {:world :world} :matrix-vb "ssid-z" "pw")]
    (with-redefs [wireless-api/get-wireless-net-by-matrix (fn [_] net)
                  platform-be/be-get-world-safe (fn [_] :world)
                  world-registry/get-world-data (fn [_] :world-data)
                  commands/destroy-network! (fn [_ _] {:success true})
                  resolver/matrix-capability (fn [tile]
                                               (when (= tile :mt) matrix))
                  wireless-api/fire-event! (fn [evt] (swap! events conj evt))]
      (is (:success (wireless-api/destroy-network! :mt)))
      (is (= 1 (count @events)))
      (let [event (first @events)]
        (is (= :destroyed (:action event)))
        (is (= "ssid-z" (:ssid event)))))))

(deftest is-node-linked-uses-network-lookup
  (with-redefs [wireless-api/get-wireless-net-by-node (fn [_] {:ssid "x"})]
    (is (true? (wireless-api/is-node-linked? :tile))))
  (with-redefs [wireless-api/get-wireless-net-by-node (fn [_] nil)]
    (is (false? (wireless-api/is-node-linked? :tile)))))

(deftest link-node-fires-connected-map
  (testing "link-node-to-network! posts topology map when link succeeds"
    (let [events (atom [])
          matrix (stubs/fake-matrix)
          node (stubs/fake-node "pw")]
      (with-redefs [wireless-api/get-wireless-net-by-matrix (fn [_] {:ssid "ssid-a"})
                    platform-be/be-get-world-safe (fn [_] :world)
                    world-registry/get-world-data (fn [_] :world-data)
                    vb/create-vnode (fn [_] :node-vb)
                    commands/link-node-to-network! (fn [_ _ _ _ _] {:success true})
                    resolver/matrix-capability (fn [tile]
                                                   (when (= tile :matrix-tile) matrix))
                    resolver/node-capability (fn [tile]
                                                 (when (= tile :node-tile) node))
                    wireless-api/fire-event! (fn [evt] (swap! events conj evt))]
        (is (:success (wireless-api/link-node-to-network! :node-tile :matrix-tile "pw")))
        (is (= 1 (count @events)))
        (let [event (first @events)]
          (is (= :topology/node (:kind event)))
          (is (= :connected (:action event)))
          (is (= matrix (:matrix event)))
          (is (= node (:node event))))))))

(deftest link-generator-fires-generator-linked-map
  (testing "link-generator-to-node! posts map when auth and link succeeds"
    (let [events (atom [])
          node (stubs/fake-node "pw")
          gen (stubs/fake-generator)]
      (with-redefs [platform-be/be-get-world-safe (fn [_] :world)
                    world-registry/get-world-data (fn [_] :world-data)
                    vb/create-vnode-conn (fn [_] :node-conn-vb)
                    commands/ensure-node-connection! (fn [_ _] :conn)
                    vb/create-vgenerator (fn [_] :gen-vb)
                    commands/link-generator-to-connection! (fn [_ _ _ _] {:success true})
                    resolver/node-capability (fn [tile]
                                                 (when (= tile :node-tile) node))
                    resolver/generator-capability (fn [tile]
                                                    (when (= tile :gen-tile) gen))
                    wireless-api/fire-event! (fn [evt] (swap! events conj evt))]
        (is (:success (wireless-api/link-generator-to-node! :gen-tile :node-tile "pw" true)))
        (is (= 1 (count @events)))
        (let [event (first @events)]
          (is (= :generator-linked (:action event)))
          (is (= node (:node event)))
          (is (= gen (:generator event))))))))

(deftest link-generator-fails-on-wrong-password
  (testing "link-generator-to-node! returns password reason when auth fails"
    (let [events (atom [])
          node (stubs/fake-node "correct-pw")]
      (with-redefs [resolver/node-capability (fn [tile]
                                               (when (= tile :node-tile) node))
                    wireless-api/fire-event! (fn [evt] (swap! events conj evt))]
        (let [result (wireless-api/link-generator-to-node! :gen-tile :node-tile "wrong-pw" true)]
          (is (not (:success result)))
          (is (= :password (:reason result)))
          (is (empty? @events)))))))

(deftest link-receiver-fires-receiver-linked-map
  (testing "link-receiver-to-node! posts map when auth and link succeeds"
    (let [events (atom [])
          node (stubs/fake-node "pw")
          rec (stubs/fake-receiver)]
      (with-redefs [platform-be/be-get-world-safe (fn [_] :world)
                    world-registry/get-world-data (fn [_] :world-data)
                    vb/create-vnode-conn (fn [_] :node-conn-vb)
                    commands/ensure-node-connection! (fn [_ _] :conn)
                    vb/create-vreceiver (fn [_] :rec-vb)
                    commands/link-receiver-to-connection! (fn [_ _ _ _] {:success true})
                    resolver/node-capability (fn [tile]
                                                 (when (= tile :node-tile) node))
                    resolver/receiver-capability (fn [tile]
                                                   (when (= tile :rec-tile) rec))
                    wireless-api/fire-event! (fn [evt] (swap! events conj evt))]
        (is (:success (wireless-api/link-receiver-to-node! :rec-tile :node-tile "pw" true)))
        (is (= 1 (count @events)))
        (let [event (first @events)]
          (is (= :receiver-linked (:action event)))
          (is (= node (:node event)))
          (is (= rec (:receiver event))))))))

(deftest link-receiver-fails-on-wrong-password
  (testing "link-receiver-to-node! returns password reason when auth fails"
    (let [events (atom [])
          node (stubs/fake-node "correct-pw")]
      (with-redefs [resolver/node-capability (fn [tile]
                                               (when (= tile :node-tile) node))
                    wireless-api/fire-event! (fn [evt] (swap! events conj evt))]
        (let [result (wireless-api/link-receiver-to-node! :rec-tile :node-tile "wrong-pw" true)]
          (is (not (:success result)))
          (is (= :password (:reason result)))
          (is (empty? @events)))))))
