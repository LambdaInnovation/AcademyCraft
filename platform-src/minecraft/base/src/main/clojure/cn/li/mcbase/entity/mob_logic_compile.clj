(ns cn.li.mcbase.entity.mob-logic-compile
  "Compile declarative mob hook maps into loader-neutral MobLogicBundle instances."
  (:import [cn.li.mcbase.entity.logic MobLogicBundle]
           [cn.li.mcbase.shim FnMobTickLogic FnMobHurtLogic FnMobDeathLogic FnMobLootLogic]))

(defn ^MobLogicBundle compile-mob-logic
  "Compile :properties.mob map (or merged cfg) into a MobLogicBundle singleton."
  [mob-props]
  (let [props (or mob-props {})
        tick-fn  (:mob-tick-fn props)
        hurt-fn  (:mob-hurt-fn props)
        death-fn (:mob-death-fn props)
        loot-fn  (:mob-loot-fn props)
        tick  (when tick-fn  (FnMobTickLogic. tick-fn))
        hurt  (when hurt-fn  (FnMobHurtLogic. (fn [mob src amt] (float (hurt-fn mob src amt)))))
        death (when death-fn (FnMobDeathLogic. death-fn))
        loot  (when loot-fn  (FnMobLootLogic. (fn [mob src recent?] (boolean (loot-fn mob src recent?)))))]
    (MobLogicBundle. tick hurt death loot)))
