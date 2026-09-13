(ns cn.li.ac.content.ability.shared.vec-reflection-interaction
  "Shared vec-reflection beam interaction for railgun / meltdowner.

  Matches original's ctx.attackReflect/ReflectEvent: reflection triggers
  unconditionally whenever the target's VecReflection toggle is active, with
  no CP check and no CP cost. attackReflect() cancels the event and skips the
  normal attack() call entirely, bypassing the LivingHurtEvent pipeline — so
  VecReflectionContext's own CP-consuming handleAttack/consumeDamage (which
  only fires from onLivingAttack/onLivingHurt) never runs for these
  beam-type reflects.

  No Minecraft imports."
  (:require [cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.content.ability.vecmanip.vec-reflection :as vec-reflection]))

(defn build-reflection-callbacks
  "Build :reflect-can-fn / :reflect-shot-fn for effect.beam/execute-beam!

  Options:
    :ctx-id            context id for FX
    :attacker-pos-fn   (fn [] -> {:x :y :z}) the beam caster's position, for the
                       defender-side wave
    :reflect-shot-fn   (fn [ctx-id reflector-player-uuid] -> truthy when hit)"
  [{:keys [ctx-id attacker-pos-fn reflect-shot-fn]}]
  {:reflect-can-fn
   (fn [target-player-uuid _incoming-damage]
     (boolean (toggle/toggle-active-for-player? target-player-uuid :vec-reflection)))

   :reflect-shot-fn
   (fn [target-player-uuid _incoming-damage]
     ;; onReflect fires on the DEFENDER's context and puts a wave in front of
     ;; them; the reflected beam itself is the caster's business below.
     (vec-reflection/notify-beam-reflected!
       target-player-uuid
       (when attacker-pos-fn (attacker-pos-fn)))
     (reflect-shot-fn ctx-id target-player-uuid))})
