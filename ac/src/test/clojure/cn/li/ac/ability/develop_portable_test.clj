(ns cn.li.ac.ability.develop-portable-test
  "Portable developer timed-development pipeline: :develop-start /
   :develop-fail reducer commands and :server-tick completion re-validation
   (player-state :develop-data — upstream PortableDevData + DevelopData)."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.model.develop :as dev]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.registry.skill :as skill]
            [cn.li.ac.ability.rules.develop-rules :as develop]
            [cn.li.ac.ability.rules.learning-rules :as learning]
            [cn.li.ac.ability.service.reducer :as reducer]))

(defn- base-state []
  {:player-uuid "u1"
   :ability-data (assoc (adata/new-ability-data) :category-id :cat1 :level 2)
   :resource-data (rdata/new-resource-data)
   :cooldown-data {}})

(defn- one-tick-from-done
  "A :portable learn-skill session (tps 25) on its final tick."
  []
  (-> (dev/start-develop (dev/new-develop-data) :portable :learn-skill {:skill-id :s1} 1)
      (assoc :tick-this-stim 24)))

(deftest develop-start-test
  (with-redefs [skill/get-skill (fn [_] {:id :s1 :level 1})
                learning/check-all-conditions (fn [& _] {:pass? true :failures []})]
    (testing "learn-skill starts a :portable session with truncated stims"
      (let [{:keys [state rejected-reason]}
            (reducer/apply-command (base-state)
                                   {:command :develop-start :action :learn-skill
                                    :skill-id :s1 :developer-type :portable})
            dd (:develop-data state)]
        (is (nil? rejected-reason))
        (is (dev/developing? dd))
        (is (= :portable (:developer-type dd)))
        (is (= (dev/skill-learning-stims 1) (:max-stim dd)))))
    (testing "already developing is rejected"
      (let [busy (assoc (base-state) :develop-data
                        (dev/start-develop (dev/new-develop-data) :portable :level-up
                                           {:target-level 3} 15))]
        (is (= :already-developing
               (:rejected-reason
                (reducer/apply-command busy {:command :develop-start :action :level-up}))))))
    (testing "level-up requires a category (shell awakens instead)"
      (let [no-cat (assoc-in (base-state) [:ability-data :category-id] nil)]
        (is (= :no-category
               (:rejected-reason
                (reducer/apply-command no-cat {:command :develop-start :action :level-up}))))))
    (testing "awaken carries the shell-decided category, 5 stims (level-up-stims 0)"
      (let [no-cat (assoc-in (base-state) [:ability-data :category-id] nil)
            {:keys [state]} (reducer/apply-command
                             no-cat {:command :develop-start :action :awaken
                                     :target-category :electromaster})
            dd (:develop-data state)]
        (is (dev/developing? dd))
        (is (= :awaken (:action-type dd)))
        (is (= :electromaster (get-in dd [:action-data :target-category])))
        (is (= (dev/level-up-stims 0) (:max-stim dd)))))
    (testing "awaken without a category decision is rejected"
      (is (= :no-target-category
             (:rejected-reason
              (reducer/apply-command (base-state)
                                     {:command :develop-start :action :awaken})))))
    (testing "learn-skill with failing conditions is rejected"
      (with-redefs [learning/check-all-conditions (fn [& _] {:pass? false :failures [{:type :level}]})]
        (is (= :conditions-not-met
               (:rejected-reason
                (reducer/apply-command (base-state)
                                       {:command :develop-start :action :learn-skill
                                        :skill-id :s1}))))))))

(deftest develop-fail-test
  (let [busy (assoc (base-state) :develop-data (one-tick-from-done))
        {:keys [state]} (reducer/apply-command busy {:command :develop-fail})]
    (is (dev/failed? (:develop-data state))))
  (testing "no-op when not developing"
    (let [idle (assoc (base-state) :develop-data (dev/new-develop-data))
          {:keys [state]} (reducer/apply-command idle {:command :develop-fail})]
      (is (dev/idle? (:develop-data state))))))

(deftest server-tick-completion-revalidation-test
  (with-redefs [skill/get-skill (fn [_] {:id :s1 :level 1})
                evt/fire-calc-event! (fn [_k v _extra] v)
                evt/make-skill-learn-event (fn [uuid sid]
                                             {:event/type :ability/skill-learn
                                              :uuid uuid :skill-id sid})]
    (testing "valid completion applies and keeps :develop-data terminal (:done)"
      (with-redefs [learning/check-all-conditions (fn [& _] {:pass? true :failures []})]
        (let [st (assoc (base-state) :develop-data (one-tick-from-done))
              {:keys [state events]} (reducer/apply-command
                                      st {:command :server-tick :uuid "u1"
                                          :cp-speed 1.0 :ol-speed 1.0})]
          (is (contains? (get-in state [:ability-data :learned-skills]) :s1))
          (is (dev/done? (:develop-data state)))
          (is (= 1.0 (dev/progress (:develop-data state))))
          (is (= :ability/skill-learn (:event/type (first events)))))))
    (testing "re-validation failure marks the session failed, nothing applied"
      (with-redefs [learning/check-all-conditions (fn [& _] {:pass? false :failures [{:type :level}]})]
        (let [st (assoc (base-state) :develop-data (one-tick-from-done))
              {:keys [state events]} (reducer/apply-command
                                      st {:command :server-tick :uuid "u1"
                                          :cp-speed 1.0 :ol-speed 1.0})]
          (is (dev/failed? (:develop-data state)))
          (is (not (contains? (get-in state [:ability-data :learned-skills]) :s1)))
          (is (empty? events)))))))

(deftest apply-completion-awaken-test
  (let [dd (-> (dev/new-develop-data)
               (assoc :state :done
                      :action-type :awaken
                      :action-data {:target-category :electromaster :target-level 1}))
        {:keys [ability-data events develop-data]}
        (develop/apply-completion dd (adata/new-ability-data) (rdata/new-resource-data) "u1")]
    (is (= :electromaster (:category-id ability-data)))
    (is (= 1 (:level ability-data)))
    (is (= 1 (count events)))
    (is (dev/idle? develop-data))))
