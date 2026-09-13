(ns cn.li.ac.achievement.registry
  "Query helpers over achievement metadata."
  (:require [cn.li.ac.achievement.data :as data]))

(defn all-achievements
  []
  data/achievements)

(defn all-tabs
  []
  data/tabs)

(defn get-by-id
  [achievement-id]
  (some #(when (= achievement-id (:id %)) %) data/achievements))

(defn by-tab
  [tab-id]
  (filter #(= tab-id (:tab %)) data/achievements))

(defn translation-maps
  "Return locale map merged from all achievement entries.
   Supports: en_us, zh_cn, zh_tw, ja_jp, ko_kr, ru_ru"
  []
  (reduce
    (fn [acc ach]
      (-> acc
          (update :en_us merge (get-in ach [:translation :en_us] {}))
          (update :zh_cn merge (get-in ach [:translation :zh_cn] {}))
          (update :zh_tw merge (get-in ach [:translation :zh_tw] {}))
          (update :ja_jp merge (get-in ach [:translation :ja_jp] {}))
          (update :ko_kr merge (get-in ach [:translation :ko_kr] {}))
          (update :ru_ru merge (get-in ach [:translation :ru_ru] {}))))
    {:en_us {} :zh_cn {} :zh_tw {} :ja_jp {} :ko_kr {} :ru_ru {}}
    data/achievements))

(defn find-by-trigger
  [kind payload]
  (keep (fn [{:keys [trigger-key id]}]
          (when (and trigger-key
                     (= kind (:kind trigger-key))
                     (case kind
                       :level-change
                       (and (= (:category payload) (:category trigger-key))
                            (= (:level payload) (:level trigger-key)))
                       :skill-learn
                       (= (:skill-id payload) (:skill-id trigger-key))
                       :skill-perform
                       (= (:skill-id payload) (:skill-id trigger-key))
                       :custom
                       (= (:event-id payload) (:event-id trigger-key))
                       false))
            id))
        data/achievements))

