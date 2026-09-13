(ns cn.li.mcmod.client.render.script-render-registry-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.client.render.script-render-abi :as abi]
            [cn.li.mcmod.client.render.script-render-registry :as registry]))

(defn- reset-registry-fixture
  [f]
  (registry/unfreeze!)
  (registry/clear!)
  (f)
  (registry/unfreeze!)
  (registry/clear!))

(use-fixtures :each reset-registry-fixture)

(deftest abi-normalize-and-validate-test
  (testing "normalize applies defaults and validate returns normalized profile"
    (let [profile (abi/validate-profile! {:id "effect-billboard"
                                          :kind :billboard-cross})]
      (is (= "effect-billboard" (:id profile)))
      (is (= :billboard-cross (:kind profile)))
      (is (= 1 (:version profile)))
      (is (true? (:enabled? profile)))
      (is (= :default (get-in profile [:state :depth-test])))
      (is (= :alpha (get-in profile [:state :blend])))
      (is (= :translucent (get-in profile [:state :layer])))
      (is (= 0.6 (get-in profile [:params :size]))))))

(deftest abi-validation-rejects-unsupported-state-test
  (testing "unsupported state values are rejected"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #":state/:depth-test"
          (abi/validate-profile! {:id "bad-depth"
                                  :kind :billboard-cross
                                  :state {:depth-test :sometimes}})))))

(deftest abi-validation-rejects-unknown-top-level-key-test
  (testing "unknown top-level keys are rejected"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"unsupported top-level keys"
          (abi/validate-profile! {:id "bad-key"
                                  :kind :ring-lines
                                  :unexpected true})))))

(deftest abi-ray-composite-acceptance-and-bounds-test
  (testing "ray-composite kind is accepted and gets translucent defaults"
    (let [profile (abi/validate-profile! {:id "ray-main"
                                          :kind :ray-composite})]
      (is (= :ray-composite (:kind profile)))
      (is (= :translucent (get-in profile [:state :layer])))
      (is (= 15.0 (get-in profile [:params :length])))))

  (testing "ray-composite-lite remains backward-compatible"
    (let [profile (abi/validate-profile! {:id "ray-lite"
                                          :kind :ray-composite-lite
                                          :params {:length 8.0}})]
      (is (= :ray-composite-lite (:kind profile)))
      (is (= 8.0 (get-in profile [:params :length])))
      (is (= :translucent (get-in profile [:state :layer])))))

  (testing "out-of-range ray width is rejected"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"out of range"
          (abi/validate-profile! {:id "bad-ray"
                                  :kind :ray-composite-lite
                                  :params {:inner-width -0.1}})))))

(deftest registry-first-write-wins-test
  (testing "duplicate registrations keep first profile"
    (let [first (registry/register-profile! {:id "ripple-mark"
                                             :kind :ring-lines
                                             :params {:rings 3}})
          second (registry/register-profile! {:id "ripple-mark"
                                              :kind :ring-lines
                                              :params {:rings 9}})
          loaded (registry/get-profile "ripple-mark")]
      (is (= 3 (get-in first [:params :rings])))
      (is (= first second))
      (is (= 3 (get-in loaded [:params :rings]))))))

(deftest registry-freeze-rejects-registration-test
  (testing "frozen registry rejects new profile registrations"
    (registry/freeze!)
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"registry is frozen"
          (registry/register-profile! {:id "frozen-profile"
                                       :kind :wire-box}))))
  (testing "registry can unfreeze and accept writes again"
    (registry/unfreeze!)
    (is (= "after-unfreeze"
           (:id (registry/register-profile! {:id "after-unfreeze"
                                            :kind :wire-box}))))))
