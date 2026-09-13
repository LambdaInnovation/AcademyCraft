(ns cn.li.mcmod.client.platform-bridge-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- reset-bridge-state! [f]
  (client-bridge/reset-client-bridge-for-test!)
  (try
    (f)
    (finally
      (client-bridge/reset-client-bridge-for-test!))))

(use-fixtures :each reset-bridge-state!)

(deftest install-client-bridge-wires-open-screen-test
  (let [calls (atom [])]
    (client-bridge/install-client-bridge!
      {:open-screen (fn [screen-key payload]
                      (swap! calls conj {:screen-key screen-key
                                         :payload payload})
                      :opened)})
    (is (= :opened (client-bridge/open-screen! :content/example-screen {:value 42})))
    (is (= [{:screen-key :content/example-screen
             :payload {:value 42}}]
           @calls))))

(deftest install-client-bridge-wires-generic-effect-host-test
  (let [calls (atom [])]
    (client-bridge/install-client-bridge!
      {:run-client-effect (fn [effect-key payload]
                            (swap! calls conj {:effect-key effect-key
                                               :payload payload})
                            :ran)})
    (is (= :ran (client-bridge/run-client-effect! :content/example-effect {:amount 3})))
    (is (= [{:effect-key :content/example-effect
             :payload {:amount 3}}]
           @calls))))

(deftest install-client-bridge-wires-key-callbacks-test
  (let [calls (atom [])]
    (client-bridge/install-client-bridge!
      {:slot-key-down (fn [player-uuid key-idx]
                        (swap! calls conj [:slot-down player-uuid key-idx]))
       :slot-key-abort (fn [player-uuid key-idx]
                         (swap! calls conj [:slot-abort player-uuid key-idx]))
       :movement-key-up (fn [player-uuid movement-key]
                          (swap! calls conj [:movement-up player-uuid movement-key]))})
    (client-bridge/on-slot-key-down! "p1" 0)
    (client-bridge/on-slot-key-abort! "p1" 0)
    (client-bridge/on-movement-key-up! "p1" :forward)
    (is (= [[:slot-down "p1" 0]
            [:slot-abort "p1" 0]
            [:movement-up "p1" :forward]]
           @calls))))

(deftest install-client-bridge-clears-replaced-hot-operations-test
  (client-bridge/install-client-bridge! {:run-client-effect (fn [_ _] :first)})
  (is (= :first (client-bridge/run-client-effect! :effect {})))
  (client-bridge/install-client-bridge! {})
  (is (thrown? NullPointerException
               (client-bridge/run-client-effect! :effect {}))))
