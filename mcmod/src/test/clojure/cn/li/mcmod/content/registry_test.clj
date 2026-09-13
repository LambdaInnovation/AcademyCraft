(ns cn.li.mcmod.content.registry-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.content.registry :as registry]))

(defn- clean-registry-fixture
  [f]
  (registry/clear-registry!)
  (f)
  (registry/clear-registry!))

(use-fixtures :each clean-registry-fixture)

(deftest empty-and-missing-descriptor-test
  (testing "empty registries expose stable neutral defaults"
    (is (= {} (registry/registry-snapshot)))
    (is (= [] (registry/list-descriptors :content-action)))
    (is (nil? (registry/get-descriptor :content-action :missing)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"not registered"
                          (registry/require-descriptor :content-action :missing)))))

(deftest register-and-dispatch-action-test
  (registry/register-action! {:id :example/run
                              :content-id "example"
                              :handler (fn [context payload]
                                         {:context context
                                          :payload payload})})
  (is (= {:context {:actor 1}
          :payload {:value 2}}
         (registry/dispatch-action! :example/run {:actor 1} {:value 2}))))

(deftest duplicate-registration-contract-test
  (let [descriptor {:id :example/stable
                    :content-id "example"
                    :handler identity}]
    (registry/register-action! descriptor)
    (registry/register-action! descriptor)
    (is (= 1 (count (registry/list-descriptors :content-action))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"already registered"
                          (registry/register-action! (assoc descriptor :handler (constantly :different)))))))

(deftest descriptor-envelope-validation-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"neutral host category"
                        (registry/register-descriptor! :feature-owned {:id :bad
                                                                       :content-id "example"})))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"neutral host envelope"
                        (registry/register-action! {:id :bad
                                                    :content-id "example"
                                                    :feature-owned-field true})))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"keyword or non-empty string"
                        (registry/register-action! {:id nil
                                                    :content-id "example"}))))

(deftest smoke-manifest-registry-test
  (registry/register-smoke-manifest! {:id :example/smoke
                                      :content-id "example"
                                      :checks [{:id :registry-present}]
                                      :fixtures {:sample true}})
  (is (= [:example/smoke]
         (mapv :id (registry/list-smoke-manifests)))))