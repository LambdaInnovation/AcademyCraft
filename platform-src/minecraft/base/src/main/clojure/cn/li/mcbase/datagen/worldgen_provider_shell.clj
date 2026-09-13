(ns cn.li.mcbase.datagen.worldgen-provider-shell
  "Shared DataProvider shell bridging worldgen-provider-core to PackOutput."
  (:require [cn.li.mcbase.datagen.worldgen-provider-core :as core]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.util.log :as log])
  (:import [com.google.gson GsonBuilder Gson JsonElement]
           [java.nio.file Path]
           [java.util.concurrent CompletableFuture]
           [net.minecraft.data CachedOutput DataProvider PackOutput]))

(def ^:private ^Gson gson
  (.. (GsonBuilder.) (setPrettyPrinting) (disableHtmlEscaping) (create)))

(defn create
  "Returns a DataProvider that writes worldgen JSON for the given platform key
  (`:forge` / `:fabric` / `:neoforge`). Optional second arg is ignored."
  ([^PackOutput pack-output platform]
   (create pack-output platform nil))
  ([^PackOutput pack-output platform _ignored]
   (let [out-root (.getOutputFolder pack-output)]
     (reify DataProvider
       (getName [_] "WorldGen Provider")
       (^CompletableFuture run [_ ^CachedOutput cached]
         (let [file-defs (core/build-worldgen-file-defs :platform platform)
               writes (atom [])]
           (doseq [{:keys [path data]} file-defs]
             (let [full-path (concat ["data" (str modid/mod-id)] path)
                   rel-path (reduce #(.resolve ^Path %1 ^String %2) out-root full-path)
                   json-tree ^JsonElement (.toJsonTree gson data)]
               (swap! writes conj (DataProvider/saveStable cached json-tree rel-path))))
           (log/info "Generated worldgen DataGen files"
                     {:platform platform :file-count (count file-defs)})
           (CompletableFuture/allOf (into-array CompletableFuture @writes))))))))
