(ns cn.li.ac.block.imag-fusor.recipes-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.block.imag-fusor.recipes :as recipes]))

(defn- reset-recipes-fixture [f]
  (recipes/reset-recipes-for-test!)
  (try
    (f)
    (finally
      (recipes/reset-recipes-for-test!))))

(use-fixtures :each reset-recipes-fixture)

(deftest recipe-lookup-and-can-craft-guards-test
  (let [r (recipes/get-recipe-by-id "if_crystal_low_to_normal")]
    (is (= "if_crystal_low_to_normal" (:id r)))
    (is (nil? (recipes/can-craft? nil nil nil 0 0)))))

(deftest recipe-registry-duplicate-and-reload-policy-test
  (let [custom {:id "if_custom"
                :input {:item "academy:crystal_low" :count 1}
                :output {:item "academy:crystal_normal" :count 1}
                :consume-liquid 1
                :time 1}]
    (recipes/register-recipe! custom)
    (recipes/register-recipe! custom)
    (is (= 1 (count (filter #(= "if_custom" (:id %)) (recipes/recipes-snapshot)))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Conflicting Imaginary Fusor recipe id"
                          (recipes/register-recipe! (assoc-in custom [:output :count] 2))))
    (recipes/replace-recipes! [custom custom])
    (is (= [custom] (recipes/recipes-snapshot)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Conflicting Imaginary Fusor recipe id"
                          (recipes/replace-recipes! [custom (assoc-in custom [:output :count] 2)])))))
