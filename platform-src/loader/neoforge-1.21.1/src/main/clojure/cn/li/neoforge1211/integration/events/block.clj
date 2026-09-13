(ns cn.li.neoforge1211.integration.events.block
  "Forge block place/break event handlers." 
  (:require [cn.li.mcbase.integration.event-support :as event-support]
            [cn.li.platform.neutral.event-runtime :as dispatcher]
            [cn.li.mc1211.integration.event-handlers :as event-handlers]
            [cn.li.neoforge1211.integration.events.event-apply :as event-apply])
  (:import [net.neoforged.neoforge.event.level BlockEvent$EntityPlaceEvent BlockEvent$BreakEvent]))

(defn handle-block-place
  [event-data]
  (event-handlers/handle-block-place
    event-data
    dispatcher/on-block-place
    "[FORGE-BLOCK-PLACE]"))

(defn handle-block-place-event
  [^BlockEvent$EntityPlaceEvent evt]
  (event-support/guarded-call
    "block place event"
    nil
    (fn []
      (let [pos (.getPos evt)
            level (.getLevel evt)
            entity (.getEntity evt)
            placed-state (.getPlacedBlock evt)]
        (when (and level pos)
          (let [ret (handle-block-place
                      {:x (.getX pos)
                       :y (.getY pos)
                       :z (.getZ pos)
                       :pos pos
                       :entity entity
                       :player entity
                       :world level
                       :block (.getBlock placed-state)})]
            (when (and (map? ret) (:cancel-place? ret))
              (event-apply/cancel-event! evt))))))))

(defn handle-block-break-event
  [^BlockEvent$BreakEvent evt]
  (event-support/guarded-call
    "block break event"
    nil
    (fn []
      (let [pos (.getPos evt)
            level (.getLevel evt)
            player (.getPlayer evt)
            block-state (.getBlockState level pos)
            ret (event-handlers/handle-block-break
                  {:x (.getX pos)
                   :y (.getY pos)
                   :z (.getZ pos)
                   :pos pos
                   :player player
                   :world level
                   :block (.getBlock block-state)}
                  dispatcher/on-block-break
                  "[FORGE-BLOCK-BREAK]")]
        (when (and (map? ret) (:cancel-break? ret))
          (event-apply/cancel-event! evt))))))
