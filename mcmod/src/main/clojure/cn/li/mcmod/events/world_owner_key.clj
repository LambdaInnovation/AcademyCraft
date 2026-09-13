(ns cn.li.mcmod.events.world-owner-key
  "Stable world owner keys for runtime caches/registries without MC reflection."
  (:require [cn.li.mcmod.platform.world :as world]))

(defn- require-owner-value
  [world label value]
  (if (some? value)
    value
    (throw (ex-info (format "World owner requires %s" label)
                    {:world world
                     :required label}))))

(defn server-session-id
  [world]
  (require-owner-value
    world
    ":server-session-id"
    (cond
      (map? world) (or (:server-session-id world) (:session-id world))
      (or (keyword? world) (string? world) (symbol? world) (number? world)) nil
      :else (world/server-session-id world))))

(defn world-id
  [world]
  (require-owner-value
    world
    ":world-id"
    (cond
      (map? world) (or (:world-id world) (:dimension-id world))
      (or (keyword? world) (string? world) (symbol? world) (number? world)) nil
      :else (world/dimension-id world))))

(defn world-key
  "Return [server-session-id dimension-id] for a world owner (map or platform Level)."
  [world]
  [(server-session-id world) (world-id world)])
