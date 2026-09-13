(ns cn.li.mcmod.block.multiblock-core-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.block.multiblock-core :as mb]
            [cn.li.mcmod.platform.be :as pbe]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]))

(defrecord BlockPosStub [x y z])

(defn- test-pos [x y z]
  (->BlockPosStub x y z))

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

(defn- controller-parts-opts []
  {:physical {:material :metal
              :hardness 5.0
              :resistance 10.0
              :requires-tool true
              :harvest-tool :pickaxe}
   :multi-block {:size {:width 2 :height 1 :depth 1}
                 :origin {:x 0 :y 0 :z 0}
                 :mode :controller-parts
                 :controller-block-id "ctl-test"
                 :part-block-id "part-test"}})

(defn- register-controller-and-part! []
  (bdsl/register-block! (bdsl/create-block-spec "ctl-test" (controller-parts-opts)))
  (bdsl/register-block! (bdsl/create-block-spec "part-test" (controller-parts-opts))))

(defn- with-test-position-ops! [f]
  (install-test-position-ops!)
  (f))

(use-fixtures :each with-test-position-ops!)

(deftest precheck-controller-place-occupied-test
  (register-controller-and-part!)
  (world/install-world-ops!
    {:world-get-block-state
     (fn [_w p]
       (when (= [11 0 0] [(pos/pos-x p) (pos/pos-y p) (pos/pos-z p)])
         :solid))}
    "test")
  (let [master (test-pos 10 0 0)]
    (is (= {:cancel-place? true :reason :multiblock-space-occupied}
           (mb/precheck-controller-place {:world :w :pos master :block-id "ctl-test"})))))

(deftest precheck-non-controller-yields-nil-test
  (is (nil? (mb/precheck-controller-place {:world :w :pos (test-pos 0 0 0) :block-id "not-a-controller"}))))

(deftest resolve-controller-pos-part-from-be-state-test
  (register-controller-and-part!)
  (with-redefs [world/get-tile-entity (constantly :fake-be)
                pbe/get-custom-state (constantly {:controller-pos-x 5
                                                  :controller-pos-y 6
                                                  :controller-pos-z 7})]
    (let [resolved (mb/resolve-controller-pos {:world :w :pos (test-pos 9 9 9) :block-id "part-test"})]
      (is (= [5 6 7] [(pos/pos-x resolved) (pos/pos-y resolved) (pos/pos-z resolved)])))))

(deftest route-to-controller-context-rewrites-ctx-test
  (register-controller-and-part!)
  (with-redefs [world/get-tile-entity (constantly :fake-be)
                pbe/get-custom-state (constantly {:controller-pos-x 10
                                                  :controller-pos-y 0
                                                  :controller-pos-z 0})]
    (let [r (mb/route-to-controller-context {:world :w
                                             :pos (test-pos 1 2 3)
                                             :block-id "part-test"})]
      (is (= "part-test" (:original-block-id r)))
      (is (= "ctl-test" (:block-id r)))
      (is (= [10 0 0] [(pos/pos-x (:pos r)) (pos/pos-y (:pos r)) (pos/pos-z (:pos r))])))))
