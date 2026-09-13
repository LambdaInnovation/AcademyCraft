(ns cn.li.forge1201.registry.content-registration
  "Forge content registration — thin DeferredRegister callbacks over
  cn.li.platform.registry.content-registration-core."
  (:require [cn.li.forge1201.integration.bootstrap :as bootstrap]
            [cn.li.mc1201.item.item-properties :as item-properties]
            [cn.li.forge1201.registry.state :as registry-state]
            [cn.li.forge1201.runtime.owner :as runtime-owner]
            [cn.li.mcbase.block.logic-pipeline :as logic-pipeline]
            [cn.li.mcbase.entity.mob-logic-pipeline :as mob-pipeline]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mc1201.block.blockstate-properties :as blockstate-props]
            [cn.li.platform.registry.content-registration-core :as core])
  (:import [cn.li.forge1201.entity ModEntities]
           [cn.li.mcbase.block IScriptedBlock]
           [cn.li.mc1201.effect ScriptedMobEffect]
           [net.minecraft.core.particles SimpleParticleType]
           [net.minecraft.resources ResourceLocation]
           [net.minecraft.sounds SoundEvent]
           [net.minecraft.world.effect MobEffectCategory]
           [net.minecraft.world.item BlockItem Item$Properties]
           [net.minecraftforge.registries DeferredRegister RegistryObject]))

(defn- register-scripted-kind-spec!
  [registry-name entity-kind fields]
  (case entity-kind
    :scripted-projectile
    (ModEntities/registerScriptedProjectileSpec
      (str registry-name)
      (:default-item-id fields)
      (:gravity fields)
      (:damage fields)
      (:pickup-distance-sqr fields)
      (:drop-item-on-discard? fields)
      (:on-hit-block fields)
      (:on-hit-entity fields)
      (:on-anchored-tick fields)
      (:on-anchored-hurt fields))

    :scripted-effect
    (ModEntities/registerScriptedEffectSpec
      (str registry-name)
      (:life-ticks fields)
      (:follow-owner? fields)
      (:renderer-id fields)
      (:hook fields)
      (:hook-params fields))

    :scripted-ray
    (ModEntities/registerScriptedRaySpec
      (str registry-name)
      (:life-ticks fields)
      (:length fields)
      (:blend-in-ms fields)
      (:blend-out-ms fields)
      (:inner-width fields)
      (:outer-width fields)
      (:glow-width fields)
      (:start-color fields)
      (:end-color fields)
      (:renderer-id fields)
      (:hook fields)
      (:hook-params fields))

    :scripted-marker
    (ModEntities/registerScriptedMarkerSpec
      (str registry-name)
      (:life-ticks fields)
      (:follow-target? fields)
      (:ignore-depth? fields)
      (:available? fields)
      (:renderer-id fields)
      (:hook fields))

    :scripted-block-body
    (ModEntities/registerScriptedBlockBodySpec
      (str registry-name)
      (:default-block-id fields)
      (:gravity fields)
      (:damage fields)
      (:place-when-collide? fields)
      (:renderer-id fields)
      (:hook fields)
      (:behavior fields)
      (:drag fields))

    nil)
  nil)

(defn- install-bundle-on-block!
  [block tile-id bundles]
  (when (and block tile-id bundles)
    (when-let [bundle (get bundles tile-id)]
      (when (instance? IScriptedBlock block)
        (logic-pipeline/install-bundle-to-block! block bundle)))))

(defn register-all-blocks!
  [{:keys [blocks-register registered-fluids-source base-properties carrier-properties]}]
  (let [bundles (logic-pipeline/compile-all-bundles)]
    (core/for-each-block-plan!
      (fn [{:keys [block-id registry-name physical fluid-id fluid-luminosity
                   needs-dynamic-properties? has-be? tile-id]}]
        (let [registered-obj
              (.register ^DeferredRegister blocks-register registry-name
                         (reify java.util.function.Supplier
                           (get [_]
                              (let [physical (or physical {})
                                    block-properties (bootstrap/create-block-properties
                                                       (name (or (:material physical) :stone))
                                                       (float (or (:hardness physical) 1.5))
                                                       (float (or (:resistance physical) 6.0))
                                                       (boolean (:requires-tool physical)))
                                    carrier-properties (bootstrap/carrier-block-properties block-properties)
                                    block (cond
                                           (and fluid-id has-be?)
                                           (when-let [fluid-source-ro (get (core/registry-source-snapshot registered-fluids-source) fluid-id)]
                                             (bootstrap/create-scripted-liquid-block
                                               (reify java.util.function.Supplier
                                                 (get [_]
                                                   (.get ^RegistryObject fluid-source-ro)))
                                               block-id
                                               tile-id
                                               fluid-luminosity))
                                           fluid-id
                                           (when-let [fluid-source-ro (get (core/registry-source-snapshot registered-fluids-source) fluid-id)]
                                             (bootstrap/create-liquid-block
                                               (reify java.util.function.Supplier
                                                 (get [_]
                                                   (.get ^RegistryObject fluid-source-ro)))
                                               fluid-luminosity))
                                           (and needs-dynamic-properties? has-be?)
                                           (let [props (blockstate-props/get-all-properties block-id)]
                                              (bootstrap/create-carrier-scripted-dynamic-block block-id tile-id props block-properties))
                                           needs-dynamic-properties?
                                           (let [props (blockstate-props/get-all-properties block-id)]
                                              (bootstrap/create-dynamic-state-block block-id props block-properties))
                                           has-be?
                                            (bootstrap/create-carrier-scripted-block block-id tile-id carrier-properties)
                                           :else
                                            (bootstrap/create-plain-block block-properties))]
                               (install-bundle-on-block! block tile-id bundles)
                               block))))]
          (registry-state/register-block! block-id registered-obj))))))

(defn register-all-fluids!
  [{:keys [fluid-types-register fluids-register items-register]}]
  (core/for-each-fluid-plan!
    (fn [{:keys [fluid-id registry-name flowing-name physical rendering behavior block-spec]}]
      (let [fluid-type-ro (.register ^DeferredRegister fluid-types-register registry-name
                                     (reify java.util.function.Supplier
                                       (get [_]
                                         (bootstrap/create-fluid-type
                                           (:luminosity physical)
                                           (:density physical)
                                           (:viscosity physical)
                                           (:temperature physical)
                                           false
                                           (:supports-boat physical)
                                           (:still-texture rendering)
                                           (:flowing-texture rendering)
                                           (:overlay-texture rendering)
                                           (:tint-color rendering)))))
            source-holder (atom nil)
            flowing-holder (atom nil)
            bucket-holder (atom nil)
            source-ro (.register ^DeferredRegister fluids-register registry-name
                                 (reify java.util.function.Supplier
                                   (get [_]
                                     (bootstrap/create-source-fluid
                                       (bootstrap/create-flowing-fluid-properties
                                         (reify java.util.function.Supplier
                                           (get [_] (.get ^RegistryObject fluid-type-ro)))
                                         (reify java.util.function.Supplier
                                           (get [_] (.get ^RegistryObject @source-holder)))
                                         (reify java.util.function.Supplier
                                           (get [_] (.get ^RegistryObject @flowing-holder)))
                                         (when (:has-bucket? block-spec)
                                           (reify java.util.function.Supplier
                                             (get [_] (.get ^RegistryObject @bucket-holder))))
                                         (when-let [block-id (:block-id block-spec)]
                                           (reify java.util.function.Supplier
                                             (get [_]
                                               (.get ^RegistryObject (registry-state/get-registered-block-ro block-id)))))
                                         (:slope-find-distance behavior)
                                         (:level-decrease-per-block behavior)
                                         (:tick-rate behavior)
                                         (:explosion-resistance behavior)
                                         (:can-convert-to-source physical))))))
            flowing-ro (.register ^DeferredRegister fluids-register flowing-name
                                  (reify java.util.function.Supplier
                                    (get [_]
                                      (bootstrap/create-flowing-fluid
                                        (bootstrap/create-flowing-fluid-properties
                                          (reify java.util.function.Supplier
                                            (get [_] (.get ^RegistryObject fluid-type-ro)))
                                          (reify java.util.function.Supplier
                                            (get [_] (.get ^RegistryObject @source-holder)))
                                          (reify java.util.function.Supplier
                                            (get [_] (.get ^RegistryObject @flowing-holder)))
                                          (when (:has-bucket? block-spec)
                                            (reify java.util.function.Supplier
                                              (get [_] (.get ^RegistryObject @bucket-holder))))
                                          (when-let [block-id (:block-id block-spec)]
                                            (reify java.util.function.Supplier
                                              (get [_]
                                                (.get ^RegistryObject (registry-state/get-registered-block-ro block-id)))))
                                          (:slope-find-distance behavior)
                                          (:level-decrease-per-block behavior)
                                          (:tick-rate behavior)
                                          (:explosion-resistance behavior)
                                          (:can-convert-to-source physical))))))]
        (reset! source-holder source-ro)
        (reset! flowing-holder flowing-ro)
        (registry-state/register-fluid-type! fluid-id fluid-type-ro)
        (registry-state/register-fluid-source! fluid-id source-ro)
        (registry-state/register-fluid-flowing! fluid-id flowing-ro)
        (when (:has-bucket? block-spec)
          (let [bucket-ro (.register ^DeferredRegister items-register (:bucket-registry-name block-spec)
                                     (reify java.util.function.Supplier
                                       (get [_]
                                         (bootstrap/create-fluid-bucket
                                           (reify java.util.function.Supplier
                                             (get [_] (.get ^RegistryObject source-ro)))))))]
            (reset! bucket-holder bucket-ro)
            (registry-state/register-item! (:bucket-item-id block-spec) bucket-ro)))))))

(defn register-block-entities!
  [{:keys [block-entities-register]}]
  (core/for-each-tile-plan!
    (fn [{:keys [tile-id registry-name block-ids]}]
      (let [ros (keep (fn [block-id]
                        (when-let [ro (registry-state/get-registered-block-ro block-id)]
                          [block-id ro]))
                      block-ids)]
        (when (seq ros)
          (let [registered-obj
                (.register
                  ^DeferredRegister block-entities-register
                  registry-name
                  (reify java.util.function.Supplier
                    (get [_]
                      (let [pairs (keep (fn [[block-id ^RegistryObject ro]]
                                          (when (.isPresent ro)
                                            [block-id (.get ro)]))
                                        ros)
                            block-insts (mapv second pairs)
                            block-id-by-inst (java.util.IdentityHashMap.)]
                        (doseq [[block-id inst] pairs]
                          (.put block-id-by-inst inst block-id))
                        (bootstrap/create-scripted-block-entity-type
                          tile-id
                          block-insts
                          (reify java.util.function.Function
                            (apply [_ block-inst]
                              (.get block-id-by-inst block-inst))))))))]
            (registry-state/register-block-entity! tile-id registered-obj)))))))

(defn register-all-entities!
  [mod-id]
  (let [mob-bundles (mob-pipeline/compile-all-mob-bundles)]
    (core/for-each-entity-plan!
      (fn [{:keys [entity-id registry-name entity-kind kind-fields category
                   width height client-tracking-range update-interval fire-immune?]}]
        (when kind-fields
          (register-scripted-kind-spec! registry-name entity-kind kind-fields))
        (let [registered-obj (ModEntities/register
                               registry-name
                               (reify java.util.function.Supplier
                                 (get [_]
                                   (bootstrap/create-entity-type-by-kind
                                     (str mod-id ":" registry-name)
                                     (name entity-kind)
                                     category
                                     width
                                     height
                                     client-tracking-range
                                     update-interval
                                     fire-immune?))))]
          (when (and (= :scripted-mob entity-kind)
                     (some? registered-obj)
                     (.isPresent ^RegistryObject registered-obj)
                     (get mob-bundles entity-id))
            (mob-pipeline/install-mob-bundle!
              (.get ^RegistryObject registered-obj)
              (get mob-bundles entity-id)))
          (registry-state/register-entity! entity-id registered-obj))))))

(defn- effect-category->forge
  ^MobEffectCategory
  [category-kw]
  (case category-kw
    :beneficial MobEffectCategory/BENEFICIAL
    :neutral MobEffectCategory/NEUTRAL
    MobEffectCategory/HARMFUL))

(defn register-all-sounds!
  [{:keys [sounds-register mod-id]}]
  (core/for-each-sound-plan!
    (fn [{:keys [sound-id registry-name]}]
      (let [registered-obj (.register ^DeferredRegister sounds-register registry-name
                                      (reify java.util.function.Supplier
                                        (get [_]
                                          (SoundEvent/createVariableRangeEvent
                                            (ResourceLocation. mod-id registry-name)))))]
        (registry-state/register-sound! sound-id registered-obj)))))

(defn register-all-effects!
  [{:keys [effects-register]}]
  (core/for-each-effect-plan!
    (fn [{:keys [effect-id registry-name category color tick-interval damage-per-tick]}]
      (let [registered-obj (.register ^DeferredRegister effects-register registry-name
                                      (reify java.util.function.Supplier
                                        (get [_]
                                          (ScriptedMobEffect. (effect-category->forge category)
                                                              color tick-interval damage-per-tick))))]
        (registry-state/register-effect! effect-id registered-obj)))))

(defn register-all-particles!
  [{:keys [particle-types-register]}]
  (core/for-each-particle-plan!
    (fn [{:keys [particle-id registry-name always-show?]}]
      (let [registered-obj (.register ^DeferredRegister particle-types-register registry-name
                                      (reify java.util.function.Supplier
                                        (get [_]
                                          (SimpleParticleType. always-show?))))]
        (registry-state/register-particle! particle-id registered-obj)))))

(defn register-all-items!
  [{:keys [items-register]}]
  (core/for-each-item-plan!
    (fn [{:keys [item-id registry-name item-spec]}]
      (let [registered-obj (.register ^DeferredRegister items-register registry-name
                                      (reify java.util.function.Supplier
                                        (get [_]
                                          (item-properties/create-standalone-item
                                            item-spec
                                            runtime-owner/with-player-owner))))]
        (registry-state/register-item! item-id registered-obj))))
  (core/for-each-block-plan!
    (fn [plan]
      (when (core/should-register-block-item? plan)
        (let [{:keys [block-id registry-name]} plan
              block-registered (registry-state/get-registered-block-ro block-id)
              registered-obj (.register ^DeferredRegister items-register registry-name
                                        (reify java.util.function.Supplier
                                          (get [_]
                                            (when (and block-registered (.isPresent ^RegistryObject block-registered))
                                              (BlockItem. (.get ^RegistryObject block-registered)
                                                          (Item$Properties.))))))]
          (registry-state/register-item! (str block-id "-item") registered-obj))))))

(defn assert-scripted-blocks-bundled!
  []
  (let [blocks (keep registry-state/get-registered-block (registry-metadata/get-all-block-ids))]
    (logic-pipeline/assert-all-blocks-have-bundle! blocks #{}))
  nil)

(defn register-core-content!
  [{:keys [mod-id] :as ctx}]
  (register-all-fluids! ctx)
  (register-all-blocks! ctx)
  (register-all-entities! mod-id)
  (register-all-sounds! (assoc ctx :mod-id mod-id))
  (register-all-effects! ctx)
  (register-all-particles! ctx)
  (register-block-entities! ctx)
  (register-all-items! ctx)
  nil)
