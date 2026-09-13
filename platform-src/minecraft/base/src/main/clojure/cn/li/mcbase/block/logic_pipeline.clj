(ns cn.li.mcbase.block.logic-pipeline
  "Loader-neutral tile bundle compile/install pipeline (shared by Forge and Fabric)."
  (:require [cn.li.mcbase.block.logic-compile :as logic-compile]
            [cn.li.platform.neutral.block-runtime :as tdsl]
            [cn.li.platform.neutral.block-runtime :as tile-kind])
  (:import [cn.li.mcbase.block IScriptedBlock]
           [cn.li.mcbase.block.logic TileLogicBundle]))

(defn compile-all-bundles
  "Pure: tile-id → TileLogicBundle for every registered tile spec."
  []
  (reduce-kv (fn [m tile-id spec]
               (assoc m tile-id (logic-compile/compile-tile-logic
                                  (tile-kind/merge-tile-kind-defaults spec))))
             {}
             (tdsl/snapshot-tiles-by-id)))


(defn install-bundle-to-block!
  "Install a compiled bundle on a single IScriptedBlock instance."
  [^IScriptedBlock block ^TileLogicBundle bundle]
  (.installTileLogic block bundle))

(defn assert-all-blocks-have-bundle!
  "Fail-fast when any IScriptedBlock still has EMPTY logic (registration bug)."
  [blocks allow-empty-tile-ids]
  (doseq [b blocks
          :when (instance? IScriptedBlock b)
          :let [^IScriptedBlock sb b
                tid (.getTileId sb)]]
    (when (and (identical? TileLogicBundle/EMPTY (.getTileLogic sb))
               (not (contains? allow-empty-tile-ids tid)))
      (throw (IllegalStateException.
               (str "tile-id " tid " has empty TileLogicBundle"))))))
