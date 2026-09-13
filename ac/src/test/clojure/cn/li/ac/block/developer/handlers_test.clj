(ns cn.li.ac.block.developer.handlers-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.developer.handlers :as handlers]
            [cn.li.ac.block.developer.logic :as developer-logic]
            [cn.li.ac.block.developer.session :as dev-session]
            [cn.li.ac.block.machine.handlers :as machine-handlers]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.test.support.network :as network-support]
            [cn.li.ac.wireless.gui.sync.handler :as net-helpers]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.mcmod.network.server :as net-server]))
(def ^:private payload {:container-id 7})

(deftest handle-start-development-guards-and-success-test
  (testing "rejects invalid structure"
    (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)
                  net-helpers/get-world (fn [_] :world)
                  entity/player-get-name (fn [_] "Player")
                  platform-be/get-custom-state (fn [_] {:structure-valid false})
                  dev-session/validate-and-start (fn [_ _ _]
                                                   {:ok? false :reason "invalid-structure"})]
      (is (= {:success false :reason "invalid-structure"}
             (handlers/handle-start-development (assoc payload :action :level-up) :player)))))

  (testing "rejects wrong user"
    (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)
                  net-helpers/get-world (fn [_] :world)
                  entity/player-get-name (fn [_] "Player")
                  ;; handle-start-development forces immediate re-validation and
                  ;; overwrites :structure-valid — stub it so the wrong-user
                  ;; guard (validate-common) is what rejects.
                  developer-logic/check-structure-valid? (constantly true)
                  machine-runtime/state-or-default (fn [_ _]
                                                       {:structure-valid true :user-uuid "owner"})
                  uuid/player-uuid (fn [_] "other")]
      (is (= {:success false :reason "wrong-user"}
             (handlers/handle-start-development payload :player)))))

  (testing "starts development for current user"
    (let [saved (atom nil)]
      (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)
                    net-helpers/get-world (fn [_] :world)
                    uuid/player-uuid (fn [_] "self")
                    entity/player-get-name (fn [_] "Player")
                    dev-session/validate-and-start (fn [state _ _]
                                                     {:ok? true
                                                      :state (assoc state :is-developing true
                                                                    :development-progress 0.0)})
                    machine-runtime/commit-state! (fn [_ _ _ _ st] (reset! saved st))]
        (is (= {:success true}
               (handlers/handle-start-development (assoc payload :action :level-up) :player)))
        (is (= "self" (:user-uuid @saved)))
        (is (= "Player" (:user-name @saved)))
        (is (true? (:is-developing @saved)))))))

(deftest handle-stop-development-test
  (let [saved (atom nil)]
    (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)
                  net-helpers/get-world (fn [_] :world)
                  machine-runtime/state-or-default (fn [_ _]
                                                       {:is-developing true
                                                        :development-progress 0.42
                                                        :development-data {:state :developing}
                                                        :development-action :level-up
                                                        :development-payload {:skill-id :x}})
                  machine-runtime/commit-state! (fn [_ _ _ _ st] (reset! saved st))]
      (is (= {:success true}
             (handlers/handle-stop-development payload :player)))
      (is (false? (:is-developing @saved)))
      (is (= 0.0 (:development-progress @saved)))
      (is (nil? (:development-data @saved)))
      (is (nil? (:development-action @saved)))
      (is (nil? (:development-payload @saved))))))

(deftest register-network-handlers-registers-all-actions-test
  (let [calls (atom [])]
    (with-redefs [net-server/register-handler (fn [msg-id _handler]
                                                (swap! calls conj msg-id))
                  msg-registry/msg (fn [_ action] [:developer action])]
      (handlers/register-network-handlers!)
      (is (= 5 (count @calls))))))
