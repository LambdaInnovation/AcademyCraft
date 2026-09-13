(ns cn.li.fabric1201.integration.optional-integrations
  "Compatibility facade for the shared Fabric optional-integration seam."
  (:require [cn.li.fabricbase.optional-integrations :as shared]))

(def init! shared/init!)
