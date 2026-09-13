(ns cn.li.ac.ability.server.service.skill-effects-test
  (:require
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.config :as ability-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(use-fixtures :each
  (fn [f]
    (ps-fix/with-test-player-state-owner
      (fn []
        (store/reset-store!)
        (try
          (f)
          (finally
            (store/reset-store!)))))))

(deftest scale-damage-default-and-custom-test
  (with-redefs [ability-config/damage-scale (fn [] 2.0)]
    (is (= 20.0 (skill-effects/scale-damage {} 10.0)))
    (is (= 30.0 (skill-effects/scale-damage {:damage-scale 1.5} 10.0)))))

(deftest apply-cost-runtime-speed-and-empty-cost-test
  (testing "missing cost branch is true"
    (is (true? (skill-effects/apply-cost! {} :down {:player-id "p"}))))
  (testing "runtime cost delegates to perform-resource!"
    (let [seen (atom nil)]
      (with-redefs [skill-effects/perform-resource! (fn [pid ol cp creative?]
                                                      (reset! seen {:pid pid :ol ol :cp cp :creative? creative?})
                                                      {:success? true})]
        (is (true? (skill-effects/apply-cost!
                    {:cp-consume-speed 1.0
                     :overload-consume-speed 1.0
                     :cost {:tick {:mode :runtime-speed :cp-speed 0.5 :overload-speed 1.0}}}
                    :tick
                    {:player-id "p"})))
        (is (= "p" (:pid @seen)))
        (is (pos? (double (:ol @seen))))
        (is (pos? (double (:cp @seen))))
        (is (false? (:creative? @seen)))))))

(deftest perform-resource-state-missing-test
  (store/reset-store!)
  (is (= {:success? false :events [] :data nil}
         (skill-effects/perform-resource! "missing" 1.0 1.0 false))))

(deftest perform-resource-uses-bound-owner-session-test
  (store/set-player-state! :skill-effects-session "p1" (store/fresh-player-state))
  (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id :skill-effects-session
                                                 :player-uuid "p-effects"}} (fn [] (is (map? (skill-effects/perform-resource! "p1" 1.0 1.0 false))))))

(deftest register-custom-skill-exp-overrides-stored-value-test
  ;; Matches original's Skill.expCustomized: a registered custom fn wins over
  ;; the normal stored/accumulated exp for every read of that skill-id.
  (store/set-player-state! :skill-effects-session "p-custom-exp" (store/fresh-player-state))
  (skill-effects/register-custom-skill-exp! :custom-exp-test-skill (fn [_player-id] 0.42))
  (runtime-hooks/with-client-ctx-fn
    {:player-owner {:server-session-id :skill-effects-session :player-uuid "p-custom-exp"}}
    (fn []
      (is (= 0.42 (skill-effects/skill-exp "p-custom-exp" :custom-exp-test-skill)))
      (is (= 0.0 (skill-effects/skill-exp "p-custom-exp" :some-other-unregistered-skill))))))

(deftest skill-effects-session-resolution-still-fail-fast-test
  (runtime-hooks/with-client-ctx-fn {:player-owner nil} (fn [] (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"requires bound session-id"
                          (skill-effects/current-cp "p1"))))))
