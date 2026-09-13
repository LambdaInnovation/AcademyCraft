(ns cn.li.fabric262.client.obj-model-registration
  "26.2 moved item rendering to the ItemModel pipeline.

  The old BakedModel/OBJ bridge was tied to the pre-26.2 renderer and cannot
  be registered through the new Fabric model-loading callbacks. Keep the
  registration seam callable while the common item-model implementation is
  migrated; vanilla JSON models continue to load normally on this target.")

(defn register!
  "No-op compatibility hook for the 26.2 item-model pipeline."
  []
  nil)
