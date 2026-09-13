(ns cn.li.fabric262.platform.bindings
  "Fabric-specific static function bindings for core platform hooks."
  (:require [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.be :as pbe]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mcmod.util.log :as log]
            [cn.li.fabric262.mod :as fabric-mod])
  (:import [cn.li.fabric262.block.entity ScriptedBlockEntity]
           [net.minecraft.core BlockPos]
           [net.minecraft.world.level Level]
           [net.minecraft.world.level.block Block]))

;; extend-type on ScriptedBlockEntity for IBlockEntity removed (defprotocol deleted — Phase 1.2)
;; BE ops now installed as plain function maps via installer_core.clj

(defn world-is-client-side
  [^Level level]
  (.isClientSide level))

(defn world-get-tile-entity
  [^Level level pos]
  (.getBlockEntity level pos))

(defn world-get-block-state
  [^Level level pos]
  (.getBlockState level pos))

(defn world-set-block
  [^Level level pos state flags]
  (.setBlock level pos state (int flags)))

(defn world-remove-block
  [^Level level pos]
  (let [^net.minecraft.world.level.block.state.BlockState bs (.getBlockState level pos)]
    ;; destroyBlock returns false for fluid blocks (matter-unit collection);
    ;; remove fluids by setting air directly.
    (if (.isEmpty (.getFluidState bs))
      (.destroyBlock level pos false)
      (.setBlock level pos
        (.defaultBlockState ^net.minecraft.world.level.block.Block
          net.minecraft.world.level.block.Blocks/AIR)
        3))))

(defn world-break-block
  [^Level level pos drop?]
  (.destroyBlock level pos (boolean drop?)))

(defn world-is-chunk-loaded?
  [^Level level chunk-x chunk-z]
  (.hasChunk level (int chunk-x) (int chunk-z)))

(defn world-get-day-time
  [^Level level]
  (.getOverworldClockTime level))

(defn world-get-game-time
  [^Level level]
  (.getGameTime level))

(defn world-get-dimension-id
  [^Level level]
  (str (.identifier (.dimension level))))

(defn world-server-session-id
  [^Level level]
  (when-let [sid (cn.li.mc262.runtime.RuntimeAccess/getWorldServerSessionId level)]
    [:server sid]))

(defn open-player-menu!
  [^net.minecraft.world.entity.player.Player player factory]
  (.openMenu player factory))

(defn player-persistent-data
  [^net.minecraft.server.level.ServerPlayer player]
  (cn.li.fabric262.runtime.FabricPlayerPersistentData/get player))

(defn world-get-players
  [^Level level]
  (seq (.players level)))

(defn world-is-raining
  [^Level level]
  (.isRaining level))

(defn world-can-see-sky
  [^Level level pos]
  (.canSeeSky level pos))

(defn- registry-name-path
  "Strip the namespace from a block id: \"academy:imag_phase\" → \"imag_phase\"."
  [block-id]
  (let [s (str block-id)
        colon (.indexOf s ":")]
    (if (neg? colon) s (subs s (inc colon)))))

(defn- block-id-candidates
  "Candidate snapshot keys for a block id. The block snapshot is keyed by DSL
  id (\"imag-phase\"), but callers may pass the Minecraft registry id
  (\"academy:imag_phase\", as returned by raytraces) — reverse-resolve that
  back to the DSL id so both id spaces place correctly."
  [block-id]
  (let [dsl-id (str block-id)
        registry-name (registry-metadata/get-block-registry-name dsl-id)
        path (registry-name-path block-id)
        reverse-dsl-id (some (fn [id]
                               (when (= path (registry-metadata/get-block-registry-name id))
                                 id))
                             (registry-metadata/get-all-block-ids))]
    (distinct (cond-> [dsl-id]
                registry-name (conj registry-name)
                reverse-dsl-id (conj reverse-dsl-id)))))

(defn- lookup-registered-block
  [block-id]
  (fabric-mod/get-registered-block block-id))

(defn world-place-block-by-id
  [^Level level block-id ^BlockPos pos flags]
  (try
    (let [^Block blk (some lookup-registered-block (block-id-candidates block-id))]
      (when blk
        (.setBlock level pos (.defaultBlockState blk) (int flags))
        true))
    (catch Exception e
      (log/stacktrace (str "Failed to place block by id" block-id ":" ) e)
      false)))
