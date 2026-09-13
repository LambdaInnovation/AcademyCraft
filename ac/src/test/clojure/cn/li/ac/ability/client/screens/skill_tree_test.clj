(ns cn.li.ac.ability.client.screens.skill-tree-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.client.managed-screens :as managed-screens]
            [cn.li.ac.ability.client.screens.skill-tree :as screen]
            [cn.li.ac.ability.client.screens.skill-tree-view :as view]
            [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.skill-query :as skill]
            [cn.li.ac.ability.rules.learning-rules :as learning-rules]
            [cn.li.ac.test.support.framework :refer [with-fresh-framework]]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.i18n :as i18n]))

(defn- reset-screen-fixture [f]
  ;; Managed-screen state lives in the Framework atom — without one, every
  ;; read/write gets a fresh throwaway atom and screen state is lost.
  (with-fresh-framework
    (fn []
      (managed-screens/call-with-managed-screen-runtime
        (managed-screens/create-managed-screen-runtime)
        (fn []
          (runtime-hooks/with-client-ctx-fn {:session-id :test-session
                                          :player-owner {:client-session-id :test-session}} (fn [] (f))))))))

(use-fixtures :each reset-screen-fixture)

(deftest build-screen-render-data-translates-skill-name-and-description-test
  (let [category-spec {:category-id :generic :name-key "ability.category.generic"}
        player-state {:ability-data {:category-id :generic
                                     :learned-skills #{:generic/brain-course}
                                     :skill-exps {:generic/brain-course 0.35}
                                     :level 3}
                      :resource-data {:cur-cp 1000.0
                                      :max-cp 2000.0
                                      :cur-overload 10.0
                                      :max-overload 100.0}}
        skill-spec {:id :generic/brain-course
                    :category-id :generic
                    :enabled true   ;; render-data filters to enabled skills
                    :name-key "ability.skill.generic.brain_course"
                    :description-key "ability.skill.generic.brain_course.desc"
                    :icon "textures/abilities/generic/skills/brain_course.png"
                    :ui-position [30 110]
                    :level 3
                    :prerequisites []}
        translate-map {"ability.category.generic" "Generic"
                       "ability.skill.generic.brain_course" "Brain Course"
                       "ability.skill.generic.brain_course.desc" "Undergo focused neural training to raise your maximum CP by 1000."}]
    (with-redefs [store/get-player-state (fn [_ _] player-state)
                  store/get-or-create-player-state! (fn [_ _] player-state)
                  category/get-category (fn [_] category-spec)
                  category/get-prog-incr-rate (fn [_] 1.0)
                  skill/get-skills-for-category (fn [_] [skill-spec])
                  skill/get-controllable-skills-at-level (fn [_ _] [skill-spec])
                  skill-registry/get-skill (fn [_] skill-spec)
                  skill/get-skill-icon-path (fn [_] "textures/abilities/generic/skills/brain_course.png")
                  learning-rules/check-all-conditions (fn [_ _ _ _] {:pass? true :failures []})
                  client-bridge/game-time-ms (constantly 0)   ;; no platform bridge in tests
                  i18n/*translate-fn* (fn [k & _] (get translate-map (str k) (str k)))]
      (screen/open-screen! "player-1")
      (let [render-data (screen/build-screen-render-data "player-1")
            node (first (:skill-nodes render-data))]
        (is (= "Generic" (get-in render-data [:ability-info :category-name])))
        (is (= "Brain Course" (:skill-name node)))
        (is (= "Undergo focused neural training to raise your maximum CP by 1000."
               (:skill-description node)))))))

(deftest skill-exp-percent-text-shows-sub-percent-exp-test
  "The skill detail popup's exp line must not truncate: exp gains are
   0.001-0.008 per use, so a few uses put exp at 0.5% — upstream's int
   truncation printed 0% while the node ring already showed the arc."
  (is (= "0.5" (view/skill-exp-percent-text 0.005)))
  (is (= "35.0" (view/skill-exp-percent-text 0.35)))
  (is (= "100.0" (view/skill-exp-percent-text 1.0)))
  (is (= "0.0" (view/skill-exp-percent-text nil))))

(deftest screen-owner-requires-explicit-session-and-player-test
  (runtime-hooks/with-client-ctx-fn {:session-id nil} (fn [] (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Client read model owner requires :client-session-id"
                          (screen/screen-state-snapshot "player-1")))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Client read model owner requires :player-uuid"
                        (screen/screen-state-snapshot {:client-session-id :session-a}))))

(deftest screen-state-isolated-by-player-owner-test
  (with-redefs [client-bridge/game-time-ms (constantly 0)]   ;; no platform bridge in tests
    (screen/open-screen! "player-1" {:developer-type :portable})
    (screen/open-screen! "player-2" {:developer-type :normal})
    (is (= {:developer-type :portable}
           (:learn-context (screen/screen-state-snapshot "player-1"))))
    (is (= {:developer-type :normal}
           (:learn-context (screen/screen-state-snapshot "player-2"))))
    (screen/close-screen! "player-1")
    (is (nil? (:player-uuid (screen/screen-state-snapshot "player-1"))))
    (is (= "player-2" (:player-uuid (screen/screen-state-snapshot "player-2"))))))
