(ns cn.li.ac.item.energy-items-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.item.energy-items :as energy-items]
            [cn.li.ac.item.developer-portable-reactive :as developer-portable-reactive]))

(deftest portable-developer-opens-reactive-screen-test
  (testing "portable developer opens the reactive developer screen on client side"
    (let [calls (atom [])]
      (with-redefs [developer-portable-reactive/open!
                    (fn [player] (swap! calls conj player))]
        (is (= {:consume? true}
               (#'energy-items/open-portable-developer! {:player :player-1
                                                         :side :client})))
        (is (= [:player-1] @calls))))))

(deftest portable-developer-skips-on-server-side-test
  (testing "portable developer does nothing on server side"
    (is (= {:consume? true}
           (#'energy-items/open-portable-developer! {:player :player-1
                                                     :side :server})))))
