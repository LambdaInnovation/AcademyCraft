(ns cn.li.ac.ability.adapters.client-ui-hooks-test
  (:require
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.adapters.client-ui-hooks :as client-ui-hooks]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.client.effects.particles :as particles]
            [cn.li.ac.ability.client.effects.sounds :as sounds]
            [cn.li.ac.test.support.hud-render-data :as hud-rd]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.keybinds :as client-keybinds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.managed-screens :as managed-screens]
            [cn.li.ac.ability.client.reactive-hud :as reactive-hud]
            [cn.li.ac.content.ability.vecmanip.vec-deviation-fx :as vdfx]
            [cn.li.ac.content.ability.teleporter.location-teleport-reactive :as location-teleport-screen]
            [cn.li.ac.ability.client.screens.preset-editor :as preset-editor-screen]
            [cn.li.ac.ability.client.screens.skill-tree :as skill-tree-screen]
            [cn.li.ac.test.support.network :as network-support]
            [cn.li.ac.content.ability.electromaster.current-charging-fx :as current-charging-fx]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.config.gameplay :as gameplay]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]            [cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.ability.messages :as catalog]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.client :as net-client]))

(defn- with-fresh-ui-runtimes
  [f]
  (client-ui-hooks/call-with-client-ui-runtime
    (client-ui-hooks/create-client-ui-runtime)
    (fn []
      (client-keybinds/call-with-client-keybind-runtime
        (client-keybinds/create-client-keybind-runtime)
        (fn []
          (managed-screens/call-with-managed-screen-runtime
            (managed-screens/create-managed-screen-runtime)
            f))))))

(defn- reset-ui-state! [f]
  (with-fresh-ui-runtimes
    (fn []
      (ps-fix/with-test-player-state-owner
        (fn []
          (client-ui-hooks/reset-client-ui-state-for-test!)
          (client-keybinds/reset-client-keybind-state-for-test!)
          (particles/reset-particle-queue-for-test!)
          (sounds/reset-sound-queue-for-test!)
          (hand-effects/reset-hand-effect-registry-for-test!)
          (store/reset-store!)
          (ctx/reset-contexts-for-test!)
          (try
            (binding [client-keybinds/*client-session-id* :test-session]
              (f))
            (finally
              (client-ui-hooks/reset-client-ui-state-for-test!)
              (client-keybinds/reset-client-keybind-state-for-test!)
              (particles/reset-particle-queue-for-test!)
              (sounds/reset-sound-queue-for-test!)
              (hand-effects/reset-hand-effect-registry-for-test!)
              (ctx/reset-contexts-for-test!)
              (store/reset-store!))))))))

(use-fixtures :each reset-ui-state!)

(defn- with-client-player-state-owner
  [player-uuid f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session
                                  :player-owner {:client-session-id :test-session
                                                  :player-uuid player-uuid}} (fn [] (binding [client-keybinds/*client-session-id* :test-session]
      (f)))))

(deftest client-slot-key-hooks-create-context-once-and-send-input-messages-test
  (let [sent (atom [])
        activated (atom [])
        hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [client-keybinds/get-skill-id-for-slot-public
                  (fn [player-uuid key-idx]
                    (when (and (= "p1" player-uuid) (= 0 key-idx))
                      :railgun))
                  ctx-mgr/activate-context!
                  (fn [_owner player-uuid skill-id]
                    (swap! activated conj {:player-uuid player-uuid :skill-id skill-id})
                    {:id "ctx-client-1"})
                  net-client/send-to-server (network-support/capture-send-to-server! sent)]
      ((:client-on-slot-key-down! hooks) "p1" 0)
      ((:client-on-slot-key-tick! hooks) "p1" 0)
      ((:client-on-slot-key-up! hooks) "p1" 0)
      ((:client-abort-all! hooks))
      (is (= [{:player-uuid "p1" :skill-id :railgun}]
             @activated))
      (is (= [catalog/MSG-SLOT-KEY-DOWN
              catalog/MSG-CTX-KEEPALIVE
              catalog/MSG-SLOT-KEY-UP]
             (mapv :msg-id @sent)))
      (is (= [{:ctx-id "ctx-client-1" :skill-id :railgun :key-idx 0}
              {:ctx-id "ctx-client-1"}
              {:ctx-id "ctx-client-1" :key-idx 0}]
             (mapv :payload @sent))))))

      (deftest client-slot-key-tick-without-active-context-sends-no-message-test
        (let [sent (atom [])
         activated (atom [])
         hooks (client-ui-hooks/runtime-client-ui-hooks)]
          (with-redefs [client-keybinds/get-skill-id-for-slot-public (fn [_ _] :railgun)
                    ctx-mgr/activate-context! (fn [_owner player-uuid skill-id]
                      (swap! activated conj {:player-uuid player-uuid
                              :skill-id skill-id})
                      {:id "ctx-should-not-exist"})
              net-client/send-to-server (network-support/capture-send-to-server! sent)]
            ((:client-on-slot-key-tick! hooks) "p1" 0)
            (is (empty? @activated))
            (is (empty? @sent)))))

(deftest client-slot-key-abort-sends-abort-message-and-clears-context-test
  (let [sent (atom [])
        terminated (atom [])
        hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [client-keybinds/get-skill-id-for-slot-public (fn [_ _] :railgun)
                  ctx-mgr/activate-context! (fn [_owner _player-uuid _skill-id] {:id "ctx-client-abort"})
                  net-client/send-to-server (network-support/capture-send-to-server! sent)
                  ctx/terminate-context! (fn
                                           ([ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil)
                                           ([_owner ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil))]
      ((:client-on-slot-key-down! hooks) "p1" 0)
      ((:client-on-slot-key-abort! hooks) "p1" 0)
      (is (= [catalog/MSG-SLOT-KEY-DOWN
              catalog/MSG-SLOT-KEY-ABORT]
             (mapv :msg-id @sent)))
      (is (= [{:ctx-id "ctx-client-abort" :skill-id :railgun :key-idx 0}
              {:ctx-id "ctx-client-abort" :key-idx 0}]
             (mapv :payload @sent)))
      (is (= [["ctx-client-abort" nil]] @terminated))
      (is (empty? (:slot-context-ids (client-ui-hooks/client-ui-state-snapshot "p1")))))))

(deftest client-abort-all-sends-abort-messages-for-active-slot-contexts-test
  (let [sent (atom [])
        terminated (atom [])
        hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [client-keybinds/get-skill-id-for-slot-public (fn [_ _] :railgun)
                  ctx-mgr/activate-context! (fn [_owner _player-uuid _skill-id] {:id "ctx-client-abort-all"})
                  net-client/send-to-server (network-support/capture-send-to-server! sent)
                  ctx/terminate-context! (fn
                                           ([ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil)
                                           ([_owner ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil))]
      ((:client-on-slot-key-down! hooks) "p1" 0)
      ((:client-abort-all! hooks))
      (is (= [catalog/MSG-SLOT-KEY-DOWN
              catalog/MSG-SLOT-KEY-ABORT]
             (mapv :msg-id @sent)))
      (is (= [["ctx-client-abort-all" nil]] @terminated))
      (is (empty? (:slot-context-ids (client-ui-hooks/client-ui-state-snapshot "p1")))))))

(deftest slot-context-state-isolated-by-client-session-test
  (let [ctx-counter (atom 0)
        hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [client-keybinds/get-skill-id-for-slot-public (fn [_ _] :railgun)
                  ctx-mgr/activate-context! (fn [_owner _player-uuid _skill-id]
                                              {:id (str "ctx-" (swap! ctx-counter inc))})
                  net-client/send-to-server (fn [& _] nil)]
      (binding [client-keybinds/*client-session-id* :session-a]
        ((:client-on-slot-key-down! hooks) "p1" 0))
      (binding [client-keybinds/*client-session-id* :session-b]
        ((:client-on-slot-key-down! hooks) "p1" 0))
      (is (= {[:session-a "p1" 0] "ctx-1"}
             (:slot-context-ids (client-ui-hooks/client-ui-state-snapshot
                                 {:client-session-id :session-a
                                  :player-uuid "p1"}))))
      (is (= {[:session-b "p1" 0] "ctx-2"}
             (:slot-context-ids (client-ui-hooks/client-ui-state-snapshot
                                 {:client-session-id :session-b
                    :player-uuid "p1"})))))))

(deftest client-ui-owner-requires-explicit-session-and-player-test
  (runtime-hooks/with-client-ctx-fn {:session-id nil} (fn [] (binding [client-keybinds/*client-session-id* nil]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Client UI owner requires :client-session-id"
                            (client-ui-hooks/client-ui-state-snapshot "p1"))))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Client UI owner requires :player-uuid"
                        (client-ui-hooks/client-ui-state-snapshot {:client-session-id :session-a}))))



(deftest client-clear-owner-state-clears-owned-runtime-state-test
  (let [hooks (client-ui-hooks/runtime-client-ui-hooks)
        owner {:client-session-id :session-a
               :player-uuid "p1"}
        context-owner {:logical-side :client
                       :client-session-id :session-a
                       :player-uuid "p1"}
        screen-calls (atom [])
        keybind-clears (atom [])]
    (ctx/with-context-owner context-owner
      (runtime-hooks/with-player-state-owner-fn
        owner
        (fn []
          (ctx/register-context! (assoc (ctx/new-context "p1" :arc-gen context-owner)
                                        :id "ctx-cleanup"))
          (store/set-player-state! :session-a "p1" {:resource-data {:activated true}}))))
    (client-ui-hooks/set-slot-context-for-test! owner 0 "ctx-cleanup")
    (client-ui-hooks/seed-vm-wave-state-for-test! owner [{:radius 1.0}] 42)
    (particles/queue-particle-effect! owner {:type :particle :particle-type :spark})
    (sounds/queue-sound-effect! owner {:type :sound :sound-id "minecraft:test"})
    (hand-effects/add-camera-pitch-delta! owner 1.5)
    (with-redefs [skill-tree-screen/close-screen! (fn [screen-owner]
                                                    (swap! screen-calls conj [:skill-tree screen-owner])
                                                    nil)
                  preset-editor-screen/close-screen! (fn [screen-owner]
                                                       (swap! screen-calls conj [:preset-editor screen-owner])
                                                       nil)
                  location-teleport-screen/close-screen! (fn [screen-owner]
                                                           (swap! screen-calls conj [:location-teleport screen-owner])
                                                           nil)
                  client-keybinds/clear-client-keybind-state! (fn [clear-owner]
                                                                (swap! keybind-clears conj clear-owner)
                                                                nil)]
      ((:client-clear-owner-state! hooks) owner))
    (is (= [[:skill-tree owner]
            [:preset-editor owner]
            [:location-teleport owner]]
           @screen-calls))
    (is (= [owner] @keybind-clears))
    (is (= {:vm-wave-circles []
            :vm-wave-last-spawn-ms 0
            :slot-context-ids {}
            :slot-key-tick-ms {}
            :charge-coin-state nil}
           (client-ui-hooks/client-ui-state-snapshot owner)))
    (is (empty? (particles/particle-queue-snapshot (:client-session-id owner))))
    (is (empty? (sounds/sound-queue-snapshot (:client-session-id owner))))
    (is (empty? (hand-effects/drain-camera-pitch-deltas! owner)))
    (runtime-hooks/with-client-ctx-fn {:player-owner owner} (fn [] (is (nil? (store/get-player-state :session-a "p1")))))
    (ctx/with-context-owner context-owner
      (is (nil? (ctx/get-context "ctx-cleanup"))))))

(deftest hud-render-data-hidden-when-not-activated-test
  (let [model {:cp {:cur 50.0 :max 100.0}
               :overload {:cur 10.0 :max 100.0 :fine true}
               :active-slots []
               :activated false}]
    (is (nil? (hud-rd/build-hud-render-data model 320 180 {})))))

(deftest client-slot-wheel-sends-ctx-channel-only-for-penetrate-with-active-context-test
  ;; The wheel listener only knows the player — the adapter resolves the
  ;; ACTIVE penetrate context from the client context registry (upstream
  ;; registers the wheel listener on MSG_MADEALIVE and unregisters on
  ;; terminate).
  (let [sent (atom [])
        hooks (client-ui-hooks/runtime-client-ui-hooks)
        ctx-data (fn [id skill-id active?]
                   {:id id :player-uuid "p1" :skill-id skill-id :active? active?})]
    (with-redefs [ctx/get-all-contexts
                  (fn []
                    {"c1" (ctx-data "ctx-penetrate" :penetrate-teleport true)
                     "c2" (ctx-data "ctx-railgun" :railgun true)
                     "c3" (ctx-data "ctx-dead" :penetrate-teleport false)})
                  ctx/active-context? (fn [ctx-data] (:active? ctx-data))
                  gameplay/use-mouse-wheel-enabled? (fn [] true)
                  net-client/send-to-server (network-support/capture-send-to-server! sent)]
      ((:client-on-slot-wheel! hooks) "p1" 0 2.0)
      (with-redefs [gameplay/use-mouse-wheel-enabled? (fn [] false)]
        ((:client-on-slot-wheel! hooks) "p1" 0 1.0))
      (is (= [{:msg-id catalog/MSG-CTX-CHANNEL
               :payload {:ctx-id "ctx-penetrate"
                         :channel :penetrate-tp/set-distance
                         :payload {:delta 2.0}}}]
             (network-support/sent-without-callbacks @sent))))))


(deftest client-terminated-push-clears-slot-context-and-terminates-local-context-test
  (let [handlers (atom {})
        terminated (atom [])]
    (client-ui-hooks/set-slot-context-for-test! "p1" 0 "ctx-dead")
    (with-redefs [net-client/register-push-handler! (fn [msg-id handler-fn]
                                                      (swap! handlers assoc msg-id handler-fn)
                                                      nil)
                  ctx/terminate-context! (fn
                                           ([ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil)
                                           ([_owner ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil))]
      (client-ui-hooks/register-client-push-handlers!)
      ((get @handlers catalog/MSG-CTX-TERMINATED) {:ctx-id "ctx-dead"})
      (is (empty? (:slot-context-ids (client-ui-hooks/client-ui-state-snapshot "p1"))))
      (is (= [["ctx-dead" nil]] @terminated)))))

(deftest sync-preset-push-rebuilds-default-key-group-test
  (let [handlers (atom {})
        rebuilt (atom [])]
    (with-redefs [net-client/register-push-handler! (fn [msg-id handler-fn]
                                                      (swap! handlers assoc msg-id handler-fn)
                                                      nil)
                  client-keybinds/update-default-group! (fn [player-uuid]
                                                         (swap! rebuilt conj player-uuid)
                                                         nil)]
      (client-ui-hooks/register-client-push-handlers!)
      ((get @handlers catalog/MSG-SYNC-V2) {:version 2
                                            :opcode 2
                                            :uuid "p1"
                                            :revision 1
                                            :dirty-mask store/preset-data-mask
                                            :preset-data {:active-preset 0
                                                          :presets []}})
      (is (= ["p1"] @rebuilt)))))

(deftest sync-runtime-push-clears-keybind-state-and-aborts-active-slot-contexts-test
  (let [handlers (atom {})
        sent (atom [])
        terminated (atom [])
        cleared (atom [])
        cleared-groups (atom [])]
    (with-client-player-state-owner
      "p1"
      #(store/set-player-state! :test-session "p1" {:ability-data {:category-id :electromaster
                                                                      :learned-skills [:railgun]}}))
    (client-ui-hooks/set-slot-context-for-test! "p1" 0 "ctx-runtime-reset")
    (with-redefs [net-client/register-push-handler! (fn [msg-id handler-fn]
                                                      (swap! handlers assoc msg-id handler-fn)
                                                      nil)
                  net-client/send-to-server (network-support/capture-send-to-server! sent)
                  ctx/terminate-context! (fn
                                           ([ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil)
                                           ([_owner ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil))
                  client-keybinds/clear-client-keybind-state! (fn [owner]
                                                                (swap! cleared conj owner)
                                                                nil)
                  client-keybinds/clear-key-group! (fn [group]
                                                     (swap! cleared-groups conj group)
                                                     nil)]
      (client-ui-hooks/register-client-push-handlers!)
      ((get @handlers catalog/MSG-SYNC-V2) {:version 2
                                            :opcode 2
                                            :uuid "p1"
                                            :revision 1
                                            :dirty-mask store/ability-data-mask
                                            :ability-data {:category-id :meltdowner
                                                           :learned-skills []}})
      (is (= [{:msg-id catalog/MSG-SLOT-KEY-ABORT
               :payload {:ctx-id "ctx-runtime-reset" :key-idx 0}}]
             (network-support/sent-without-callbacks @sent)))
      (is (= [["ctx-runtime-reset" nil]] @terminated))
      (is (= ["p1"] @cleared))
      (is (= [:default] @cleared-groups))
      (is (empty? (:slot-context-ids (client-ui-hooks/client-ui-state-snapshot "p1")))))))

(deftest sync-resource-push-disables-held-input-and-clears-keybind-state-test
  (let [handlers (atom {})
        sent (atom [])
        terminated (atom [])
        cleared (atom [])]
    (with-client-player-state-owner
      "p1"
      #(store/set-player-state! :test-session "p1" {:resource-data {:activated true
                                                                       :overload-fine true
                                                                       :interferences #{}}}))
    (client-ui-hooks/set-slot-context-for-test! "p1" 0 "ctx-resource-reset")
    (with-redefs [net-client/register-push-handler! (fn [msg-id handler-fn]
                                                      (swap! handlers assoc msg-id handler-fn)
                                                      nil)
                  net-client/send-to-server (network-support/capture-send-to-server! sent)
                  ctx/terminate-context! (fn
                                           ([ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil)
                                           ([_owner ctx-id terminate-fn]
                                            (swap! terminated conj [ctx-id terminate-fn])
                                            nil))
                  client-keybinds/clear-client-keybind-state! (fn [owner]
                                                                (swap! cleared conj owner)
                                                                nil)]
      (client-ui-hooks/register-client-push-handlers!)
      ((get @handlers catalog/MSG-SYNC-V2) {:version 2
                                            :opcode 2
                                            :uuid "p1"
                                            :revision 1
                                            :dirty-mask store/resource-data-mask
                                            :resource-data {:activated false
                                                            :overload-fine true
                                                            :interferences #{}}})
      (is (= [{:msg-id catalog/MSG-SLOT-KEY-ABORT
               :payload {:ctx-id "ctx-resource-reset" :key-idx 0}}]
             (network-support/sent-without-callbacks @sent)))
      (is (= [["ctx-resource-reset" nil]] @terminated))
      (is (= ["p1"] @cleared))
      (is (empty? (:slot-context-ids (client-ui-hooks/client-ui-state-snapshot "p1")))))))

(deftest charge-coin-hooks-notify-and-visual-state-test
  (let [hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [skill-config/tunable-int (fn [skill-id field-id]
                                                               (case [skill-id field-id]
                                                                 [:railgun :qte.coin-window-ms] 1000
                                                                 [:railgun :charge.item-charge-ticks] 20
                                                                 1))
                  skill-config/tunable-double (fn [skill-id field-id]
                                                                  (case [skill-id field-id]
                                                                    [:railgun :qte.coin-active-threshold] 0.6
                                                                    0.0))
                  read-model/get-player-contexts-for-player (fn [& _] [])]
      ((:client-notify-visual-event! hooks) :ac/charge-coin-throw {:player-uuid "p1" :now-ms 1000})
      (let [visual ((:client-visual-state hooks) :ac/charge-coin {:player-uuid "p1" :now-ms 1000})]
        (is (true? (:active? visual)))
        (is (false? (:coin-active? visual)))
        (is (number? (:coin-progress visual)))
        (is (number? (:charge-ratio visual)))))))

(deftest charge-coin-state-isolated-by-client-session-test
  (let [hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [skill-config/tunable-int (fn [skill-id field-id]
                                             (case [skill-id field-id]
                                               [:railgun :qte.coin-window-ms] 1000
                                               [:railgun :charge.item-charge-ticks] 20
                                               1))
                  skill-config/tunable-double (fn [_ _] 0.6)
                  read-model/get-player-contexts-for-player (fn [& _] [])]
      (binding [client-keybinds/*client-session-id* :session-a]
        ((:client-notify-visual-event! hooks) :ac/charge-coin-throw {:player-uuid "p1" :now-ms 1000}))
      (is (some? (:charge-coin-state (client-ui-hooks/client-ui-state-snapshot
                                      {:client-session-id :session-a :player-uuid "p1"}))))
      (is (nil? (:charge-coin-state (client-ui-hooks/client-ui-state-snapshot
                                     {:client-session-id :session-b :player-uuid "p1"})))))))

(deftest charge-coin-visual-state-prefers-item-charge-context-test
  (let [hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [skill-config/tunable-int (fn [skill-id field-id]
                                                               (case [skill-id field-id]
                                                                 [:railgun :qte.coin-window-ms] 1000
                                                                 [:railgun :charge.item-charge-ticks] 20
                                                                 1))
                  skill-config/tunable-double (fn [skill-id field-id]
                                                                  (case [skill-id field-id]
                                                                    [:railgun :qte.coin-active-threshold] 0.6
                                                                    0.0))
                  read-model/get-player-contexts-for-player (fn [& _]
                                                              [{:skill-id :railgun
                                                                :skill-state {:mode :item-charge :charge-ticks 10}}])]
      (let [visual ((:client-visual-state hooks) :ac/charge-coin {:player-uuid "p1"})]
        (is (true? (:active? visual)))
        (is (= 10 (:charge-ticks visual)))
        (is (false? (:coin-active? visual)))
        (is (= 0.0 (:coin-progress visual)))
        (is (= 0.5 (:charge-ratio visual)))))))

(deftest body-intensify-charge-visual-state-default-and-active-test
  (let [hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [skill-config/tunable-int (fn [skill-id field-id]
                                              (case [skill-id field-id]
                                                [:body-intensify :charge.max-ticks] 40
                                                1))
                  read-model/get-player-contexts-for-player (fn [& _] [])]
      (let [visual ((:client-visual-state hooks) :ac/body-intensify-charge {:player-uuid "p1"})]
        (is (false? (:active? visual)))
        (is (= 0 (:charge-ticks visual)))
        (is (= 0.0 (:charge-ratio visual)))))
    (with-redefs [skill-config/tunable-int (fn [skill-id field-id]
                                              (case [skill-id field-id]
                                                [:body-intensify :charge.max-ticks] 40
                                                1))
                  read-model/get-player-contexts-for-player (fn [& _]
                                                              [{:skill-id :body-intensify
                                                                :skill-state {:hold-ticks 20}}])]
      (let [visual ((:client-visual-state hooks) :ac/body-intensify-charge {:player-uuid "p1"})]
        (is (true? (:active? visual)))
        (is (= 20 (:charge-ticks visual)))
        (is (= 0.5 (:charge-ratio visual)))))))

(deftest current-charging-visual-state-default-and-active-test
  (let [hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [current-charging-fx/current-state
                  (fn [_] {:active? false
                           :blending? false
                           :is-item false
                           :good? false
                           :charge-ticks 0
                           :charge-ratio 0.0})]
      (let [visual ((:client-visual-state hooks) :ac/current-charging {:player-uuid "p1"})]
        (is (false? (:active? visual)))
        (is (false? (:is-item visual)))
        (is (false? (:good? visual)))
        (is (= 0 (:charge-ticks visual)))
        (is (= 0.0 (:charge-ratio visual)))))
    (with-redefs [current-charging-fx/current-state
          (fn [_] {:active? true
             :blending? false
             :is-item true
             :good? true
             :charge-ticks 30
             :charge-ratio 0.75})]
      (let [visual ((:client-visual-state hooks) :ac/current-charging {:player-uuid "p1"})]
        (is (true? (:active? visual)))
        (is (true? (:is-item visual)))
        (is (true? (:good? visual)))
        (is (= 30 (:charge-ticks visual)))
        (is (= 0.75 (:charge-ratio visual)))))))

(deftest build-client-overlay-plan-falls-back-when-activated-override-nil-test
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (with-redefs [store/get-player-state (fn [_ _]
                                        {:resource-data {:activated true
                                                         :cur-cp 80.0
                                                         :max-cp 100.0
                                                         :cur-overload 0.0
                                                         :max-overload 100.0}
                                         :cooldown-data {}
                                         :preset-data {}})
                  client-keybinds/get-activate-hint (fn [_] nil)
                  client-keybinds/get-preset-switch-state (fn [_] nil)]
      (let [plan (client-ui-hooks/build-client-overlay-plan
                  "p1" 320 180 {:activated-override nil :now-ms 1000})]
        (is (seq (:elements plan))))))))

(deftest build-client-overlay-plan-renders-reflection-crosshair-and-vm-wave-test
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (with-redefs [store/get-player-state (fn [_ _]
                                        {:resource-data {:activated true
                                                         :cur-cp 80.0
                                                         :max-cp 100.0
                                                         :cur-overload 0.0
                                                         :max-overload 100.0}
                                         :cooldown-data {}
                                         :preset-data {}})
                  ctx/get-all-contexts (fn []
                                         {"ctx-reflection" {:player-uuid "p1"
                                                             :skill-state {:toggle {:vec-reflection {:active true}
                                                                                    :vec-deviation {:active true}}}}})
                  client-keybinds/get-activate-hint (fn [_] nil)
                  client-keybinds/get-preset-switch-state (fn [_] nil)]
      ;; Pre-seed a wave circle born 100ms ago so alpha>0 at now-ms=1000
      (client-ui-hooks/seed-vm-wave-state-for-test!
       "p1"
       [{:x 160.0 :y 90.0 :born-ms 900 :life-ms 600
         :start-size 10.0 :end-size 50.0 :seed 0.0}])
      (let [plan (client-ui-hooks/build-client-overlay-plan
                  "p1" 320 180 {:now-ms 1000})
            kinds (mapv :kind (:elements plan))]
        (is (= 1 (count (filter #{:content-crosshair} kinds))))
        (is (some #{:blit-texture} kinds)))))))

(deftest movement-key-hooks-route-to-flashing-channel-test
  (let [sent (atom [])
        hooks (client-ui-hooks/runtime-client-ui-hooks)]
    (with-redefs [client-keybinds/get-skill-id-for-slot-public (fn [_ _] :flashing)
                  ctx-mgr/activate-context! (fn [_owner _player-uuid _skill-id] {:id "ctx-flashing"})
                  ctx/get-context (fn [_owner ctx-id]
                                    (when (= ctx-id "ctx-flashing")
                                      {:id "ctx-flashing" :skill-id :flashing}))
                  ;; active-context-ids-for-skill scans the client context
                  ;; registry (not the slot map) for keep-active contexts.
                  ctx/get-all-contexts (fn []
                                         {"ctx-flashing" {:id "ctx-flashing"
                                                          :player-uuid "p1"
                                                          :skill-id :flashing
                                                          :status :alive}})
                  net-client/send-to-server (network-support/capture-send-to-server! sent)]
      ((:client-on-slot-key-down! hooks) "p1" 0)
      ((:client-on-movement-key-down! hooks) "p1" :forward)
      ((:client-on-movement-key-tick! hooks) "p1" :forward)
      ((:client-on-movement-key-up! hooks) "p1" :forward)
      (is (= [catalog/MSG-SLOT-KEY-DOWN
              catalog/MSG-CTX-CHANNEL
              catalog/MSG-CTX-CHANNEL
              catalog/MSG-CTX-CHANNEL]
             (mapv :msg-id @sent)))
      (is (= [{:ctx-id "ctx-flashing" :skill-id :flashing :key-idx 0}
              {:ctx-id "ctx-flashing" :channel :flashing/move-down :payload {:key :forward}}
              {:ctx-id "ctx-flashing" :channel :flashing/move-tick :payload {:key :forward}}
              {:ctx-id "ctx-flashing" :channel :flashing/move-up :payload {:key :forward}}]
             (mapv :payload @sent))))))

(deftest build-client-overlay-plan-renders-movement-hints-when-storm-wing-active-test
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session}
    (fn []
      (with-redefs [store/get-player-state (fn [_ _]
                                             {:resource-data {:activated true
                                                              :cur-cp 80.0
                                                              :cur-overload 0.0
                                                              :max-overload 100.0}
                                              :cooldown-data {}
                                              :preset-data {}})
                    ctx/get-all-contexts (fn [] {})
                    level-effects/effect-state-snapshot
                    (fn [effect-id]
                      (when (= :storm-wing effect-id)
                        {:effect-state {[:ctx "ctx-sw"] {:active? true
                                                         :phase :flying
                                                         :source-player-id "p1"}}}))]
        (let [plan (client-ui-hooks/build-client-overlay-plan
                    "p1" 320 180 {:now-ms 1000})]
          (is (some #{:movement-hints} (mapv :kind (:elements plan)))))))))

(deftest scan-vm-contexts-detects-server-toggle-via-real-registration-test
  (testing "a server context activated through the real toggle command path is
            visible to the client-side scan-vm-contexts in a shared
            (single-player) runtime — get-all-contexts projection carries the
            skill-state from the store, not from a stubbed context map"
    (let [owner {:logical-side :server :server-session-id :test-session :player-uuid "p1"}]
      (ps-fix/with-test-player-state-owner
        (fn []
          (store/reset-store!)
          (ps-fix/seed-player-state! "p1" {})
          (ctx/with-context-owner owner
            (ctx/register-context!
             (assoc (ctx/new-server-context "p1" :vec-deviation "ctx-dev" owner)
                    :status ctx/STATUS-ALIVE
                    :last-keepalive-ms 0))
            (toggle/activate-toggle! "ctx-dev" :vec-deviation)
            (is (true? (:deviation-active? (@#'client-ui-hooks/scan-vm-contexts "p1")))
                "client overlay scan sees the server toggle through store projection")
            (is (false? (:reflection-active? (@#'client-ui-hooks/scan-vm-contexts "p1")))
                "vec-reflection stays inactive")))))))

(deftest scan-vm-contexts-detects-fx-state-without-server-context-test
  (testing "server-mode (dedicated/remote) client: no server context is
            visible, but the fx-start channel already delivered active state —
            scan-vm-contexts must light the vm feedback from the fx signal"
    (with-redefs [ctx/get-all-contexts (fn [] {})  ;; no server contexts at all
                  level-effects/effect-state-snapshot
                  (fn [effect-id]
                    (case effect-id
                      :vec-deviation {:effect-state {[:ctx "ctx-dev"] {:active? true :ticks 3}}}
                      :vec-reflection {:effect-state {}}
                      {}))]
      (is (true? (:deviation-active? (@#'client-ui-hooks/scan-vm-contexts "p1")))
          "deviation detected purely from fx state")
      (is (false? (:reflection-active? (@#'client-ui-hooks/scan-vm-contexts "p1")))
          "reflection stays inactive without fx state"))))

(deftest overlay-plan-renders-vm-wave-from-fx-state-alone-test
  (testing "server-mode client: build-client-overlay-plan renders the vm wave
            when the fx-start channel delivered active state, even though the
            context registry holds nothing"
    (runtime-hooks/with-client-ctx-fn {:session-id :test-session}
      (fn []
        (with-redefs [store/get-player-state
                      (fn [_ _]
                        {:resource-data {:activated true
                                         :cur-cp 80.0
                                         :cur-overload 0.0
                                         :max-overload 100.0}
                         :cooldown-data {}
                         :preset-data {}})
                      ctx/get-all-contexts (fn [] {})
                      level-effects/effect-state-snapshot
                      (fn [effect-id]
                        (case effect-id
                          :vec-deviation {:effect-state {[:ctx "ctx-dev"] {:active? true :ticks 3}}}
                          {}))
                      client-keybinds/get-activate-hint (fn [_] nil)
                      client-keybinds/get-preset-switch-state (fn [_] nil)]
          (client-ui-hooks/seed-vm-wave-state-for-test!
           "p1"
           [{:x 160.0 :y 90.0 :born-ms 900 :life-ms 600
             :start-size 10.0 :end-size 50.0 :seed 0.0}])
          (let [plan (client-ui-hooks/build-client-overlay-plan
                      "p1" 320 180 {:now-ms 1000})]
            (is (some #{:blit-texture} (mapv :kind (:elements plan)))
                "vm wave blit renders from fx state alone")))))))

(deftest fx-start-channel-writes-active-state-and-scan-detects-test
  (testing "the real fx message flow — server fx/send! → client channel push →
            fx-registry → vec-deviation level effect-state — writes :active?,
            which scan-vm-contexts reads for the HUD feedback signal"
    (vdfx/init!)
    ;; on-context-channel-push! (private) forwards the channel to
    ;; fx-registry/dispatch-fx-channel! — drive that public dispatch point
    ;; directly, same as the client message handler does.
    (fx-registry/dispatch-fx-channel!
     "ctx-dev" :vec-deviation/fx-start {:mode :start})
    (is (true? (reactive-hud/fx-effect-active? :vec-deviation))
        "effect-state carries the active entry after the fx-start push")
    (is (true? (:deviation-active? (@#'client-ui-hooks/scan-vm-contexts "p1")))
        "scan sees deviation active from the fx state alone")
    ;; fx-end clears it again — the toggle off message ends the feedback
    (fx-registry/dispatch-fx-channel!
     "ctx-dev" :vec-deviation/fx-end {:mode :end})
    (is (false? (reactive-hud/fx-effect-active? :vec-deviation))
        "fx-end clears the active entry")
    (is (false? (:deviation-active? (@#'client-ui-hooks/scan-vm-contexts "p1")))
        "scan returns to inactive after fx-end")))
