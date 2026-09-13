(ns cn.li.fabric1211.registry.creative-tab
  "Fabric creative-tab key mapping shared by metadata-driven population."
  (:import [net.minecraft.world.item CreativeModeTabs]))

(defn resolve-tab-key [tab]
  (case tab
    :building-blocks CreativeModeTabs/BUILDING_BLOCKS
    :colored-blocks CreativeModeTabs/COLORED_BLOCKS
    :natural-blocks CreativeModeTabs/NATURAL_BLOCKS
    :functional-blocks CreativeModeTabs/FUNCTIONAL_BLOCKS
    :redstone-blocks CreativeModeTabs/REDSTONE_BLOCKS
    :tools CreativeModeTabs/TOOLS_AND_UTILITIES
    :tools-and-utilities CreativeModeTabs/TOOLS_AND_UTILITIES
    :combat CreativeModeTabs/COMBAT
    :food CreativeModeTabs/FOOD_AND_DRINKS
    :food-and-drinks CreativeModeTabs/FOOD_AND_DRINKS
    :spawn-eggs CreativeModeTabs/SPAWN_EGGS
    :op-blocks CreativeModeTabs/OP_BLOCKS
    CreativeModeTabs/INGREDIENTS))
