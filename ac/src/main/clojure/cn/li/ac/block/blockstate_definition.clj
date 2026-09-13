(ns cn.li.ac.block.blockstate-definition
  "BlockState定义层 - 独立于平台

   定义所有block的blockstate结构、属性和model对应关系。
   这些定义在core中，可被所有平台（forge, fabric）使用。

  每个平台的datagen实现可以根据这些定义调用对应的API生成JSON文件。

  Node-specific blockstate logic lives in wireless-node/blockstate.clj.
  Schema-backed machine multipart definitions live in blockstate-datagen.clj."
  (:require [cn.li.ac.block.blockstate-datagen :as schema-datagen]
            [cn.li.mcmod.config :as mcmod-config]
            [cn.li.mcmod.protocol.metadata :as registry-metadata]
            [cn.li.ac.block.wireless-node.blockstate :as node-blockstate]))

;; ============================================================================
;; BlockState部分定义
;; ============================================================================

(defrecord BlockStatePart
  [condition models])

(defrecord BlockStateDefinition
  [registry-name
   properties
   parts])

;; ============================================================================
;; Block定义常量
;; ============================================================================

(defn- mod-id []
  mcmod-config/mod-id)

(defn- plain->definition
  [{:keys [registry-name properties parts]}]
  (->BlockStateDefinition registry-name properties parts))

(defn- complex-blocks
  []
  (into {}
        (map (fn [[block-key plain]]
               [block-key (plain->definition plain)])
             (schema-datagen/complex-definitions-map))))

;; 简单 block = 所有未在复杂 multipart / node 定义中的 block
(defn- simple-blocks
  []
  (let [complex-block-keys (into (schema-datagen/complex-block-keys)
                                 (keys (node-blockstate/get-all-node-definitions)))]
    (into {}
          (for [block-id (registry-metadata/get-all-block-ids)
                :let [block-key (keyword block-id)
                      registry-name (registry-metadata/get-block-registry-name block-id)]
                :when (not (contains? complex-block-keys block-key))]
            [block-key
             (->BlockStateDefinition
              registry-name
              {}
              [{:condition nil
                :models [(str (mod-id) ":block/" registry-name)]}])]))))

;; ============================================================================
;; 模型纹理配置 (业务逻辑)
;; ============================================================================

(defn get-model-cube-texture-config
  "Get explicit per-face cube textures for generated models.

   Returns nil when model uses default handling."
  [model-name]
  (schema-datagen/get-model-cube-texture-config model-name))

(defn get-model-texture-config
  [model-name]
  (or (node-blockstate/get-node-model-texture-config model-name)
      nil))

;; ============================================================================
;; 查询接口
;; ============================================================================

(defn get-block-state-definition
  [block-key]
  (or (get (complex-blocks) block-key)
      (get (simple-blocks) block-key)
      (node-blockstate/get-node-blockstate-definition block-key)))

(defn get-all-definitions
  []
  (merge (simple-blocks) (complex-blocks) (node-blockstate/get-all-node-definitions)))

(defn get-definitions-for-platform
  [_platform]
  (get-all-definitions))

(defn is-multipart-block?
  [definition]
  (> (count (:parts definition)) 1))

(defn is-node-block?
  [registry-name]
  (node-blockstate/is-node-block? registry-name))

(defn get-item-model-id
  [mod-id registry-name]
  (cond
    (node-blockstate/is-node-block? registry-name)
    (node-blockstate/get-node-item-model-id mod-id registry-name)

    :else
    (or (schema-datagen/default-item-model-id registry-name)
        (str mod-id ":block/" registry-name))))
