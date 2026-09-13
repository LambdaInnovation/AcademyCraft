(ns cn.li.mcbase.datagen.item-model-patterns
  "Shared item model metadata helpers for datagen.

  Keeps pure data selection and naming logic loader-agnostic so Forge/Fabric
  providers can share the same model discovery rules."
  (:require [clojure.string :as str]))

(defn registry-model-basename
  "Normalize an item id into a safe model basename."
  [item-id]
  (str/replace (str item-id) #"-" "_"))

(defn energy-tier-model-spec
  "Build derived model names for energy-tier items.

  Returns a map with keys:
  - :base
  - :empty-texture
  - :half-texture
  - :full-texture
  - :half-model
  - :full-model"
  [item-id {:keys [texture-empty texture-half texture-full]}]
  (let [base (registry-model-basename item-id)]
    {:base base
     :empty-texture (or texture-empty (str base "_empty"))
     :half-texture (or texture-half (str base "_half"))
     :full-texture (or texture-full (str base "_full"))
     :half-model (str base "_half")
     :full-model (str base "_full")}))

(defn simple-model-spec
  "Normalize a simple item model spec into a portable map.

  Optional `:item-model-display` in `:properties` becomes model JSON `display`
  (perspective → {:rotation :translation :scale}), matching Forge/vanilla
  item-model camera transforms."
  [item-name item-spec]
  (when-let [texture (get-in item-spec [:properties :model-texture])]
    (cond-> {:item-name (str item-name)
             :model-texture texture
             :model-parent (get-in item-spec [:properties :model-parent] "item/generated")}
      (map? (get-in item-spec [:properties :item-model-display]))
      (assoc :display (get-in item-spec [:properties :item-model-display])))))

(defn damage-frame-variant?
  "Predicate: does an item spec request a damage-variant model with a frame
   animation on the filled variant (upstream ItemMatterUnit)?"
  [item-spec]
  (boolean (get-in item-spec [:properties :item-model-damage-frame])))

(defn energy-tier-item?
  "Predicate: does an item spec request energy-tier models?"
  [item-spec]
  (boolean (get-in item-spec [:properties :item-model-energy-levels])))

(defn obj-3d-item?
  "Predicate: does an item spec request a forge:obj 3D model?"
  [item-spec]
  (boolean (get-in item-spec [:properties :item-model-3d-obj])))

(defn obj-3d-model-spec
  "Build the 3D OBJ model spec from item DSL properties.
  Returns a map with :model-name (suffixed with _3d), :obj-model, :texture, :display.

  The texture default lives under `item/` because the mesh samples an atlas, and
  the block/item atlases only stitch `textures/block` and `textures/item`."
  [item-id {:keys [obj-model texture display]}]
  (let [base (registry-model-basename item-id)]
    {:model-name (str base "_3d")
     :obj-model (or obj-model (str "models/" base ".obj"))
     :texture (or texture (str "item/" base "_3d"))
     :display (or display {})}))
