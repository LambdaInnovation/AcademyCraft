(ns cn.li.ac.recipe.crafting-recipes-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [cn.li.ac.recipe.crafting-recipes :as cr]))

(defn- ingredient-shape? [x]
  (and (map? x)
       (or (string? (:item x)) (string? (:tag x)))))

(defn- valid-recipe? [r]
  (and (string? (:id r))
       (not (str/blank? (:id r)))
       (#{:shaped :shapeless :smelting} (:type r))
       (map? (:result r))
       (string? (get-in r [:result :item]))
       (pos-int? (:count (:result r)))
       (case (:type r)
         :shaped (and (vector? (:pattern r))
                      (map? (:key r))
                      (every? string? (:pattern r))
                      (every? ingredient-shape? (vals (:key r))))
         :shapeless (and (vector? (:ingredients r))
                         (every? ingredient-shape? (:ingredients r)))
         :smelting (and (ingredient-shape? (:ingredient r))
                        (number? (:experience r))
                        (pos-int? (:cooking-time r)))
         false)))

(deftest all-crafted-recipes-contract-test
  (let [recipes (cr/get-all-recipes)]
    (is (pos? (count recipes)))
    (doseq [r recipes]
      (is (valid-recipe? r) (str "invalid recipe: " (pr-str (:id r)))))))
