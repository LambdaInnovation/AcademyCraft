(ns cn.li.ac.content.ability.generic.course-chain-test
  (:require 
                                          [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.model.ability :as adata]
                                          [cn.li.ac.ability.passive :as passive]
            [cn.li.ac.ability.registry.event :as evt]                                          [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.content.ability.generic.course-chain :as courses]))

(defn- reset-runtime-fixture [f]
       (ps-fix/with-test-player-state-owner
              (fn []
                     (store/reset-store!)
                     (evt/reset-ability-event-subscribers-for-test!)
                     (passive/reset-passive-handler-registry-for-test!)
                     (try
                            (f)
                            (finally
                                   (store/reset-store!)
                                   (evt/reset-ability-event-subscribers-for-test!)
                                   (passive/reset-passive-handler-registry-for-test!))))))

(use-fixtures :each reset-runtime-fixture)

(deftest build-skill-specs-produces-category-specific-generic-course-skills-test
  (let [specs (courses/build-skill-specs :brain-course)
        ids (set (map :id specs))]
    (is (= 4 (count specs)))
    (is (= #{:electromaster/brain-course
             :meltdowner/brain-course
             :teleporter/brain-course
             :vecmanip/brain-course}
           ids))
    (testing "metadata and translations are present on every generated skill"
      (doseq [spec specs]
        (is (= :passive (:pattern spec)))
        (is (= false (:controllable? spec)))
        (is (= "ability.skill.generic.brain_course" (:name-key spec)))
        (is (= "ability.skill.generic.brain_course.desc" (:description-key spec)))
        (is (= "Brain Course"
               (get-in spec [:translations :en_us "ability.skill.generic.brain_course"])))
        (is (= "脑域课程"
               (get-in spec [:translations :zh_cn "ability.skill.generic.brain_course"])))))))

(deftest register-passive-hooks-apply-only-for-learned-skills-test
  (courses/register-passive-hooks! :brain-course-advanced)
  (courses/register-passive-hooks! :mind-course)
       (store/set-player-state!
        ps-fix/test-session-id
   "u-learned"
        (assoc (store/fresh-player-state)
          :ability-data (-> (adata/new-ability-data)
                            (adata/learn-skill :electromaster/brain-course-advanced)
                            (adata/learn-skill :electromaster/mind-course))))
       (store/set-player-state!
        ps-fix/test-session-id
   "u-unlearned"
        (assoc (store/fresh-player-state)
          :ability-data (adata/new-ability-data)))

  (testing "advanced brain course increases max CP and max overload"
    (is (= 2500.0
           (evt/fire-calc-event! evt/CALC-MAX-CP 1000.0 {:uuid "u-learned"})))
    (is (= 300.0
           (evt/fire-calc-event! evt/CALC-MAX-OVERLOAD 200.0 {:uuid "u-learned"})))
    (is (= 1000.0
           (evt/fire-calc-event! evt/CALC-MAX-CP 1000.0 {:uuid "u-unlearned"}))))

  (testing "mind course scales CP recovery speed"
    (is (= 12.0
           (evt/fire-calc-event! evt/CALC-CP-RECOVER-SPEED 10.0 {:uuid "u-learned"})))
    (is (= 10.0
           (evt/fire-calc-event! evt/CALC-CP-RECOVER-SPEED 10.0 {:uuid "u-unlearned"})))))


