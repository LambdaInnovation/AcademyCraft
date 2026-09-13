(ns hooks.forbid-loader-api
  "Flag :import of Minecraft/Forge/Fabric/NeoForge APIs in neutral layers (ac/mcmod).

  Clojurephant reflection=fail and Reflection Guard only catch untyped interop
  warnings — they do not see (:import [net.minecraft...]). Platform layers may
  import those APIs; ac/mcmod must not."
  (:require [clj-kondo.hooks-api :as api]
            [clojure.string :as str]))

(def ^:private forbidden-package-re
  #"^net\.(minecraft|minecraftforge|fabricmc|neoforged)\.")

(defn- neutral-layer-file?
  [filename]
  (when filename
    (let [f (-> filename (str/replace "\\" "/") str/lower-case)]
      (boolean (or (str/includes? f "/ac/src/")
                   (str/includes? f "/mcmod/src/"))))))

(defn- forbidden-java-name?
  [sym-or-str]
  (boolean (re-find forbidden-package-re (str sym-or-str))))

(defn- report-forbidden!
  [node filename name]
  (api/reg-finding!
   (assoc (meta node)
          :filename filename
          :message (str "Neutral layer (ac/mcmod) must not import loader/Minecraft API: "
                        name
                        ". Keep net.minecraft.* / Forge / Fabric / NeoForge in platform-src only.")
          :type :loader-api-in-neutral-layer)))

(defn- check-import-form!
  [form filename]
  (cond
    (api/token-node? form)
    (when (forbidden-java-name? (api/sexpr form))
      (report-forbidden! form filename (api/sexpr form)))

    (or (api/vector-node? form) (api/list-node? form))
    (doseq [child (:children form)]
      (when (and (api/token-node? child)
                 (forbidden-java-name? (api/sexpr child)))
        (report-forbidden! child filename (api/sexpr child))))))

(defn analyze-ns
  "clj-kondo hook for (ns ...). Errors on forbidden :import in ac/mcmod sources."
  [{:keys [node filename]}]
  (when (neutral-layer-file? filename)
    (doseq [child (rest (:children node))]
      (when (api/list-node? child)
        (let [head (first (:children child))]
          (when (and head (api/keyword-node? head) (= :import (api/sexpr head)))
            (doseq [form (rest (:children child))]
              (check-import-form! form filename)))))))
  {:node node})
