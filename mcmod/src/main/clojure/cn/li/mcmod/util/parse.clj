(ns cn.li.mcmod.util.parse
  (:require [clojure.string :as str]))

(defn parse-float
  ([s] (parse-float s nil))
  ([s default]
   (if s
     (try
       (Float/parseFloat (str/trim (str s)))
       (catch Exception _ default))
     default)))

(defn parse-int
  ([s] (parse-int s nil))
  ([s default]
   (if s
     (try
       (Integer/parseInt (str/trim (str s)))
       (catch Exception _ default))
     default)))

(defn parse-bool [s]
  (when s
    (= (str/lower-case (str/trim (str s))) "true")))

(defn parse-color [s]
  (when s
    (try
      (let [trimmed (str/trim (str s))
            hex (if (str/starts-with? trimmed "#") (subs trimmed 1) trimmed)]
        (Long/parseLong hex 16))
      (catch Exception _ 0xFFFFFF))))
