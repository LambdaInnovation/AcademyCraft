(ns cn.li.mcbase.datagen.metadata-resolver
  "Shared datagen metadata resolution helpers.

  Item lookups via RegistryValues; ingredients via Ingredients seam."
  (:import [cn.li.mcver Ingredients RegistryValues]
           [net.minecraft.core HolderGetter]
           [net.minecraft.core.registries Registries]
           [net.minecraft.tags TagKey]
           [net.minecraft.world.item.crafting Ingredient]
           [net.minecraft.world.level ItemLike]))

(defn resolve-item
  "Resolve item ID string to Minecraft ItemLike. Throws if missing or AIR."
  ^ItemLike [item-id parse-rl-fn]
  (let [item (RegistryValues/getItem (parse-rl-fn item-id))]
    (when (nil? item)
      (throw (ex-info "Unknown item id in recipe metadata" {:item-id item-id})))
    item))

(defn resolve-tag
  "Resolve tag ID string to TagKey for the item registry."
  ^TagKey [tag-id parse-rl-fn]
  (TagKey/create Registries/ITEM (parse-rl-fn tag-id)))

(defn ingredient-from-spec
  "Create Ingredient from {:item ...} or {:tag ...}.

  Optional third arg is HolderGetter<Item> (required for :tag on 26.2)."
  (^Ingredient [spec parse-rl-fn]
   (ingredient-from-spec spec parse-rl-fn nil))
  (^Ingredient [spec parse-rl-fn ^HolderGetter items]
   (cond
     (:item spec)
     (Ingredients/ofItem (resolve-item (:item spec) parse-rl-fn))

     (:tag spec)
     (Ingredients/ofTag (resolve-tag (:tag spec) parse-rl-fn) items)

     :else
     (throw (ex-info "Invalid ingredient spec" {:spec spec})))))
