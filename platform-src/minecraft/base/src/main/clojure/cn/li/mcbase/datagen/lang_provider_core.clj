(ns cn.li.mcbase.datagen.lang-provider-core
  "Shared language datagen helpers used by platform providers."
  (:require [cn.li.mcbase.datagen.lang-data :as lang-data]
            [cn.li.mcmod.datagen.metadata :as datagen-metadata])
  (:import [java.util.concurrent CompletableFuture]))

(defn merged-language-data
  []
  (lang-data/merged-lang-data datagen-metadata/get-translation-maps))

(defn language-map
  [lang-code]
  (get (merged-language-data)
       (str lang-code ".json")
       {}))

(defn merged-language-entries
  []
  (seq (merged-language-data)))

(defn language-entry
  [lang-code]
  [(str lang-code ".json")
   (language-map lang-code)])

(defn save-language-files!
  "Emit language file save futures through `emit-entry!`, which receives
  `[file-name data]` pairs and must return a CompletableFuture."
  [entries emit-entry!]
  (let [writes (mapv (fn [[file-name data]]
                       (emit-entry! file-name data))
                     entries)]
    (CompletableFuture/allOf (into-array CompletableFuture writes))))