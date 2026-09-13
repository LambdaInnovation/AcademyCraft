(ns cn.li.ac.config.nbt-keys
  "Centralized NBT key constants for AC module.

  All NBT keys used in block entity schemas are defined here to:
  - Prevent typos and inconsistencies
  - Enable easy refactoring
  - Document all persistent data keys in one place")

(def nbt-keys
  "Map of semantic keys to NBT string keys used in block entity serialization."
  {;; Common keys
   :placer "Placer"
   :placer-uuid "PlacerUuid"
   :energy "Energy"
  :mode "Mode"
  :input-slot "InputSlot"
  :output-slot "OutputSlot"
  :face-config "FaceConfig"

   ;; Wireless Node keys
   :node-type "NodeType"
   :node-name "NodeName"
   :password "Password"
   :enabled "Enabled"
   :node-inventory "NodeInventory"
  :wireless-mode "WirelessMode"
  :wireless-bandwidth "WirelessBandwidth"

   ;; Wireless Matrix keys
   :plate-count "PlateCount"
   :core-level "CoreLevel"
   :direction "Direction"
   :sub-id "SubId"
   :controller-pos-x "ControllerPosX"
   :controller-pos-y "ControllerPosY"
   :controller-pos-z "ControllerPosZ"
   :matrix-inventory "Inventory"

   ;; Solar Generator keys
   :battery "Battery"

   ;; Ability Developer
   :developer-inventory "DeveloperInv"})

(defn get-key
  "Get NBT key string by semantic keyword.

  Args:
    k - Keyword identifying the NBT field

  Returns: String NBT key, or throws if not found"
  [k]
  (or (get nbt-keys k)
      (throw (ex-info "Unknown NBT key" {:key k :available (keys nbt-keys)}))))
