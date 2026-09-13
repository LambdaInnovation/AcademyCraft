(ns cn.li.ac.ability.adapters.client-overlay-lifecycle-test
  (:require
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.adapters.client-ui-hooks :as client-ui-hooks]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.client.keybinds :as client-keybinds]
            [cn.li.ac.content.ability.electromaster.current-charging-fx :as current-charging-fx]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.test.support.network :as network-support]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.client :as net-client]))

(defn- reset-ui-state! [f]
  (client-ui-hooks/reset-client-ui-state-for-test!)
  (f)
  (client-ui-hooks/reset-client-ui-state-for-test!))

(use-fixtures :each reset-ui-state!)

(def ^:private test-client-session :test-client-session)

(deftest movement-keys-ignore-terminated-flashing-context-test
  (let [sent (atom [])
        hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (runtime-hooks/with-client-ctx-fn {:session-id test-client-session} (fn [] (client-ui-hooks/set-slot-context-for-test! "p1" 0 "ctx-flashing-dead")))
    (with-redefs [ctx/get-context (fn [_owner _ctx-id]
                                    {:id "ctx-flashing-dead"
                                     :player-uuid "p1"
                                     :skill-id :flashing
                                     :status ctx/STATUS-TERMINATED})
                  net-client/send-to-server (fn [& args]
                                              (let [[msg-id payload] (if (= 4 (count args))
                                                                       [(nth args 1) (nth args 2)]
                                                                       [(first args) (second args)])]
                                                (swap! sent conj [msg-id payload])))]
      (runtime-hooks/with-client-ctx-fn {:session-id test-client-session} (fn [] ((:client-on-movement-key-down! hooks) "p1" :forward)
        ((:client-on-movement-key-tick! hooks) "p1" :forward)
        ((:client-on-movement-key-up! hooks) "p1" :forward)))
      (is (empty? @sent)))))

(deftest terminated-contexts-do-not-drive-charge-or-crosshair-overlays-test
  (let [hooks (client-ui-hooks/runtime-client-ui-hooks)
        terminated-player-contexts [{:id "ctx-railgun-dead"
                                     :player-uuid "p1"
                                     :skill-id :railgun
                                     :status ctx/STATUS-TERMINATED
                                     :skill-state {:mode :item-charge :charge-ticks 10}}
                                    {:id "ctx-body-dead"
                                     :player-uuid "p1"
                                     :skill-id :body-intensify
                                     :status ctx/STATUS-TERMINATED
                                     :skill-state {:hold-ticks 20}}]
        terminated-toggle-contexts {"ctx-reflection-dead"
                                    {:id "ctx-reflection-dead"
                                     :player-uuid "p1"
                                     :skill-id :vec-reflection
                                     :status ctx/STATUS-TERMINATED
                                     :skill-state {:toggle {:vec-reflection {:active true}
                                                            :vec-deviation {:active true}}}}}]
    (with-redefs [read-model/get-player-contexts-for-player (fn [& _] terminated-player-contexts)
                  ctx/get-all-contexts (fn [] terminated-toggle-contexts)
                  store/get-player-state (fn [_ _]
                                        {:resource-data {:activated true
                                                         :cur-cp 80.0
                                                         :max-cp 100.0
                                                         :cur-overload 0.0
                                                         :max-overload 100.0}
                                         :cooldown-data {}
                                         :preset-data {:active-preset 0 :slots {}}})
                  client-keybinds/get-activate-hint (fn [_] nil)
                  client-keybinds/get-preset-switch-state (fn [_] nil)
                  current-charging-fx/current-state (fn [& _] {:active? false
                                                                :blending? false
                                                                :is-item false
                                                                :good? false
                                                                :charge-ticks 0
                                                                :charge-ratio 0.0})
                  skill-config/tunable-int (fn [skill-id field-id]
                                             (case [skill-id field-id]
                                               [:railgun :qte.coin-window-ms] 1000
                                               [:railgun :charge.item-charge-ticks] 20
                                               [:body-intensify :charge.max-time] 40
                                               1))
                  skill-config/tunable-double (fn [skill-id field-id]
                                                (case [skill-id field-id]
                                                  [:railgun :qte.coin-active-threshold] 0.6
                                                  0.0))]
        (runtime-hooks/with-client-ctx-fn {:session-id test-client-session} (fn [] (is (false? (:active? ((:client-visual-state hooks) :ac/charge-coin {:player-uuid "p1"}))))
        (is (false? (:active? ((:client-visual-state hooks) :ac/body-intensify-charge {:player-uuid "p1"}))))
        (let [plan (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1000})
            kinds (set (map :kind (:elements plan)))]
          (is (not (contains? kinds :content-crosshair)))
          (is (not (contains? kinds :blit-texture)))))))))
