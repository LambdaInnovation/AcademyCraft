(ns cn.li.mcbase.datagen.advancement-provider-shell
  "Shared DataProvider shell for advancement JSON from achievement metadata."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.mcbase.datagen.gson-util :as gson-util]
            [cn.li.mcbase.datagen.advancement-provider-core :as adv-core]
            [cn.li.mcbase.datagen.item-registry :as item-registry]
            [cn.li.mcmod.datagen.metadata :as datagen-metadata]
            [cn.li.platform.registry.metadata :as protocol-metadata])
  (:import [com.google.gson Gson JsonElement]
           [java.nio.file Path]
           [java.util.concurrent CompletableFuture]
           [net.minecraft.data CachedOutput DataProvider PackOutput]))

(def ^:private ^Gson gson
  (gson-util/create-pretty-gson))

(defn- make-known-item-ids
  []
  (item-registry/known-item-ids
   protocol-metadata/get-all-item-ids
   protocol-metadata/get-item-registry-name
   protocol-metadata/get-all-block-ids
   protocol-metadata/get-block-registry-name
   (str modid/mod-id)))

(defn create
  "Create a DataProvider that writes advancement JSON under the pack output root."
  ([^PackOutput pack-output]
   (create pack-output nil))
  ([^PackOutput pack-output _ignored]
   (let [out-root (.getOutputFolder pack-output)]
     (reify DataProvider
       (^CompletableFuture run [_ ^CachedOutput cached]
         (let [known (make-known-item-ids)
               tabs (datagen-metadata/get-achievement-tabs)
               all-achievements (datagen-metadata/get-achievements)
               writes (atom [])]
           (doseq [tab tabs]
             (let [root-rel (adv-core/root-path (:id tab))
                   root-json* (adv-core/root-json tab)
                   root-tree (.toJsonTree gson root-json*)
                   root-path* (.resolve ^Path out-root ^String root-rel)]
               (swap! writes conj (DataProvider/saveStable cached ^JsonElement root-tree ^Path root-path*))))
           (doseq [ach all-achievements]
             (let [root-rl (adv-core/tab-root-id (:tab ach))
                   rel-path (adv-core/ach-path (:id ach))
                   json-map (adv-core/ach-json ach root-rl known)
                   json-tree (.toJsonTree gson json-map)
                   target-path (.resolve ^Path out-root ^String rel-path)]
               (swap! writes conj (DataProvider/saveStable cached ^JsonElement json-tree ^Path target-path))))
           (CompletableFuture/allOf (into-array CompletableFuture @writes))))
       (getName [_] (str modid/mod-id " Advancement Provider"))))))
