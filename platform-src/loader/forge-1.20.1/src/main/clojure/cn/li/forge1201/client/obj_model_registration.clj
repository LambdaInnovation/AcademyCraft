(ns cn.li.forge1201.client.obj-model-registration
  "Forge ModelEvent registration only — baking + composite model live in mc1201."
  (:require [cn.li.mc1201.client.render.obj-model-baking :as baking])
  (:import [net.minecraftforge.client.event ModelEvent$RegisterAdditional ModelEvent$ModifyBakingResult]
           [net.minecraft.client.resources.model BakedModel ModelResourceLocation]
           [cn.li.forge1201.client.render.item ObjCompositeOverrides]))

(defn register-additional-obj-models!
  "ModelEvent.RegisterAdditional → register `_3d` inventory variants."
  [^ModelEvent$RegisterAdditional event]
  (doseq [^ModelResourceLocation mrl (baking/additional-obj-inventory-model-locations)]
    (.register event mrl)))

(defn replace-obj-composite-models!
  "ModelEvent.ModifyBakingResult → install mc1201 ObjCompositeBakedModel."
  [^ModelEvent$ModifyBakingResult event]
  (baking/install-obj-composite-models!
    (.getModels event)
    (fn [^BakedModel flat-base ^BakedModel world-model]
      (ObjCompositeOverrides. flat-base world-model))))
