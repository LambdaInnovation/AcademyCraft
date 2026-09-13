(ns cn.li.mcmod.util.xml-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.util.xml :as xml]))

(deftest xml-core-test
  (testing "element selectors and text extraction"
    (let [doc {:tag :root
               :content [{:tag :item :content [" first "]}
                         {:tag :item :content ["second"]}
                         {:tag :other :content ["ignore"]}]}
          first-item (xml/get-element doc :item)]
      (is (= 2 (count (xml/get-elements doc :item))))
      (is (= :item (:tag first-item)))
      (is (= "first" (xml/get-text first-item))))))

(deftest xml-edge-cases-test
  (testing "non-maps and non-strings are ignored safely"
    (let [doc {:content [{:tag :item :content [42 "ok"]} "junk" 1 nil]}]
      (is (= 1 (count (xml/get-elements doc :item))))
      (is (= "ok" (xml/get-text {:content [1 nil " ok "]})))
      (is (= nil (xml/get-text {:content [1 nil]})))))
  (testing "normalize handles nil and whitespace"
    (is (= nil (xml/normalize-xml-texture nil)))
    (is (= "assets/academy/textures/a.png"
           (xml/normalize-xml-texture " assets/academy/textures/a.png ")))))

(deftest xml-texture-contract-test
  (testing "texture normalization contract"
    (let [legacy-namespace (str "academy" ":")]
      (doseq [{:keys [input expected]}
            [{:input (str legacy-namespace "block/a") :expected (str legacy-namespace "block/a")}
             {:input "academy:block/a" :expected "academy:block/a"}
             {:input "other:block/a" :expected "other:block/a"}
             {:input "plain/path" :expected "plain/path"}
             {:input " assets/x " :expected "assets/x"}]]
        (is (= expected (xml/normalize-xml-texture input)))))))
