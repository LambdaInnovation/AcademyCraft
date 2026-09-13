(ns cn.li.mc1201.block.pipeline-install-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcbase.block.logic-compile :as lc]
            [cn.li.mcbase.block.logic-pipeline :as pipeline])
  (:import [cn.li.mcbase.block IScriptedBlock]
           [cn.li.mcbase.block.logic TileLogicBundle]))

(defn- mock-scripted-block []
  (let [state (atom {:installed nil :tile-id "test-tile"})]
    (reify IScriptedBlock
      (getTileLogic [_] (or (:installed @state) TileLogicBundle/EMPTY))
      (getTileId [_] (:tile-id @state))
      (getBlockId [_] "test-block")
      (installTileLogic [_ bundle]
        (reset! state (assoc @state :installed bundle))))))

(deftest install-bundle-to-block-test
  (let [block (mock-scripted-block)
        bundle (lc/compile-tile-logic {:tick-fn (fn [_ _ _ _] nil)})]
    (is (identical? TileLogicBundle/EMPTY (.getTileLogic block)))
    (pipeline/install-bundle-to-block! block bundle)
    (is (identical? bundle (.getTileLogic block)))))

(deftest assert-all-blocks-have-bundle-fail-fast-test
  (let [empty-block (mock-scripted-block)
        ok-block (mock-scripted-block)
        bundle (lc/compile-tile-logic {})]
    (pipeline/install-bundle-to-block! ok-block bundle)
    (is (thrown? IllegalStateException
          (pipeline/assert-all-blocks-have-bundle! [empty-block ok-block] #{})))
    (is (nil? (pipeline/assert-all-blocks-have-bundle! [ok-block] #{"test-tile"})))))
