(ns cn.li.mcbase.gui.slots.data-slot
  "Atom-backed Minecraft DataSlot implementations."
  (:require [cn.li.platform.neutral.gui-runtime :as codec])
  (:import [cn.li.mcbase.gui AtomBackedDataSlot AtomBackedDataSlot$Getter AtomBackedDataSlot$Setter]
           [net.minecraft.world.inventory DataSlot]))

(defn create-atom-backed-data-slot
  "Create a DataSlot whose get/set encode/decode a container atom."
  [atom-ref field-codec]
  (when-not (instance? clojure.lang.Atom atom-ref)
    (throw (ex-info "DataSlot requires atom ref" {:atom-ref atom-ref})))
  (let [encode (:encode field-codec)
        decode (:decode field-codec)]
    (AtomBackedDataSlot.
     (reify AtomBackedDataSlot$Getter
       (getAsInt [_]
         (codec/clamp-int (encode @atom-ref))))
     (reify AtomBackedDataSlot$Setter
       (^void accept [_ ^int v]
         (reset! atom-ref (decode v))
         nil)))))

(defn materialize-data-slots!
  "Attach atom-backed DataSlots to container under :data-slots (vector, stable order)."
  [clj-container field-specs]
  (let [slots (mapv (fn [{:keys [container-key codec]}]
                      (when-let [atom-ref (get clj-container container-key)]
                        (create-atom-backed-data-slot atom-ref codec)))
                    field-specs)]
    (assoc clj-container :data-slots slots :data-slot-specs field-specs)))

(defn register-data-slots-on-menu!
  [^cn.li.mcbase.gui.CMenuBridge menu clj-container]
  (when-let [slots (:data-slots clj-container)]
    (doseq [^DataSlot slot slots]
      (when slot
        (.addDataSlotPublic menu slot)))))
