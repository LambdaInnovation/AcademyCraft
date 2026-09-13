(ns cn.li.mcmod.gui.slot-schema-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.gui.slot-schema :as slot-schema]))

(defn- reset-slot-schema-registry! [f]
  (slot-schema/set-slot-schema-registry! {})
  (f)
  (slot-schema/set-slot-schema-registry! {}))

(use-fixtures :each reset-slot-schema-registry!)

(def sample-config
  {:schema-id :machine/core
   :slots [{:id :input :type :io :x 10 :y 20}
           {:id :output :type :io :x 30 :y 20}
           {:id :core :type :core :x 20 :y 40}]})

(deftest register-and-query-core-test
  (testing "register-slot-schema! derives indexes and ranges"
    (let [schema (slot-schema/register-slot-schema! sample-config)]
      (is (= :machine/core (:schema-id schema)))
      (is (= 3 (:tile-slot-count schema)))
      (is (= 0 (slot-schema/slot-index :machine/core :input)))
      (is (= :output (slot-schema/slot-id :machine/core 1)))
      (is (= :core (slot-schema/slot-type :machine/core 2)))
      (is (= [0 2] (slot-schema/get-slot-range :machine/core :tile)))
      (is (= [3 29] (slot-schema/get-slot-range :machine/core :player-main)))
      (is (= [30 38] (slot-schema/get-slot-range :machine/core :player-hotbar))))))

(deftest slot-schema-edge-cases-test
  (testing "duplicate ids and coordinates are rejected"
    (is (thrown? clojure.lang.ExceptionInfo
                 (slot-schema/register-slot-schema!
                  {:schema-id :dup/id
                   :slots [{:id :a :type :x :x 0 :y 0}
                           {:id :a :type :y :x 1 :y 0}]})))
    (is (thrown? clojure.lang.ExceptionInfo
                 (slot-schema/register-slot-schema!
                  {:schema-id :dup/coord
                   :slots [{:id :a :type :x :x 0 :y 0}
                           {:id :b :type :y :x 0 :y 0}]}))))
  (testing "unknown slot id in slot-indexes throws"
    (slot-schema/register-slot-schema! sample-config)
    (is (thrown? clojure.lang.ExceptionInfo
                 (slot-schema/slot-indexes :machine/core [:input :missing])))))

(deftest quick-move-contract-test
  (testing "build-quick-move-config resolves slot ids and slot types"
    (slot-schema/register-slot-schema! sample-config)
    (let [config (slot-schema/build-quick-move-config
                  :machine/core
                  {:inventory-pred (fn [slot-index player-start] (>= slot-index player-start))
                   :rules [{:accept? (constantly true) :slot-ids [:input :core]}
                           {:accept? (constantly true) :slot-type :io}
                           {:accept? (constantly false)}]})]
      (is (= #{0 1 2} (:container-slots config)))
      (is (= [0 2] (get-in config [:rules 0 :slots])))
      (is (= [0 1] (get-in config [:rules 1 :slots])))
      (is (= [] (get-in config [:rules 2 :slots]))))))
