(ns cn.li.ac.tutorial.content-test
  "Tests for tutorial content loading and section parsing."
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [cn.li.ac.tutorial.content :as content]))

;; --- parse-sections ---

(deftest parse-sections-all-three
  (let [raw "![title]\nResearch Note\n\n![brief]\nA brief summary\n\n![content]\nMain content here"
        result (content/parse-sections raw)]
    (is (= "Research Note" (:title result)))
    (is (= "A brief summary" (:brief result)))
    (is (= "Main content here" (:content result)))))

(deftest parse-sections-missing-brief
  (let [raw "![title]\nMy Title\n![content]\nSome content"
        result (content/parse-sections raw)]
    (is (= "My Title" (:title result)))
    (is (= "" (:brief result)))
    (is (= "Some content" (:content result)))))

(deftest parse-sections-missing-content
  (let [raw "![title]\nMy Title\n![brief]\nA brief"
        result (content/parse-sections raw)]
    (is (= "My Title" (:title result)))
    (is (= "A brief" (:brief result)))
    (is (= "" (:content result)))))

(deftest parse-sections-empty-input
  (let [result (content/parse-sections nil)]
    (is (= "" (:title result)))
    (is (= "" (:brief result)))
    (is (= "" (:content result))))
  (let [result (content/parse-sections "")]
    (is (= "" (:title result)))
    (is (= "" (:brief result)))
    (is (= "" (:content result)))))

(deftest parse-sections-no-markers
  (let [raw "Plain text without any markers\nJust some content"
        result (content/parse-sections raw)]
    (is (= "" (:title result)))
    (is (= "" (:brief result)))
    (is (= "" (:content result)))))

(deftest parse-sections-preserves-markdown
  "Content should preserve markdown formatting like __bold__, ![images](url), # headings."
  (let [raw (str "![title]\n# Welcome\n\n"
                 "![brief]\n__Important__ overview of the tutorial\n\n"
                 "![content]\nHere is an image:\n![alt](academy:textures/foo.png)\n\n"
                 "Use ![key id=\"my_key\"] to perform actions.")
        result (content/parse-sections raw)]
    (is (str/includes? (:title result) "# Welcome"))
    (is (str/includes? (:brief result) "__Important__"))
    (is (str/includes? (:content result) "![alt](academy:textures/foo.png)"))
    (is (str/includes? (:content result) "![key id=\"my_key\"]"))))

(deftest parse-sections-trims-head-whitespace
  (let [raw "![title]\n  \n  \nMy Title  \n\n![brief]\n  A brief  \n\n![content]\n  Content  "
        result (content/parse-sections raw)]
    (is (= "My Title" (:title result)))
    (is (= "A brief" (:brief result)))
    (is (= "Content" (:content result)))))

;; --- resolve-lang ---

(deftest resolve-lang-case-insensitive
  (is (= "en_US" (#'content/resolve-lang "en_us")))
  (is (= "en_US" (#'content/resolve-lang "EN_US")))
  (is (= "zh_CN" (#'content/resolve-lang "zh_cn"))))

(deftest resolve-lang-fallback
  (is (= "en_US" (#'content/resolve-lang nil)))
  (is (= "en_US" (#'content/resolve-lang "ja_JP")))
  (is (= "en_US" (#'content/resolve-lang ""))))
