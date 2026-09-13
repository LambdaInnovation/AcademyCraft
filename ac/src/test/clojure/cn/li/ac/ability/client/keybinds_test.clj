(ns cn.li.ac.ability.client.keybinds-test
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.api :as client-api]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.client.keybinds :as keybinds]            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.ac.ability.client.input-state-machine :as input-state-machine]))

(defn- reset-fixture [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (keybinds/reset-client-keybind-state-for-test!)
      (keybinds/reset-keybind-registries-for-test!)
      (store/reset-store!)
      (try
        (f)
        (finally
          (keybinds/reset-client-keybind-state-for-test!)
          (keybinds/reset-keybind-registries-for-test!)
          (store/reset-store!))))))

(use-fixtures :each reset-fixture)

(defn- activated-state []
  (assoc-in (store/fresh-player-state) [:resource-data :activated] true))

(deftest key-state-isolated-by-client-owner-test
  (let [opened (atom [])]
    (with-redefs [client-bridge/open-screen! (fn [screen-key payload]
                                               (swap! opened conj [screen-key payload]))]
      (binding [keybinds/*client-session-id* :session-a
                keybinds/*get-player-uuid-fn* (constantly "player-a")]
        (keybinds/on-gui-key-event :skill-tree true))
      (binding [keybinds/*client-session-id* :session-a
                keybinds/*get-player-uuid-fn* (constantly "player-b")]
        (keybinds/on-gui-key-event :preset-editor true)))
    (is (= [[:ac/skill-tree {:player-uuid "player-a"}]
            [:ac/preset-editor {:player-uuid "player-b"}]]
           @opened))
    (is (= true (get-in (keybinds/key-state-snapshot {:client-session-id :session-a
                                                       :player-uuid "player-a"})
                        [:gui-keys :skill-tree])))
    (is (= false (get-in (keybinds/key-state-snapshot {:client-session-id :session-a
                                                        :player-uuid "player-a"})
                         [:gui-keys :preset-editor])))
    (is (= true (get-in (keybinds/key-state-snapshot {:client-session-id :session-a
                                                       :player-uuid "player-b"})
                        [:gui-keys :preset-editor])))))

(deftest clear-client-keybind-state-clears-only-owner-test
  (with-redefs [client-bridge/open-screen! (fn [_ _] nil)]
    (binding [keybinds/*client-session-id* :session-a
              keybinds/*get-player-uuid-fn* (constantly "player-a")]
      (keybinds/on-gui-key-event :skill-tree true))
    (binding [keybinds/*client-session-id* :session-a
              keybinds/*get-player-uuid-fn* (constantly "player-b")]
      (keybinds/on-gui-key-event :skill-tree true)))
  (keybinds/clear-client-keybind-state! {:client-session-id :session-a
                                          :player-uuid "player-a"})
  (is (= false (get-in (keybinds/key-state-snapshot {:client-session-id :session-a
                                                     :player-uuid "player-a"})
                       [:gui-keys :skill-tree])))
  (is (= true (get-in (keybinds/key-state-snapshot {:client-session-id :session-a
                                                    :player-uuid "player-b"})
                      [:gui-keys :skill-tree]))))

(deftest client-keybind-owner-requires-explicit-session-and-player-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Client keybind owner requires :client-session-id"
                        (keybinds/key-state-snapshot "player-a")))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Client keybind owner requires :player-uuid"
                        (keybinds/key-state-snapshot {:client-session-id :session-a}))))

(deftest preset-switch-state-isolated-by-player-test
  (store/reset-store!)
  (store/set-player-state! :session-a "player-a" (activated-state))
  (store/set-player-state! :session-a "player-b" (activated-state))
  (let [requests (atom [])]
    (with-redefs [client-bridge/game-time-ms (constantly 0)   ;; no platform bridge in tests
                  client-api/req-switch-preset! (fn [_owner preset-idx callback]
                                                  (swap! requests conj preset-idx)
                                                  (when callback (callback {:success true})))]
      (binding [keybinds/*client-session-id* :session-a]
        (keybinds/switch-preset! "player-a")
        (keybinds/switch-preset! "player-a")
        (keybinds/switch-preset! "player-b"))))
  (is (= 2 (:current-preset (keybinds/get-preset-switch-state {:client-session-id :session-a
                                                               :player-uuid "player-a"}))))
  (is (= 1 (:current-preset (keybinds/get-preset-switch-state {:client-session-id :session-a
                                                               :player-uuid "player-b"})))))

(deftest activate-handler-registry-policy-test
  (let [handler-a {:id :test/handler
                   :priority 10
                   :handles-fn (fn [_] true)
                   :on-key-down-fn (fn [_] nil)}
        handler-b (assoc handler-a :priority 20)]
    (keybinds/add-activate-handler! handler-a)
    (keybinds/add-activate-handler! (assoc handler-a :on-key-down-fn (fn [_] :new-function)))
    (is (= 1 (count (:activate-handlers (keybinds/keybind-registries-snapshot)))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Conflicting activate handler id"
                          (keybinds/add-activate-handler! handler-b)))
    (keybinds/freeze-keybind-registries!)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Keybind registries are frozen"
                          (keybinds/add-activate-handler!
                            (assoc handler-a :id :test/after-freeze))))
    ;; key delegates are rebound by runtime preset syncs, so they stay writable
    ;; after the registration phase is frozen.
    (is (nil? (keybinds/register-key-delegate! :default 0 {:skill-id :railgun})))))

(deftest default-abort-handler-uses-client-abort-hook-test
  (let [aborted (atom [])]
    (keybinds/install-default-handlers!)
    (binding [keybinds/*client-session-id* :session-a
              keybinds/*get-player-uuid-fn* (constantly "p1")]
      (keybinds/on-skill-key-event 0 true))
    (with-redefs [read-model/get-player-state (fn [& _] {:ability-data {:category-id :test-cat}})
                  runtime-hooks/client-abort-all! (fn [] (swap! aborted conj :abort-hook))
                  runtime-hooks/set-client-overlay-activated! (fn [_ _] nil)
                  client-api/req-set-activated! (fn [& _] nil)
                  ctx/abort-all-contexts-for-player! (fn [& _]
                                                       (throw (ex-info "legacy abort path should not be used" {})))]
      (binding [keybinds/*client-session-id* :session-a
                keybinds/*get-player-uuid-fn* (constantly "p1")]
        (keybinds/trigger-mode-switch! "p1")))
    (is (= [:abort-hook] @aborted))))

(deftest no-active-delegate-does-not-block-toggle-test
  (let [aborted (atom [])
        toggled (atom [])
        overlay (atom [])]
    (keybinds/install-default-handlers!)
    (with-redefs [read-model/get-player-state (fn [& _]
                                                {:ability-data {:category-id :test-cat}
                                                 :resource-data {:activated true}})
                  runtime-hooks/client-abort-all! (fn [] (swap! aborted conj :abort-hook))
                  client-api/req-set-activated! (fn [_owner activated _callback]
                                                 (swap! toggled conj activated)
                                                 nil)
                  runtime-hooks/set-client-overlay-activated! (fn [player-uuid activated]
                                                                (swap! overlay conj [player-uuid activated]))]
      (binding [keybinds/*client-session-id* :session-a]
        (keybinds/trigger-mode-switch! "p1")))
    (is (empty? @aborted))
    (is (= [false] @toggled))
    (is (= [["p1" false]] @overlay))))

(deftest shared-v-toggle-helper-emits-on-short-release-and-suppresses-long-hold-test
  (let [state (atom (input-state-machine/initial-button-state))
        emitted (atom [])]
    (input-state-machine/handle-button-state! state true
      {:now-ns 0
       :on-down (fn [] nil)})
    (input-state-machine/handle-button-state! state false
      {:now-ns (* 200 1000 1000)
       :on-short-up (fn [] (swap! emitted conj :short-up))})
    (is (= [:short-up] @emitted)))
  (let [state (atom (input-state-machine/initial-button-state))
        emitted (atom [])]
    (input-state-machine/handle-button-state! state true
      {:now-ns 0
       :on-down (fn [] nil)})
    (input-state-machine/handle-button-state! state false
      {:now-ns (* 500 1000 1000)
       :on-short-up (fn [] (swap! emitted conj :short-up))})
    (is (empty? @emitted))))

(defn- register-recording-skill-delegate!
  "Register a skill delegate on slot 0 that records every key event."
  [events]
  (keybinds/register-key-delegate!
    :default 0
    {:skill-id     :railgun
     :on-key-down  (fn [uuid] (swap! events conj [:down uuid]))
     :on-key-tick  (fn [uuid] (swap! events conj [:tick uuid]))
     :on-key-up    (fn [uuid] (swap! events conj [:up uuid]))
     :on-key-abort (fn [uuid] (swap! events conj [:abort uuid]))}))

(deftest screen-open-close-click-never-fires-bound-skill-test
  ;; Reported bug: in ability mode with LMB bound to a skill, pressing Esc and
  ;; clicking "Back to Game" fired the skill once. The pause menu closes on the
  ;; mouse press while the button is still physically held; the next in-game
  ;; tick then saw a "fresh" press. Upstream instead polls the raw physical
  ;; state every tick and gates only dispatch on ClientUtils.isPlayerInGame(),
  ;; so the press that happened under the Screen is absorbed.
  (store/set-player-state! :session-a "player-a" (activated-state))
  (let [events (atom [])]
    (register-recording-skill-delegate! events)
    (binding [keybinds/*client-session-id* :session-a
              keybinds/*get-player-uuid-fn* (constantly "player-a")]
      ;; In game, LMB up.
      (keybinds/on-skill-key-event 0 false)
      ;; Esc → pause menu opens (first suppressed tick, nothing held).
      (keybinds/on-skill-key-event 0 false true true)
      ;; Click "Back to Game": LMB pressed while the menu is open — the press
      ;; is tracked but must not dispatch.
      (keybinds/on-skill-key-event 0 true true false)
      ;; Menu closes while LMB is still physically held (just-closed tick) —
      ;; the hold is absorbed, no fresh press edge, no tick.
      (keybinds/on-skill-key-event 0 true false false true)
      ;; LMB released.
      (keybinds/on-skill-key-event 0 false false false))
    (is (not-any? (fn [[event _]] (= event :down)) @events)
        "the menu-close click must never reach the skill's on-key-down")
    (is (= [[:up "player-a"]] @events)
        "the absorbed hold only produces a no-op release against the dead slot")))

(deftest screen-open-aborts-held-skill-once-and-absorbs-the-hold-test
  ;; Upstream ClientRuntime: `state.state && shouldAbort → onKeyAbort` — a
  ;; skill key held when a Screen opens is aborted once; the ongoing physical
  ;; hold is absorbed (realState keeps tracking), so it cannot restart the
  ;; skill; a fresh press after release works normally.
  (store/set-player-state! :session-a "player-a" (activated-state))
  (let [events (atom [])]
    (register-recording-skill-delegate! events)
    (binding [keybinds/*client-session-id* :session-a
              keybinds/*get-player-uuid-fn* (constantly "player-a")]
      ;; Hold LMB in game — skill starts.
      (keybinds/on-skill-key-event 0 true false false)
      ;; Pause menu opens while LMB held → one abort (upstream onKeyAbort).
      (keybinds/on-skill-key-event 0 true true true)
      ;; Still held under the menu — nothing more fires.
      (keybinds/on-skill-key-event 0 true true false)
      ;; Released while the menu is open — suppressed but tracked.
      (keybinds/on-skill-key-event 0 false true false)
      ;; Menu closes with LMB up.
      (keybinds/on-skill-key-event 0 false false false)
      ;; Fresh press in game works normally.
      (keybinds/on-skill-key-event 0 true false false)
      (keybinds/on-skill-key-event 0 false false false))
    (is (= [[:down "player-a"] [:abort "player-a"]
            [:down "player-a"] [:up "player-a"]]
           @events))))

(deftest tick-keys-screen-open-boundary-uses-physical-state-test
  ;; End-to-end through tick-keys!: the loader passes glfw-key-state-fn always
  ;; plus screen-open?; the boundary latch must fire the abort exactly once and
  ;; absorb the close-while-held press.
  (store/set-player-state! :session-a "player-a" (activated-state))
  (let [events  (atom [])
        physical (atom {[:slot 0] false})]
    (register-recording-skill-delegate! events)
    (let [key-state-fn (fn [[kind sub-key]]
                         (case kind
                           :slot   (get @physical [:slot sub-key] false)
                           :movement false
                           :screen   false
                           :raw     false))]
      (binding [keybinds/*client-session-id* :session-a
                keybinds/*get-player-uuid-fn* (constantly "player-a")]
        ;; In game, LMB up.
        (keybinds/tick-keys! key-state-fn false)
        ;; Pause menu opens; player clicks "Back to Game" (LMB down under the
        ;; menu) — press tracked, not dispatched.
        (keybinds/tick-keys! key-state-fn true)
        (reset! physical {[:slot 0] true})
        (keybinds/tick-keys! key-state-fn true)
        ;; Menu closes while LMB is still physically held.
        (keybinds/tick-keys! key-state-fn false)
        ;; LMB released.
        (reset! physical {[:slot 0] false})
        (keybinds/tick-keys! key-state-fn false))
      (is (not-any? (fn [[event _]] (= event :down)) @events)
          "menu-close click never fires the skill through tick-keys!")
      (is (not-any? (fn [[event _]] (= event :abort)) @events)
          "nothing was held when the screen opened — no abort")
      (is (= [[:up "player-a"]] @events)
          "the press under the menu is absorbed; only the trailing release no-ops"))))

(deftest same-frame-menu-close-click-never-fires-skill-test
  ;; The actual bug path: the menu button closes the Screen on mouse-down, in
  ;; the SAME frame's GLFW event poll — the END-tick sample after the close
  ;; never saw the press under the Screen, so the just-closed tick must absorb
  ;; any key that is physically down (it is by definition a click-hold).
  (store/set-player-state! :session-a "player-a" (activated-state))
  (let [events (atom [])]
    (register-recording-skill-delegate! events)
    (binding [keybinds/*client-session-id* :session-a
              keybinds/*get-player-uuid-fn* (constantly "player-a")]
      ;; In game, LMB up.
      (keybinds/on-skill-key-event 0 false)
      ;; Pause menu open for a couple of ticks, LMB up.
      (keybinds/on-skill-key-event 0 false true true false)
      (keybinds/on-skill-key-event 0 false true false false)
      ;; Click "Back to Game": press + menu close land in the same frame, so
      ;; the first sample after the close already has screen-open? = false and
      ;; LMB physically down (just-closed tick).
      (keybinds/on-skill-key-event 0 true false false true)
      ;; LMB released in-game.
      (keybinds/on-skill-key-event 0 false false false false))
    (is (not-any? (fn [[event _]] (= event :down)) @events)
        "the click-hold absorbed on the just-closed tick must never reach on-key-down")
    (is (= [[:up "player-a"]] @events)
        "only the trailing release no-ops against the dead slot")))

(deftest vanilla-override-key-codes-follow-upstream-control-overrider-test
  (binding [keybinds/*client-session-id* :session-a
            keybinds/*get-player-uuid-fn* (constantly "p1")]
    (with-redefs [read-model/get-player-state (fn [& _] {:resource-data {:activated false}})]
      (is (= [] (keybinds/vanilla-override-key-codes "p1"))))
    (with-redefs [read-model/get-player-state (fn [& _] {:resource-data {:activated true}})]
      (is (= [] (keybinds/vanilla-override-key-codes "p1"))
          "activated with empty preset must not override vanilla keys"))
    (keybinds/register-key-delegate! :default 0 {:skill-id :railgun})
    (keybinds/register-key-delegate! :default 1 {:skill-id :arc-gen})
    (with-redefs [read-model/get-player-state (fn [& _] {:resource-data {:activated true}})]
      (is (= [-100 -99] (keybinds/vanilla-override-key-codes "p1"))
          "delegates on default LMB/RMB slots suppress vanilla attack/use"))
    (with-redefs [read-model/get-player-state (fn [& _] {:resource-data {:activated false}})]
      (is (= [] (keybinds/vanilla-override-key-codes "p1"))
          "deactivated clears overrides even if delegates remain"))))

