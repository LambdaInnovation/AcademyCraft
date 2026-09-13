(ns cn.li.mc1211.runtime.damage-interception-core
  "Thin re-export of cn.li.mcbase.runtime.damage-interception-core."
  (:require [cn.li.mcbase.runtime.damage-interception-core :as shared]))

(def make-damage-interception shared/make-damage-interception)
(def install-damage-interception! shared/install-damage-interception!)
(def should-allow-attack? shared/should-allow-attack?)
(def attack-precheck-result shared/attack-precheck-result)
(def process-damage shared/process-damage)
(def damage-process-result shared/damage-process-result)
(def allow-attack? shared/allow-attack?)
(def apply-attack-result! shared/apply-attack-result!)
(def rewrite-damage shared/rewrite-damage)
(def apply-damage-result! shared/apply-damage-result!)
