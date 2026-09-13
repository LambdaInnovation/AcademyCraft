(ns cn.li.mcbase.client.texture-registry
  "Registry for named texture ResourceLocations/Identifiers.

   Delegates path storage to the platform-neutral mcmod registry.
   Resolves keyword->path-string entries via ResourceLocations seam."
  (:require [cn.li.platform.neutral.client-runtime :as mcmod-tex]
            [cn.li.platform.neutral.config :as modid-config]
            [clojure.string :as str])
  (:import [cn.li.mcver ResourceLocations]))

(defn register-texture!
  "Register a named texture. Delegates to the mcmod platform-neutral registry.

   key is a keyword; path is either 'textures/guis/...' (prefixed with current
   mod-id at resolve time) or 'mod-id:textures/guis/...' (explicit namespace)."
  [key path]
  (mcmod-tex/register-texture! key path))

(defn resolve-texture
  "Return the ResourceLocation/Identifier registered under key, or nil.

   Reads the path string from the mcmod registry and converts it via
   ResourceLocations. Paths without an explicit mod-id prefix are resolved
   against the current mod-id."
  [key]
  (when-let [path (mcmod-tex/get-texture-path key)]
    (let [s (str path)]
      (if (str/includes? s ":")
        (let [idx (.indexOf s ":")]
          (ResourceLocations/of (subs s 0 idx) (subs s (inc idx))))
        (ResourceLocations/of modid-config/mod-id s)))))

(defn reset-texture-registry-for-test!
  "Clear all registered textures. Intended for tests."
  []
  (mcmod-tex/reset-texture-registry-for-test!)
  nil)
