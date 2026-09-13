(ns cn.li.mc1201.item.item-properties
  "Vanilla item property assembly and ScriptedItem/NbtBarItem construction.

  Loaders supply a with-owner callback for player-scoped runtime (Forge) or
  a no-op (Fabric)."
  (:require [cn.li.mc1201.runtime.item-callback :as item-callback]
            [cn.li.mcbase.item.spec :as item-spec])
  (:import [net.minecraft.world.food FoodProperties$Builder]
           [net.minecraft.world.item Item Item$Properties Rarity]
           [cn.li.mc1201.item NbtBarItem ScriptedItem]))

(defn- rarity->mc-rarity
  ^Rarity
  [rarity-kw]
  (case rarity-kw
    :uncommon Rarity/UNCOMMON
    :rare Rarity/RARE
    :epic Rarity/EPIC
    Rarity/COMMON))

(defn apply-item-properties
  ^Item$Properties
  [^Item$Properties base item-spec]
  (let [max-stack-size (:max-stack-size item-spec)
        durability (:durability item-spec)
        rarity (:rarity item-spec)
        food-props (:food-properties item-spec)
        food (when food-props
               (let [^FoodProperties$Builder b (FoodProperties$Builder.)
                     _ (.nutrition b (int (or (:nutrition food-props) 0)))
                     _ (.saturationMod b (float (or (:saturation food-props) 0.0)))]
                 (when (:meat food-props) (.meat b))
                 (when (:fast-to-eat food-props) (.fast b))
                 (when (:always-edible food-props) (.alwaysEat b))
                 (.build b)))]
    (cond-> base
      (some? max-stack-size) (.stacksTo (int max-stack-size))
      (some? durability) (.durability (int durability))
      (some? rarity) (.rarity (rarity->mc-rarity rarity))
      (some? food) (.food food))))

(defn create-standalone-item
  "Build a ScriptedItem or NbtBarItem from DSL spec.

  `with-owner` is `(fn [player side f] (f))` — Forge wraps player session,
  Fabric may pass identity."
  (^Item [item-spec]
   (create-standalone-item item-spec (fn [_player _side f] (f))))
  (^Item [item-spec with-owner]
   (let [props (apply-item-properties (Item$Properties.) item-spec)
         {:keys [energy-item? enchantability tooltip-lines current-key max-key default-max bar-color]}
         (item-spec/standalone-values item-spec)
         callback (item-callback/build "mc1201" with-owner)]
     (if energy-item?
       (NbtBarItem. props current-key max-key default-max bar-color callback)
       (ScriptedItem. props enchantability tooltip-lines callback)))))
