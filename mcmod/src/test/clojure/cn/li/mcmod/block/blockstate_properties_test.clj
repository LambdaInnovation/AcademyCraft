(ns cn.li.mcmod.block.blockstate-properties-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.block.blockstate-properties :as props]))

(deftest create-property-core-test
  (testing "create-property dispatches by :type"
    (let [int-calls (atom [])
          bool-calls (atom [])
          facing-calls (atom [])
          int-fn (fn [name min-v max-v]
                   (swap! int-calls conj [name min-v max-v])
                   [:int name min-v max-v])
          bool-fn (fn [name]
                    (swap! bool-calls conj [name])
                    [:bool name])
          facing-fn (fn [name]
                      (swap! facing-calls conj [name])
                      [:facing name])]
      (is (= [:int "power" 1 10]
             (props/create-property :power {:type :integer :min 1 :max 10} int-fn bool-fn facing-fn)))
      (is (= [:bool "active"]
             (props/create-property :active {:type :boolean} int-fn bool-fn facing-fn)))
      (is (= [:facing "dir"]
             (props/create-property :dir {:type :horizontal-facing} int-fn bool-fn facing-fn)))
      (is (= [:facing "facing"]
             (props/create-property :facing {:type :direction} int-fn bool-fn facing-fn)))
      (is (= [["power" 1 10]] @int-calls))
      (is (= [["active"]] @bool-calls))
      (is (= [["dir"] ["facing"]] @facing-calls)))))

(deftest create-property-edge-cases-test
  (testing "unknown type and missing facing factory return nil"
    (let [int-fn (fn [& _] :int)
          bool-fn (fn [& _] :bool)]
      (is (= nil (props/create-property :x {:type :unknown} int-fn bool-fn nil)))
      (is (= nil (props/create-property :x {:type :horizontal-facing} int-fn bool-fn nil)))
      (is (= nil (props/create-property :x {:type :direction} int-fn bool-fn nil))))))

(deftest registry-contract-test
  (testing "register-block-properties! and query helpers work on registry atom"
    (let [registry (props/create-property-registry)
          int-fn (fn [name min-v max-v] {:kind :int :name name :min min-v :max max-v})
          bool-fn (fn [name] {:kind :bool :name name})
          facing-fn (fn [name] {:kind :facing :name name})]
      (props/register-block-properties!
       registry
       :block/a
       {:power {:type :integer :min 0 :max 15}
        :enabled {:type :boolean}
        :face {:type :direction}
        :skip {:type :unknown}}
       int-fn bool-fn facing-fn)
      (is (= {:kind :int :name "power" :min 0 :max 15}
             (props/get-property registry :block/a :power)))
      (is (= {:kind :bool :name "enabled"}
             (props/get-property registry :block/a :enabled)))
      (is (= {:kind :facing :name "face"}
             (props/get-property registry :block/a :face)))
      (is (= nil (props/get-property registry :block/a :skip)))
      (is (= 3 (count (props/get-all-properties registry :block/a))))
      (is (= [] (props/get-all-properties registry :block/missing))))))
