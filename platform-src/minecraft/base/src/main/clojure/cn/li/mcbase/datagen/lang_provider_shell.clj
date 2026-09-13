(ns cn.li.mcbase.datagen.lang-provider-shell
  "Shared DataProvider shell that emits all merged language files."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.mcbase.datagen.lang-provider-core :as lang-core]
            [cn.li.mcbase.datagen.gson-util :as gson-util])
  (:import [net.minecraft.data DataProvider CachedOutput PackOutput]
           [java.nio.file Path]
           [java.util.concurrent CompletableFuture]
           [com.google.gson Gson]))

(def ^:private ^Gson gson
  (gson-util/create-pretty-gson))

(defn create
  "Create a DataProvider that writes all merged language JSON files."
  ([^PackOutput pack-output]
   (create pack-output nil))
  ([^PackOutput pack-output _ignored]
   (let [out-root (.getOutputFolder pack-output)
         ^Path base (.resolve ^Path out-root (str "assets/" modid/mod-id "/lang"))]
     (reify DataProvider
       (^CompletableFuture run [_ ^CachedOutput cached]
         (lang-core/save-language-files!
          (lang-core/merged-language-entries)
          (fn [file-name data]
            (let [target-path (.resolve base ^String file-name)
                  json-tree (.toJsonTree gson data)]
              (DataProvider/saveStable cached json-tree target-path)))))

       (getName [_] (str modid/mod-id " Lang Provider"))))))
