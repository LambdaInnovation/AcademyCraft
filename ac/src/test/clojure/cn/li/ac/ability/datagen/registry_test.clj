(ns cn.li.ac.ability.datagen.registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.datagen.registry :as ability-datagen]
                                          [cn.li.ac.ability.runtime-container :as runtime-container]
                                          [cn.li.ac.ability.item-actions :as item-actions]
            [cn.li.ac.content.ability :as ability-content]
            [cn.li.mcmod.datagen.metadata :as metadata]))

(deftest register-datagen-metadata-populates-ability-translations
       (item-actions/reset-item-action-registries!)
       (runtime-container/install-ability-runtime-container!
              (runtime-container/create-ability-runtime-container))
  (ability-content/init-ability-content!)
  (metadata/reset-datagen-metadata-for-test!)

  (ability-datagen/register-datagen-metadata!)

  (let [translations (metadata/get-translation-maps)]

  (testing "ability skill translation keys are exported"
    (is (= "Brain Course"
           (get-in translations [:en_us "ability.skill.generic.brain_course"])))
    (is (= "Undergo focused neural training to raise your maximum CP by 1000."
           (get-in translations [:en_us "ability.skill.generic.brain_course.desc"])))
    (is (= "Advanced Brain Course"
           (get-in translations [:en_us "ability.skill.generic.brain_course_advanced"])))
    (is (= "Deepen your neural development to raise maximum CP by 1500 and maximum overload by 100."
           (get-in translations [:en_us "ability.skill.generic.brain_course_advanced.desc"])))
    (is (= "Mind Course"
           (get-in translations [:en_us "ability.skill.generic.mind_course"])))
    (is (= "Train your mental composure so CP recovers 20% faster."
           (get-in translations [:en_us "ability.skill.generic.mind_course.desc"])))
    (is (= "Flesh Ripping"
           (get-in translations [:en_us "ability.skill.teleporter.flesh_ripping"])))
    (is (= "Rip apart your enemy's inside and inflict gory damage. The over-bloody effect may make you feel uncomfortable."
           (get-in translations [:en_us "ability.skill.teleporter.flesh_ripping.desc"])))
    (is (= "Dimension Folding Theorem"
           (get-in translations [:en_us "ability.skill.teleporter.dim_folding_theorem"])))
    (is (= "Exploit weak spatial folding during teleportation attacks to split space unpredictably and amplify the final hit."
           (get-in translations [:en_us "ability.skill.teleporter.dim_folding_theorem.desc"])))
    (is (= "Critical Hit %s"
           (get-in translations [:en_us "ability.teleporter.critical_hit"]))))

  (testing "zh_cn receives teleporter-specific overrides"
    (is (= "脑域课程"
           (get-in translations [:zh_cn "ability.skill.generic.brain_course"])))
    (is (= "接受针对脑域的基础训练，使最大 CP 提高 1000。"
           (get-in translations [:zh_cn "ability.skill.generic.brain_course.desc"])))
    (is (= "脑域进阶课程"
           (get-in translations [:zh_cn "ability.skill.generic.brain_course_advanced"])))
    (is (= "进一步开发脑部思维能力，使最大 CP 提高 1500，最大过载提高 100。"
           (get-in translations [:zh_cn "ability.skill.generic.brain_course_advanced.desc"])))
    (is (= "心智课程"
           (get-in translations [:zh_cn "ability.skill.generic.mind_course"])))
    (is (= "学习更高效地放松精神，使 CP 恢复速度提高 20%。"
           (get-in translations [:zh_cn "ability.skill.generic.mind_course.desc"])))
    (is (= "肌体撕裂"
           (get-in translations [:zh_cn "ability.skill.teleporter.flesh_ripping"])))
    (is (= "折叠空间概论"
           (get-in translations [:zh_cn "ability.skill.teleporter.dim_folding_theorem"])))
    (is (= "利用传送攻击中不稳定的微弱空间折叠现象，让空间产生概率性的分裂，从而进一步提高伤害。"
           (get-in translations [:zh_cn "ability.skill.teleporter.dim_folding_theorem.desc"])))
    (is (= "暴击 %s"
           (get-in translations [:zh_cn "ability.teleporter.critical_hit"]))))))
