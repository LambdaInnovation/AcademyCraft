(ns cn.li.mcmod.config
  "Mod-id and resource helper utilities shared across platforms.
   The mod-id value is sourced from cn.li.mcmod.ModId/ID,
   which is generated at build time from gradle.properties mod_id."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]))

(def mod-id
  "Primary mod identifier used by resource locations.
   Sourced from cn.li.mcmod.ModId (generated from gradle.properties)."
  cn.li.mcmod.ModId/ID)

(defn resource-location
  "Create a platform-specific resource identifier.

   ([path]) uses the current mod-id
   ([modid path]) uses an explicit namespace."
  ([path]
   (resource-location mod-id path))
  ([modid path]
   (if-let [fw-atom (fw/fw-atom)]
     (or (platform/call-adapter fw-atom :resource :factory modid path)
         (throw (ex-info "Resource factory not initialized"
                         {:namespace modid
                          :path path
                          :hint "Minecraft component must install :resource before content init"})))
     (throw (ex-info "Resource factory not initialized"
                     {:namespace modid
                      :path path
                      :hint "Framework must be injected before creating resource locations"})))))

(defn namespaced-path
  "Create a fully qualified resource path string 'modid:path'."
  ([path]
   (namespaced-path mod-id path))
  ([modid path]
   (str modid ":" path)))

(defn config-file-path
  "Place a configuration file below the current mod-id directory."
  [file-name]
  (str mod-id "/" file-name))

(defn asset-path
  "Create an asset resource path string: 'modid:category/filename'."
  ([category filename]
   (asset-path mod-id category filename))
  ([modid category filename]
   (namespaced-path modid (str category "/" filename))))
