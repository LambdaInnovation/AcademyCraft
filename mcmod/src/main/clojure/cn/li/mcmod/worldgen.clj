(ns cn.li.mcmod.worldgen
  "Platform-neutral worldgen registries.

  Content modules register descriptors during init. Platform worldgen
  providers read these registries at datagen/feature-init time instead
  of hardcoding content-specific block/feature IDs."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.registry :as registry]))

;; --- pool fill block ---

(def ^:private pool-fill-block-key ::pool-fill-block)

(defn register-pool-fill-block-id!
  "Register the block ID (e.g. \"modid:block_name\") used as the fill block
  in configurable pool features. Called by content during block registration."
  [mod-colon-path]
  (registry/register! (fw/fw-atom) :worldgen pool-fill-block-key (str mod-colon-path))
  nil)

(defn get-pool-fill-block-id
  "Return the registered pool fill block ID string, or nil if not yet set."
  []
  (registry/get-spec (fw/fw-atom) :worldgen pool-fill-block-key))

(defn reset-pool-fill-block-id-for-test!
  []
  (swap! (fw/fw-atom) update-in [:registry :worldgen] dissoc pool-fill-block-key)
  nil)

;; --- worldgen ore descriptors ---

(defn register-worldgen-ore!
  "Register an ore generation descriptor.
  descriptor: {:id key :name str :size int :count int :enabled? bool ...}"
  [descriptor]
  (registry/register! (fw/fw-atom) :worldgen [::ore (:id descriptor)] descriptor)
  nil)

(defn list-worldgen-ores
  []
  (->> (get-in @(fw/fw-atom) [:registry :worldgen])
       (filter (fn [[k _]] (and (vector? k) (= ::ore (first k)))))
       (mapv val)))

(defn reset-worldgen-ores-for-test!
  []
  (swap! (fw/fw-atom) update-in [:registry :worldgen]
    (fn [m] (into {} (remove (fn [[k _]] (and (vector? k) (= ::ore (first k))))) m)))
  nil)

;; --- worldgen liquid descriptors ---

(defn register-worldgen-liquid!
  "Register a liquid generation descriptor.
  descriptor: {:id key :block-id str :name str :count int :enabled? bool ...}"
  [descriptor]
  (registry/register! (fw/fw-atom) :worldgen [::liquid (:id descriptor)] descriptor)
  nil)

(defn list-worldgen-liquids
  []
  (->> (get-in @(fw/fw-atom) [:registry :worldgen])
       (filter (fn [[k _]] (and (vector? k) (= ::liquid (first k)))))
       (mapv val)))

(defn reset-worldgen-liquids-for-test!
  []
  (swap! (fw/fw-atom) update-in [:registry :worldgen]
    (fn [m] (into {} (remove (fn [[k _]] (and (vector? k) (= ::liquid (first k))))) m)))
  nil)
