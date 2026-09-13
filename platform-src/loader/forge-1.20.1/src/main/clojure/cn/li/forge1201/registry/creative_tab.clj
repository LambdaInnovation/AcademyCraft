(ns cn.li.forge1201.registry.creative-tab
  "Forge creative tab registration extracted from the mod entrypoint.

  Tab contents are populated via BuildCreativeModeTabContentsEvent handler
  (see cn.li.forge1201.registry.creative-tab-event) — the 1.20+ data-driven
  approach recommended by official Forge docs."
  (:require [cn.li.forge1201.registry.state :as registry-state])
  (:import [net.minecraft.network.chat Component]
           [net.minecraft.world.item CreativeModeTab Items Item]
           [net.minecraftforge.registries DeferredRegister]))

(defn build-creative-tab
  "Build the mod creative tab shell (title + icon only).

  Items are populated lazily by BuildCreativeModeTabContentsEvent
  (registered on the ModEventBus via creative-tab-event/handle-build-contents).
  This removes the legacy displayItems callback in favour of the 1.20+
  data-driven event pattern."
  [mod-id]
  (let [icon-supplier
        (reify java.util.function.Supplier
          (get [_]
            (try
              (if-let [^Item logo-item (registry-state/get-registered-item "logo")]
                (net.minecraft.world.item.ItemStack. logo-item)
                (.getDefaultInstance Items/BARRIER))
              (catch Exception _
                net.minecraft.world.item.ItemStack/EMPTY))))]
    (-> (CreativeModeTab/builder)
        (.title (Component/translatable (str "itemGroup." mod-id ".items")))
        (.icon icon-supplier)
        (.build))))

(defn register-creative-tab!
  [^DeferredRegister creative-tabs-register mod-id]
  (.register creative-tabs-register
             "items"
             (reify java.util.function.Supplier
               (get [_]
                 (build-creative-tab mod-id)))))
