(ns cn.li.mcbase.entity.mob-logic-pipeline
  "Loader-neutral mob bundle compile/install pipeline (shared by Forge and Fabric)."
  (:require [cn.li.mcbase.entity.mob-logic-compile :as mob-compile]
            [cn.li.mcmod.entity.dsl :as edsl])
  (:import [cn.li.mcbase.entity ScriptedEntityLogicRegistry]
           [cn.li.mcbase.entity.logic MobLogicBundle]
           [net.minecraft.world.entity EntityType]))

(defn compile-all-mob-bundles
  "Pure: entity-id → MobLogicBundle for every :scripted-mob entity spec."
  []
  (into {}
        (keep (fn [entity-id]
                (let [spec (edsl/get-entity entity-id)]
                  (when (= :scripted-mob (:entity-kind spec))
                    [entity-id (mob-compile/compile-mob-logic (get-in spec [:properties :mob]))])))
              (edsl/list-entities))))

(defn install-mob-bundle!
  "Install a compiled mob bundle on a registered EntityType instance."
  [^EntityType entity-type ^MobLogicBundle bundle]
  (ScriptedEntityLogicRegistry/installMobLogic entity-type bundle))

(defn assert-all-mobs-have-bundle!
  "Fail-fast when any scripted-mob EntityType still has EMPTY logic."
  [entity-types allow-empty-entity-ids]
  (doseq [[entity-id ^EntityType type] entity-types
          :when type]
    (when (and (identical? MobLogicBundle/EMPTY (ScriptedEntityLogicRegistry/getMobLogic type))
               (not (contains? allow-empty-entity-ids entity-id)))
      (throw (IllegalStateException.
               (str "entity-id " entity-id " has empty MobLogicBundle"))))))
