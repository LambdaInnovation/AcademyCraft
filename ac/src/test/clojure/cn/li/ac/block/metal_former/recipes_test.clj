(ns cn.li.ac.block.metal-former.recipes-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.block.metal-former.recipes :as recipes]))

(defn- reset-recipes-fixture [f]
  (recipes/reset-recipes-for-test!)
  (try
    (f)
    (finally
      (recipes/reset-recipes-for-test!))))

(use-fixtures :each reset-recipes-fixture)

(deftest mode-normalization-test
  (is (= :plate (recipes/normalize-mode nil)))
  (is (= :incise (recipes/normalize-mode "incise")))
  (is (= "academy:textures/guis/icons/icon_former_refine.png"
         (recipes/mode->icon-texture :refine))))

(deftest recipe-by-id-and-form-guard-test
  (is (some? (recipes/get-recipe-by-id "mf_plate_iron")))
  (is (nil? (recipes/can-form? nil nil nil :plate))))

(deftest recipe-registry-duplicate-and-reload-policy-test
  (let [custom {:id "mf_custom"
                :mode "plate"
                :input {:item "minecraft:iron_ingot" :count 1}
                :output {:item "academy:reinforced_iron_plate" :count 1}}]
    (recipes/register-recipe! custom)
    (recipes/register-recipe! custom)
    (is (= 1 (count (filter #(= "mf_custom" (:id %)) (recipes/recipes-snapshot)))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Conflicting Metal Former recipe id"
                          (recipes/register-recipe! (assoc-in custom [:output :count] 2))))
    (recipes/replace-recipes! [custom custom])
    (is (= [custom] (recipes/recipes-snapshot)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Conflicting Metal Former recipe id"
                          (recipes/replace-recipes! [custom (assoc-in custom [:output :count] 2)])))))
