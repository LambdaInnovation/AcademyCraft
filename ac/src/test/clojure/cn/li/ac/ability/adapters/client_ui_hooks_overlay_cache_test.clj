(ns cn.li.ac.ability.adapters.client-ui-hooks-overlay-cache-test
  "Covers the overlay-plan reactive cache (Cache A: skill-slot shape keyed on
   preset-data identity; Cache B: context-derived data keyed on a context
   snapshot token) added to build-client-overlay-plan."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.adapters.client-ui-hooks :as client-ui-hooks]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.client.keybinds :as client-keybinds]
            [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.content.ability.electromaster.current-charging-fx :as current-charging-fx]
            [cn.li.ac.client.toast :as toast]
            [cn.li.ac.tutorial.client.notification :as tutorial-notification]
            [cn.li.ac.ability.client.debug-overlay :as debug-overlay]
            [cn.li.ac.test.support.player-state :as player-state-support]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

;; The overlay-plan cache lives inside the Framework atom (client-ui-hooks'
;; owner-keyed runtime state, see client_ui_hooks.clj's `client-ui-runtime-state-atom`),
;; which only persists across calls when framework is initialized — hence
;; clean-player-states-fixture (not just reset-client-ui-state-for-test!) is required
;; here, unlike simpler tests that only call build-client-overlay-plan once.
(defn- reset-ui-state! [f]
  (player-state-support/clean-player-states-fixture
    (fn []
      (client-ui-hooks/reset-client-ui-state-for-test!)
      (f)
      (client-ui-hooks/reset-client-ui-state-for-test!))))

(use-fixtures :each reset-ui-state!)

(def ^:private test-client-session :test-client-session-overlay-cache)

(defn- preset-data-with-slot [ctrl-id]
  {:active-preset 0 :slots {[0 0] [:electromaster ctrl-id]}})

(defn- player-state-with [preset-data cooldown-data]
  {:resource-data {:activated true :cur-cp 80.0 :max-cp 100.0
                   :cur-overload 0.0 :max-overload 100.0 :overload-fine true}
   :ability-data {:category-id :electromaster}
   :cooldown-data cooldown-data
   :preset-data preset-data})

(defmacro with-overlay-stubs
  "Common stubs shared by all cache tests, plus caller-supplied extra bindings
   (a with-redefs bindings vector) layered on top."
  [extra-bindings & body]
  `(with-redefs [client-keybinds/get-activate-hint (fn [_#] nil)
                 client-keybinds/get-preset-switch-state (fn [_#] nil)
                 current-charging-fx/current-state (fn [& _#] {:active? false :blending? false
                                                                :is-item false :good? false
                                                                :charge-ticks 0 :charge-ratio 0.0})
                 ctx/get-all-contexts (fn [] {})
                 ;; charge-coin-visual-state independently reads player-contexts (railgun
                 ;; coin-QTE feature, unrelated to and untouched by the overlay-plan cache
                 ;; under test here) — neutralize it so read-model/get-player-contexts-for-player
                 ;; counters below measure only what Cache B is responsible for.
                 client-ui-hooks/charge-coin-visual-state (fn [_a# _b#] {:active? false :coin-progress 0.0})
                 ;; toast/tutorial-notification/debug-overlay each lazily initialize their own
                 ;; runtime atom under the SAME Framework path prefix ([:service :client-ui ...])
                 ;; that client-ui-hooks' own runtime state atom occupies as a leaf value —
                 ;; a pre-existing path collision (ClassCastException) unrelated to the overlay
                 ;; cache under test, triggered only once framework is actually live across
                 ;; calls (see clean-player-states-fixture above). Stub these away rather than
                 ;; fix that unrelated bug here — matches the plan's explicit scope boundary.
                 toast/active-toasts-snapshot (fn [] [])
                 tutorial-notification/active-snapshot (fn [] [])
                 debug-overlay/current-state (fn [] :none)
                 ~@extra-bindings]
     ~@body))

(deftest skill-shape-cache-hits-when-preset-data-unchanged-test
  (let [shape-calls (atom 0)
        preset-data (preset-data-with-slot :railgun)
        state {:resource-data {:activated true :cur-cp 80.0 :max-cp 100.0
                               :cur-overload 0.0 :max-overload 100.0 :overload-fine true}
               :ability-data {:category-id :electromaster}
               :cooldown-data {}
               :preset-data preset-data}]
    (with-overlay-stubs
      [store/get-player-state (fn [_ _] state)
       category/get-category (fn [_] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_ _] (swap! shape-calls inc) :railgun)
       skill-registry/get-skill (fn [_] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_] "textures/skills/railgun.png")
       read-model/get-player-contexts-for-player (fn [& _] [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-client-session} (fn [] (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1000})
        (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1050})
        (is (= 1 @shape-calls)
            "skill-slot shape registry lookups should run once, not once per frame"))))))

(deftest skill-shape-cache-invalidates-on-preset-rebind-test
  (let [shape-calls (atom 0)
        preset-a (atom (preset-data-with-slot :railgun))]
    (with-overlay-stubs
      [store/get-player-state (fn [_ _] (player-state-with @preset-a {}))
       category/get-category (fn [_] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_ ctrl-id] (swap! shape-calls inc) ctrl-id)
       skill-registry/get-skill (fn [_] {:name "Skill"})
       skill-query/get-skill-icon-path (fn [_] "textures/skills/x.png")
       read-model/get-player-contexts-for-player (fn [& _] [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-client-session} (fn [] (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1000})
        (reset! preset-a (preset-data-with-slot :body-intensify))
        (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1050})
        (is (= 2 @shape-calls)
            "rebinding a skill slot (new preset-data) must rebuild the cached shape"))))))

(deftest context-cache-hits-and-shared-between-consumption-hint-and-visual-test
  (let [context-calls (atom 0)
        preset-data (preset-data-with-slot :railgun)]
    (with-overlay-stubs
      [store/get-player-state (fn [_ _] (player-state-with preset-data {}))
       category/get-category (fn [_] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_ _] :railgun)
       skill-registry/get-skill (fn [_] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_] "textures/skills/railgun.png")
       ctx/contexts-version-token (fn [] ::stable-token)
       read-model/get-player-contexts-for-player (fn [& _] (swap! context-calls inc) [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-client-session} (fn [] (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1000})
        (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1050})
        (is (= 1 @context-calls)
            "context scan must run once per real context change, not once per frame,
             and must be shared between delegate-state and consumption-hint"))))))

(deftest context-cache-invalidates-on-context-change-test
  (let [context-calls (atom 0)
        token (atom :token-a)
        preset-data (preset-data-with-slot :railgun)]
    (with-overlay-stubs
      [store/get-player-state (fn [_ _] (player-state-with preset-data {}))
       category/get-category (fn [_] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_ _] :railgun)
       skill-registry/get-skill (fn [_] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_] "textures/skills/railgun.png")
       ctx/contexts-version-token (fn [] @token)
       read-model/get-player-contexts-for-player (fn [& _] (swap! context-calls inc) [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-client-session} (fn [] (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1000})
        (reset! token :token-b)
        (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1050})
        (is (= 2 @context-calls)
            "a context-registry change must invalidate the cached context data"))))))

(deftest cooldown-refreshes-every-frame-without-invalidating-either-cache-test
  (let [shape-calls (atom 0)
        context-calls (atom 0)
        cooldown (atom {})
        preset-data (preset-data-with-slot :railgun)]
    (with-overlay-stubs
      [store/get-player-state (fn [_ _] (player-state-with preset-data @cooldown))
       category/get-category (fn [_] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_ _] (swap! shape-calls inc) :railgun)
       skill-registry/get-skill (fn [_] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_] "textures/skills/railgun.png")
       ctx/contexts-version-token (fn [] ::stable-token)
       read-model/get-player-contexts-for-player (fn [& _] (swap! context-calls inc) [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-client-session} (fn [] (let [plan-1 (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1000})
              slot-1 (first (filter #(= :content-slot (:kind %)) (:elements plan-1)))]
          (is (= 0 (:timer-remaining slot-1))))
        (reset! cooldown {[:railgun :main] {:ticks 40 :max 60}})
        (let [plan-2 (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1050})
              slot-2 (first (filter #(= :content-slot (:kind %)) (:elements plan-2)))]
          (is (= 40 (:timer-remaining slot-2))
              "cooldown numeric fields must refresh every frame"))
        (is (= 1 @shape-calls) "cooldown ticking must not invalidate Cache A")
        (is (= 1 @context-calls) "cooldown ticking must not invalidate Cache B"))))))

(deftest overlay-caches-cleared-on-client-ui-state-reset-test
  (let [shape-calls (atom 0)
        preset-data (preset-data-with-slot :railgun)]
    (with-overlay-stubs
      [store/get-player-state (fn [_ _] (player-state-with preset-data {}))
       category/get-category (fn [_] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_ _] (swap! shape-calls inc) :railgun)
       skill-registry/get-skill (fn [_] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_] "textures/skills/railgun.png")
       read-model/get-player-contexts-for-player (fn [& _] [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-client-session} (fn [] (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1000})
        (client-ui-hooks/reset-client-ui-state-for-test!)
        (client-ui-hooks/build-client-overlay-plan "p1" 320 180 {:now-ms 1050})
        (is (= 2 @shape-calls)
            "resetting client-ui state must clear the cache, not leak across sessions"))))))
