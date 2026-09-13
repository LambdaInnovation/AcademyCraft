(ns cn.li.mcmod.gui.slot-registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.gui.slot-registry :as registry]))

(deftest register-and-get-validator-test
  (testing "register-slot-validator! registers under the given keyword"
    (registry/set-slot-validators! {})
    (let [validator (fn [_] true)]
      (is (nil? (registry/register-slot-validator! :energy validator)))
      (is (identical? validator (registry/get-slot-validator :energy)))))
  (testing "unknown slot type returns nil"
    (is (nil? (registry/get-slot-validator :unknown)))))

