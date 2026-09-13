(ns cn.li.mcmod.runtime.block-runtime-provider
  (:require [cn.li.mcmod.block.query :as query]
            [cn.li.mcmod.block.tile-dsl :as tiles]
            [cn.li.mcmod.block.tile-kind :as kinds]
            [cn.li.mcmod.block.blockstate-properties :as properties]))

(defn runtime-provider [_]
  {:get-block-spec #'query/get-block-spec
   :list-all-blocks #'query/list-all-blocks
   :identify-block-from-full-name #'query/identify-block-from-full-name
   :is-part-block? #'query/is-part-block?
   :has-block-event-handler? #'query/has-block-event-handler?
   :snapshot-tiles-by-id #'tiles/snapshot-tiles-by-id
   :register-tile-capability-keys! #'tiles/register-tile-capability-keys!
   :merge-tile-kind-defaults #'kinds/merge-tile-kind-defaults
   :create-property-registry #'properties/create-property-registry
   :register-block-properties! #'properties/register-block-properties!
   :get-property #'properties/get-property
   :get-all-properties #'properties/get-all-properties})
