(ns cn.li.ac.ability.skill-destroy-gate-test
  "Skill-level destroy gate — upstream Skill.shouldDestroyBlocks()
   (getOptionalBool(\"destroy_blocks\", true)): a skill whose config disables
   block destruction must not break blocks even when the global Settings
   \"Destroy blocks\" toggle is on."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.registry.skill :as skill-reg]))

(defn- with-skill [skill-id spec f]
  (skill-reg/install-skill-registry-runtime!
    (skill-reg/create-skill-registry-runtime))
  (skill-reg/register-skill! spec)
  (try
    (f)
    (finally
      (skill-reg/install-skill-registry-runtime!
        (skill-reg/create-skill-registry-runtime)))))

(deftest skill-destroy-allowed-defaults-to-true-test
  (with-skill :test-skill {:id :test-skill :category-id :test-cat :level 1 :pattern :instant :actions {:perform! (fn [& _] nil)}}
    (fn []
      (is (true? (skill-effects/skill-destroy-allowed? :test-skill))
          "missing :destroy-blocks? defaults to true like upstream"))))

(deftest skill-destroy-allowed-honors-config-test
  (with-skill :test-skill {:id :test-skill :category-id :test-cat :level 1 :pattern :instant
                           :actions {:perform! (fn [& _] nil)}
                           :destroy-blocks? false}
    (fn []
      (is (false? (skill-effects/skill-destroy-allowed? :test-skill))
          "config-disabled skill may not destroy blocks"))))

(deftest skill-destroy-allowed-unknown-skill-safe-test
  (is (true? (skill-effects/skill-destroy-allowed? :no-such-skill))
      "unknown skill stays permissive (no registry entry)"))
