(ns cn.li.mcbase.datagen.lang-data
  "Shared language file data for datagen.
  
  Provides base translations and merging logic for multi-language support
  (English and Simplified Chinese). Can be consumed by both Forge and Fabric
  lang providers."
  )

(defn base-lang-data
  "Get base translations for creative tab and Minecraft-owned generic commands.

  Returns: map with keys for all supported language JSON files, each containing
  translation key→value pairs for commands and UI elements"
  []
  {"en_us.json"
    {"itemGroup.academy.items" "Academy"
    "command.mcmod.grant_advancement.success" "Granted advancement %s to %s"
    "command.mcmod.grant_advancement.not_found" "Advancement not found: %s"
    "tooltip.academy.energy_info" "%s ⚡ / %s ⚡"}

   "zh_cn.json"
    {"itemGroup.academy.items" "Academy"
    "command.mcmod.grant_advancement.success" "已授予进度 %s 给 %s"
    "command.mcmod.grant_advancement.not_found" "未找到进度: %s"
    "tooltip.academy.energy_info" "%s ⚡ / %s ⚡"}

   "zh_tw.json"
    {"itemGroup.academy.items" "Academy"
    "command.mcmod.grant_advancement.success" "已授予進度 %s 給 %s"
    "command.mcmod.grant_advancement.not_found" "未找到進度: %s"
    "tooltip.academy.energy_info" "%s ⚡ / %s ⚡"}

   "ja_jp.json"
    {"itemGroup.academy.items" "Academy"
    "command.mcmod.grant_advancement.success" "進捗 %s を %s に付与しました"
    "command.mcmod.grant_advancement.not_found" "進捗が見つかりません: %s"
    "tooltip.academy.energy_info" "%s ⚡ / %s ⚡"}

   "ko_kr.json"
    {"itemGroup.academy.items" "Academy"
    "command.mcmod.grant_advancement.success" "%s에게 발전 과제 %s을(를) 부여했습니다"
    "command.mcmod.grant_advancement.not_found" "발전 과제를 찾을 수 없습니다: %s"
    "tooltip.academy.energy_info" "%s ⚡ / %s ⚡"}

   "ru_ru.json"
    {"itemGroup.academy.items" "Academy"
    "command.mcmod.grant_advancement.success" "Достижение %s выдано %s"
    "command.mcmod.grant_advancement.not_found" "Достижение не найдено: %s"
    "tooltip.academy.energy_info" "%s ⚡ / %s ⚡"}})

(defn merged-lang-data
  "Merge base translations with metadata-provided translations.

  Args:
    translation-maps-fn: function returning {:en_us {...} :zh_cn {...} ...}
                        from datagen metadata source

  Returns: map with keys for all supported language JSON files, each containing
  merged base + metadata translations (metadata takes precedence on conflicts)"
  [translation-maps-fn]
  (let [{:keys [en_us zh_cn zh_tw ja_jp ko_kr ru_ru]} (translation-maps-fn)
        base (base-lang-data)]
    {"en_us.json" (merge (get base "en_us.json") en_us)
     "zh_cn.json" (merge (get base "zh_cn.json") zh_cn)
     "zh_tw.json" (merge (get base "zh_tw.json") zh_tw)
     "ja_jp.json" (merge (get base "ja_jp.json") ja_jp)
     "ko_kr.json" (merge (get base "ko_kr.json") ko_kr)
     "ru_ru.json" (merge (get base "ru_ru.json") ru_ru)}))
