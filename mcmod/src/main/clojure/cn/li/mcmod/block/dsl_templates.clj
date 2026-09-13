(ns cn.li.mcmod.block.dsl-templates
  "Block template helpers for common data shapes.
   Provides convenient builders for ore blocks, wood, metal, multiblocks, etc."
  (:require [cn.li.mcmod.block.dsl-multiblock :as mb]))

;; ============================================================================
;; Simple Material Templates
;; ============================================================================

(defn ore-template
  "Create an ore block template with common properties"
  [harvest-level]
  {:physical {:material :stone
              :hardness 3.0
              :resistance 3.0
              :requires-tool true
              :harvest-tool :pickaxe
              :harvest-level harvest-level
              :sounds :stone}})

(defn wood-template
  "Create a wood block template with common properties"
  []
  {:physical {:material :wood
              :hardness 2.0
              :resistance 3.0
              :requires-tool false
              :harvest-tool :axe
              :harvest-level 0
              :sounds :wood}})

(defn metal-template
  "Create a metal block template with common properties"
  [harvest-level]
  {:physical {:material :metal
              :hardness 5.0
              :resistance 6.0
              :requires-tool true
              :harvest-tool :pickaxe
              :harvest-level harvest-level
              :sounds :metal}})

(defn glass-template
  "Create a glass block template with common properties"
  []
  {:physical {:material :glass
              :hardness 0.3
              :resistance 0.3
              :requires-tool false
              :sounds :glass}})

(defn light-block-template
  "Create a light-emitting block template"
  [light-level]
  {:physical {:material :glass
              :hardness 1.0
              :resistance 1.0
              :sounds :glass}
   :rendering {:light-level light-level}})

(defn- deep-merge
  [& maps]
  (apply merge-with
         (fn [x y]
           (if (and (map? x) (map? y))
             (apply deep-merge [x y])
             y))
         maps))

;; ============================================================================
;; Multi-block Templates
;; ============================================================================

(defn multi-block-template
  "Create a regular multi-block template.
   size: {:width 2 :height 3 :depth 2}
   Example: (multi-block-template {:width 3 :height 4 :depth 3})"
  [size & additional-options]
  (apply deep-merge
         {:multi-block {:size size
                        :origin {:x 0 :y 0 :z 0}}
          :physical {:material :metal
                     :hardness 5.0
                     :resistance 10.0
                     :requires-tool true
                     :harvest-tool :pickaxe}}
         additional-options))

(defn irregular-multi-block-template
  "Create an irregular multi-block template with custom positions.
   positions: [{:x 0 :y 0 :z 0} {:x 1 :y 0 :z 0} {:x 0 :y 1 :z 0} ...]
   Example: (irregular-multi-block-template [{:x 0 :y 0 :z 0} {:x 1 :y 1 :z 0}])"
  [positions & additional-options]
  (apply deep-merge
         {:multi-block {:positions (mb/normalize-positions positions)
                        :origin {:x 0 :y 0 :z 0}}
          :physical {:material :metal
                     :hardness 5.0
                     :resistance 10.0
                     :requires-tool true
                     :harvest-tool :pickaxe}}
         additional-options))

;; ============================================================================
;; Template Combination
;; ============================================================================

(defn merge-templates
  "Merge multiple templates with options"
  [& template-and-options]
  (apply deep-merge template-and-options))