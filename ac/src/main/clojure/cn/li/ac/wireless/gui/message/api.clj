(ns cn.li.ac.wireless.gui.message.api
  "Aggregated wireless GUI message catalog for validation and lookup."
  (:require [cn.li.mcmod.gui.message.dsl :as msg-dsl]
            [cn.li.ac.wireless.gui.message.registry :as registry]))

(defn catalog []
  (registry/build-catalog))

(defn msg
  [domain action]
  (registry/msg domain action))

(defn find-by-msg-id
  [message-id]
  (msg-dsl/find-by-msg-id (catalog) message-id))
