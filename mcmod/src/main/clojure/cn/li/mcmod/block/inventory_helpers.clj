(ns cn.li.mcmod.block.inventory-helpers
  "Generic inventory NBT serialization utilities.

  Provides load/save functions for item vector 鈫?NBT ListTag conversion.
  Reusable across any block with inventory slots."
  (:require [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.be :as pbe]
            [cn.li.mcmod.util.log :as log]))

(defn load-inventory
  "Deserialize NBT ListTag into item vector [slot0 slot1 ...].

  Args:
    tag - CompoundTag containing inventory data
    nbt-key - String key for inventory ListTag
    default - Default vector (e.g. [nil nil])

  Returns: Vector of ItemStack or nil per slot"
  [tag nbt-key default]
  (if (sd/has-key-safe? tag nbt-key)
    (let [inv-tag (sd/get-list tag nbt-key)
          size    (sd/list-size inv-tag)]
      (reduce
        (fn [v i]
          (let [st   (sd/list-structured inv-tag i)
                slot (sd/get-int st "Slot")
                item (pitem/from-data st)]
            (if (and (>= slot 0) (< slot (count v)))
              (assoc v slot (when-not (pitem/empty? item) item))
              v)))
        default
        (range size)))
    default))

(defn save-inventory
  "Serialize item vector into NBT ListTag.

  Args:
    state - State map containing :inventory key
    tag - CompoundTag to write to
    nbt-key - String key for inventory ListTag

  Side effects: Writes ListTag to tag"
  [state tag nbt-key]
  (let [inv      (get state :inventory [])
        inv-list (sd/create-list)]
    (doseq [slot (range (count inv))]
      (when-let [item (nth inv slot nil)]
        (let [st (sd/create-structured)]
          (sd/set-int! st "Slot" slot)
          (pitem/save-to-data item st)
          (sd/append! inv-list st))))
    (sd/set-entry! tag nbt-key inv-list)))

(defn update-be-field!
  "Update single field in BE's customState.

  Args:
    be - BlockEntity
    field - Keyword field name
    value - New value

  Side effects: Updates BE state and marks changed"
  [be field value]
  (let [state (or (pbe/get-custom-state be) {})]
    (pbe/set-custom-state! be (assoc state field value))
    (try (pbe/set-changed! be) (catch Exception e (log/warn "set-changed! failed in update-be-field!:" (ex-message e))))
    (try (pbe/sync-to-client! be) (catch Exception e (log/warn "sync-to-client! failed in update-be-field!:" (ex-message e))))
    be))

