(ns cn.li.ac.block.ores
  "Ore blocks and shared misc material blocks."
  (:require [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]
            [cn.li.ac.config.modid :as modid]))

;; ============================================================================
;; Ore Definitions
;; ============================================================================

;; Avoid top-level registration side effects during namespace load.
(defn init-ores!
  []
  (install/framework-once! ::ores-installed?
  (fn []
    (bdsl/register-block!
      (bdsl/create-block-spec
        "constrained-ore"
        {:registry-name "constraint_metal"
         :physical {:material :stone
                    :hardness 4.0
                     :resistance 20.0
                    :requires-tool true
                    :harvest-tool :pickaxe
                    :harvest-level 1
                     :sounds :stone}
         :loot {:type :ore :drop-item "constraint_metal" :min-count 1 :max-count 2}
         :rendering {:model-parent "minecraft:block/cube_all"
                     :textures {:all (modid/asset-path "block" "constraint_metal")}
                     :flat-item-icon? true}}))
    (bdsl/register-block!
      (bdsl/create-block-spec
        "imaginary-ore"
        {:registry-name "imagsil_ore"
         :physical {:material :stone
                    :hardness 3.75
                     :resistance 18.75
                    :requires-tool true
                    :harvest-tool :pickaxe
                    :harvest-level 2
                     :sounds :stone}
         :loot {:type :ore :drop-item "imagsil_ore" :min-count 1 :max-count 2}
         :rendering {:model-parent "minecraft:block/cube_all"
                     :textures {:all (modid/asset-path "block" "imagsil_ore")}
                     :flat-item-icon? true}}))
    (bdsl/register-block!
      (bdsl/create-block-spec
        "reso-ore"
        {:registry-name "reso_ore"
         :physical {:material :stone
                    :hardness 3.0
                     :resistance 15.0
                    :requires-tool true
                    :harvest-tool :pickaxe
                    :harvest-level 2
                     :sounds :stone}
         :loot {:type :ore :drop-item "crystal_low" :min-count 1 :max-count 3}
         :rendering {:model-parent "minecraft:block/cube_all"
                     :textures {:all (modid/asset-path "block" "reso_ore")}
                     :flat-item-icon? true}}))
    (bdsl/register-block!
      (bdsl/create-block-spec
        "crystal-ore"
        {:registry-name "crystal_ore"
         :physical {:material :stone
                    :hardness 3.0
                     :resistance 15.0
                    :requires-tool true
                    :harvest-tool :pickaxe
                    :harvest-level 2
                     :sounds :stone}
         :loot {:type :ore :drop-item "reso_crystal" :min-count 1 :max-count 2}
         :rendering {:model-parent "minecraft:block/cube_all"
                     :textures {:all (modid/asset-path "block" "crystal_ore")}
                     :flat-item-icon? true}}))
    (bdsl/register-block!
      (bdsl/create-block-spec
        "machine-frame"
        {:registry-name "machine_frame"
         ;; Upstream is a bare `new Block(Material.ROCK)` (ACBlocks.java) — no
         ;; setHardness/setResistance/setHarvestLevel, so it inherits the 1.12
         ;; Block defaults: hardness 2.0, resistance 10.0, pickaxe of ANY tier
         ;; (harvestLevel -1; ROCK material reports "pickaxe"). Level 0 here =
         ;; wood tier = any pickaxe, matching that.
         :physical {:material :stone
                    :hardness 2.0
                    :resistance 10.0
                    :requires-tool true
                    :harvest-tool :pickaxe
                    :harvest-level 0
                    :sounds :stone}
         :rendering {:model-parent "minecraft:block/cube_all"
                     :textures {:all (modid/asset-path "block" "machine_frame")}
                     :flat-item-icon? true}}))
    (log/debug "Initialized ore blocks"))))
