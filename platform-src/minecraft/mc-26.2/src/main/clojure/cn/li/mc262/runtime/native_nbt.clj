(ns cn.li.mc262.runtime.native-nbt
  "Thin re-export of cn.li.mcbase.runtime.native-nbt."
  (:require [cn.li.mcbase.runtime.native-nbt :as shared]))

(def encode-value shared/encode-value)
(def decode-value shared/decode-value)
