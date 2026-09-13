(ns cn.li.forge1201.runtime.multipart-entity-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.forge1201.runtime.multipart-entity :as multipart])
  (:import [cn.li.acapi.entity MultipartEntityApi]))

(def ^:private resolver-id "academycraft:forge_part_entity")

(deftest forge-registers-only-its-parent-resolver-test
  (MultipartEntityApi/unregisterParentResolver resolver-id)
  (try
    (multipart/register-parent-resolver!)
    (is (some #{resolver-id}
              (MultipartEntityApi/registeredResolverIds)))
    (finally
      (MultipartEntityApi/unregisterParentResolver resolver-id))))
