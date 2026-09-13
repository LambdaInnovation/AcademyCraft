(ns cn.li.mcbase.datagen.block-tag-provider-core
  "DataProvider for modern vanilla block mining tags derived from block DSL metadata."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mcbase.datagen.gson-util :as gson-util])
  (:import [net.minecraft.data CachedOutput DataProvider PackOutput]
           [com.google.gson Gson]
           [java.nio.file Path]
           [java.util.concurrent CompletableFuture]))

(def ^:private ^Gson gson (gson-util/create-pretty-gson))

(defn- block-id [registry-name]
  (str modid/mod-id ":" registry-name))

(defn- tag-values [predicate]
  (->> (registry-metadata/get-all-block-ids)
       (keep (fn [id]
               (let [spec (registry-metadata/get-block-spec id)
                     physical (:physical spec)
                     registry-name (registry-metadata/get-block-registry-name id)]
                 (when (predicate physical)
                   (block-id registry-name)))))
       sort
       vec))

(defn create [^PackOutput pack-output]
  (let [root (.getOutputFolder pack-output)
        base (.resolve ^Path root "data/minecraft/tags/blocks")
        tags {"mineable/pickaxe" (tag-values #(and (:requires-tool %) (= :pickaxe (:harvest-tool %))))
              "needs_stone_tool" (tag-values #(and (:requires-tool %) (= 1 (:harvest-level %))))
              "needs_iron_tool" (tag-values #(and (:requires-tool %) (= 2 (:harvest-level %))))
              "needs_diamond_tool" (tag-values #(and (:requires-tool %) (= 3 (:harvest-level %))))}]
    (reify DataProvider
      (^CompletableFuture run [_ ^CachedOutput cached]
        (let [writes (for [[tag values] tags
                           :let [target (.resolve base (str tag ".json"))
                                 payload {:replace false :values values}]]
                       (DataProvider/saveStable cached
                         (.toJsonTree gson (gson-util/normalize-json payload))
                         target))]
          (CompletableFuture/allOf (into-array CompletableFuture writes))))
      (getName [_] (str modid/mod-id " Block Tag Provider")))))
