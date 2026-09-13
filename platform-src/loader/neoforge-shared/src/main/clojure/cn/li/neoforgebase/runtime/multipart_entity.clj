(ns cn.li.neoforgebase.runtime.multipart-entity
  "Forge multipart API registration adapter."
  (:import [cn.li.acapi.entity MultipartEntityApi MultipartEntityApi$ParentResolver]
           [net.neoforged.neoforge.entity PartEntity]))

(def ^:private resolver-id "academycraft:forge_part_entity")

(defn register-parent-resolver!
  []
  (MultipartEntityApi/registerParentResolver
    resolver-id
    100
    (reify MultipartEntityApi$ParentResolver
      (findParent [_ entity]
        (when (instance? PartEntity entity)
          (.getParent ^PartEntity entity)))))
  nil)
