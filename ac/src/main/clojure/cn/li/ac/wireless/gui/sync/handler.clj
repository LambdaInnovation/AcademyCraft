(ns cn.li.ac.wireless.gui.sync.handler
  "Shared network handler utility functions

  Provides common helper functions for server-side network message handlers,
  eliminating duplication between node_network_handler and matrix_network_handler."
  (:require [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.platform.entity :as entity]))

;; ============================================================================
;; World and Tile Access Helpers
;; ============================================================================

(defn get-world
  "Get world instance from player, handling cross-version API differences

  Args:
  - player: Player entity instance

  Returns: World/Level instance or nil if not accessible"
  [player]
  (entity/player-get-level player))

(defn get-tile-at
  "Fetch tile entity at position from network payload
  
  Args:
  - world: World/Level instance
  - payload: Map containing :pos-x, :pos-y, :pos-z keys
  
  Returns: TileEntity/BlockEntity or nil if not found"
  [world {:keys [pos-x pos-y pos-z]}]
  (when (and world (number? pos-x) (number? pos-y) (number? pos-z))
    (let [block-pos (pos/create-block-pos pos-x pos-y pos-z)]
      (world/get-tile-entity world block-pos))))

;; ============================================================================
;; Payload Construction Helpers
;; ============================================================================

(defn tile-pos-payload
  "Extract position payload from a tile entity for network messages

  Args:
  - tile: ScriptedBlockEntity / BlockEntity instance

  Returns: Map with :pos-x, :pos-y, :pos-z keys.
  Throws ex-info if position cannot be resolved."
  [tile]
  (let [block-pos (pos/block-pos tile)]
    (when-not block-pos
      (throw (ex-info "tile-pos-payload: tile has no position"
                      {:tile tile})))
    {:pos-x (pos/pos-x block-pos)
     :pos-y (pos/pos-y block-pos)
     :pos-z (pos/pos-z block-pos)}))
