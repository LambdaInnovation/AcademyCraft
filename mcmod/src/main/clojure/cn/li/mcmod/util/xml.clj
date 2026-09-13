(ns cn.li.mcmod.util.xml
  (:require [clojure.string :as str]))

(defn get-elements [parent tag]
  (filter #(and (map? %) (= (:tag %) tag)) (:content parent)))

(defn get-element [parent tag]
  (first (get-elements parent tag)))

(defn get-text [element]
  (some->> (:content element) (filter string?) first str/trim))

(defn normalize-xml-texture [s]
  (when s
    (str/trim s)))
