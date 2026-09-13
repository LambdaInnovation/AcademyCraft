(ns cn.li.fabric262.mod
  "Fabric 26.2 loader entrypoint.

  Owns Fabric-specific bootstrap ordering and delegates cross-loader lifecycle
  phases to shared setup namespaces."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.fabric262.init :as init]
            [cn.li.platform.bootstrap :as platform-bootstrap]
            [cn.li.mc262.block.blockstate-properties :as bsp]
            [cn.li.fabric262.setup.lifecycle-init :as lifecycle-init]
            [cn.li.fabric262.setup.content-registration :as content-registration]
            [cn.li.fabric262.setup.runtime-setup :as runtime-setup]
            [cn.li.fabric262.setup.event-wiring :as event-wiring]
            [cn.li.fabric262.config.bridge :as config-bridge]
            [cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mcmod.runtime.deferred :as deferred]
	            [cn.li.mcmod.framework :as fw]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mc262.entity.hooks :as entity-hooks])
  (:import [cn.li.fabric262.shim FabricBootstrapHelper]))

(defn- current-mod-id
  []
  modid/mod-id)

;; Unified deferred holders (AOT-safe lazy + runtime tripwire)
(defonce registered-blocks-holder
  (deferred/deferred #(registry-core/atom-registry {})))

(defonce registered-items-holder
  (deferred/deferred #(registry-core/atom-registry {})))

(defonce registered-block-entities-holder
  (deferred/deferred #(registry-core/atom-registry {})))

(defonce registered-fluids-source-holder
  (deferred/deferred #(registry-core/atom-registry {})))

(defonce registered-fluids-flowing-holder
  (deferred/deferred #(registry-core/atom-registry {})))

(defonce base-properties-holder
  (deferred/deferred #(FabricBootstrapHelper/createStoneProperties)))

(defonce carrier-properties-holder
  (deferred/deferred #(FabricBootstrapHelper/carrierBlockProperties @base-properties-holder)))

(defn- registered-blocks
  []
  @registered-blocks-holder)

(defn- registered-items
  []
  @registered-items-holder)

(defn- registered-block-entities
  []
  @registered-block-entities-holder)

(defn- registered-fluids-source
  []
  @registered-fluids-source-holder)

(defn- registered-fluids-flowing
  []
  @registered-fluids-flowing-holder)

(defn- base-properties
  []
  @base-properties-holder)

(defn- carrier-properties
  []
  @carrier-properties-holder)

(defn- registration-context
  []
  {:mod-id (current-mod-id)
   :registered-blocks (registered-blocks)
   :registered-items (registered-items)
   :registered-block-entities (registered-block-entities)
   :registered-fluids-source (registered-fluids-source)
   :registered-fluids-flowing (registered-fluids-flowing)
   :base-properties (base-properties)
   :carrier-properties (carrier-properties)})

(defn start-fabric-mod!
  "Main Fabric mod initialization called from the Java ModInitializer."
  []
  (log/debug "Initializing AcademyCraft (Fabric 26.2) from Clojure...")
  (when-let [fw-inst (fw/create-framework)]
    (alter-var-root #'fw/framework (constantly fw-inst)))
  (lifecycle-init/init-lifecycle!
    {:init-platform! platform-bootstrap/start!
     :preload-platform-runtime! runtime-setup/preload-platform-runtime!
     :init-from-java! init/init-from-java
     :load-config! #(do (config-bridge/load-all!)
                         (config-bridge/install-config-persist-op!))
     :activate-runtime-content! (platform-bootstrap/runtime-content-activation-callback!)
     :init-blockstate-properties! bsp/init-all-properties!
     :register-content! #(do
                           (content-registration/register-content! (registration-context))
                           (entity-hooks/register-all-hooks!))
     :install-runtime! runtime-setup/install-runtime!
     :register-events! event-wiring/register-events!})
  (log/info "Fabric mod initialization complete"))

(defn get-registered-block
  "Get a registered block by its DSL ID."
  [block-id]
  (registry-core/lookup (registered-blocks) block-id))

(defn get-registered-item
  "Get a registered item by its DSL ID."
  [item-id]
  (registry-core/lookup (registered-items) item-id))

(defn get-registered-block-entity-type
  "Get a registered BlockEntityType by tile-id or block-id."
  [tile-or-block-id]
  (let [be-snapshot ((:snapshot (registered-block-entities)))
        tile-id (or (when (contains? be-snapshot tile-or-block-id)
                      tile-or-block-id)
                    (registry-metadata/get-block-tile-id tile-or-block-id))]
    (get be-snapshot tile-id)))

(defn get-registered-block-item
  "Get a registered block item by its block ID."
  [block-id]
  (registry-core/lookup (registered-items) (str block-id "-item")))

(defn get-registered-fluid-source
  "Get a registered source Fluid by fluid DSL id."
  [fluid-id]
  (registry-core/lookup (registered-fluids-source) fluid-id))

(defn get-registered-fluid-flowing
  "Get a registered flowing Fluid by fluid DSL id."
  [fluid-id]
  (registry-core/lookup (registered-fluids-flowing) fluid-id))
