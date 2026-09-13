(ns cn.li.mcbase.integration.event-feedback
  "Shared event feedback helpers (server-side player messages)."
  (:import [net.minecraft.network.chat Component]
           [net.minecraft.server.level ServerPlayer]
           [net.minecraft.world.level Level]))

(defn feedback-component
  [{:keys [type key args text]}]
  (case type
    :translatable (Component/translatable (str key) (into-array Object (map str (or args []))))
    :literal (Component/literal (str text))
    nil))

(defn emit-feedback!
  [event-data ret]
  (let [^Level world (:world event-data)
        ^ServerPlayer player (:player event-data)
        messages (when (map? ret) (:messages ret))]
    (when (and world player (not (.isClientSide world)) (seq messages))
      (doseq [m messages]
        (when-let [c (feedback-component m)]
          (.sendSystemMessage player c))))))
