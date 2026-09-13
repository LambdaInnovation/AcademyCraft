(ns cn.li.mcmod.block.tile-dsl-logic-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.block.tile-dsl :as tdsl]
            [cn.li.mcmod.block.tile-kind :as tile-kind]))

(defn- reset-all! [f]
  (tile-kind/reset-tile-kinds-for-test!)
  (f)
  (tile-kind/reset-tile-kinds-for-test!))

(use-fixtures :each reset-all!)

(deftest validate-tile-spec-errors-test
  (is (thrown-with-msg? Exception #"non-empty string :id"
                        (tdsl/validate-tile-spec {:id "" :impl :x :blocks ["a"]})))
  (is (thrown-with-msg? Exception #":impl must be a keyword"
                        (tdsl/validate-tile-spec {:id "t" :impl "bad" :blocks ["a"]})))
  (is (thrown-with-msg? Exception #":blocks must be a non-empty vector"
                        (tdsl/validate-tile-spec {:id "t" :impl :scripted :blocks []}))))

(deftest create-tile-spec-and-registry-test
  (let [spec (tdsl/create-tile-spec "my-tile" {:impl :scripted :blocks [:a "b"]})]
    (is (= "my-tile" (:id spec)))
    (is (= ["a" "b"] (:blocks spec)))
    (is (= :scripted (:impl spec)))
    (tdsl/register-tile! spec)
    (is (= spec (tdsl/get-tile "my-tile")))
    (is (= "my-tile" (tdsl/get-tile-id-for-block "a")))
    (is (= #{"my-tile"} (set (tdsl/list-tiles))))))

(deftest merge-tile-kind-defaults-test
  (tile-kind/register-tile-kind!
    :k
    {:tick-fn (fn [_ _ _ _] :kind-tick)
     :read-nbt-fn (fn [_] {:from :kind})
     :write-nbt-fn (fn [_ _] nil)})
  (let [merged-a (tile-kind/merge-tile-kind-defaults
                   {:tile-kind :k
                    :tick-fn (fn [_ _ _ _] :override)})
        merged-b (tile-kind/merge-tile-kind-defaults {:tile-kind :k})]
    (is (= :override ((:tick-fn merged-a) nil nil nil nil)))
    (is (= :kind-tick ((:tick-fn merged-b) nil nil nil nil)))
    (is (= {:from :kind} ((:read-nbt-fn merged-b) :tag)))))

(deftest register-tile-capability-keys-test
  (tdsl/register-tile!
    (tdsl/create-tile-spec "cap-tile" {:impl :scripted :blocks ["cap-block"]}))
  (tdsl/register-tile-capability-keys! "cap-tile" :wireless-receiver)
  (is (= #{:wireless-receiver} (:capability-keys (tdsl/get-tile "cap-tile")))))
