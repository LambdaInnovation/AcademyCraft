(ns cn.li.mcmod.block.dsl-test
  "Unit tests for Block DSL"
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]))

(deftest create-block-spec-nested-only-test
  (testing "nested syntax maps into block records"
    (let [spec (bdsl/create-block-spec "nested-priority"
                                       {:physical {:material :metal
                                                   :hardness 8.0}
                                        :rendering {:light-level 7}
                                        :events {:on-right-click :handler}})]
      (is (= :metal (get-in spec [:physical :material])))
      (is (= 8.0 (get-in spec [:physical :hardness])))
      (is (= 7 (get-in spec [:rendering :light-level])))
      (is (= :handler (get-in spec [:events :on-right-click]))))))

(deftest register-and-lookup-test
  (testing "register-block! + get-block-spec supports string and keyword ids"
    (let [spec (bdsl/create-block-spec "registry-block" {:physical {:material :stone}})]
      (bdsl/register-block! spec)
      (is (= "registry-block" (:id (bdsl/get-block-spec "registry-block"))))
      (is (= "registry-block" (:id (bdsl/get-block-spec :registry-block))))
      (is (= #{"registry-block"} (set (bdsl/list-blocks)))))))

(deftest validate-block-spec-invalid-material-test
  (testing "invalid material is rejected"
    (let [spec (bdsl/create-block-spec "bad-material" {:physical {:material :not-a-material}})]
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Invalid material"
            (bdsl/validate-block-spec spec))))))

(deftest validate-multiblock-positions-test
  (testing "multi-block positions must include origin"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"must include origin"
          (bdsl/validate-multi-block-positions [{:x 1 :y 0 :z 0}]))))
  (testing "multi-block positions accept vector and map forms"
    (is (true? (bdsl/validate-multi-block-positions [[0 0 0] {:x 1 :y 0 :z 0}])))))

(deftest multiblock-position-helpers-test
  (testing "regular shape expands with relative coordinates"
    (let [positions (bdsl/calculate-multi-block-positions {:width 2 :height 1 :depth 1}
                                                          {:x 0 :y 0 :z 0})]
      (is (= 2 (count positions)))
      (is (some :is-origin? positions))
      (is (= #{0 1} (set (map :relative-x positions))))))
  (testing "normalize-positions shifts minimum corner to origin"
    (let [normalized (bdsl/normalize-positions [{:x 3 :y 5 :z 7}
                                                {:x 4 :y 5 :z 8}])]
      (is (= [{:x 0 :y 0 :z 0}
              {:x 1 :y 0 :z 1}]
             normalized)))))

(defrecord BlockPosStub [x y z])

(defn- install-test-position-ops! []
  (pos/install-position-ops!
    {:pos-x (fn [p] (:x p))
     :pos-y (fn [p] (:y p))
     :pos-z (fn [p] (:z p))
     :pos-above (fn [p] (->BlockPosStub (:x p) (inc (:y p)) (:z p)))
     :create-block-pos (fn [x y z] (->BlockPosStub x y z))
     :position-get-block-pos (fn [_] nil)
     :position-get-pos (fn [_] nil)}
    "test"))

(deftest get-multi-block-master-pos-test
  (is (= {:x 4 :y 8 :z 2}
         (bdsl/get-multi-block-master-pos {:x 5 :y 10 :z 3}
                                          {:relative-x 1 :relative-y 2 :relative-z 1}))))

(deftest all-multi-block-positions-with-factory-test
  (install-test-position-ops!)
  (let [spec (bdsl/create-block-spec "mb"
                                     (bdsl/multi-block-template {:width 2 :height 1 :depth 1}))
        master (pos/create-block-pos 10 20 30)
        ps (bdsl/all-multi-block-positions master spec)]
    (is (= 2 (count ps)))
    (is (= #{[10 20 30] [11 20 30]}
           (set (map (fn [p] [(pos/pos-x p) (pos/pos-y p) (pos/pos-z p)]) ps))))))

(deftest can-place-multi-block-binding-test
  (testing "all footprint cells empty"
    (install-test-position-ops!)
    (world/install-world-ops! {:world-get-block-state (constantly nil)} "test")
    (let [spec (bdsl/create-block-spec "empty-mb"
                                       (bdsl/multi-block-template {:width 1 :height 1 :depth 1}))]
      (is (true? (bdsl/can-place-multi-block? :fake-world (pos/create-block-pos 0 0 0) spec)))))
  (testing "blocked cell fails placement"
    (install-test-position-ops!)
    (world/install-world-ops!
      {:world-get-block-state
       (fn [_w p]
         (when (= [1 0 0] [(pos/pos-x p) (pos/pos-y p) (pos/pos-z p)])
           :blocked))}
      "test")
    (let [spec (bdsl/create-block-spec "blocked-mb"
                                       (bdsl/multi-block-template {:width 2 :height 1 :depth 1}))]
      (is (false? (bdsl/can-place-multi-block? :fake-world (pos/create-block-pos 0 0 0) spec))))))

(deftest template-and-merge-helpers-test
(is (= 2 (get-in (bdsl/ore-template 2) [:physical :harvest-level])))
  (is (= :wood (get-in (bdsl/wood-template) [:physical :material])))
  (is (= 3 (get-in (bdsl/metal-template 3) [:physical :harvest-level])))
  (is (= :glass (get-in (bdsl/glass-template) [:physical :material])))
  (is (= 15 (get-in (bdsl/light-block-template 15) [:rendering :light-level])))
  (is (some? (get-in (bdsl/multi-block-template {:width 2 :height 1 :depth 1})
                     [:multi-block :size])))
  (is (vector? (get-in (bdsl/irregular-multi-block-template [{:x 1 :y 0 :z 0} {:x 0 :y 0 :z 0}])
                       [:multi-block :positions])))
  (is (pos? (count (bdsl/create-l-shape 2 2))))
  (is (pos? (count (bdsl/create-t-shape 3 2))))
  (is (pos? (count (bdsl/create-cross-shape 1))))
  (is (pos? (count (bdsl/create-pyramid-shape 3 2))))
  (is (pos? (count (bdsl/create-hollow-cube 2))))
  (is (= :stone (get-in (bdsl/merge-templates (bdsl/wood-template) {:physical {:material :stone}})
                        [:physical :material]))))

(deftest get-block-properties-test
  (let [spec (bdsl/create-block-spec "props"
              (merge (bdsl/metal-template 2)
                                            {:rendering {:light-level 9}}))]
    (is (= :metal (:material (bdsl/get-block-properties spec))))
    (is (= 9 (:light-level (bdsl/get-block-properties spec))))
    (is (true? (:requires-tool (bdsl/get-block-properties spec))))))
