(ns cn.li.ac.datagen.bootstrap
  "AC datagen metadata bootstrap.

  AC registers this through mcmod.lifecycle so platform datagen entrypoints do
  not depend on AC business namespaces."
  (:require [cn.li.ac.ability.datagen.registry :as ability-datagen]
            [cn.li.ac.block.datagen.registry :as block-datagen]
            [cn.li.ac.block.platform-bridge :as block-bridge]
            [cn.li.ac.datagen.worldgen :as worldgen-datagen]
            [cn.li.ac.wireless.datagen.registry :as wireless-datagen]
            [cn.li.ac.energy.datagen.registry :as energy-datagen]))

(defn register-datagen-metadata!
  "Register datagen metadata from all AC business domains.

  Each domain registry populates shared mcmod.datagen.metadata atoms used by
  selected Minecraft datagen providers."
  []
  ;; Blockstate/model providers consult mcmod blockstate hooks at generation time.
  ;; Install AC overrides here so node multipart and texture rules are applied.
  (block-bridge/install-blockstate-hooks!)
  (ability-datagen/register-datagen-metadata!)
  ;; Machine recipes must be collected after ability recipes (set-recipes!
  ;; replaces); block-datagen reads existing recipes and appends machine ones.
  (block-datagen/register-datagen-metadata!)
  (wireless-datagen/register-datagen-metadata!)
  (energy-datagen/register-datagen-metadata!)
  (worldgen-datagen/register-datagen-metadata!)
  nil)
