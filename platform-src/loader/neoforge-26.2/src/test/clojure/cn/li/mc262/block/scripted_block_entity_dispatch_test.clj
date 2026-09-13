(ns cn.li.mc262.block.scripted-block-entity-dispatch-test
  (:require [clojure.test :refer [deftest is]]))

(deftest abstract-scripted-be-has-no-rt-var-test
  (let [candidates ["platform-src/minecraft/mc-26.2/src/main/java/cn/li/mc262/block/entity/AbstractScriptedBlockEntity.java"
                    "../platform-src/minecraft/mc-26.2/src/main/java/cn/li/mc262/block/entity/AbstractScriptedBlockEntity.java"]
        src (some (fn [rel]
                    (let [f (java.io.File. (str (System/getProperty "user.dir") "/" rel))]
                      (when (.exists f) (slurp f))))
                  candidates)]
    (is (some? src) "AbstractScriptedBlockEntity.java readable")
    (when src
      (is (not (re-find #"RT\.var" src)))
      (is (not (re-find #"tileLogicNamespace" src)))
      (is (re-find #"bundle\(\)" src)))))
