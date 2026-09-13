(ns cn.li.mcbase.datagen.blockstate-support
  "Shared helpers for blockstate/model/item-model datagen JSON payloads."
  (:require [clojure.string :as str]))

(defn normalize-texture-id
  [texture]
  (str texture))

(defn model-id->model-name
  [model-id]
  (str/replace (str model-id) #".*:block/" ""))

(defn- normalize-condition-value
  [v]
  (cond
    (keyword? v) (name v)
    :else (str v)))

(defn simple-blockstate-json
  [model-id]
  {:variants
   {"" {:model (str model-id)}}})

(defn multipart-blockstate-json
  [parts]
  {:multipart
   (mapv (fn [{:keys [condition models]}]
           (let [entry {:apply {:model (str (first models))}}]
             (if (map? condition)
               (assoc entry :when (reduce-kv (fn [m k v]
                                                (assoc m (name k) (normalize-condition-value v)))
                                              {}
                                              condition))
               entry)))
         parts)})

(defn cube-all-model-json
  [texture-all]
  {:parent "minecraft:block/cube_all"
   :textures {:all (normalize-texture-id texture-all)}})

(defn cube-model-json
  [{:keys [down up north south east west]}]
  {:parent "minecraft:block/cube"
   :textures {:down (normalize-texture-id down)
              :up (normalize-texture-id up)
              :north (normalize-texture-id north)
              :south (normalize-texture-id south)
              :east (normalize-texture-id east)
              :west (normalize-texture-id west)}})

(defn parent-model-json
  ([parent]
   {:parent (str parent)})
  ([parent textures]
   (cond-> {:parent (str parent)}
     (seq textures) (assoc :textures (reduce-kv (fn [m k v]
                                           (assoc m (name k) (normalize-texture-id v)))
                                           {} textures)))))

(defn flat-item-model-json
  [layer0]
  {:parent "minecraft:item/generated"
   :textures {:layer0 (normalize-texture-id layer0)}})
