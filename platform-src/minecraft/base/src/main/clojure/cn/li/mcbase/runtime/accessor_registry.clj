(ns cn.li.mcbase.runtime.accessor-registry
  "Explicit bootstrap for built-in runtime accessor declarations."
  (:require [cn.li.mcbase.runtime.accessor-registry-world :as world]
            [cn.li.mcbase.runtime.accessor-registry-entity :as entity]
            [cn.li.mcbase.runtime.accessor-registry-render :as render]
            [cn.li.mcbase.runtime.accessor-registry-lifecycle :as lifecycle]))

(defn init-default-accessors!
  "Register all built-in accessor declarations.

  Requiring this namespace only loads declarations; registration happens when
  this function is explicitly invoked by platform bootstrap."
  []
  (world/register-world-accessors!)
  (entity/register-entity-accessors!)
  (render/register-render-accessors!)
  (lifecycle/register-lifecycle-accessors!)
  nil)
