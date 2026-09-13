(ns cn.li.ac.energy.api-impl-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.energy.api.impl :as energy-api]
            [cn.li.ac.energy.api.protocol :as proto]
            [cn.li.ac.energy.domain.container :as container]))

(defn- reset-default-system-fixture [f]
  (energy-api/install-default-impls!)
  (energy-api/call-with-energy-system-runtime
    (energy-api/create-energy-system-runtime)
    f))

(use-fixtures :each reset-default-system-fixture)

(def ^:private test-owner {:server-session-id :test-session :world-id :overworld})

(deftest atom-container-provider-registration-and-subscription-test
  (let [system (energy-api/energy-system test-owner)
        tank (atom (container/energy-container 100.0 10.0))
        seen (atom [])]
    (testing "registration delegates to provider registry"
      (is (= :tank (energy-api/register-provider! test-owner :tank tank)))
      (is (= [:tank] (proto/list-energy-providers system)))
      (is (= 10.0 (proto/get-energy system :tank)))
      (is (= 100.0 (proto/get-capacity system :tank))))
    (testing "atom-backed containers can be mutated and notify subscribers"
      (let [sid (proto/subscribe-to-changes system :tank (fn [old new]
                                                           (swap! seen conj [old new])))]
        (is (= {:success true :reason "ok"}
               (proto/set-energy system :tank 60.0)))
        (is (= 60.0 (container/get-current @tank)))
        (is (= [[10.0 60.0]] @seen))
        (is (nil? (proto/unsubscribe-from-changes system sid)))
        (is (= {:success true :reason "ok"}
               (proto/set-energy system :tank 30.0)))
        (is (= [[10.0 60.0]] @seen))))
    (testing "admin dump and unregister remain available through impl facade"
      (is (= {:tank {:kind :container :energy 30.0 :capacity 100.0}}
             (proto/admin-dump-state system)))
      (is (nil? (energy-api/unregister-provider! test-owner :tank)))
      (is (= [] (proto/list-energy-providers system))))))

(deftest immutable-container-provider-is-readable-but-not-settable-test
  (let [system (energy-api/energy-system test-owner)
        tank (container/energy-container 50.0 12.0)]
    (is (= :immutable (energy-api/register-provider! test-owner :immutable tank)))
    (is (= 12.0 (proto/get-energy system :immutable)))
    (is (= {:success false
            :reason "container is immutable unless registered with an atom ref"}
           (proto/set-energy system :immutable 20.0)))))

(deftest owner-scoped-energy-systems-isolate-provider-ids-test
  (let [owner-a {:server-session-id :session-a :world-id :overworld}
        owner-b {:server-session-id :session-b :world-id :overworld}
        system-a (energy-api/energy-system owner-a)
        system-b (energy-api/energy-system owner-b)
        tank-a (atom (container/energy-container 100.0 10.0))
        tank-b (atom (container/energy-container 100.0 70.0))]
    (is (not (identical? system-a system-b)))
    (is (= :tank (energy-api/register-provider! owner-a :tank tank-a)))
    (is (= :tank (energy-api/register-provider! owner-b :tank tank-b)))
    (is (= 10.0 (proto/get-energy system-a :tank)))
    (is (= 70.0 (proto/get-energy system-b :tank)))

    (is (= {:success true :reason "ok"}
           (proto/set-energy system-a :tank 45.0)))
    (is (= 45.0 (container/get-current @tank-a)))
    (is (= 70.0 (container/get-current @tank-b)))
    (is (= #{(energy-api/energy-owner-key owner-a)
             (energy-api/energy-owner-key owner-b)}
           (set (keys (energy-api/energy-systems-snapshot)))))))



(deftest energy-owner-is-required-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"requires :server-session-id"
                        (energy-api/energy-system {:world-id :overworld})))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"requires :world-id"
                        (energy-api/register-provider! {:server-session-id :test-session}
                                                       :tank
                                                       (atom (container/energy-container 10.0 0.0))))))
