(ns cn.li.fabric262.setup.content-registration
  "Fabric content registration — thin Registry callbacks over
  cn.li.platform.registry.content-registration-core."
  (:require [cn.li.fabric262.registry.fabric-dispatch :as fabric-dispatch]
            [cn.li.mcbase.block.logic-pipeline :as logic-pipeline]
            [cn.li.mcbase.entity.mob-logic-pipeline :as mob-pipeline]
            [cn.li.mcmod.entity.dsl :as edsl]
            [cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mc262.block.blockstate-properties :as bsp]
            [cn.li.mc262.item.item-properties :as item-properties]
            [cn.li.platform.registry.content-registration-core :as core])
  (:import [cn.li.fabric262.entity FabricScriptedEntityAccess]
           [cn.li.fabric262.shim FabricBootstrapHelper]
           [cn.li.mcbase.block IScriptedBlock]
           [cn.li.mcbase.entity.spec ScriptedProjectileSpec ScriptedEffectSpec ScriptedRaySpec ScriptedMarkerSpec ScriptedBlockBodySpec]
           [net.minecraft.world.level.block LiquidBlock]
           [net.minecraft.world.level.material FlowingFluid]
           [java.util.function Supplier]))

(defn- install-bundle-on-block!
  [block tile-id bundles]
  (when (and block tile-id bundles)
    (when-let [bundle (get bundles tile-id)]
      (when (instance? IScriptedBlock block)
        (logic-pipeline/install-bundle-to-block! block bundle)))))

(defn- atom-supplier
  ^Supplier [holder]
  (reify Supplier
    (get [_] @holder)))

(defonce ^:private fluid-block-holders
  (atom {}))

(defn register-all-fluids!
  "Register source/flowing fluids (+ buckets) before liquid blocks."
  [{:keys [registered-fluids-source registered-fluids-flowing registered-items]}]
  (core/for-each-fluid-plan!
    (fn [{:keys [fluid-id registry-name flowing-name physical behavior block-spec]}]
      (let [source-holder (atom nil)
            flowing-holder (atom nil)
            bucket-holder (atom nil)
            block-holder (atom nil)
            make-props (fn []
                         (FabricBootstrapHelper/createFlowingFluidProperties
                           (atom-supplier source-holder)
                           (atom-supplier flowing-holder)
                           (when (:has-bucket? block-spec) (atom-supplier bucket-holder))
                           (when (:block-id block-spec)
                             (reify Supplier
                               (get [_]
                                 (cast LiquidBlock @block-holder))))
                           (int (or (:slope-find-distance behavior) 4))
                           (int (or (:level-decrease-per-block behavior) 1))
                           (int (or (:tick-rate behavior) 5))
                           (float (or (:explosion-resistance behavior) 100.0))
                           (boolean (:can-convert-to-source physical))))
            source (FabricBootstrapHelper/createSourceFluid (make-props))
            flowing (FabricBootstrapHelper/createFlowingFluid (make-props))
            registered-source (fabric-dispatch/register-fluid registry-name source)
            registered-flowing (fabric-dispatch/register-fluid flowing-name flowing)]
        (reset! source-holder registered-source)
        (reset! flowing-holder registered-flowing)
        (swap! fluid-block-holders assoc fluid-id block-holder)
        ((:swap-state! registered-fluids-source) #(assoc % fluid-id registered-source))
        ((:swap-state! registered-fluids-flowing) #(assoc % fluid-id registered-flowing))
        (when (:has-bucket? block-spec)
          (let [bucket (FabricBootstrapHelper/createFluidBucket
                         (:bucket-registry-name block-spec)
                         (reify Supplier
                           (get [_] @source-holder)))
                registered-bucket (fabric-dispatch/register-item
                                    (:bucket-registry-name block-spec)
                                    bucket)]
            (reset! bucket-holder registered-bucket)
            ((:swap-state! registered-items)
             #(assoc % (:bucket-item-id block-spec) registered-bucket))))))))

(defn- create-fluid-backed-block
  [registry-name block-id fluid-id has-be? tile-id light-level registered-fluids-source]
  (let [source (registry-core/lookup registered-fluids-source fluid-id)]
    (when-not source
      (throw (ex-info "Fluid source missing for liquid block"
                      {:block-id block-id :fluid-id fluid-id})))
    (let [fluid-supplier (reify Supplier
                           (get [_]
                             (cast FlowingFluid source)))
          block-inst (if has-be?
                       (FabricBootstrapHelper/createScriptedLiquidBlock
                         registry-name fluid-supplier block-id tile-id (int light-level))
                       (FabricBootstrapHelper/createLiquidBlock
                         registry-name fluid-supplier (int light-level)))]
      (when-let [block-holder (get @fluid-block-holders fluid-id)]
        (reset! block-holder block-inst))
      block-inst)))

(defn register-all-blocks!
  [{:keys [registered-blocks registered-fluids-source base-properties carrier-properties]}]
  (let [bundles (logic-pipeline/compile-all-bundles)]
    (core/for-each-block-plan!
      (fn [{:keys [block-id registry-name physical fluid-id fluid-block? fluid-luminosity
                   needs-dynamic-properties? has-be? tile-id]}]
        (let [physical (or physical {})
              block-properties (FabricBootstrapHelper/createBlockProperties
                                registry-name
                                (name (or (:material physical) :stone))
                                (float (or (:hardness physical) 1.5))
                                (float (or (:resistance physical) 6.0))
                                (boolean (:requires-tool physical)))
              carrier-properties (FabricBootstrapHelper/carrierBlockProperties block-properties)
              block-inst (cond
                           (and fluid-id fluid-block?)
                           (create-fluid-backed-block
                             registry-name block-id fluid-id has-be? tile-id
                             fluid-luminosity registered-fluids-source)

                           (and fluid-id (not fluid-block?))
                           (FabricBootstrapHelper/createPlainBlock
                             block-properties)

                           (and needs-dynamic-properties? has-be?)
                           (let [props (bsp/get-all-properties block-id)]
                             (FabricBootstrapHelper/createCarrierScriptedDynamicBlock
                               block-id tile-id props
                               carrier-properties))

                           needs-dynamic-properties?
                           (let [props (bsp/get-all-properties block-id)]
                             (FabricBootstrapHelper/createDynamicStateBlock
                               block-id props block-properties))

                           has-be?
                           (FabricBootstrapHelper/createCarrierScriptedBlock
                             block-id tile-id
                             carrier-properties)

                           :else
                           (FabricBootstrapHelper/createPlainBlock
                             block-properties))
              _ (install-bundle-on-block! block-inst tile-id bundles)
              registered (fabric-dispatch/register-block registry-name block-inst)]
          ((:swap-state! registered-blocks) #(assoc % block-id registered)))))))

(defn assert-scripted-blocks-bundled!
  [{:keys [registered-blocks]}]
  (let [blocks (atom [])]
    (core/for-each-block-plan!
      (fn [{:keys [block-id]}]
        (when-let [b (registry-core/lookup registered-blocks block-id)]
          (swap! blocks conj b))))
    (let [scripted (filter #(instance? IScriptedBlock %) @blocks)]
      (logic-pipeline/assert-all-blocks-have-bundle! scripted #{})))
  nil)

(defn register-block-entities!
  [{:keys [mod-id registered-blocks registered-block-entities]}]
  (core/for-each-tile-plan!
    (fn [{:keys [tile-id registry-name block-ids]}]
      (let [blocks (keep #(registry-core/lookup registered-blocks %) block-ids)]
        (when (seq blocks)
          (let [pairs (map vector block-ids blocks)
                block-id-map (java.util.IdentityHashMap.)]
            (doseq [[resolved-block-id block-inst] pairs]
              (.put block-id-map block-inst resolved-block-id))
            (let [be-type (FabricBootstrapHelper/createScriptedBlockEntityType
                            tile-id
                            blocks
                            (reify java.util.function.Function
                              (apply [_ block-inst]
                                (.get block-id-map block-inst))))
                  registered (FabricBootstrapHelper/registerBlockEntityType mod-id registry-name be-type)]
              ((:swap-state! registered-block-entities) #(assoc % tile-id registered)))))))))

(defn- create-standalone-item
  [item-spec registry-name]
  (item-properties/create-standalone-item item-spec (fn [_player _side f] (f)) registry-name))

(defn register-all-items!
  [{:keys [registered-items registered-blocks]}]
  (core/for-each-item-plan!
    (fn [{:keys [item-id registry-name item-spec]}]
      (let [item-inst (create-standalone-item item-spec registry-name)
            registered (fabric-dispatch/register-item registry-name item-inst)]
        ((:swap-state! registered-items) #(assoc % item-id registered)))))
  (core/for-each-block-plan!
    (fn [plan]
      (when (core/should-register-block-item? plan)
        (let [{:keys [block-id registry-name]} plan]
          (when-let [block-inst (registry-core/lookup registered-blocks block-id)]
            (let [block-item (FabricBootstrapHelper/createBlockItem registry-name block-inst)
                  registered (fabric-dispatch/register-item registry-name block-item)]
              ((:swap-state! registered-items) #(assoc % (str block-id "-item") registered)))))))))

(defn- register-scripted-kind-spec!
  [registry-name entity-kind entity-spec fields]
  (case entity-kind
    :scripted-projectile
    (FabricScriptedEntityAccess/registerScriptedProjectileSpec
      (str registry-name)
      (ScriptedProjectileSpec.
        (:default-item-id fields)
        (:gravity fields)
        (:damage fields)
        (:pickup-distance-sqr fields)
        (:drop-item-on-discard? fields)
        (:on-hit-block fields)
        (:on-hit-entity fields)
        (:on-anchored-tick fields)
        (:on-anchored-hurt fields)))

    :scripted-effect
    (FabricScriptedEntityAccess/registerScriptedEffectSpec
      (str registry-name)
      (ScriptedEffectSpec.
        (:life-ticks fields)
        (:follow-owner? fields)
        (edsl/resolve-render-profile-key entity-spec :effect "effect-billboard")
        (:hook fields)
        (:hook-params fields)))

    :scripted-ray
    (FabricScriptedEntityAccess/registerScriptedRaySpec
      (str registry-name)
      (ScriptedRaySpec.
        (:life-ticks fields)
        (:length fields)
        (:blend-in-ms fields)
        (:blend-out-ms fields)
        (:inner-width fields)
        ;; Fabric defaults differ from Forge for these visual knobs.
        (double (or (get-in entity-spec [:properties :ray :outer-width]) 0.1))
        (double (or (get-in entity-spec [:properties :ray :glow-width]) 0.05))
        (int (or (get-in entity-spec [:properties :ray :start-color]) 0xFFFFFFFF))
        (int (or (get-in entity-spec [:properties :ray :end-color]) 0xFFFFFFFF))
        (edsl/resolve-render-profile-key entity-spec :ray "ray-composite")
        (:hook fields)))

    :scripted-marker
    (let [marker (get-in entity-spec [:properties :marker])]
      (FabricScriptedEntityAccess/registerScriptedMarkerSpec
        (str registry-name)
        (ScriptedMarkerSpec.
          (int (or (:life-ticks marker) 15))
          (:follow-target? fields)
          (:ignore-depth? fields)
          (:available? fields)
          (edsl/resolve-render-profile-key entity-spec :marker "marker-billboard")
          (:hook fields))))

    :scripted-block-body
    (let [block-body (get-in entity-spec [:properties :block-body])]
      (FabricScriptedEntityAccess/registerScriptedBlockBodySpec
        (str registry-name)
        (ScriptedBlockBodySpec.
          (str (or (:default-block-id block-body) ""))
          (:gravity fields)
          (:damage fields)
          (:place-when-collide? fields)
          (edsl/resolve-render-profile-key entity-spec :block-body "block-body")
          (:hook fields)
          (:behavior fields)
          (:drag fields))))

    nil)
  nil)

(defn register-all-entities!
  []
  (core/for-each-entity-plan!
    (fn [{:keys [registry-name entity-kind entity-spec kind-fields]}]
      (case entity-kind
        :scripted-mob (do (mob-pipeline/compile-all-mob-bundles) nil)
        (when kind-fields
          (register-scripted-kind-spec! registry-name entity-kind entity-spec kind-fields))))))

(defn register-all-particles!
  []
  (core/for-each-particle-plan!
    (fn [{:keys [registry-name always-show?]}]
      (fabric-dispatch/register-particle registry-name always-show?))))

(defn register-content!
  [{:keys [mod-id] :as ctx}]
  (register-all-fluids! ctx)
  (register-all-blocks! ctx)
  (assert-scripted-blocks-bundled! ctx)
  (register-block-entities! (assoc ctx :mod-id mod-id))
  (register-all-items! ctx)
  (register-all-entities!)
  (register-all-particles!)
  nil)
