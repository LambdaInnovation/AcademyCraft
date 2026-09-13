(ns cn.li.mcmod.events.dispatcher-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.block.query :as bquery]
            [cn.li.mcmod.events.dispatcher :as dispatcher]
            [cn.li.mcmod.block.multiblock-core :as core]))

(deftest right-click-routing-test
  (let [called (atom nil)
        ctx {:player :p :world :w :pos :pos :block-id :part :sneaking false :item-stack :stack}]
    (with-redefs [core/route-to-controller-context (fn [_] {:player :p :world :w :pos :pos :block-id :controller :x 1 :sneaking false :item-stack :stack})
                  bquery/get-block-spec (fn [block-id]
                                          (when (= block-id :controller)
                                            {:events {:on-right-click (fn [player world pos block-id & {:keys [sneaking item-stack]}]
                                                                        (reset! called [player world pos block-id {:sneaking sneaking :item-stack item-stack}])
                                                                        :ok)}}))]
      (is (= :ok (dispatcher/on-block-right-click ctx)))
      (is (= [:p :w :pos :controller {:sneaking false :item-stack :stack}] @called))))
  (testing "no handler returns nil"
    (with-redefs [core/route-to-controller-context (fn [x] x)
                  bquery/get-block-spec (fn [_] nil)]
      (is (= nil (dispatcher/on-block-right-click {:block-id :none}))))))

(deftest on-place-priority-test
  (testing "precheck short-circuits all downstream processing"
    (let [ctx {:player :p :world :w :pos :pos :block-id :a}]
      (with-redefs [core/precheck-controller-place (fn [_] {:prechecked true})
                    bquery/get-block-spec (fn [_] {:events {:on-place (fn [_ _ _ _] {:handler true})}})
                    core/post-place-controller! (fn [_] {:core true})]
        (is (= {:prechecked true} (dispatcher/on-block-place ctx))))))
  (testing "core result has higher priority than handler result"
    (let [ctx {:player :p :world :w :pos :pos :block-id :a}]
      (with-redefs [core/precheck-controller-place (fn [_] nil)
                    bquery/get-block-spec (fn [_] {:events {:on-place (fn [_ _ _ _] {:handler true})}})
                    core/post-place-controller! (fn [_] {:core true})]
        (is (= {:core true} (dispatcher/on-block-place ctx)))))
    (with-redefs [core/precheck-controller-place (fn [_] nil)
                  bquery/get-block-spec (fn [_] {:events {:on-place (fn [_ _ _ _] {:handler true})}})
                  core/post-place-controller! (fn [_] nil)]
      (is (= {:handler true} (dispatcher/on-block-place {:player :p :world :w :pos :pos :block-id :a}))))
    (with-redefs [core/precheck-controller-place (fn [_] nil)
                  bquery/get-block-spec (fn [_] nil)
                  core/post-place-controller! (fn [_] nil)]
      (is (= nil (dispatcher/on-block-place {:block-id :a}))))))

(deftest on-break-merge-test
  (let [ctx {:world :w :pos :pos :block-id :part}
        routed {:world :w :pos :pos :block-id :controller :meta 1}]
    (testing "merges handler and core results"
      (let [called (atom nil)]
        (with-redefs [core/route-to-controller-context (fn [_] routed)
                      bquery/get-block-spec (fn [_] {:events {:on-break (fn [world pos block-id]
                                                                          (reset! called [world pos block-id])
                                                                          {:a 1 :b 2})}})
                      core/apply-structure-break! (fn [_ _] {:b 9 :c 3})]
          (is (= {:a 1 :b 9 :c 3}
                 (dispatcher/on-block-break ctx)))
          (is (= [:w :pos :controller] @called)))))
    (testing "returns empty map when no handler or core result"
      (with-redefs [core/route-to-controller-context (fn [_] routed)
                    bquery/get-block-spec (fn [_] nil)
                    core/apply-structure-break! (fn [_ _] nil)]
        (is (= {} (dispatcher/on-block-break ctx)))))))
