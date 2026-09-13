(ns cn.li.mcbase.runtime.multipart-entity-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [cn.li.mcbase.runtime.multipart-entity :as multipart])
  (:import [cn.li.acapi.entity MultipartEntityApi MultipartEntityApi$ParentResolver MultipartEntityApi$ParentValidator MultipartEntityPart]))

(def ^:private resolver-ids
  ["academycraft_test:throwing"
   "academycraft_test:high"
   "academycraft_test:low"
   "academycraft_test:mc_entity_parent"])

(defn- resolver
  [f]
  (reify MultipartEntityApi$ParentResolver
    (findParent [_ entity]
      (f entity))))

(defn- with-clean-resolvers
  [f]
  (doseq [resolver-id resolver-ids]
    (MultipartEntityApi/unregisterParentResolver resolver-id))
  (try
    (f)
    (finally
      (doseq [resolver-id resolver-ids]
        (MultipartEntityApi/unregisterParentResolver resolver-id)))))

(defn- api-part
  [parent-fn]
  (reify MultipartEntityPart
    (getMultipartParent [_] (parent-fn))))

(defn- with-parent-validation
  [valid-parent? f]
  (let [valid-parent-var (ns-resolve 'cn.li.mcbase.runtime.multipart-entity
                                     'valid-parent)
        validator-var (ns-resolve 'cn.li.mcbase.runtime.multipart-entity
                                  'entity-parent-validator)]
    (with-redefs-fn
      {valid-parent-var (fn [entity candidate]
                          (when (and (valid-parent? candidate)
                                     (not (identical? entity candidate)))
                            candidate))
       validator-var (reify MultipartEntityApi$ParentValidator
                       (isValid [_ candidate]
                         (boolean (valid-parent? candidate))))}
      f)))

(deftest deterministic-priority-and-failure-isolation-test
  (with-clean-resolvers
    (fn []
      (let [entity (Object.)
            expected-parent (Object.)
            calls (atom [])]
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:low" 0
          (resolver (fn [_]
                      (swap! calls conj :low)
                      expected-parent)))
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:high" 100
          (resolver (fn [_]
                      (swap! calls conj :high)
                      nil)))
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:throwing" 200
          (resolver (fn [_]
                      (swap! calls conj :throwing)
                      (throw (IllegalStateException. "incompatible optional mod")))))
        (is (identical? expected-parent
                        (MultipartEntityApi/resolveParent entity)))
        (is (= [:throwing :high :low] @calls))
        (is (= ["academycraft_test:throwing"
                "academycraft_test:high"
                "academycraft_test:low"]
               (filterv #(str/starts-with? % "academycraft_test:")
                        (MultipartEntityApi/registeredResolverIds))))))))

(deftest registration-validation-replacement-and-unregister-test
  (with-clean-resolvers
    (fn []
      (testing "resolver ids must be namespaced"
        (is (thrown? IllegalArgumentException
                     (MultipartEntityApi/registerParentResolver
                       "not-namespaced" 0 (resolver (constantly nil))))))
      (testing "same id is replaceable for reloadable integrations"
        (let [entity (Object.)
              old-parent (Object.)
              new-parent (Object.)]
          (MultipartEntityApi/registerParentResolver
            "academycraft_test:high" 0 (resolver (constantly old-parent)))
          (MultipartEntityApi/registerParentResolver
            "academycraft_test:high" 0 (resolver (constantly new-parent)))
          (is (identical? new-parent
                          (MultipartEntityApi/resolveParent entity)))
          (is (true? (MultipartEntityApi/unregisterParentResolver
                       "academycraft_test:high")))
          (is (false? (MultipartEntityApi/unregisterParentResolver
                        "academycraft_test:high"))))))))

(deftest self-parent-does-not-mask-later-resolver-test
  (with-clean-resolvers
    (fn []
      (let [entity (Object.)
            expected-parent (Object.)]
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:high" 100 (resolver identity))
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:low" 0 (resolver (constantly expected-parent)))
        (is (identical? expected-parent
                        (MultipartEntityApi/resolveParent entity)))))))

(deftest invalid-parent-type-does-not-mask-later-resolver-test
  (with-clean-resolvers
    (fn []
      (let [entity (Object.)
            invalid-parent (Object.)
            expected-parent "valid-parent"]
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:high" 100
          (resolver (constantly invalid-parent)))
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:low" 0
          (resolver (constantly expected-parent)))
        (is (= expected-parent
               (MultipartEntityApi/resolveParent entity String)))))))

(deftest loader-neutral-contract-resolves-parent-test
  (let [parent (Object.)
        part (api-part (constantly parent))]
    (with-parent-validation
      #(identical? parent %)
      (fn []
        (is (identical? parent (multipart/parent part)))
        (is (true? (multipart/multipart? part)))
        (is (identical? parent (multipart/combat-root part)))))))

(deftest registered-resolver-supports-nested-parts-test
  (with-clean-resolvers
    (fn []
      (let [root (Object.)
            middle (Object.)
            leaf (Object.)]
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:mc_entity_parent"
          0
          (resolver
            (fn [entity]
              (cond
                (identical? entity leaf) middle
                (identical? entity middle) root
                :else nil))))
        (with-parent-validation
          #(or (identical? root %)
               (identical? middle %))
          (fn []
            (is (identical? root (multipart/combat-root leaf)))
            (is (true? (multipart/multipart? middle)))
            (is (false? (multipart/multipart? root)))))))))

(deftest invalid-and-throwing-contract-results-are-isolated-test
  (with-clean-resolvers
    (fn []
      (let [parent (Object.)
            throwing-part (api-part #(throw (IllegalStateException. "broken contract")))
            invalid-part (api-part #(Object.))]
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:mc_entity_parent"
          0
          (resolver
            (fn [entity]
              (when (or (identical? entity throwing-part)
                        (identical? entity invalid-part))
                parent))))
        (with-parent-validation
          #(identical? parent %)
          (fn []
            (testing "a failing direct contract falls through to registered compatibility"
              (is (identical? parent (multipart/parent throwing-part))))
            (testing "a non-entity direct result does not mask registered compatibility"
              (is (identical? parent (multipart/parent invalid-part))))))))))

(deftest cycles-and-self-parents-are-bounded-test
  (with-clean-resolvers
    (fn []
      (let [part-a (Object.)
            part-b (Object.)
            self-part (Object.)]
        (MultipartEntityApi/registerParentResolver
          "academycraft_test:mc_entity_parent"
          0
          (resolver
            (fn [entity]
              (cond
                (identical? entity part-a) part-b
                (identical? entity part-b) part-a
                (identical? entity self-part) self-part
                :else nil))))
        (with-parent-validation
          #(or (identical? part-a %)
               (identical? part-b %)
               (identical? self-part %))
          (fn []
            (is (identical? part-a (multipart/combat-root part-a)))
            (is (nil? (multipart/parent self-part)))
            (is (false? (multipart/multipart? self-part)))))))))
