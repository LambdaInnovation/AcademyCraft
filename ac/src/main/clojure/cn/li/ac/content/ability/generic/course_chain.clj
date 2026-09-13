(ns cn.li.ac.content.ability.generic.course-chain
  "Shared metadata and passive registration for the generic course skill chain."
  (:require [cn.li.ac.ability.definition-core :as definition-core]
            [cn.li.ac.ability.passive :as passive]
            [cn.li.ac.ability.registry.event :as evt]))

(def categories
  [:electromaster :meltdowner :teleporter :vecmanip])

(def ^:private course-definitions
  {:brain-course
   {:id-suffix "brain-course"
    :name-key "ability.skill.generic.brain_course"
    :description-key "ability.skill.generic.brain_course.desc"
    :icon "textures/abilities/generic/skills/brain_course.png"
    :ui-position [30 110]
    :level 3
    :ctrl-id :brain-course
    :conditions [{:type :any-skill-level :level 3}]
    :effects [{:handler-suffix "max-cp"
               :event evt/CALC-MAX-CP
               :transform (fn [value _event] (+ value 1000.0))}]
    :translations
    {:en_us {"ability.skill.generic.brain_course" "Brain Course"
             "ability.skill.generic.brain_course.desc" "Undergo focused neural training to raise your maximum CP by 1000."}
     :zh_cn {"ability.skill.generic.brain_course" "脑域课程"
             "ability.skill.generic.brain_course.desc" "接受针对脑域的基础训练，使最大 CP 提高 1000。"}
     :zh_tw {"ability.skill.generic.brain_course" "大腦訓練課程"
             "ability.skill.generic.brain_course.desc" "鍛鍊能力者的大腦，使其能力的有效持續時間延長。提高最大計算力1000點。"}
     :ja_jp {"ability.skill.generic.brain_course" "ブレイン・コース"
             "ability.skill.generic.brain_course.desc" "脳のための訓練を開始し、スキルをより長く使用できるようにします。"}
     :ko_kr {"ability.skill.generic.brain_course" "브레인 코스"
             "ability.skill.generic.brain_course.desc" "당신의 뇌를 위한 훈련을 받으세요. 최대 CP가 1000만큼 증가합니다."}
     :ru_ru {"ability.skill.generic.brain_course" "Мозговой курс"
             "ability.skill.generic.brain_course.desc" "Пройти тренировку для вашего мозга. Увеличивает Макс. CP на 1000."}}}

   :brain-course-advanced
   {:id-suffix "brain-course-advanced"
    :name-key "ability.skill.generic.brain_course_advanced"
    :description-key "ability.skill.generic.brain_course_advanced.desc"
    :icon "textures/abilities/generic/skills/brain_course_advanced.png"
    :ui-position [115 110]
    :level 4
    :ctrl-id :brain-course-advanced
    :prerequisites [{:skill-id-suffix "brain-course" :min-exp 0.0}]
    :conditions [{:type :any-skill-level :level 4}]
    :effects [{:handler-suffix "max-cp"
               :event evt/CALC-MAX-CP
               :transform (fn [value _event] (+ value 1500.0))}
              {:handler-suffix "max-overload"
               :event evt/CALC-MAX-OVERLOAD
               :transform (fn [value _event] (+ value 100.0))}]
    :translations
    {:en_us {"ability.skill.generic.brain_course_advanced" "Advanced Brain Course"
             "ability.skill.generic.brain_course_advanced.desc" "Deepen your neural development to raise maximum CP by 1500 and maximum overload by 100."}
     :zh_cn {"ability.skill.generic.brain_course_advanced" "脑域进阶课程"
             "ability.skill.generic.brain_course_advanced.desc" "进一步开发脑部思维能力，使最大 CP 提高 1500，最大过载提高 100。"}
     :zh_tw {"ability.skill.generic.brain_course_advanced" "大腦訓練課程 (高階)"
             "ability.skill.generic.brain_course_advanced.desc" "更深入有效的開發大腦的思維能力。提高最大計算力1000點，最大過載100點。"}
     :ja_jp {"ability.skill.generic.brain_course_advanced" "ブレイン・コース・アドバンスド"
             "ability.skill.generic.brain_course_advanced.desc" "さらに深く、効率的に思考能力を開発します。これにより、スキル保持時間と忍耐力を向上させます。"}
     :ko_kr {"ability.skill.generic.brain_course_advanced" "브레인 고급 코스"
             "ability.skill.generic.brain_course_advanced.desc" "여러분의 두뇌의 사고력을 깊이 발전시키세요. 최대 CP는 1000개, 최대 과부하는 100개 증가합니다."}
     :ru_ru {"ability.skill.generic.brain_course_advanced" "Продвинутый курс мозга"
             "ability.skill.generic.brain_course_advanced.desc" "Развить мыслительные способности вашего мозга глубже. Увеличивает Макс. CP на 1000 и Макс. Перегрузку на 100."}}}

   :mind-course
   {:id-suffix "mind-course"
    :name-key "ability.skill.generic.mind_course"
    :description-key "ability.skill.generic.mind_course.desc"
    :icon "textures/abilities/generic/skills/mind_course.png"
    :ui-position [205 110]
    :level 5
    :ctrl-id :mind-course
    :prerequisites [{:skill-id-suffix "brain-course-advanced" :min-exp 0.0}]
    :conditions [{:type :any-skill-level :level 5}]
    :effects [{:handler-suffix "cp-recovery"
               :event evt/CALC-CP-RECOVER-SPEED
               :transform (fn [value _event] (* value 1.2))}]
    :translations
    {:en_us {"ability.skill.generic.mind_course" "Mind Course"
             "ability.skill.generic.mind_course.desc" "Train your mental composure so CP recovers 20% faster."}
     :zh_cn {"ability.skill.generic.mind_course" "心智课程"
             "ability.skill.generic.mind_course.desc" "学习更高效地放松精神，使 CP 恢复速度提高 20%。"}
     :zh_tw {"ability.skill.generic.mind_course" "思維修養課程"
             "ability.skill.generic.mind_course.desc" "學習如何更好的放鬆自己的思想。計算力恢復速率提高20%。"}
     :ja_jp {"ability.skill.generic.mind_course" "マインド・コース"
             "ability.skill.generic.mind_course.desc" "脳をよりリラックスさせる方法を学び、CPの回復速度を向上させます。"}
     :ko_kr {"ability.skill.generic.mind_course" "마인드 코스"
             "ability.skill.generic.mind_course.desc" "여러분의 뇌를 더 잘 이완시키는 방법을 배우세요. CP 복구 속도를 20% 향상시킵니다."}
     :ru_ru {"ability.skill.generic.mind_course" "Курс разума"
             "ability.skill.generic.mind_course.desc" "Узнайте, как лучше расслабить свой мозг. Ускоряет восстановление CP на 20%."}}}})

(defn skill-id
  [cat-id id-suffix]
  (keyword (name cat-id) id-suffix))

(defn course-definition
  [course-key]
  (or (get course-definitions course-key)
      (throw (ex-info "Unknown generic course definition"
                      {:course-key course-key}))))

(defn build-skill-specs
  [course-key]
  (let [{:keys [id-suffix name-key description-key icon ui-position level ctrl-id
                prerequisites conditions translations]}
        (course-definition course-key)]
    (mapv (fn [cat-id]
            (-> (definition-core/build-skill-spec
                  (symbol (name cat-id) id-suffix)
                  {:id (skill-id cat-id id-suffix)
                   :category-id cat-id
                   :name-key name-key
                   :description-key description-key
                   :icon icon
                   :ui-position ui-position
                   :level level
                   :controllable? false
                   :ctrl-id ctrl-id
                   :pattern :passive
                   :prerequisites (mapv (fn [{:keys [skill-id-suffix min-exp]}]
                                          {:skill-id (skill-id cat-id skill-id-suffix)
                                           :min-exp min-exp})
                                        prerequisites)
                   :conditions conditions
                   :translations translations})
                (assoc :ac/content-type :skill)))
          categories)))

(defn register-passive-hooks!
  [course-key]
  (let [{:keys [id-suffix effects]} (course-definition course-key)]
    (doseq [cat-id categories
            {:keys [handler-suffix event transform]} effects]
      (passive/register-passive-calc-handler!
       (keyword (name cat-id) (str id-suffix "/" handler-suffix))
       event
       (skill-id cat-id id-suffix)
       transform))))