(ns cn.li.mcbase.datagen.blockstate-provider-shell
  "Shared DataProvider shell for blockstate/block-model/item-model datagen.
  Uses PackOutput PathProviders; id type via ResourceLocations seam."
  (:require [cn.li.mcbase.datagen.blockstate-provider-core :as blockstate-core]
            [cn.li.mcbase.datagen.resource-location :as rl]
            [cn.li.mcbase.datagen.gson-util :as gson-util])
  (:import [cn.li.mcver ResourceLocations]
           [net.minecraft.data CachedOutput DataProvider PackOutput PackOutput$PathProvider PackOutput$Target]
           [com.google.gson Gson JsonElement]
           [java.util.concurrent CompletableFuture]))

(def ^:private ^Gson gson (gson-util/create-pretty-gson))

(defn- parse-rl
  [s]
  (rl/parse-resource-location s))

(defn- write-json!
  [^CachedOutput cached-output ^PackOutput$PathProvider path-provider id payload]
  (DataProvider/saveStable
   cached-output
   ^JsonElement (.toJsonTree gson (gson-util/normalize-json payload))
   ;; Via the seam: `id` is the version's own resource-location type, which
   ;; this shared shell cannot name, and 26.2 overloads json().
   (ResourceLocations/jsonPath path-provider id)))

(defn create-provider
  [^PackOutput output provider-name]
  (let [blockstate-path-provider (.createPathProvider output PackOutput$Target/RESOURCE_PACK "blockstates")
        block-model-path-provider (.createPathProvider output PackOutput$Target/RESOURCE_PACK "models/block")
        item-model-path-provider (.createPathProvider output PackOutput$Target/RESOURCE_PACK "models/item")]
    (reify DataProvider
      (^CompletableFuture run [_this ^CachedOutput cached-output]
        (let [block-writes (for [{:keys [path-key id payload]} (blockstate-core/blockstate-write-entries)
                                 :let [rl-id (parse-rl id)]]
                             (case path-key
                               :blockstate (write-json! cached-output blockstate-path-provider rl-id payload)
                               :block-model (write-json! cached-output block-model-path-provider rl-id payload)
                               :item-model (write-json! cached-output item-model-path-provider rl-id payload)
                               (throw (ex-info "Unknown blockstate write path" {:path-key path-key :id id}))))]
          (CompletableFuture/allOf
           (into-array CompletableFuture block-writes))))

      (getName [_this]
        provider-name))))
