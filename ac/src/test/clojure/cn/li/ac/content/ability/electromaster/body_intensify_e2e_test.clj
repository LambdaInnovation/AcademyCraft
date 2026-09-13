(ns cn.li.ac.content.ability.electromaster.body-intensify-e2e-test
  "Whole server-side lifecycle for body-intensify, driven through the real
  per-tick pipeline (context-manager/tick-player-contexts!) instead of calling
  the skill's :tick! action directly.

  Regression guard: :input-state is authoritative only in the runtime-store
  projection, so a tick driver that filters raw transport contexts sees every
  context as :idle, never ticks it, and the charge stays at 0 — the release
  then silently applies no potions and reports performed? false."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.effects.potion :as potion-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.model.resource :as rd]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as cm]
            [cn.li.ac.ability.service.context-state :as ctx-rt]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.content.ability.electromaster.body-intensify :as bi]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(def ^:private player-id "e2e-player")
(def ^:private ctx-id "e2e-ctx")

;; Level-3-ish pools: body-intensify's 200 down-stage overload must fit under
;; max-overload, otherwise the cap aborts the context (as it does upstream,
;; where OverloadEvent disposes the player's contexts).
(def ^:private max-cp 12000.0)
(def ^:private max-overload 320.0)

(defn- server-owner []
  {:logical-side :server
   :server-session-id test-player/test-session-id
   :player-uuid player-id})

(defn- reset-fixture [f]
  (test-contexts/clean-contexts-fixture
   #(test-player/clean-player-states-fixture
     (fn []
       (skill-registry/install-skill-registry-runtime!
        (skill-registry/create-skill-registry-runtime))
       (evt/install-event-subscriber-runtime!
        (evt/create-event-subscriber-runtime))
       (cm/register-send-fns! {:to-client nil :to-server nil})
       (skill-registry/register-skill! bi/body-intensify)
       (f)))))

(use-fixtures :each reset-fixture)

(defn- seed-player! []
  (store/set-player-state!
   test-player/test-session-id player-id
   {:ability-data (-> (ad/new-ability-data)
                      (ad/learn-skill :body-intensify))
    :resource-data (assoc (rd/new-resource-data max-cp max-overload)
                          :activated true)}))

(defn- seed-alive-context!
  "`created-ms` back-dates the transport-level keepalive stamp, which is what a
  context that has already been held for a while looks like."
  ([] (seed-alive-context! (System/currentTimeMillis)))
  ([created-ms]
   (ctx/register-context!
    (assoc (ctx/new-server-context player-id :body-intensify ctx-id (server-owner))
           :status ctx/STATUS-ALIVE
           :last-keepalive-ms created-ms))
   (command-rt/run-command-in-session!
    test-player/test-session-id player-id
    {:command :register-context :ctx-id ctx-id
     :skill-id :body-intensify :status :alive})))

(defn- server-tick! []
  (runtime-hooks/with-client-ctx-fn
    {:player-owner {:server-session-id test-player/test-session-id
                    :player-uuid player-id}}
    (fn [] (cm/tick-player-contexts! player-id))))

(defn- client-keepalive! []
  (command-rt/run-command-in-session!
   test-player/test-session-id player-id
   {:command :touch-context-keepalive :ctx-id ctx-id
    :timestamp-ms (System/currentTimeMillis)}))

(defn- reaped-server-tick!
  "A server tick with the context-manager's keepalive reaper running too, as
  it does in game, and with the client keepalive the held key keeps sending."
  []
  (client-keepalive!)
  (cm/tick-context-manager!)
  (server-tick!))

(defn- current-cp []
  (get-in (store/get-player-state test-player/test-session-id player-id)
          [:resource-data :cur-cp]))

(deftest server-tick-drives-charge-through-to-a-performed-release-test
  (let [applied* (atom [])
        fx* (atom [])]
    (runtime-hooks/with-player-state-owner-fn
      {:server-session-id test-player/test-session-id :player-uuid player-id}
      (fn []
        (seed-player!)
        (with-redefs [potion-effects/available? (constantly true)
                      potion-effects/apply-effect!
                      (fn [_player effect _duration _amplifier]
                        (swap! applied* conj effect)
                        true)
                      fx/send! (fn [_ctx-id entry _evt payload]
                                 (swap! fx* conj [(:topic entry) (:performed? payload)])
                                 nil)]
          (ctx/with-context-owner (server-owner)
            (seed-alive-context!)
            (ctx-rt/handle-key-down! (server-owner) ctx-id {} nil)
            (dotimes [_ 20] (server-tick!))

            (is (= 20 (get-in (ctx-skill/get-context ctx-id) [:skill-state :hold-ticks]))
                "every server tick advances the charge")
            (is (= (- max-cp (* 20 20.0)) (current-cp))
                "each charged tick pays its CP cost once")

            (ctx-rt/handle-key-up! (server-owner) ctx-id {} nil)
            (is (some #{:hunger} @applied*)
                "a released charge past min-ticks always applies the hunger buff")
            (is (= [[:body-intensify/fx-start nil]
                    [:body-intensify/fx-end true]
                    [:body-intensify/fx-end true]]
                   @fx*)
                "release fans out performed? true to the owner and nearby players")))))))

(deftest charge-outliving-the-keepalive-timeout-still-performs-test
  ;; The useful hold runs to max-ticks (40 ticks = 2s), well past the 1.5s
  ;; keepalive tolerance. The reaper judged expiry from the transport map's
  ;; :last-keepalive-ms — stamped once at creation, never refreshed — so it
  ;; killed the context around tick 30 and wiped :skill-state. The key-up that
  ;; followed hit a dead context: no buffs, no release FX, no reaction at all.
  (let [applied* (atom [])
        fx* (atom [])]
    (runtime-hooks/with-player-state-owner-fn
      {:server-session-id test-player/test-session-id :player-uuid player-id}
      (fn []
        (seed-player!)
        (with-redefs [potion-effects/available? (constantly true)
                      potion-effects/apply-effect!
                      (fn [_player effect _duration _amplifier]
                        (swap! applied* conj effect)
                        true)
                      fx/send! (fn [_ctx-id entry _evt payload]
                                 (swap! fx* conj [(:topic entry) (:performed? payload)])
                                 nil)]
          (ctx/with-context-owner (server-owner)
            (seed-alive-context! (- (System/currentTimeMillis)
                                    (* 4 (cm/keepalive-timeout-ms))))
            (ctx-rt/handle-key-down! (server-owner) ctx-id {} nil)
            (dotimes [_ 35] (reaped-server-tick!))

            (is (= ctx/STATUS-ALIVE (:status (ctx/get-context (server-owner) ctx-id)))
                "a keepalive-refreshed context must survive a hold longer than the timeout")
            (is (= 35 (get-in (ctx-skill/get-context ctx-id) [:skill-state :hold-ticks])))

            (ctx-rt/handle-key-up! (server-owner) ctx-id {} nil)
            (is (some #{:hunger} @applied*)
                "the release must still apply its buffs")
            (is (some #{[:body-intensify/fx-end true]} @fx*)
                "and still report performed? to the client")))))))

(deftest release-below-min-charge-performs-nothing-test
  (let [applied* (atom [])
        fx* (atom [])]
    (runtime-hooks/with-player-state-owner-fn
      {:server-session-id test-player/test-session-id :player-uuid player-id}
      (fn []
        (seed-player!)
        (with-redefs [potion-effects/available? (constantly true)
                      potion-effects/apply-effect!
                      (fn [& _] (swap! applied* conj :applied) true)
                      fx/send! (fn [_ctx-id entry _evt payload]
                                 (swap! fx* conj [(:topic entry) (:performed? payload)])
                                 nil)]
          (ctx/with-context-owner (server-owner)
            (seed-alive-context!)
            (ctx-rt/handle-key-down! (server-owner) ctx-id {} nil)
            (dotimes [_ 5] (server-tick!))
            (ctx-rt/handle-key-up! (server-owner) ctx-id {} nil)
            (is (empty? @applied*))
            (is (= [[:body-intensify/fx-start nil]
                    [:body-intensify/fx-end false]
                    [:body-intensify/fx-end false]]
                   @fx*))))))))
