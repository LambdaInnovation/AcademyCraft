(ns cn.li.ac.ability.fx-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]))

(deftest send-ignores-invalid-entry-test
  (let [calls* (atom [])]
    (with-redefs [ctx/ctx-send-to-client! (fn [ctx-id topic body]
                                           (swap! calls* conj [:client ctx-id topic body]))]
      (is (nil? (fx/send! "ctx-1" nil)))
      (is (nil? (fx/send! "ctx-1" {:mode :end})))
      (is (empty? @calls*)))))

(deftest send-routes-to-client-by-default-test
  (let [calls* (atom [])]
    (with-redefs [ctx/ctx-send-to-client! (fn [ctx-id topic body]
                                           (swap! calls* conj [:client ctx-id topic body]))
                  ctx/ctx-send-to-self! (fn [& args] (swap! calls* conj (into [:self] args)))
                  ctx/ctx-send-to-except-local! (fn [& args] (swap! calls* conj (into [:except-local] args)))]
      (fx/send! "ctx-1" {:topic :skill/fx-end :mode :end} nil {:performed? false})
      (is (= [[:client "ctx-1" :skill/fx-end {:ctx-id "ctx-1" :mode :end :performed? false}]]
             @calls*)))))

(deftest send-routes-by-to-keyword-test
  (let [calls* (atom [])]
    (with-redefs [ctx/ctx-send-to-client! (fn [& args] (swap! calls* conj (into [:client] args)))
                  ctx/ctx-send-to-self! (fn [& args] (swap! calls* conj (into [:self] args)))
                  ctx/ctx-send-to-except-local! (fn [& args] (swap! calls* conj (into [:except-local] args)))]
      (fx/send! "ctx-self" {:topic :skill/fx-update :mode :update :to :self})
      (fx/send! "ctx-ex" {:topic :skill/fx-start :mode :start :to :except-local})
      (is (= [[:self "ctx-self" :skill/fx-update {:ctx-id "ctx-self" :mode :update}]
              [:except-local "ctx-ex" :skill/fx-start {:ctx-id "ctx-ex" :mode :start}]]
             @calls*)))))

(deftest send-injects-meta-from-evt-and-merges-payload-test
  (let [calls* (atom [])]
    (with-redefs [ctx/ctx-send-to-client! (fn [_ topic body]
                                           (swap! calls* conj [topic body]))]
      (fx/send! nil
               {:topic :skill/fx-perform
                :mode :perform
                :payload (fn [evt] {:charge-ticks (:hold-ticks evt)})}
               {:ctx-id "ctx-evt"
                :skill-id :body-intensify
                :player-id "p1"
                :hold-ticks 12}
               {:performed? true})
      (is (= [:skill/fx-perform
              {:ctx-id "ctx-evt"
               :mode :perform
               :skill-id :body-intensify
               :player-id "p1"
               :charge-ticks 12
               :performed? true}]
             (first @calls*))))))

(deftest send-payload-fn-failure-is-swallowed-test
  (let [calls* (atom [])]
    (with-redefs [ctx/ctx-send-to-client! (fn [_ _ body] (swap! calls* conj body))]
      (fx/send! "ctx-1"
               {:topic :skill/fx-end
                :mode :end
                :payload (fn [_] (throw (ex-info "boom" {})))}
               nil
               {:performed? false})
      (is (= [{:ctx-id "ctx-1" :mode :end :performed? false}]
             @calls*)))))

(deftest send-local-and-nearby-fans-out-to-client-and-except-local-test
  (let [calls* (atom [])]
    (with-redefs [ctx/ctx-send-to-client! (fn [& args] (swap! calls* conj (into [:client] args)))
                  ctx/ctx-send-to-except-local! (fn [& args] (swap! calls* conj (into [:except-local] args)))]
      (fx/send-local-and-nearby! "ctx-fan"
                                 {:topic :skill/fx-update :mode :update}
                                 nil
                                 {:ticks 3})
      (is (= [[:client "ctx-fan" :skill/fx-update {:ctx-id "ctx-fan" :mode :update :ticks 3}]
              [:except-local "ctx-fan" :skill/fx-update {:ctx-id "ctx-fan" :mode :update :ticks 3}]]
             @calls*)))))
