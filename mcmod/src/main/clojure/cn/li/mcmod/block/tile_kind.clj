(ns cn.li.mcmod.block.tile-kind
  "Declarative tile-kind defaults merged at bundle compile time (registration phase only).

  State stored in Framework [:registry :tile-kinds]."
  (:require [cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; Tile Kind Registry — stored in Framework [:registry :tile-kinds]

(def ^:private tile-kind-path [:registry :tile-kinds])

(defn- tile-kind-state []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom tile-kind-path {})
    {}))

(defn register-tile-kind!
  "Register reusable tile logic defaults by tile-kind keyword."
  [tile-kind cfg]
  (when-not (keyword? tile-kind)
    (throw (ex-info "register-tile-kind!: tile-kind must be keyword"
                    {:tile-kind tile-kind})))
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in tile-kind-path
           (fn [current] (assoc (or current {}) tile-kind cfg))))
  (log/debug "Registered tile-kind logic" tile-kind)
  nil)

(defn merge-with-kind
  "Merge kind-cfg with normalized. Nil values in normalized do not override kind defaults."
  [kind-cfg normalized]
  (when-not (and (map? kind-cfg)
                 (or (map? normalized) (nil? normalized)))
    (throw (ex-info "merge-with-kind expects map kind-cfg and map-or-nil normalized"
                    {:kind-cfg kind-cfg :normalized normalized})))
  (reduce-kv (fn [m k v]
               (if (nil? v)
                 m
                 (assoc m k v)))
             (or kind-cfg {})
             (or normalized {})))

(defn get-tile-kind
  "Get registered tile-kind cfg (nil if missing)."
  [tile-kind]
  (get (tile-kind-state) tile-kind))

(defn merge-tile-kind-defaults
  "Merge :tile-kind defaults into a tile spec map for bundle compilation."
  [spec]
  (let [kind-cfg (when-let [k (:tile-kind spec)]
                   (get-tile-kind k))
        hook-keys [:tick-fn :read-nbt-fn :write-nbt-fn :container :capability-keys]]
    (if kind-cfg
      (reduce (fn [m k]
                (if (nil? (get m k))
                  (assoc m k (get kind-cfg k))
                  m))
              spec hook-keys)
      spec)))

(defn list-tile-kinds
  "Return all registered tile-kind keywords."
  []
  (keys (tile-kind-state)))

(defn snapshot-tile-kinds
  "Full snapshot for bundle compilation."
  []
  (tile-kind-state))

(defn reset-tile-kinds-for-test!
  "Clear tile-kind registry for tests."
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in tile-kind-path {}))
  nil)
