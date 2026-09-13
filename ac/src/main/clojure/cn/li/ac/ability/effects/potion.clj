(ns cn.li.ac.ability.effects.potion
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]))

(defn available?
  []
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/get-adapter fw-atom :potion-effects))))

(defn apply-effect!
  [player-uuid effect-type duration amplifier]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :potion-effects :apply-potion-effect!
                            player-uuid effect-type duration amplifier))))

(defn remove-effect!
  [player-uuid effect-type]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :potion-effects :remove-potion-effect!
                            player-uuid effect-type))))

(defn has-effect?
  [player-uuid effect-type]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :potion-effects :has-potion-effect?
                            player-uuid effect-type))))

(defn clear-all!
  [player-uuid]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :potion-effects :clear-all-effects!
                            player-uuid))))

(defn- apply-potion!
  [evt {:keys [target effect-id ticks amplifier]}]
  (when (and (available?) target)
    (let [uuid (or (when (map? target) (:uuid target))
                   (get evt target))]
      (when uuid
        (apply-effect! uuid effect-id (int ticks) (int amplifier)))))
  evt)

(defn execute-potion!
  [evt {:keys [target effect-id ticks amplifier]}]
  (apply-potion! evt {:target target :effect-id effect-id :ticks ticks :amplifier amplifier}))

(defn execute-potion-roll!
  [evt {:keys [chance] :as params}]
  (if (< (rand) (double (or chance 1.0)))
    (apply-potion! evt (dissoc params :chance))
    evt))

