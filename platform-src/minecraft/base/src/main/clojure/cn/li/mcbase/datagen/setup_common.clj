(ns cn.li.mcbase.datagen.setup-common
  "Shared datagen setup utilities, platform-independent.

  Provides common content initialization for Forge/Fabric/NeoForge datagen phases."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mcmod.datagen.metadata :as datagen-metadata]
            [cn.li.mcmod.framework :as fw]
            [cn.li.platform.bootstrap :as platform-bootstrap]
            [cn.li.platform.target :as target]))

(defn- snapshot-counts
  []
  {:items (count (registry-metadata/get-all-item-ids))
   :blocks (count (registry-metadata/get-all-block-ids))
   :recipes (count (datagen-metadata/get-recipes))})

(defn- populated?
  [{:keys [items blocks recipes]}]
  (or (pos? (long items))
      (pos? (long blocks))
      (pos? (long recipes))))

(defn ensure-content-loaded!
  "Datagen runs outside normal mod init.
   We need content DSL registries populated (blocks/items/gui metadata)
   and content blockstate hooks installed, but this shared layer must not
   depend on concrete content namespaces at compile time.

   This function uses mcmod indirection to:
   - load discovered content bootstrap providers so they can register lifecycle init
   - run content init (installs hooks, binds mod-id, etc.)
   - activate runtime content (loads all DSL namespaces; fills registry metadata)
   - run content-owned datagen metadata hooks

   Called by Forge/Fabric/NeoForge datagen entry points.
   Note: Uses cn.li.platform.neutral.config/mod-id for logging, so modid binding
   must be set up before calling this function."
  []
  (try
    ;; Ensure Framework is initialized.
    ;; Normal mod init already injects via alter-var-root.
    ;; Only inject here if datagen runs standalone (e.g. via --existing).
    (when (nil? fw/framework)
      (when-let [fw-inst (fw/create-framework)]
        (alter-var-root #'fw/framework (constantly fw-inst))))
    (let [target-model (target/current-target!)]
      (platform-bootstrap/initialize-datagen-content! target-model))
    (let [initial (snapshot-counts)]
      (when-not (populated? initial)
        (println (str "[" modid/mod-id "] WARNING: datagen metadata still empty after SPI bootstrap, "
                      "counts=" initial))))
    (catch Throwable t
      (println (str "[" modid/mod-id "] WARNING: failed to load content for datagen: "
                    (ex-message t)))
      (.printStackTrace t))))
