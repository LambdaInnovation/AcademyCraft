(ns cn.li.ac.block.blockstate-datagen-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.block.blockstate-datagen :as datagen]
            [cn.li.ac.block.blockstate-definition :as blockstate-def]
            [cn.li.ac.block.wireless-node.blockstate :as node-blockstate]
            [cn.li.mcmod.config :as mcmod-config]
            [cn.li.mcmod.protocol.metadata :as metadata]))

(deftest properties-from-schema-match-runtime-shape-test
  (with-redefs [mcmod-config/mod-id "academy"]
    (let [fusor-def (get (datagen/complex-definitions-map) :imag-fusor)
          former-def (get (datagen/complex-definitions-map) :metal-former)
          fusor (:properties fusor-def)
          former (:properties former-def)]
      (is (contains? fusor :facing))
      (is (contains? fusor :frame))
      (is (= :horizontal-facing (:type (:facing fusor))))
      (is (= 20 (count (:parts fusor-def))))
      (is (contains? former :facing))
      (is (not (contains? former :frame)))
      (is (= 4 (count (:parts former-def)))))))

(deftest ability-interferer-on-parts-test
  (with-redefs [mcmod-config/mod-id "academy"]
    (let [def (get (datagen/complex-definitions-map) :ability-interferer)]
      (is (= #{true false}
             (set (map #(get-in % [:condition :on]) (:parts def)))))
      (is (= ["academy:block/ability_interferer"
              "academy:block/ability_interf_off"]
             (map first (map :models (:parts def))))))))

(deftest blockstate-definition-uses-schema-datagen-test
  (with-redefs [mcmod-config/mod-id "academy"
                metadata/get-all-block-ids (fn [] [])
                metadata/get-block-registry-name identity]
    (let [def (blockstate-def/get-block-state-definition :metal-former)]
      (is (= "metal_former" (:registry-name def)))
      (is (= 4 (count (:parts def))))
      (is (= "academy:block/metal_former_north"
             (first (:models (first (:parts def)))))))))

(deftest state-driven-shape-blocks-use-exclusive-conditional-parts-test
  (with-redefs [mcmod-config/mod-id "academy"]
    (let [complex-defs (datagen/complex-definitions-map)
          node-defs (node-blockstate/get-all-node-definitions)
          target-defs [(get complex-defs :ability-interferer)
                       (get complex-defs :imag-fusor)
                       (get complex-defs :metal-former)
                       (get node-defs :wireless-node-basic)
                       (get node-defs :wireless-node-standard)
                       (get node-defs :wireless-node-advanced)]]
      (doseq [definition target-defs]
        (is (some? definition))
        (let [parts (:parts definition)]
          ;; Guardrail: no unconditional base/overlay stacking for state-driven models.
          (is (every? map? (map :condition parts))
              (str "all parts must be conditional for " (:registry-name definition)))
          ;; Sanity: every condition map should be unique (mutually exclusive mapping).
          (is (= (count parts) (count (set (map :condition parts))))
              (str "conditions must be unique for " (:registry-name definition))))))))
