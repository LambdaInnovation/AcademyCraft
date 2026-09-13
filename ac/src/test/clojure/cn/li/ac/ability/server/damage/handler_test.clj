(ns cn.li.ac.ability.server.damage.handler-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.server.damage.handler :as h]
            [cn.li.ac.ability.server.damage.runtime :as damage-runtime]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.player-state :as ps-fix]))

(defn- reset-registries! [f]
  (h/reset-attack-check-registries-for-test!)
  (try
    (f)
    (finally
      (h/reset-attack-check-registries-for-test!))))

(use-fixtures :each reset-registries!)

(deftest attack-cancel-check-any-true-test
  (let [source-seen (atom nil)]
    (h/register-attack-cancel-check! :false-1 (fn [_ _ _ damage-source]
                                                (reset! source-seen damage-source)
                                                false))
    (h/register-attack-cancel-check! :true-1 (fn [_ _ _ _] true))
    (is (true? (h/should-cancel-attack? "p" "a" 5.0 :src)))
    (is (= :src @source-seen))))

(deftest attack-cancel-check-exception-isolated-test
  (h/register-attack-cancel-check! :boom (fn [_ _ _ _] (throw (Exception. "boom"))))
  (h/register-attack-cancel-check! :true-1 (fn [_ _ _ _] true))
  (is (true? (h/should-cancel-attack? "p" "a" 5.0 :src))))

(deftest run-attack-precheck-side-effects-success-test
  (let [calls (atom [])]
    (h/register-attack-precheck-side-effect!
      :fx
      (fn [player-id attacker-id damage damage-source]
        (swap! calls conj [player-id attacker-id damage damage-source])
        :ok))
    (is (true? (h/run-attack-precheck-side-effects! "p" "a" 8.0 :magic)))
    (is (= [["p" "a" 8.0 :magic]] @calls))))

(deftest run-attack-precheck-side-effects-exception-isolated-test
  (let [calls (atom 0)]
    (h/register-attack-precheck-side-effect!
      :boom
      (fn [_ _ _ _] (throw (Exception. "boom"))))
    (h/register-attack-precheck-side-effect!
      :ok
      (fn [_ _ _ _] (swap! calls inc) true))
    (is (true? (h/run-attack-precheck-side-effects! "p" "a" 8.0 :magic)))
    (is (= 1 @calls))))

(deftest attack-check-registry-duplicate-and-freeze-policy-test
  (h/register-attack-cancel-check! :dup (fn [_ _ _ _] false))
  (h/register-attack-cancel-check! :dup (fn [_ _ _ _] true))
  (is (false? (h/should-cancel-attack? "p" "a" 1.0 :src))
      "duplicate check id preserves the first registered predicate")
  (h/register-attack-precheck-side-effect! :fx (fn [_ _ _ _] false))
  (h/register-attack-precheck-side-effect! :fx (fn [_ _ _ _] true))
  (is (false? (h/run-attack-precheck-side-effects! "p" "a" 1.0 :src))
      "duplicate side-effect id preserves the first registered callback")
  (h/freeze-attack-check-registries!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Attack check registries are frozen"
                        (h/register-attack-cancel-check! :new-check (fn [_ _ _ _] false))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Attack check registries are frozen"
                        (h/register-attack-precheck-side-effect! :new-fx (fn [_ _ _ _] true)))))

(deftest toggle-damage-handler-registers-without-adapter-test
  (testing "register-toggle-damage-handler! registers directly into the AC
            damage registry even when no :damage-interception adapter is
            installed (content init! runs during mod-constructor
            runtime-activation, before the platform adapter installs in
            common setup), and only rewrites damage while the toggle context
            is active"
    (let [handler-id :test-toggle-damage
          skill-id   :test-toggle-skill
          owner      {:logical-side :server :server-session-id :test-session :player-uuid "p1"}]
      (ps-fix/with-test-player-state-owner
        (fn []
          (store/reset-store!)
          (ps-fix/seed-player-state! "p1" {})
          (try
            (h/register-toggle-damage-handler!
             handler-id skill-id
             (fn [_ _ damage _] [(- (double damage) 1.0) {}])
             50)
            (is (contains? (set (damage-runtime/get-active-handlers)) handler-id)
                "handler registered into the AC registry without any adapter")
            (is (= 10.0 (damage-runtime/process-damage! "p1" nil 10.0 nil))
                "toggle inactive → damage passes through unchanged")
            (ctx/with-context-owner owner
              (ctx/register-context!
               (assoc (ctx/new-server-context "p1" skill-id "c1" owner)
                      :skill-state {:toggle {skill-id {:active true}}})))
            (is (= 9.0 (damage-runtime/process-damage! "p1" nil 10.0 nil))
                "toggle active → handler rewrites damage")
            (finally
              (damage-runtime/unregister-damage-handler! handler-id))))))))
