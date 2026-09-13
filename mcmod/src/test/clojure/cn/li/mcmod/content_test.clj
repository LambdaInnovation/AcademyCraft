(ns cn.li.mcmod.content-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.content :as content]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.runtime.bootstrap :as runtime-bootstrap]))

(defn noop [])
(defn runtime-provider [_] {:register-content! noop})

(defn- with-fresh-framework [f]
  (let [previous fw/framework
        fresh (fw/create-framework)]
    (alter-var-root #'fw/framework (constantly fresh))
    (try
      (f fresh)
      (finally
        (alter-var-root #'fw/framework (constantly previous))))))

(def ^:private module
  {:id "content-test"
   :namespace "cn.li.mcmod.content-test"
   :function "runtime-provider"
   :provides ["register-content!"]})

(def ^:private provider-manifests
  {:common [module]
   :server []
   :client [(assoc module :id "client-only")]
   :datagen []})

(def ^:private target-model {:id "test" :loader "none" :minecraft-version "test"})

(def ^:private target-with-provider-manifests
  (assoc target-model :provider-manifests provider-manifests))

(deftest register-content-contract-test
  (testing "content metadata is explicit"
    (is (thrown? clojure.lang.ExceptionInfo (content/register-content! "example" target-model)))
    (with-fresh-framework
      (fn [_]
        (is (= [:register-content!]
               (-> (content/register-content! (assoc module :side :common) target-model)
                   second
                   keys
                   sort
                   vec)))))))

(deftest register-content-repeatability-test
  (testing "duplicate provider installation fails rather than replacing callbacks"
    (with-fresh-framework
      (fn [_]
        (is (some? (content/register-content! (assoc module :side :common) target-model)))
        (is (thrown? clojure.lang.ExceptionInfo
                     (content/register-content! (assoc module :side :common) target-model)))))))

(deftest discover-and-register-all-content-contract-test
  (testing "content metadata is explicit and target-owned"
    (is (= ["content-test"] (content/available-content-ids provider-manifests :common)))
    (with-fresh-framework
      (fn [_]
        (is (= #{:content-test}
               (set (keys (content/register-all-content! provider-manifests target-model)))))))))

(deftest common-content-init-does-not-load-client-provider-test
  (with-fresh-framework
    (fn [fw-atom]
      (content/register-all-content! provider-manifests target-model)
      (is (some? (get-in @fw-atom [:service :runtime-providers :content-test])))
      (is (nil? (get-in @fw-atom [:service :runtime-providers :client-only]))))))

(deftest neutral-bootstrap-runs-common-provider-before-lifecycle-test
  (with-fresh-framework
    (fn [fw-atom]
      (is (= #{:content-test}
             (set (keys (runtime-bootstrap/initialize-common-content! target-with-provider-manifests)))))
      (is (some? (get-in @fw-atom [:service :runtime-providers :content-test])))
      (is (nil? (get-in @fw-atom [:service :runtime-providers :client-only]))))))
