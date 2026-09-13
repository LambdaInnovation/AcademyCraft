(ns cn.li.ac.config.gameplay-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.config.common :as config-common]
            [cn.li.ac.config.gameplay :as gameplay]
            [cn.li.mcmod.config.registry :as config-reg]))

(deftest gameplay-default-branch-test
  (try
    (gameplay/init-config!)
    (is (false? (gameplay/use-mouse-wheel-enabled?)))
    (is (true? (gameplay/give-cloud-terminal-enabled?)))
    (is (= "Microsoft YaHei" (gameplay/get-font)))
    (finally
      (config-reg/set-config-values! config-common/gameplay-domain gameplay/default-values))))

(deftest gameplay-registry-override-test
  (try
    (gameplay/init-config!)
    (config-reg/set-config-values!
     config-common/gameplay-domain
     {:use-mouse-wheel true
      :give-cloud-terminal false
      :font "Inter"})
    (is (true? (gameplay/use-mouse-wheel-enabled?)))
    (is (false? (gameplay/give-cloud-terminal-enabled?)))
    (is (= "Inter" (gameplay/get-font)))
    (finally
      (config-reg/set-config-values! config-common/gameplay-domain gameplay/default-values))))

(deftest gameplay-validate-contract-test
  (testing "valid defaults pass"
    (try
      (gameplay/init-config!)
      (is (= nil (gameplay/validate-config!)))
      (finally
        (config-reg/set-config-values! config-common/gameplay-domain gameplay/default-values))))
  (testing "invalid numeric registry values fail validation"
    (try
      (gameplay/init-config!)
      (config-reg/set-config-values!
       config-common/gameplay-domain
       {:font 42})
      (try
        (gameplay/validate-config!)
        (is false)
        (catch clojure.lang.ExceptionInfo e
          (let [errors (:errors (ex-data e))]
            (is (some #(= % "font must be a string") errors)))))
      (finally
        (config-reg/set-config-values! config-common/gameplay-domain gameplay/default-values)))))
