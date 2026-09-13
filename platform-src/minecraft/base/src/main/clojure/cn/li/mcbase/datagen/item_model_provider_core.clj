(ns cn.li.mcbase.datagen.item-model-provider-core
  "Shared item model datagen helpers for loader-specific providers."
  (:require [clojure.string :as str]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.item.dsl :as item-dsl]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mcbase.datagen.item-model-patterns :as item-model-patterns]))

(defn- texture-path
  "Build a full texture ResourceLocation string from a texture name.
  If the name already contains a colon, return it as-is (full path e.g.
  'academy:block/phase_liquid'). Otherwise prepend modid:item/."
  [texture-name]
  (if (str/includes? (str texture-name) ":")
    (str texture-name)
    (str (str modid/mod-id) ":item/" texture-name)))

(defn- normalize-parent
  [parent]
  (str (or parent "item/generated")))

(defn- simple-model-entry
  [{:keys [item-name model-texture model-parent display]}]
  {:model-name item-name
   :json (cond-> {:parent (normalize-parent model-parent)
                  :textures {:layer0 (texture-path model-texture)}}
           (map? display) (assoc :display display))})

(defn- damage-frame-model-entries
  "Model specs for a damage-variant item with a frame animation on the filled
   variant (upstream ItemMatterUnit, ACItems.java): damage 0 → empty-unit
   model, damage 1 → filled model driven by the custom `frame` property
   (frame = time*4 mod 4, a 4-frame flowing-liquid animation).

   The variant predicate is the custom `<modid>:matter_kind` (reads
   ItemStack damage directly) — NOT the vanilla `minecraft:damage` predicate,
   which divides by maxDamage and is unusable on stackable items (matter
   units stack to 16, so they carry no durability). Upstream keys its
   per-damage models off the same damage field.

   Overrides are emitted in ASCENDING threshold order. This is the order
   vanilla's loader pipeline needs: BlockModel hands the JSON list to
   ItemOverrides, whose constructor iterates it BACKWARDS (1.20.1/1.21.1
   bytecode: i = size-1 → 0), and resolve() walks the resulting array
   FORWARD picking the FIRST override whose value >= threshold. Ascending
   JSON therefore yields a descending array = 'largest threshold first'
   match. The Forge/NeoForge datagen builders must NOT re-reverse when
   writing (older providers did, producing descending JSON — the frame
   animation froze on the lowest-threshold override).

   Returns the animation frames first, then the filled model, then the base
   model LAST — the Forge/NeoForge datagen builders construct
   ModelFile$ExistingModelFile for every override target and the model file
   must already be written, so every referenced model precedes its referrer
   (same ordering as energy-tier-model-entries)."
  [item-id props]
  (let [mod-id (str modid/mod-id)
        tex (fn [t] (if (str/includes? (str t) ":") (str t) (str mod-id ":item/" t)))
        {:keys [texture-empty texture-filled frames frame-predicate damage-predicate damage-threshold]} props
        frame-names (vec (or frames []))
        base-name (str item-id)
        filled-name (or (when (seq texture-filled)
                          (last (str/split (str texture-filled) #"/")))
                        (str base-name "_filled"))
        model-rl (fn [t] (str mod-id ":item/" (last (str/split (str t) #"/"))))
        frame-overrides (mapv (fn [idx frame-tex]
                                {:predicate {(str frame-predicate) (double (inc idx))}
                                 :model (model-rl frame-tex)})
                              (range (count frame-names)) frame-names)]
    (concat
      (mapv (fn [frame-tex]
              {:model-name (last (str/split (str frame-tex) #"/"))
               :json {:parent "item/generated"
                      :textures {:layer0 (tex frame-tex)}}})
            frame-names)
      [{:model-name filled-name
        :json (cond-> {:parent "item/generated"
                       :textures {:layer0 (tex texture-filled)}}
                (seq frame-overrides) (assoc :overrides frame-overrides))}
       {:model-name base-name
        :json {:parent "item/generated"
               :textures {:layer0 (tex texture-empty)}
               :overrides [{:predicate {(str (or damage-predicate "academy:matter_kind"))
                                        (double (or damage-threshold 1.0))}
                            :model (model-rl filled-name)}]}}])))

(defn- energy-tier-model-entries
  [item-id {:keys [texture-empty texture-half texture-full]}]
  (let [{:keys [base empty-texture half-texture full-texture half-model full-model]}
        (item-model-patterns/energy-tier-model-spec item-id {:texture-empty texture-empty
                                                             :texture-half texture-half
                                                             :texture-full texture-full})
        mod-id (str modid/mod-id)]
    [{:model-name half-model
      :json {:parent "item/generated"
             :textures {:layer0 (texture-path half-texture)}}}
     {:model-name full-model
      :json {:parent "item/generated"
             :textures {:layer0 (texture-path full-texture)}}}
     {:model-name base
      :json {:parent "item/generated"
             :textures {:layer0 (texture-path empty-texture)}
             ;; ASCENDING thresholds — see damage-frame-model-entries for why
             ;; the JSON must list ascending (ItemOverrides bakes reversed,
             ;; resolve walks forward, first match = highest threshold).
             :overrides [{:predicate {(str mod-id ":energy") 0.5}
                          :model (str mod-id ":item/" half-model)}
                         {:predicate {(str mod-id ":energy") 1.0}
                          :model (str mod-id ":item/" full-model)}]}}]))

(defn- fluid-bucket-model-entries
  "Generate item model entries for fluid bucket items from the fluid DSL.
  Bucket items are registered via Forge's fluid registration, not in item-dsl,
  so the standard gather-model-specs path misses them."
  []
  (keep (fn [fluid-id]
          (when-let [fluid-spec (registry-metadata/get-fluid-spec fluid-id)]
            (when (get-in fluid-spec [:block :has-bucket?])
              (let [still-texture (get-in fluid-spec [:rendering :still-texture])
                    bucket-registry-name (get-in fluid-spec [:block :bucket-registry-name])]
                (when (and bucket-registry-name still-texture)
                  {:model-name bucket-registry-name
                   :json {:parent "item/generated"
                          :textures {:layer0 (if (str/includes? still-texture ":")
                                               still-texture
                                               (str modid/mod-id ":item/" still-texture))}}})))))
        (registry-metadata/get-all-fluid-ids)))

(defn flatten-nested-override-chains
  "Legacy-loaders-only transform: flatten nested single-chain overrides into
  one flat override list with COMBINED predicates.

  Vanilla 1.20.1/1.21.1 ItemOverrides.resolve resolves exactly ONE override
  level — the model selected by an override is returned as-is and its own
  overrides are never applied. The damage-frame pattern (base model →
  filled model → per-frame models) therefore freezes on the filled model's
  base frame: `matter_kind` resolves and picks the filled model, but the
  filled model's `frame` overrides never run.

  This flattens that chain onto the base model: each frame override becomes
  an entry whose predicate merges the parent's (matter_kind) with its own
  (frame). The entries are ordered most-specific LAST: the ItemOverrides
  constructor bakes the JSON list in reverse and resolve() walks the array
  forward picking the first all-matchers-pass, so the last JSON entry (frame
  3) is checked first.

  The 26.2 providers must NOT use this — their minecraft:range_dispatch
  trees resolve recursively and need the nested structure."
  [models]
  (let [by-name (into {} (map (fn [s] [(str (:model-name s)) s])) models)
        basename (fn [s] (some-> s str (str/split #"/") last))]
    (mapv
      (fn [{:keys [model-name json] :as spec}]
        (let [overrides (:overrides json)
              single (when (= 1 (count overrides)) (first overrides))
              target (when single (get by-name (basename (:model single))))]
          (if (and target (seq (:overrides (:json target))))
            (let [frame-overrides (:overrides (:json target))]
              (assoc-in spec [:json :overrides]
                        (vec (concat overrides
                                     (mapv (fn [{:keys [predicate model]}]
                                             {:predicate (merge (:predicate single) predicate)
                                              :model model})
                                           frame-overrides)))))
            spec)))
      models)))

(defn item-model-tree
  "Recursively build a 1.21.4+ item-model tree for a model spec: each override
   list (single predicate id) becomes a nested minecraft:range_dispatch keyed
   on that property; a model with no overrides is a plain model reference.
   Shared by the 26.2 providers — covers energy tiers (one property) and the
   matter-unit damage+frame chain (nested: damage dispatch → filled model →
   frame dispatch → per-frame models)."

  [specs-by-name model-name]
  (let [spec (get specs-by-name model-name)
        overrides (seq (:overrides (:json spec)))]
    (if overrides
      (let [by-prop (group-by (fn [o] (str (ffirst (:predicate o)))) overrides)]
        (reduce-kv
          (fn [fallback pred-id entries]
            {:type "minecraft:range_dispatch"
             :property pred-id
             :fallback fallback
             :entries (->> entries
                           (map (fn [{:keys [predicate model]}]
                                  (let [[_ thr] (first predicate)
                                        target (some-> model str (str/split #"/") last)]
                                    {:threshold (double thr)
                                     :model (if (and target (get specs-by-name target))
                                              (item-model-tree specs-by-name target)
                                              {:type "minecraft:model" :model (str model)})})))
                           (sort-by :threshold)
                           vec)})
          {:type "minecraft:model"
           :model (str (str modid/mod-id) ":item/" model-name)}
          by-prop))
      {:type "minecraft:model"
       :model (str (str modid/mod-id) ":item/" model-name)})))

(defn gather-model-specs
  []
  (let [all-item-names (item-dsl/list-items)
        energy-tier-items (filter #(item-model-patterns/energy-tier-item? (item-dsl/get-item %))
                                  all-item-names)
        damage-frame-items (filter #(item-model-patterns/damage-frame-variant? (item-dsl/get-item %))
                                   all-item-names)
        obj-3d-items (filter #(item-model-patterns/obj-3d-item? (item-dsl/get-item %))
                              all-item-names)
        simple-items (keep (fn [item-name]
                             (let [item-spec (item-dsl/get-item item-name)]
                               (when-not (or (item-model-patterns/energy-tier-item? item-spec)
                                            (item-model-patterns/damage-frame-variant? item-spec)
                                            (item-model-patterns/obj-3d-item? item-spec))
                                 (item-model-patterns/simple-model-spec item-name item-spec))))
                           all-item-names)
        bucket-entries (fluid-bucket-model-entries)]
    {:all-item-count (count all-item-names)
     :energy-tier-count (count energy-tier-items)
     :damage-frame-count (count damage-frame-items)
     :obj-3d-count (count obj-3d-items)
     :simple-count (count simple-items)
     :bucket-count (count bucket-entries)
     :models (vec
              (concat
               (mapcat (fn [item-name]
                         (energy-tier-model-entries item-name
                                                    (get-in (item-dsl/get-item item-name)
                                                            [:properties :item-model-energy-levels])))
                       energy-tier-items)
               (mapcat (fn [item-name]
                         (damage-frame-model-entries
                           item-name
                           (get-in (item-dsl/get-item item-name)
                                   [:properties :item-model-damage-frame])))
                       damage-frame-items)
               (map (fn [item-name]
                      (let [item-spec (item-dsl/get-item item-name)]
                        (item-model-patterns/obj-3d-model-spec
                          item-name
                          (get-in item-spec [:properties :item-model-3d-obj]))))
                    obj-3d-items)
               (map simple-model-entry simple-items)
               bucket-entries))}))
