(ns cn.li.mc1201.gui.reactive.bake-slots
  "Install native id class then re-export shared bake-slots."
  (:require [cn.li.mcbase.gui.reactive.bake-slots :as shared])
  (:import [cn.li.mcver ResourceLocations]))

(shared/install-id-class! (ResourceLocations/idClass))

(def bake-asserts-enabled? shared/bake-asserts-enabled?)
(def assert-bake-slots! shared/assert-bake-slots!)
(def maybe-assert-bake-slots! shared/maybe-assert-bake-slots!)
