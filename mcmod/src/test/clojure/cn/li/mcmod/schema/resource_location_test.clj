(ns cn.li.mcmod.schema.resource-location-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.schema.resource-location :as rl-schema]))

(deftest resource-location-validity-test
  (testing "full namespace:path strings"
    (is (true? (rl-schema/valid-resource-location? "minecraft:stone")))
    (is (true? (rl-schema/valid-resource-location? "academy:textures/gui/panel"))))
  (testing "path-only strings"
    (is (true? (rl-schema/valid-path? "textures/gui/panel")))
    (is (false? (rl-schema/valid-path? "Textures/Gui"))))
  (testing "invalid forms"
    (is (false? (rl-schema/valid-resource-location? "bad namespace:stone")))
    (is (false? (rl-schema/valid-resource-location? "minecraft:")))
    (is (false? (rl-schema/valid-resource-location? "minecraft:bad path")))))

(deftest parse-and-normalize-test
  (testing "parse full resource location"
    (is (= {:namespace "minecraft" :path "stone"}
           (rl-schema/parse-resource-location "minecraft:stone"))))
  (testing "parse path with default namespace"
    (is (= {:namespace "academy" :path "textures/gui/panel"}
           (rl-schema/parse-resource-location "textures/gui/panel" "academy"))))
  (testing "normalize"
    (is (= "minecraft:stone"
           (rl-schema/normalize-resource-location "minecraft:stone")))
    (is (= "academy:textures/gui/panel"
           (rl-schema/normalize-resource-location "textures/gui/panel" "academy"))))
  (testing "invalid input throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid resource location string"
                          (rl-schema/parse-resource-location "Bad Path" "academy")))))
