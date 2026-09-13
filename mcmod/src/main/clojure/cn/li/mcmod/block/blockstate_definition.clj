(ns cn.li.mcmod.block.blockstate-definition
  "BlockState datagen business logic (platform independent).

   This module provides basic blockstate definitions from metadata.
   The content layer overrides these with specialized multipart definitions.

   It derives all needed information from `cn.li.mcmod.protocol.metadata`,
   which is populated by the DSL namespaces at runtime."
  (:require [cn.li.mcmod.protocol.metadata :as registry-metadata]
            [cn.li.mcmod.config :as config]
            [cn.li.mcmod.runtime.install :as install]))

;; ============================================================================
;; Records
;; ============================================================================

(defrecord BlockStatePart
  [condition
   models])

(defrecord BlockStateDefinition
  [registry-name
   properties
   parts])

(declare get-all-definitions)

;; ============================================================================
;; Definitions generation (datagen)
;; ============================================================================

(defn- basic-get-all-definitions
  "Return map: block-id(keyword) -> BlockStateDefinition.
   Generates simple single-model definitions for all blocks from metadata.
   The content layer will override these with specialized multipart definitions."
  []
  (into {}
        (for [block-id (registry-metadata/get-all-block-ids)
              :let [registry-name (registry-metadata/get-block-registry-name block-id)]]
          [(keyword block-id)
           (BlockStateDefinition.
            registry-name
            {}
            [(->BlockStatePart nil
                               [(str config/mod-id ":block/" registry-name)])])])))

(defn- basic-get-block-state-definition
  [block-key]
  (or (some-> (basic-get-all-definitions) (get block-key))
      (some-> (basic-get-all-definitions) (get (keyword (name block-key))))))

(defn- basic-is-multipart-block?
  [definition]
  (> (count (:parts definition)) 1))

(defn- basic-get-item-model-id
  [mod-id registry-name]
  (str mod-id ":block/" registry-name))

(def ^:private get-all-definitions-hook
  basic-get-all-definitions)

(def ^:private get-block-state-definition-hook
  basic-get-block-state-definition)

(def ^:private is-multipart-block-hook
  basic-is-multipart-block?)

(def ^:private get-model-cube-texture-config-hook
  (fn [_model-name] nil))

(def ^:private get-model-texture-config-hook
  (fn [_model-name] nil))

(def ^:private get-item-model-id-hook
  basic-get-item-model-id)

(defn blockstate-hooks-snapshot
  []
  {:get-all-definitions get-all-definitions-hook
   :get-block-state-definition get-block-state-definition-hook
   :is-multipart-block? is-multipart-block-hook
   :get-model-cube-texture-config get-model-cube-texture-config-hook
   :get-model-texture-config get-model-texture-config-hook
   :get-item-model-id get-item-model-id-hook})

(defn restore-blockstate-hooks!
  [{:keys [get-all-definitions
           get-block-state-definition
           is-multipart-block?
           get-model-cube-texture-config
           get-model-texture-config
           get-item-model-id]}]
  (install/install-root! #'get-all-definitions-hook get-all-definitions)
  (install/install-root! #'get-block-state-definition-hook get-block-state-definition)
  (install/install-root! #'is-multipart-block-hook is-multipart-block?)
  (install/install-root! #'get-model-cube-texture-config-hook get-model-cube-texture-config)
  (install/install-root! #'get-model-texture-config-hook get-model-texture-config)
  (install/install-root! #'get-item-model-id-hook get-item-model-id)
  nil)

(defn register-blockstate-hooks!
  [hooks]
  (when-let [f (:get-all-definitions hooks)]
    (install/install-root! #'get-all-definitions-hook f))
  (when-let [f (:get-block-state-definition hooks)]
    (install/install-root! #'get-block-state-definition-hook f))
  (when-let [f (:is-multipart-block? hooks)]
    (install/install-root! #'is-multipart-block-hook f))
  (when-let [f (:get-model-cube-texture-config hooks)]
    (install/install-root! #'get-model-cube-texture-config-hook f))
  (when-let [f (:get-model-texture-config hooks)]
    (install/install-root! #'get-model-texture-config-hook f))
  (when-let [f (:get-item-model-id hooks)]
    (install/install-root! #'get-item-model-id-hook f))
  nil)

(defn get-all-definitions
  []
  (get-all-definitions-hook))

(defn get-block-state-definition
  [block-key]
  (get-block-state-definition-hook block-key))

(defn is-multipart-block?
  "Multipart when parts count > 1."
  [definition]
  (is-multipart-block-hook definition))

(defn get-model-cube-texture-config
  [model-name]
  (get-model-cube-texture-config-hook model-name))

(defn get-model-texture-config
  [model-name]
  (get-model-texture-config-hook model-name))

(defn get-item-model-id
  [mod-id registry-name]
  (get-item-model-id-hook mod-id registry-name))

;; End of file
