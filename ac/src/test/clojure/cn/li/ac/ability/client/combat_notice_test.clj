(ns cn.li.ac.ability.client.combat-notice-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.client.combat-notice :as notice]))

(deftest notices-are-session-scoped-and-transient-test
  (let [component (notice/create-combat-notice-component {:now-ms-fn (constantly 1000)})]
    (notice/show-notice! component :session-a :hit {:text "Hit" :duration-ms 100 :color [1 2 3]})
    (notice/show-notice! component :session-b :alt {:text "Other" :duration-ms 500})
    (is (= #{:hit} (set (keys (notice/session-notices-snapshot component :session-a)))))
    (is (= #{:alt} (set (keys (notice/session-notices-snapshot component :session-b)))))
    (is (= "Hit" (:text (notice/active-notice component :session-a :hit 1050))))
    (is (nil? (notice/active-notice component :session-a :hit 1200)))
    (is (empty? (notice/session-notices-snapshot component :session-a)))
    (is (contains? (notice/session-notices-snapshot component :session-b) :alt))))

(deftest notice-component-reset-and-dispose-lifecycle-test
  (let [component (notice/create-combat-notice-component {:now-ms-fn (constantly 2000)})]
    (notice/show-notice! component :session-a :a {:text "A"})
    (notice/show-notice! component :session-b :b {:text "B"})
    (let [snapshot (notice/notices-snapshot component)]
      (notice/clear-session! component :session-a)
      (is (empty? (notice/session-notices-snapshot component :session-a)))
      (is (contains? (notice/session-notices-snapshot component :session-b) :b))
      (notice/reset-notices-for-test! component)
      (is (empty? (notice/notices-snapshot component)))
      (notice/reset-notices-for-test! component snapshot)
      (is (contains? (notice/session-notices-snapshot component :session-a) :a))
      (notice/clear-notice! component :session-a :a)
      (is (empty? (notice/session-notices-snapshot component :session-a)))
      (notice/dispose! component)
      (is (empty? (notice/notices-snapshot component))))))
