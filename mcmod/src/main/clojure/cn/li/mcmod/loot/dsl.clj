(ns cn.li.mcmod.loot.dsl
  "Loot injection DSL for declarative loot table augmentation."
  (:require [cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))





(defrecord LootInjectionSpec
  [id target-table item-id weight quality min-count max-count])

(defn create-loot-injection-spec
  [injection-id options]
  (map->LootInjectionSpec
    {:id injection-id
     :target-table (:target-table options)
     :item-id (:item-id options)
     :weight (int (or (:weight options) 1))
     :quality (int (or (:quality options) 0))
     :min-count (float (or (:min-count options) 1.0))
     :max-count (float (or (:max-count options) 1.0))}))

(defn register-loot-injection!
  [spec]
  (when-not (string? (:id spec))
    (throw (ex-info "Loot injection :id must be string" {:spec spec})))
  (when-not (string? (:target-table spec))
    (throw (ex-info "Loot injection :target-table must be string" {:spec spec})))
  (when-not (string? (:item-id spec))
    (throw (ex-info "Loot injection :item-id must be string" {:spec spec})))
  (log/debug "Registering loot injection:" (:id spec) "table:" (:target-table spec) "item:" (:item-id spec))
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:registry :loot (:id spec)] spec))
  spec)

(defn list-loot-injections [] (keys (get-in @(fw/fw-atom) [:registry :loot])))
(defn get-loot-injection [injection-id] (get-in @(fw/fw-atom) [:registry :loot injection-id]))

(defn get-loot-injections-for-table
  [target-table]
  (->> (get-in @(fw/fw-atom) [:registry :loot])
       vals
       (filter (fn [spec] (= (:target-table spec) target-table)))))

(defmacro defloot
  [injection-name & options]
  (if (map? injection-name)
    (let [options-map injection-name
          injection-id (:id options-map)]
      (when-not (string? injection-id)
        (throw (ex-info "Map-form defloot requires string :id" {:form options-map})))
      `(register-loot-injection!
         (create-loot-injection-spec ~injection-id ~(dissoc options-map :id))))
    (let [options-map (if (and (= 1 (count options)) (map? (first options)))
                        (first options)
                        (apply hash-map options))
          injection-id (or (:id options-map) (name injection-name))]
      `(def ~injection-name
         (register-loot-injection!
           (create-loot-injection-spec ~injection-id ~(dissoc options-map :id)))))))
