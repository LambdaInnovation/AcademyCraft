(ns cn.li.mcmod.datagen.metadata-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [cn.li.mcmod.datagen.metadata :as metadata]))

(defn- clean-fixture
  [f]
  (metadata/reset-datagen-metadata-for-test!)
  (f)
  (metadata/reset-datagen-metadata-for-test!))

(use-fixtures :each clean-fixture)

(deftest recipes-validation-test
  (testing "valid recipes are accepted"
    (metadata/set-recipes! [{:id "demo"
                             :type :shapeless
                             :ingredients [{:item "minecraft:stone"}]
                             :result {:item "minecraft:cobblestone" :count 1}}])
    (is (= 1 (count (metadata/get-recipes)))))
  (testing "invalid recipes fail fast"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"recipes contract violation"
                          (metadata/set-recipes! [{:id "bad" :type :shaped}])))))

(deftest achievement-and-translation-validation-test
  (testing "achievement tabs and achievements are validated"
    (metadata/set-achievement-tabs! [{:id :default :background "academy:textures/gui/bg.png"}])
    (metadata/set-achievements! [{:id "demo"
                                  :tab :default
                                  :criteria [{:type :custom :criterion-id "demo"}]
                                  :translation {:en_us {"k" "v"}
                                                :zh_cn {"k" "值"}}}])
    (is (= 1 (count (metadata/get-achievement-tabs))))
    (is (= 1 (count (metadata/get-achievements)))))
  (testing "invalid achievement shape fails"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"achievements contract violation"
                          (metadata/set-achievements! [{:id :bad}]))))
  (testing "translations require en_us and zh_cn maps"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"translations contract violation"
                          (metadata/set-translations! {:en_us {"k" "v"}})))))