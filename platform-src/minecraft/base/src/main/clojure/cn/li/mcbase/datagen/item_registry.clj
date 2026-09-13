(ns cn.li.mcbase.datagen.item-registry
  "Shared item registry safety helpers for datagen.

  Registry lookups go through cn.li.mcver.RegistryValues."
  (:import [java.util HashSet]
           [cn.li.mcver RegistryValues]))

(defn known-item-ids
  "Build set of known item IDs from mod metadata + Minecraft registry."
  [get-item-ids-fn get-item-name-fn get-block-ids-fn get-block-name-fn mod-id]
  (let [known (HashSet.)]
    (doseq [item-id (get-item-ids-fn)]
      (let [registry-name (get-item-name-fn item-id)]
        (.add known (str mod-id ":" registry-name))))
    (doseq [block-id (get-block-ids-fn)]
      (let [registry-name (get-block-name-fn block-id)]
        (.add known (str mod-id ":" registry-name))))
    known))

(defn item-exists?
  "Check if item reference exists in registry or mod metadata."
  [^String id known parse-rl-fn]
  (or (.contains ^HashSet known id)
      (some? (RegistryValues/getItem (parse-rl-fn id)))))

(defn safe-item-id
  "Resolve item ID, defaulting to minecraft:book if not found."
  [id known parse-rl-fn]
  (let [sid (str id)]
    (if (item-exists? sid known parse-rl-fn)
      sid
      "minecraft:book")))

(defn with-safe-items
  "Apply safe-item-id to all items in criteria collection."
  [criteria known parse-rl-fn]
  (mapv
   (fn [entry]
     (if (= :inventory-changed (:type entry))
       (update entry :items (fn [xs] (mapv #(safe-item-id % known parse-rl-fn) xs)))
       entry))
   criteria))
