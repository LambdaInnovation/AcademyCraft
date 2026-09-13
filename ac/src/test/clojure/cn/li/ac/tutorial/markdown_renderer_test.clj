(ns cn.li.ac.tutorial.markdown-renderer-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.ac.tutorial.markdown-renderer :as mr]))

(def ^:private lang
  {"settings.academy.prop.open_data_terminal" "Open Data Terminal"})

(defn- with-lang
  "Bind a language table so ![key id=…] can resolve. Unknown keys echo back,
  matching the platform translator."
  [f]
  (binding [i18n/*translate-fn* (fn [k _args] (get lang k k))]
    (f)))

;; --- Basic rendering ---

(deftest empty-content
  ;; Empty input produces a single empty spacing segment
  (is (= 1 (count (mr/render-segments nil))))
  (is (= "" (:text (first (mr/render-segments nil)))))
  (is (= 1 (count (mr/render-segments "")))))

(deftest plain-text-line
  (let [segs (mr/render-segments "Hello world")]
    (is (= 1 (count segs)))
    (is (= "Hello world" (:text (first segs))))
    (is (= mr/default-font-size (:font-size (first segs))))
    (is (false? (:bold? (first segs))))))

(deftest heading-line
  (let [segs (mr/render-segments "# Heading")]
    (is (= 1 (count segs)))
    (is (= "Heading" (:text (first segs))))
    (is (= mr/heading-font-size (:font-size (first segs))))
    (is (true? (:bold? (first segs))))))

(deftest bold-line
  (let [segs (mr/render-segments "This is __bold__ text")]
    (is (= 1 (count segs)))
    (is (= "This is bold text" (:text (first segs))))
    (is (true? (:bold? (first segs))))))

(deftest multiple-lines
  (let [segs (mr/render-segments "Line 1\nLine 2\n# Heading")]
    (is (= 3 (count segs)))
    (is (= "Line 1" (:text (nth segs 0))))
    (is (= "Line 2" (:text (nth segs 1))))
    (is (= "Heading" (:text (nth segs 2))))))

(deftest empty-lines-become-spacing
  (let [segs (mr/render-segments "A\n\nB")]
    (is (= 3 (count segs)))
    (is (= "" (:text (nth segs 1))))))

;; --- Tag resolution ---

(deftest misakaname-tag-with-id
  (let [segs (mr/render-segments "Hello ![misakaname]" 1234)]
    (is (str/includes? (:text (first segs)) "Misaka No.1234"))))

(deftest misakaname-tag-without-id
  (let [segs (mr/render-segments "Hello ![misakaname]")]
    (is (str/includes? (:text (first segs)) "Misaka No.????"))))

(deftest key-tag-known
  (with-lang
    (fn []
      (let [segs (mr/render-segments "Use ![key id=\"open_data_terminal\"]")]
        (is (str/includes? (:text (first segs)) "Open Data Terminal"))))))

(deftest key-tag-unknown
  (let [segs (mr/render-segments "Press ![key id=\"nonexistent\"]")]
    (is (str/includes? (:text (first segs)) "[nonexistent]"))))

;; --- Multiline content ---

(deftest multiline-with-tags
  (with-lang
    (fn []
      (let [content (str "# Welcome\n"
                         "\n"
                         "Hello ![misakaname]\n"
                         "Use ![key id=\"open_data_terminal\"] to open.")
            segs (mr/render-segments content 5000)]
        (is (= 4 (count segs)))
        (is (= "Welcome" (:text (nth segs 0))))
        (is (true? (:bold? (nth segs 0))))
        (is (= "" (:text (nth segs 1))))  ;; empty line
        (is (str/includes? (:text (nth segs 2)) "Misaka No.5000"))
        (is (str/includes? (:text (nth segs 3)) "Open Data Terminal"))))))
