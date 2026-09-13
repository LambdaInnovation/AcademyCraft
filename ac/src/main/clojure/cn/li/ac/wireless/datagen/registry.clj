(ns cn.li.ac.wireless.datagen.registry
  "Datagen registry for wireless domain.
   Provides lightweight metadata output for wireless configuration and labels."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.ac.wireless.config :as wireless-config]
            [cn.li.mcmod.datagen.metadata :as metadata]))

(defn- descriptor->entry
  [{:keys [path comment default]}]
  [(str "config." modid/MOD-ID "." path)
   (str comment " (default=" default ")")])

(defn- wireless-translation-map
  []
  (let [descriptor-entries (into {} (map descriptor->entry wireless-config/descriptors))]
    {:en_us (merge
              {(str "domain." modid/MOD-ID ".wireless") "Wireless System"
               (str "wireless." modid/MOD-ID ".network") "Wireless Network"
               (str "wireless." modid/MOD-ID ".node") "Wireless Node"}
              descriptor-entries)
     :zh_cn (merge
              {(str "domain." modid/MOD-ID ".wireless") "无线系统"
               (str "wireless." modid/MOD-ID ".network") "无线网络"
               (str "wireless." modid/MOD-ID ".node") "无线节点"}
              descriptor-entries)
     :zh_tw (merge
              {(str "domain." modid/MOD-ID ".wireless") "無線系統"
               (str "wireless." modid/MOD-ID ".network") "無線網路"
               (str "wireless." modid/MOD-ID ".node") "無線節點"}
              descriptor-entries)
     :ja_jp (merge
              {(str "domain." modid/MOD-ID ".wireless") "無線システム"
               (str "wireless." modid/MOD-ID ".network") "無線ネットワーク"
               (str "wireless." modid/MOD-ID ".node") "無線ノード"}
              descriptor-entries)
     :ko_kr (merge
              {(str "domain." modid/MOD-ID ".wireless") "무선 시스템"
               (str "wireless." modid/MOD-ID ".network") "무선 네트워크"
               (str "wireless." modid/MOD-ID ".node") "무선 노드"}
              descriptor-entries)
     :ru_ru (merge
              {(str "domain." modid/MOD-ID ".wireless") "Беспроводная система"
               (str "wireless." modid/MOD-ID ".network") "Беспроводная сеть"
               (str "wireless." modid/MOD-ID ".node") "Беспроводной узел"}
              descriptor-entries)}))

(defn register-datagen-metadata!
  "Register wireless domain's datagen content.
   Called during datagen initialization phase."
  []
  (let [translation-map (wireless-translation-map)]
    (metadata/merge-translations! translation-map)
    {:domain :wireless
     :translations (count (:en_us translation-map))
    :config-descriptors (count wireless-config/descriptors)}))
