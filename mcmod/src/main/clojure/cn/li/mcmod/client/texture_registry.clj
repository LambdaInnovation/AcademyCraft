(ns cn.li.mcmod.client.texture-registry
  "Platform-neutral registry for named texture paths.

  Content modules register keyword → path-string mappings during client init.
  Platform components resolve path strings to
  platform-specific texture handles at render time."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.registry :as registry]))

(defn register-texture!
  "Register a named texture path. key is a keyword; path is a string of the form
   'textures/guis/...' or 'mod-id:textures/guis/...'."
  [key path]
  (registry/register! (fw/fw-atom) :textures key (str path))
  nil)

(defn get-texture-path
  "Return the path string registered under key, or nil."
  [key]
  (registry/get-spec (fw/fw-atom) :textures key))

(defn reset-texture-registry-for-test!
  []
  (registry/reset-domain-for-test! (fw/fw-atom) :textures)
  nil)
