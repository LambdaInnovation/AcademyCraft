(ns cn.li.ac.ability.datagen.registry
  "Datagen registry for ability domain: achievements and recipes.
   Registers metadata during datagen phase for inclusion in JSON outputs."
  (:require [clojure.string :as str]
            [cn.li.ac.ability.datagen.ac-content-translations :as ac-content]
            [cn.li.ac.ability.datagen.skill-translations :as skill-translations]
            [cn.li.ac.ability.datagen.teleporter-translations :as teleporter-translations]
            [cn.li.ac.achievement.registry :as achievement-registry]
            [cn.li.ac.achievement.data :as achievement-data]
            [cn.li.ac.ability.registry.category :as category-registry]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.recipe.crafting-recipes :as crafting-recipes]
            [cn.li.mcmod.datagen.metadata :as metadata]))

(defn- title-case-id
  [v]
  (let [text (-> (name v)
        (str/replace #"[-_]" " "))]
    (->> (str/split text #"\s+")
      (remove str/blank?)
      (map str/capitalize)
      (str/join " "))))

(defn- explicit-skill-translations
  [skills locale]
  (into {}
        (mapcat (fn [{:keys [translations]}]
                  (seq (get translations locale {}))))
        skills))

(defn- ability-translation-map
  []
  (let [categories (category-registry/get-all-categories)
      skills (skill-query/list-skills)
      teleporter-translation-map (teleporter-translations/translation-map)
      skill-translation-map (skill-translations/translation-map)
      ac-content-map (ac-content/translation-map)
        category-name-entries
        (into {}
              (keep (fn [{:keys [id name-key]}]
                      (when (and (keyword? id) (string? name-key))
                        [name-key (title-case-id id)])))
              categories)
        skill-name-entries
        (into {}
              (keep (fn [{:keys [id name-key]}]
                      (when (and (keyword? id) (string? name-key))
                        [name-key (title-case-id id)])))
              skills)
        skill-desc-entries
        (into {}
              (keep (fn [{:keys [id description-key]}]
                      (when (and (keyword? id) (string? description-key))
                        [description-key (str "Ability skill: " (title-case-id id))])))
              skills)
        explicit-en (explicit-skill-translations skills :en_us)
        explicit-zh (explicit-skill-translations skills :zh_cn)
        explicit-tw (explicit-skill-translations skills :zh_tw)
        explicit-ja (explicit-skill-translations skills :ja_jp)
        explicit-ko (explicit-skill-translations skills :ko_kr)
        explicit-ru (explicit-skill-translations skills :ru_ru)
        ;; en is the base: auto-generated names + AC content + skill translations
        en (merge category-name-entries
                  skill-name-entries
                  skill-desc-entries
                  (:en_us ac-content-map)
                  (:en_us skill-translation-map)
                  explicit-en
                  (:en_us teleporter-translation-map))
        ;; other locales: fall back to en, layer AC content, central skill translations, explicit per-skill, and teleporter
        zh (merge en (:zh_cn ac-content-map) (:zh_cn skill-translation-map) explicit-zh (:zh_cn teleporter-translation-map))
        tw (merge en (:zh_tw ac-content-map) (:zh_tw skill-translation-map) explicit-tw (:zh_tw teleporter-translation-map))
        ja (merge en (:ja_jp ac-content-map) (:ja_jp skill-translation-map) explicit-ja (:ja_jp teleporter-translation-map))
        ko (merge en (:ko_kr ac-content-map) (:ko_kr skill-translation-map) explicit-ko (:ko_kr teleporter-translation-map))
        ru (merge en (:ru_ru ac-content-map) (:ru_ru skill-translation-map) explicit-ru (:ru_ru teleporter-translation-map))]
      {:en_us en
       :zh_cn zh
       :zh_tw tw
       :ja_jp ja
       :ko_kr ko
       :ru_ru ru}))

(def ^:private command-translation-map
  {:en_us
   {"command.academy.acach.missing_advancement" "Missing advancement argument"
    "command.academy.aim.cat.success" "Switched to category: %s"
    "command.academy.aim.cat.current" "Current category: %s"
    "command.academy.aim.cat.none" "No ability category selected"
    "command.academy.aim.level.current" "Current level: %s"
    "command.academy.aim.node.exp.current" "%s experience: %s%%"
    "command.academy.aim.cat.not_found" "Category not found: %s"
    "command.academy.aim.catlist.success" "Available categories: %s"
    "command.academy.aim.catlist.empty" "No categories available"
    "command.academy.aim.reset.success" "Reset all abilities"
    "command.academy.aim.node.learn.success" "Learned node: %s"
    "command.academy.aim.node.learn.not_found" "Node not found: %s"
    "command.academy.aim.node.learn.already" "Already learned: %s"
    "command.academy.aim.node.unlearn.success" "Unlearned node: %s"
    "command.academy.aim.node.unlearn.not_found" "Node not found: %s"
    "command.academy.aim.node.learn_all.success" "Learned all nodes in category"
    "command.academy.aim.node.learned.list" "Learned nodes: %s"
    "command.academy.aim.node.list" "Available nodes: %s"
    "command.academy.aim.level.success" "Set level to %s"
    "command.academy.aim.level.invalid" "Invalid level: %s (must be 1-5)"
    "command.academy.aim.node.exp.success" "Set experience for %s to %s"
    "command.academy.aim.node.exp.not_found" "Node not found: %s"
    "command.academy.aim.node.exp.invalid" "Invalid experience value: %s (must be 0.0-1.0)"
    "command.academy.aim.fullcp.success" "Restored CP to full"
    "command.academy.aim.cd_clear.success" "Cleared all cooldowns"
    "command.academy.aim.maxout.success" "Maxed out progression"
    "command.academy.aim.max_all.success" "Level 5, all skills learned and fully mastered"
    "command.academy.aim.nonecathint" "No ability category selected. Run \"/aim cat <category>\" first."
    "command.academy.aim.cheats_on.success" "Enabled cheat mode"
    "command.academy.aim.cheats_off.success" "Disabled cheat mode"
    "command.academy.aim.notactive" "Cheat mode is off. Run \"/aim cheats_on\" first."
    "command.academy.aim.help" "Available commands: cat, catlist, reset, learn, unlearn, learn_all, learned, nodes, level, exp, fullcp, cd_clear, maxout, max_all, help, cheats_on, cheats_off"}
   :zh_cn
   {"command.academy.acach.missing_advancement" "缺少进度参数"
    "command.academy.aim.cat.success" "已切换到类别: %s"
    "command.academy.aim.cat.current" "当前系别: %s"
    "command.academy.aim.cat.none" "尚未选择能力系别"
    "command.academy.aim.level.current" "当前等级: %s"
    "command.academy.aim.node.exp.current" "%s 熟练度: %s%%"
    "command.academy.aim.cat.not_found" "未找到类别: %s"
    "command.academy.aim.catlist.success" "可用类别: %s"
    "command.academy.aim.catlist.empty" "没有可用类别"
    "command.academy.aim.reset.success" "已重置所有能力"
    "command.academy.aim.node.learn.success" "已学习节点: %s"
    "command.academy.aim.node.learn.not_found" "未找到节点: %s"
    "command.academy.aim.node.learn.already" "已经学习过: %s"
    "command.academy.aim.node.unlearn.success" "已遗忘节点: %s"
    "command.academy.aim.node.unlearn.not_found" "未找到节点: %s"
    "command.academy.aim.node.learn_all.success" "已学习类别中的所有节点"
    "command.academy.aim.node.learned.list" "已学习的节点: %s"
    "command.academy.aim.node.list" "可用节点: %s"
    "command.academy.aim.level.success" "已设置等级为 %s"
    "command.academy.aim.level.invalid" "无效等级: %s (必须为 1-5)"
    "command.academy.aim.node.exp.success" "已设置 %s 的经验值为 %s"
    "command.academy.aim.node.exp.not_found" "未找到节点: %s"
    "command.academy.aim.node.exp.invalid" "无效经验值: %s (必须为 0.0-1.0)"
    "command.academy.aim.fullcp.success" "已恢复CP至满值"
    "command.academy.aim.cd_clear.success" "已清除所有冷却"
    "command.academy.aim.maxout.success" "已最大化进度"
    "command.academy.aim.max_all.success" "已设为 5 级、学会全部技能并全部满经验"
    "command.academy.aim.nonecathint" "尚未选择能力系别，请先执行 \"/aim cat <category>\"。"
    "command.academy.aim.cheats_on.success" "已启用作弊模式"
    "command.academy.aim.cheats_off.success" "已禁用作弊模式"
    "command.academy.aim.notactive" "作弊模式未启用，请先执行 \"/aim cheats_on\"。"
    "command.academy.aim.help" "可用命令: cat, catlist, reset, learn, unlearn, learn_all, learned, nodes, level, exp, fullcp, cd_clear, maxout, max_all, help, cheats_on, cheats_off"}
   :zh_tw
   {"command.academy.acach.missing_advancement" "缺少進度參數"
    "command.academy.aim.cat.success" "已切換到類別: %s"
    "command.academy.aim.cat.current" "目前系別: %s"
    "command.academy.aim.cat.none" "尚未選擇能力系別"
    "command.academy.aim.level.current" "目前等級: %s"
    "command.academy.aim.node.exp.current" "%s 熟練度: %s%%"
    "command.academy.aim.cat.not_found" "未找到類別: %s"
    "command.academy.aim.catlist.success" "可用類別: %s"
    "command.academy.aim.catlist.empty" "沒有可用類別"
    "command.academy.aim.reset.success" "已重置所有能力"
    "command.academy.aim.node.learn.success" "已學習節點: %s"
    "command.academy.aim.node.learn.not_found" "未找到節點: %s"
    "command.academy.aim.node.learn.already" "已經學習過: %s"
    "command.academy.aim.node.unlearn.success" "已遺忘節點: %s"
    "command.academy.aim.node.unlearn.not_found" "未找到節點: %s"
    "command.academy.aim.node.learn_all.success" "已學習類別中的所有節點"
    "command.academy.aim.node.learned.list" "已學習的節點: %s"
    "command.academy.aim.node.list" "可用節點: %s"
    "command.academy.aim.level.success" "已設定等級為 %s"
    "command.academy.aim.level.invalid" "無效等級: %s (必須為 1-5)"
    "command.academy.aim.node.exp.success" "已設定 %s 的經驗值為 %s"
    "command.academy.aim.node.exp.not_found" "未找到節點: %s"
    "command.academy.aim.node.exp.invalid" "無效經驗值: %s (必須為 0.0-1.0)"
    "command.academy.aim.fullcp.success" "已恢復CP至滿值"
    "command.academy.aim.cd_clear.success" "已清除所有冷卻"
    "command.academy.aim.maxout.success" "已最大化進度"
    "command.academy.aim.max_all.success" "已設為 5 級、學會全部技能並全部滿經驗"
    "command.academy.aim.nonecathint" "尚未選擇能力系別，請先執行 \"/aim cat <category>\"。"
    "command.academy.aim.cheats_on.success" "已啟用作弊模式"
    "command.academy.aim.cheats_off.success" "已禁用作弊模式"
    "command.academy.aim.notactive" "作弊模式未啟用，請先執行 \"/aim cheats_on\"。"
    "command.academy.aim.help" "可用命令: cat, catlist, reset, learn, unlearn, learn_all, learned, nodes, level, exp, fullcp, cd_clear, maxout, max_all, help, cheats_on, cheats_off"}
   :ja_jp
   {"command.academy.acach.missing_advancement" "進捗引数がありません"
    "command.academy.aim.cat.success" "カテゴリを切り替えました: %s"
    "command.academy.aim.cat.current" "現在の系統: %s"
    "command.academy.aim.cat.none" "能力系統が未選択です"
    "command.academy.aim.level.current" "現在のレベル: %s"
    "command.academy.aim.node.exp.current" "%s の熟練度: %s%%"
    "command.academy.aim.cat.not_found" "カテゴリが見つかりません: %s"
    "command.academy.aim.catlist.success" "利用可能なカテゴリ: %s"
    "command.academy.aim.catlist.empty" "利用可能なカテゴリがありません"
    "command.academy.aim.reset.success" "すべての能力をリセットしました"
    "command.academy.aim.node.learn.success" "ノードを学習しました: %s"
    "command.academy.aim.node.learn.not_found" "ノードが見つかりません: %s"
    "command.academy.aim.node.learn.already" "既に学習済みです: %s"
    "command.academy.aim.node.unlearn.success" "ノードの学習を解除しました: %s"
    "command.academy.aim.node.unlearn.not_found" "ノードが見つかりません: %s"
    "command.academy.aim.node.learn_all.success" "カテゴリ内のすべてのノードを学習しました"
    "command.academy.aim.node.learned.list" "学習済みノード: %s"
    "command.academy.aim.node.list" "利用可能なノード: %s"
    "command.academy.aim.level.success" "レベルを %s に設定しました"
    "command.academy.aim.level.invalid" "無効なレベル: %s (1-5の範囲で指定してください)"
    "command.academy.aim.node.exp.success" "%s の経験値を %s に設定しました"
    "command.academy.aim.node.exp.not_found" "ノードが見つかりません: %s"
    "command.academy.aim.node.exp.invalid" "無効な経験値: %s (0.0-1.0の範囲で指定してください)"
    "command.academy.aim.fullcp.success" "CPを最大まで回復しました"
    "command.academy.aim.cd_clear.success" "すべてのクールダウンを解除しました"
    "command.academy.aim.maxout.success" "進行度を最大にしました"
    "command.academy.aim.max_all.success" "レベル5・全スキル習得・全スキル熟練度最大にしました"
    "command.academy.aim.nonecathint" "能力系統が未選択です。先に \"/aim cat <category>\" を実行してください。"
    "command.academy.aim.cheats_on.success" "チートモードを有効にしました"
    "command.academy.aim.cheats_off.success" "チートモードを無効にしました"
    "command.academy.aim.notactive" "チートモードが無効です。先に \"/aim cheats_on\" を実行してください。"
    "command.academy.aim.help" "使用可能なコマンド: cat, catlist, reset, learn, unlearn, learn_all, learned, nodes, level, exp, fullcp, cd_clear, maxout, max_all, help, cheats_on, cheats_off"}
   :ko_kr
   {"command.academy.acach.missing_advancement" "발전 과제 인수가 없습니다"
    "command.academy.aim.cat.success" "카테고리를 전환했습니다: %s"
    "command.academy.aim.cat.current" "현재 계열: %s"
    "command.academy.aim.cat.none" "능력 계열이 선택되지 않았습니다"
    "command.academy.aim.level.current" "현재 레벨: %s"
    "command.academy.aim.node.exp.current" "%s 숙련도: %s%%"
    "command.academy.aim.cat.not_found" "카테고리를 찾을 수 없습니다: %s"
    "command.academy.aim.catlist.success" "사용 가능한 카테고리: %s"
    "command.academy.aim.catlist.empty" "사용 가능한 카테고리가 없습니다"
    "command.academy.aim.reset.success" "모든 능력을 초기화했습니다"
    "command.academy.aim.node.learn.success" "노드를 학습했습니다: %s"
    "command.academy.aim.node.learn.not_found" "노드를 찾을 수 없습니다: %s"
    "command.academy.aim.node.learn.already" "이미 학습했습니다: %s"
    "command.academy.aim.node.unlearn.success" "노드 학습을 해제했습니다: %s"
    "command.academy.aim.node.unlearn.not_found" "노드를 찾을 수 없습니다: %s"
    "command.academy.aim.node.learn_all.success" "카테고리의 모든 노드를 학습했습니다"
    "command.academy.aim.node.learned.list" "학습한 노드: %s"
    "command.academy.aim.node.list" "사용 가능한 노드: %s"
    "command.academy.aim.level.success" "레벨을 %s(으)로 설정했습니다"
    "command.academy.aim.level.invalid" "유효하지 않은 레벨: %s (1-5 사이여야 합니다)"
    "command.academy.aim.node.exp.success" "%s의 경험치를 %s(으)로 설정했습니다"
    "command.academy.aim.node.exp.not_found" "노드를 찾을 수 없습니다: %s"
    "command.academy.aim.node.exp.invalid" "유효하지 않은 경험치: %s (0.0-1.0 사이여야 합니다)"
    "command.academy.aim.fullcp.success" "CP를 최대치까지 회복했습니다"
    "command.academy.aim.cd_clear.success" "모든 쿨다운을 해제했습니다"
    "command.academy.aim.maxout.success" "진행도를 최대로 설정했습니다"
    "command.academy.aim.max_all.success" "레벨 5, 모든 스킬 습득 및 숙련도 최대로 설정했습니다"
    "command.academy.aim.nonecathint" "능력 계열이 선택되지 않았습니다. 먼저 \"/aim cat <category>\" 를 실행하세요."
    "command.academy.aim.cheats_on.success" "치트 모드를 활성화했습니다"
    "command.academy.aim.cheats_off.success" "치트 모드를 비활성화했습니다"
    "command.academy.aim.notactive" "치트 모드가 꺼져 있습니다. 먼저 \"/aim cheats_on\" 을 실행하세요."
    "command.academy.aim.help" "사용 가능한 명령어: cat, catlist, reset, learn, unlearn, learn_all, learned, nodes, level, exp, fullcp, cd_clear, maxout, max_all, help, cheats_on, cheats_off"}
   :ru_ru
   {"command.academy.acach.missing_advancement" "Отсутствует аргумент достижения"
    "command.academy.aim.cat.success" "Переключено на категорию: %s"
    "command.academy.aim.cat.current" "Текущая категория: %s"
    "command.academy.aim.cat.none" "Категория способностей не выбрана"
    "command.academy.aim.level.current" "Текущий уровень: %s"
    "command.academy.aim.node.exp.current" "Опыт %s: %s%%"
    "command.academy.aim.cat.not_found" "Категория не найдена: %s"
    "command.academy.aim.catlist.success" "Доступные категории: %s"
    "command.academy.aim.catlist.empty" "Нет доступных категорий"
    "command.academy.aim.reset.success" "Все способности сброшены"
    "command.academy.aim.node.learn.success" "Узел изучен: %s"
    "command.academy.aim.node.learn.not_found" "Узел не найден: %s"
    "command.academy.aim.node.learn.already" "Уже изучено: %s"
    "command.academy.aim.node.unlearn.success" "Узел забыт: %s"
    "command.academy.aim.node.unlearn.not_found" "Узел не найден: %s"
    "command.academy.aim.node.learn_all.success" "Все узлы категории изучены"
    "command.academy.aim.node.learned.list" "Изученные узлы: %s"
    "command.academy.aim.node.list" "Доступные узлы: %s"
    "command.academy.aim.level.success" "Уровень установлен на %s"
    "command.academy.aim.level.invalid" "Неверный уровень: %s (должен быть 1-5)"
    "command.academy.aim.node.exp.success" "Опыт для %s установлен на %s"
    "command.academy.aim.node.exp.not_found" "Узел не найден: %s"
    "command.academy.aim.node.exp.invalid" "Неверное значение опыта: %s (должно быть 0.0-1.0)"
    "command.academy.aim.fullcp.success" "CP полностью восстановлено"
    "command.academy.aim.cd_clear.success" "Все перезарядки сброшены"
    "command.academy.aim.maxout.success" "Прогресс максимизирован"
    "command.academy.aim.max_all.success" "Уровень 5, все навыки изучены и полностью освоены"
    "command.academy.aim.nonecathint" "Категория способностей не выбрана. Сначала выполните \"/aim cat <category>\"."
    "command.academy.aim.cheats_on.success" "Режим читов включен"
    "command.academy.aim.cheats_off.success" "Режим читов выключен"
    "command.academy.aim.notactive" "Режим читов выключен. Сначала выполните \"/aim cheats_on\"."
    "command.academy.aim.help" "Доступные команды: cat, catlist, reset, learn, unlearn, learn_all, learned, nodes, level, exp, fullcp, cd_clear, maxout, max_all, help, cheats_on, cheats_off"}})

(defn register-datagen-metadata!
  "Register ability domain's datagen content into shared metadata registry.
   Called during datagen initialization phase."
  []
  (let [achievement-tabs (achievement-registry/all-tabs)
        achievements achievement-data/achievements
        recipes (crafting-recipes/get-all-recipes)
        achievement-translation-map (achievement-registry/translation-maps)
        ability-translation-map* (ability-translation-map)]
    (metadata/set-achievement-tabs! achievement-tabs)
    (metadata/set-achievements! achievements)
    (metadata/merge-translations! achievement-translation-map)
    (metadata/merge-translations! ability-translation-map*)
    (metadata/merge-translations! command-translation-map)
    (metadata/set-recipes! recipes)))
