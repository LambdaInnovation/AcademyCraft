(ns cn.li.mcbase.block.blockstate-properties
  "Shared orchestration for platform BlockState property adapters.

  BlockState property creation here uses only vanilla Minecraft APIs, so the
  registry and default constructors live in shared Minecraft glue rather than
  loader wrappers."
  (:require [cn.li.platform.neutral.block-runtime :as shared]
            [cn.li.platform.neutral.block-runtime :as bquery]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.world.level.block.state.properties IntegerProperty BooleanProperty BlockStateProperties]))

(defn create-adapter-registry []
  (shared/create-property-registry))

(defonce ^:private property-registry
  (atom nil))

(defn- default-property-registry!
  "Create the shared registry only after neutral providers have been installed.

   Loading this AOT/remapped namespace must not call a runtime-source provider:
   the provider is installed during platform bootstrap, before any content or
   block-state registration invokes this function. compare-and-set! keeps the
   registry identity stable when two startup hooks race."
  []
  (or @property-registry
      (let [created (shared/create-property-registry)]
        (if (compare-and-set! property-registry nil created)
          created
          @property-registry))))

(defn- create-integer-property [property-name min-value max-value]
  (IntegerProperty/create property-name (int min-value) (int max-value)))

(defn- create-boolean-property [property-name]
  (BooleanProperty/create property-name))

(defn- create-horizontal-facing-property
  [_property-name]
  BlockStateProperties/HORIZONTAL_FACING)

(defn register-block-properties!
  [property-registry block-id block-state-properties create-integer-fn create-boolean-fn create-facing-fn]
  (shared/register-block-properties!
   property-registry block-id block-state-properties
   create-integer-fn
   create-boolean-fn
   create-facing-fn))

(defn register-default-block-properties!
  [block-id block-state-properties]
  (register-block-properties!
   (default-property-registry!)
   block-id
   block-state-properties
   create-integer-property
   create-boolean-property
   create-horizontal-facing-property))

(defn get-property
  ([property-registry block-id property-key]
   (shared/get-property property-registry block-id property-key))
  ([block-id property-key]
   (shared/get-property (default-property-registry!) block-id property-key)))

(defn get-all-properties
  ([property-registry block-id]
   (shared/get-all-properties property-registry block-id))
  ([block-id]
   (shared/get-all-properties (default-property-registry!) block-id)))

(defn init-all-properties!
  ([]
   (init-all-properties!
    "shared adapter"
    (default-property-registry!)
    (fn [block-id]
      (get-in (bquery/get-block-spec block-id)
              [:block-state :block-state-properties]))
    create-integer-property
    create-boolean-property
    create-horizontal-facing-property)
   (log/debug "Shared BlockState properties initialized"))
  ([platform-label property-registry resolve-block-properties-fn create-integer-fn create-boolean-fn create-facing-fn]
   (log/debug (str "Initializing BlockState properties (" platform-label ")..."))
   (doseq [block-id (bquery/list-all-blocks)]
     (when-let [props (resolve-block-properties-fn block-id)]
       (register-block-properties!
        property-registry
        block-id
        props
        create-integer-fn
        create-boolean-fn
        create-facing-fn)))))
