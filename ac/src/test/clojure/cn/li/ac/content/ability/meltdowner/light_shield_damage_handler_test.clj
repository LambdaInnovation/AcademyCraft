(ns cn.li.ac.content.ability.meltdowner.light-shield-damage-handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
            [cn.li.ac.content.ability.meltdowner.light-shield :as ls]
            [cn.li.ac.ability.server.damage.handler :as damage-handler]))

(deftest init-registers-light-shield-toggle-damage-handler-test
  (testing "init! registers LightShield damage handler with expected id/skill/priority"
    (let [calls* (atom [])]
      (with-redefs [md-damage/init! (fn [] nil)
                    damage-handler/register-toggle-damage-handler!
                    (fn [handler-id skill-id handler-fn priority]
                      (swap! calls* conj {:handler-id handler-id
                                          :skill-id skill-id
                                          :handler-fn handler-fn
                                          :priority priority})
                      true)]
        (is (nil? (ls/init!)))
        (is (= 1 (count @calls*)))
        (is (= :light-shield-damage (:handler-id (first @calls*))))
        (is (= :light-shield (:skill-id (first @calls*))))
        (is (fn? (:handler-fn (first @calls*))))
        (is (= 80 (:priority (first @calls*))))))))
