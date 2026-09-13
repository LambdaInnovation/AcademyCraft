(ns cn.li.forge1201.client.key-input-state-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.forge1201.client.key-input-fixtures :as key-input]))

(def ^:private owner-a {:client-session-id [:client :session-a]
                        :player-uuid "player-a"})
(def ^:private owner-b {:client-session-id [:client :session-b]
                        :player-uuid "player-b"})

(defn- owner-key [owner]
  [(:client-session-id owner) (:player-uuid owner)])

(use-fixtures :each
  (fn [f]
    (key-input/call-with-key-input-runtime
      (key-input/create-key-input-runtime)
      (fn []
        (f)))))

(deftest clear-owner-input-state-removes-only-target-owner-test
  (key-input/reset-input-state-for-test!
    {:raw-v-state-map {(owner-key owner-a) {:was-down true :down-at-ns 1}
                       (owner-key owner-b) {:was-down false :down-at-ns nil}}
     :override-active-map {(owner-key owner-a) true
                           (owner-key owner-b) false}})
  (key-input/clear-owner-input-state! owner-a)
  (let [snapshot (key-input/input-state-snapshot)]
    (is (nil? (get-in snapshot [:raw-v-state (owner-key owner-a)])))
    (is (nil? (get-in snapshot [:override-active? (owner-key owner-a)])))
    (is (= {:was-down false :down-at-ns nil}
           (get-in snapshot [:raw-v-state (owner-key owner-b)])))))

(deftest clear-client-input-session-removes-only-target-session-test
  (key-input/reset-input-state-for-test!
    {:raw-v-state-map {(owner-key owner-a) {:was-down true :down-at-ns 1}
                       (owner-key owner-b) {:was-down false :down-at-ns nil}}
     :override-active-map {(owner-key owner-a) true
                           (owner-key owner-b) false}})
  (key-input/clear-client-input-session! [:client :session-a])
  (let [snapshot (key-input/input-state-snapshot)]
    (is (nil? (get-in snapshot [:raw-v-state (owner-key owner-a)])))
    (is (nil? (get-in snapshot [:override-active? (owner-key owner-a)])))
    (is (= {:was-down false :down-at-ns nil}
           (get-in snapshot [:raw-v-state (owner-key owner-b)])))
    (is (false? (get-in snapshot [:override-active? (owner-key owner-b)])))))

(deftest key-input-runtime-isolation-test
  (let [runtime-b (key-input/create-key-input-runtime)]
    (key-input/reset-input-state-for-test!
      {:raw-v-state-map {(owner-key owner-a) {:was-down true :down-at-ns 1}}
       :override-active-map {(owner-key owner-a) true}
       :key-scheme-value :original})
    (key-input/call-with-key-input-runtime
      runtime-b
      (fn []
        (key-input/reset-input-state-for-test!
          {:raw-v-state-map {(owner-key owner-b) {:was-down false :down-at-ns nil}}
           :override-active-map {(owner-key owner-b) false}
           :key-scheme-value :alternative})
        (is (= :alternative (key-input/get-key-scheme)))
        (is (= {:was-down false :down-at-ns nil}
               (get-in (key-input/input-state-snapshot) [:raw-v-state (owner-key owner-b)])))))
    (is (= :original (key-input/get-key-scheme)))
    (is (= {:was-down true :down-at-ns 1}
           (get-in (key-input/input-state-snapshot) [:raw-v-state (owner-key owner-a)])))))