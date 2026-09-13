(ns cn.li.ac.ability.client.read-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.block.developer.panel-reactive :as panel]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(deftest local-client-owner-prefers-default-client-owner-test
  (testing "uses platform default-client-owner session (sync partition), not ThreadLocal player-owner"
    (let [default-session [:client-session :default]
          bound-session [:client-session :bound]]
      (with-redefs [runtime-hooks/default-client-owner
                    (fn [] {:logical-side :client
                            :client-session-id default-session
                            :player-uuid "other-uuid"})]
        (runtime-hooks/with-client-ctx-fn
          {:session-id bound-session
           :player-owner {:logical-side :client
                          :client-session-id bound-session
                          :player-uuid "player-1"}}
          (fn []
            (is (= {:logical-side :client
                    :client-session-id default-session
                    :player-uuid "player-1"}
                   (read-model/local-client-owner "player-1" "test.component")))))))))

(deftest local-client-owner-falls-back-to-threadlocal-session-test
  (testing "without default-client-owner, uses ThreadLocal session-id"
    (with-redefs [runtime-hooks/default-client-owner (fn [] nil)]
      (runtime-hooks/with-client-ctx-fn
        {:session-id [:client-session :tl]}
        (fn []
          (is (= {:logical-side :client
                  :client-session-id [:client-session :tl]
                  :player-uuid "player-1"}
                 (read-model/local-client-owner "player-1" "test.component"))))))))

(deftest portable-container-owner-reads-synced-ability-test
  (testing "developer panel model reads ability-data from container owner session"
    (let [session-id [:client-session :portable-test]
          player-uuid "player-portable"
          ability (assoc (adata/new-ability-data)
                         :category-id :electromaster
                         :level 2
                         :level-progress 0.5)
          owner {:logical-side :client
                 :client-session-id session-id
                 :player-uuid player-uuid}
          container {:energy (atom 100.0)
                     :max-energy (atom 10000.0)
                     :tier (atom :portable)
                     :is-developing (atom false)
                     :container-type :portable-developer
                     :owner owner
                     :client-session-id session-id
                     :player-uuid player-uuid}
          player (reify Object)]
      (store/set-player-state! session-id player-uuid
                               (assoc (store/fresh-player-state) :ability-data ability))
      (with-redefs [cn.li.ac.ability.util.uuid/player-uuid (fn [_] player-uuid)
                    cn.li.ac.ability.registry.category/get-category
                    (fn [cat-id]
                      (when (= cat-id :electromaster)
                        {:id :electromaster
                         :name-key "ac.cat.electromaster"
                         :icon "academy:textures/guis/icons/electromaster.png"}))]
        (let [model (panel/current-ui-model container player)]
          (is (true? (:has-category? model)))
          (is (= "Level 2" (:level-label model)))
          (is (not= "N/A" (:ability-name model))))))))
