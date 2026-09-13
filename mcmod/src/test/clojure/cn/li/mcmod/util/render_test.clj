(ns cn.li.mcmod.util.render-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.util.render :as render]
            [cn.li.mcmod.util.log :as log]))

(defn- reset-render-state! [f]
  (render/reset-render-runtime-state-for-test!)
  (f)
  (render/reset-render-runtime-state-for-test!))

(use-fixtures :each reset-render-state!)

(deftest texture-binder-contract-test
  (testing "registered binder receives texture argument"
    (let [calls (atom [])]
      (render/register-texture-binder! (fn [tex] (swap! calls conj tex)))
      (render/bind-texture "a/b")
      (is (= ["a/b"] @calls))))
  (testing "missing binder warns once only"
    (let [warns (atom [])]
      ;; First subtest registers a binder; clear it so this block exercises the no-binder path.
      (render/register-texture-binder! nil)
      (render/reset-render-runtime-state-for-test!)
      (with-redefs [log/warn (fn [& xs] (swap! warns conj xs))]
        (render/bind-texture "x")
        (render/bind-texture "y")
        (is (= 1 (count @warns)))))))

(deftest render-time-contract-test
  (testing "get-render-time returns seconds as double and does not go backwards"
    (let [t1 (render/get-render-time)
          t2 (render/get-render-time)]
      (is (double? t1))
      (is (double? t2))
      (is (<= t1 t2)))))
