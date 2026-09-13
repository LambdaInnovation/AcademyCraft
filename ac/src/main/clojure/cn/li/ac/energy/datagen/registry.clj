(ns cn.li.ac.energy.datagen.registry
  "Datagen registry for energy domain.
   Registers shared energy labels and baseline energy type metadata."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.ac.energy.imaginary-energy-impl :as imaginary-energy]
            [cn.li.mcmod.datagen.metadata :as metadata]))

(defn- energy-translation-map
  []
  {:en_us {(str "domain." modid/MOD-ID ".energy") "Energy System"
           (str "energy." modid/MOD-ID ".imaginary") "Imaginary Energy"
           (str "energy." modid/MOD-ID ".unit.if") "IF"}
   :zh_cn {(str "domain." modid/MOD-ID ".energy") "能量系统"
           (str "energy." modid/MOD-ID ".imaginary") "幻想能量"
           (str "energy." modid/MOD-ID ".unit.if") "IF"}
   :zh_tw {(str "domain." modid/MOD-ID ".energy") "能量系統"
           (str "energy." modid/MOD-ID ".imaginary") "幻想能量"
           (str "energy." modid/MOD-ID ".unit.if") "IF"}
   :ja_jp {(str "domain." modid/MOD-ID ".energy") "エネルギーシステム"
           (str "energy." modid/MOD-ID ".imaginary") "虚数エネルギー"
           (str "energy." modid/MOD-ID ".unit.if") "IF"}
   :ko_kr {(str "domain." modid/MOD-ID ".energy") "에너지 시스템"
           (str "energy." modid/MOD-ID ".imaginary") "상상 에너지"
           (str "energy." modid/MOD-ID ".unit.if") "IF"}
   :ru_ru {(str "domain." modid/MOD-ID ".energy") "Энергетическая система"
           (str "energy." modid/MOD-ID ".imaginary") "Воображаемая энергия"
           (str "energy." modid/MOD-ID ".unit.if") "IF"}})

(defn register-datagen-metadata!
  "Register energy domain's datagen content.
   Called during datagen initialization phase."
  []
  (imaginary-energy/register-default-energy-type!)
  (let [translation-map (energy-translation-map)]
    (metadata/merge-translations! translation-map)
    {:domain :energy
     :translations (count (:en_us translation-map))
     :energy-type :imaginary-energy}))
