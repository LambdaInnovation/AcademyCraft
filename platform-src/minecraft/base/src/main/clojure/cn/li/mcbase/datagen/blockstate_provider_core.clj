(ns cn.li.mcbase.datagen.blockstate-provider-core
  "Shared blockstate/model/item-model datagen inference helpers."
  (:require [clojure.string :as str]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.platform.registry.metadata :as blockstate-def]
            [cn.li.mcbase.datagen.blockstate-support :as bs-support]))

(defn normalize-candidates
  [s]
  (when (and s (not (str/blank? s)))
    [s
     (str/replace s "-" "_")
     (str/replace s "_" "-")]))

(defn registry-name->block-spec
  [registry-name]
  (let [candidates (distinct (normalize-candidates registry-name))]
    (or
     (some (fn [candidate]
             (registry-metadata/get-block-spec (keyword candidate)))
           candidates)
     (some (fn [block-id]
             (when (some #{(registry-metadata/get-block-registry-name block-id)} candidates)
               (registry-metadata/get-block-spec block-id)))
           (or (registry-metadata/get-all-block-ids) [])))))

(defn item-texture-from-spec
  [registry-name]
  (let [block-spec (registry-name->block-spec registry-name)]
    (or (get-in block-spec [:rendering :item-texture])
        (get-in block-spec [:rendering :textures :all])
        (get-in block-spec [:rendering :texture])
        (str modid/mod-id ":block/" registry-name))))

(defn block-texture-from-spec
  [registry-name]
  (let [block-spec (registry-name->block-spec registry-name)]
    (or (get-in block-spec [:rendering :textures :all])
        (get-in block-spec [:rendering :texture])
        (str modid/mod-id ":block/" registry-name))))

(defn block-parent-from-spec
  [registry-name]
  (let [block-spec (registry-name->block-spec registry-name)]
    (or (get-in block-spec [:rendering :model-parent])
        "minecraft:block/cube_all")))

(defn model-id->model-name
  [model-id]
  (bs-support/model-id->model-name model-id))

(defn model-id->registry-name
  [model-name]
  (let [from-node (when (and (str/starts-with? (str model-name) "node_")
                             (blockstate-def/get-model-texture-config model-name))
                    (second (re-find #"^(node_[^_]+)" model-name)))]
    (or from-node
        model-name)))

(defn block-model-spec
  [model-name]
  (if-let [cube-config (blockstate-def/get-model-cube-texture-config model-name)]
    {:kind :cube
     :textures cube-config
     :particle (:north cube-config)}
    (if-let [texture-config (blockstate-def/get-model-texture-config model-name)]
      ;; particle must be explicit: the vanilla cube parent's own particle is
      ;; "#particle", which no child texture ever fills, so without it the
      ;; breaking overlay and break particles render the purple-black missing
      ;; texture (wireless-node blocks).
      {:kind :cube
       :textures {:down (:vert texture-config)
                  :up (:vert texture-config)
                  :north (:side texture-config)
                  :south (:side texture-config)
                  :east (:side texture-config)
                  :west (:side texture-config)}
       :particle (:side texture-config)}
      (let [registry-name (model-id->registry-name model-name)
            block-spec (registry-name->block-spec registry-name)
            parent (block-parent-from-spec registry-name)
            explicit-texture (or (get-in block-spec [:rendering :textures :all])
                                 (get-in block-spec [:rendering :texture]))]
        (if (or explicit-texture (not= parent "minecraft:block/cube_all"))
          {:kind :parent
           :parent parent
           ;; cube_all's own "particle": "#all" resolves through the child's
           ;; "all" texture; other parents (block/block) declare no particle,
           ;; so without an explicit one the breaking overlay and break
           ;; particles render the purple-black missing sprite.
           :textures (cond-> (when explicit-texture {:all explicit-texture})
                       (and explicit-texture (not= parent "minecraft:block/cube_all"))
                       (assoc :particle explicit-texture))}
          {:kind :cube-all
           :texture (block-texture-from-spec registry-name)})))))

(defn block-model-json
  [model-name]
  (let [{:keys [kind textures particle parent texture]} (block-model-spec model-name)]
    (case kind
      :cube (cond-> (bs-support/cube-model-json textures)
              particle (assoc-in [:textures :particle] (bs-support/normalize-texture-id particle)))
      :parent (bs-support/parent-model-json parent textures)
      :cube-all (bs-support/cube-all-model-json texture))))

(defn item-model-json
  [registry-name multipart?]
  (let [block-spec (registry-name->block-spec registry-name)
        flat-item? (true? (get-in block-spec [:rendering :flat-item-icon?]))
  item-model-id (blockstate-def/get-item-model-id modid/mod-id registry-name)]
    (if (and flat-item? (not multipart?))
      (bs-support/flat-item-model-json (item-texture-from-spec registry-name))
      (bs-support/parent-model-json item-model-id))))

(defn blockstate-write-entries
  "Build platform-neutral write entries for blockstates/models/item-models.

  Returns a sequence of maps:
  {:path-key :blockstate|:block-model|:item-model
   :id       \"<namespace:path>\"
   :payload  <json-like map>}"
  []
  (let [definitions (or (blockstate-def/get-all-definitions) [])]
    (mapcat
     (fn [[block-key definition]]
       (let [registry-name (:registry-name definition)
             blockstate-id (str modid/mod-id ":" registry-name)
             first-model (some-> definition :parts first :models first)
             blockstate-json (if (blockstate-def/is-multipart-block? definition)
                               (bs-support/multipart-blockstate-json (:parts definition))
                               (bs-support/simple-blockstate-json first-model))
             model-ids (->> (:parts definition)
                            (mapcat #(get % :models))
                            distinct)
             block-model-entries (for [model-id model-ids
                                       :let [model-name (model-id->model-name model-id)
                                             model-id-str (str modid/mod-id ":" model-name)
                                             model-json (block-model-json model-name)]]
                                   {:path-key :block-model
                                    :id model-id-str
                                    :payload model-json})
             item-model-entries (when (registry-metadata/should-create-block-item? (name block-key))
                      (let [item-model-id (blockstate-def/get-item-model-id modid/mod-id registry-name)
                                        item-model-name (model-id->model-name item-model-id)
                                        known-model-ids (set model-ids)
                                        item-block-model-entry (when-not (contains? known-model-ids item-model-id)
                                                                 {:path-key :block-model
                                                                  :id (str modid/mod-id ":" item-model-name)
                                                                  :payload (block-model-json item-model-name)})
                                        item-id (str modid/mod-id ":" registry-name)
                                        multipart? (blockstate-def/is-multipart-block? definition)]
                                    (concat
                                     (when item-block-model-entry [item-block-model-entry])
                                     [{:path-key :item-model
                                       :id item-id
                                       :payload (item-model-json registry-name multipart?)}])))]
         (concat
          [{:path-key :blockstate
            :id blockstate-id
            :payload blockstate-json}]
          block-model-entries
          item-model-entries)))
     definitions)))
