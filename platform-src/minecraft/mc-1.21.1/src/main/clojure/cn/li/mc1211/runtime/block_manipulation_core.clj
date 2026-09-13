(ns cn.li.mc1211.runtime.block-manipulation-core
  "Thin re-export of cn.li.mcbase.runtime.block-manipulation-core."
  (:require [cn.li.mcbase.runtime.block-manipulation-core :as shared]))

(def get-level-by-id shared/get-level-by-id)
(def break-block! shared/break-block!)
(def can-break-block? shared/can-break-block?)
(def set-block! shared/set-block!)
(def get-block shared/get-block)
(def get-block-hardness shared/get-block-hardness)
(def block-collidable? shared/block-collidable?)
(def find-blocks-in-line shared/find-blocks-in-line)
(def liquid-block? shared/liquid-block?)
(def requires-high-tier-tool? shared/requires-high-tier-tool?)
(def farmland-block? shared/farmland-block?)
