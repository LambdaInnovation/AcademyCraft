(ns cn.li.mcmod.runtime.keyboard-input-provider
  "Neutral keyboard-dispatch callback for platform event adapters."
  (:require [cn.li.mcmod.protocol.keyboard-input :as keyboard-input]))

(defn runtime-provider [_]
  {:emit-keyboard-input! #'keyboard-input/emit-keyboard-input!})
