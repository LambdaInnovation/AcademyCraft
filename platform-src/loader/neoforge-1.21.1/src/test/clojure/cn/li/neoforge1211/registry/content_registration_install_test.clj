(ns cn.li.neoforge1211.registry.content-registration-install-test
  "Plan S3: bundle install + assert-all-blocks-have-bundle! smoke tests."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcbase.block.logic-compile :as lc]
            [cn.li.mcbase.block.logic-pipeline :as pipeline])
  (:import [cn.li.mcbase.block IScriptedBlock]
           [cn.li.mcbase.block.logic TileLogicBundle]))

(defn- mock-scripted-block []
  (let [state (atom {:bundle TileLogicBundle/EMPTY :tile-id "tile-a"})]
    (reify IScriptedBlock
      (getTileLogic [_] (:bundle @state))
      (getTileId [_] (:tile-id @state))
      (getBlockId [_] "block-a")
      (installTileLogic [_ b] (swap! state assoc :bundle b)))))

(deftest content-registration-bundle-install-smoke-test
  (let [block (mock-scripted-block)
        bundle (lc/compile-tile-logic {:tick-fn (fn [_ _ _ _] nil)})]
    (pipeline/install-bundle-to-block! block bundle)
    (is (identical? bundle (.getTileLogic block)))))

(deftest content-registration-assert-bundled-fail-fast-test
  (is (thrown? IllegalStateException
        (pipeline/assert-all-blocks-have-bundle! [(mock-scripted-block)] #{}))))
