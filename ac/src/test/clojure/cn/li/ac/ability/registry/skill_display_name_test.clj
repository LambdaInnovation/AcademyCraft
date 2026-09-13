(ns cn.li.ac.ability.registry.skill-display-name-test
  "skill-display-name localization: :name-key translation wins when the key
  resolves, raw keyword name falls back when it does not (the platform
  translate-fn returns the key itself for missing keys)."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.content.ability :as content-ability]
            [cn.li.ac.ability.integration.external-providers :as external-providers]
            [cn.li.ac.ability.registry.skill :as skill-reg]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.mcmod.i18n :as i18n]))

(defn- with-skill-registry [f]
  (skill-reg/install-skill-registry-runtime!
    (skill-reg/create-skill-registry-runtime))
  (with-redefs [external-providers/load-external-providers! (fn [] nil)]
    (content-ability/init-ability-content!))
  (try
    (f)
    (finally
      (skill-reg/install-skill-registry-runtime!
        (skill-reg/create-skill-registry-runtime)))))

(deftest skill-display-name-localizes-via-name-key-test
  (with-skill-registry
    (fn []
      (testing "translated name-key wins"
        (with-redefs [i18n/*translate-fn* (fn [k _args] (str "TR[" k "]"))]
          (is (= "TR[ability.skill.electromaster.arc_gen]"
                 (skill-query/skill-display-name :arc-gen)))))
      (testing "untranslated key falls back to the raw name"
        (with-redefs [i18n/*translate-fn* (fn [k _args] (str k))]
          (is (= "arc-gen" (skill-query/skill-display-name :arc-gen)))))
      (testing "unknown skill falls back to the keyword name"
        (is (= "no-such-skill" (skill-query/skill-display-name :no-such-skill)))))))
