(ns cn.li.mcbase.runtime.damage-interception-core
  "Shared Minecraft-side damage interception helpers (no loader API imports)."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.platform.neutral.hooks :as damage-hooks]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcver McAccess]
           [cn.li.mcbase.runtime DamageInterception]
           [net.minecraft.server.level ServerPlayer]
           [net.minecraft.world.damagesource DamageSource]
           [net.minecraft.world.entity Entity]))

(defn make-damage-interception
  []
  {:register-damage-handler! (fn [handler-id handler-fn priority]
                               (damage-hooks/register-damage-handler! handler-id handler-fn priority))
   :unregister-damage-handler! (fn [handler-id]
                                 (damage-hooks/unregister-damage-handler! handler-id))
   :get-active-handlers (fn []
                          (damage-hooks/get-active-damage-handlers))})

(declare rewrite-damage)

(defn install-damage-interception!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (platform/install-adapter! fw-atom :damage-interception (make-damage-interception))
    (DamageInterception/installModifier
     (fn [entity damage-source amount]
       (rewrite-damage entity damage-source amount))))
  nil)

(defn should-allow-attack?
  [player-id attacker-id original-damage damage-source]
  (not (damage-hooks/should-cancel-attack-interception?
         player-id attacker-id original-damage damage-source)))

(defn- attacker-id
  [^DamageSource damage-source]
  (when-let [^Entity attacker (.getEntity damage-source)]
    (str (.getUUID attacker))))

(defn- with-damaged-player-owner
  "Bind the damaged player's server owner for the duration of f.

  Hurt events fire on the server thread OUTSIDE any player action context --
  content damage handlers (vec-reflection's attack precheck, etc.) resolve
  the victim's player-state via :player-state-owner/:client-session-id, so
  without this binding they throw 'requires bound session-id' and the
  handler silently degrades. The server session id matches the one
  lifecycle-core's server-owner builds ([:server identityHashCode])."
  [^ServerPlayer player f]
  (damage-hooks/with-client-ctx-fn
    {:player-owner {:server-session-id [:server (System/identityHashCode (McAccess/serverOf player))]
                    :player-uuid (str (.getUUID player))}}
    f))

(defn attack-precheck-result
  "Return shared attack precheck result for player damage, or nil when the
  damaged entity is not a server player. Platform event layers decide how to
  apply the result to their native event/callback object."
  [entity damage-source amount]
  (when (instance? ServerPlayer entity)
    (let [^ServerPlayer player entity
          player-id (str (.getUUID player))
          original-damage (double amount)
          attacker-id (attacker-id damage-source)
          allow? (with-damaged-player-owner
                   player
                   #(should-allow-attack?
                      player-id attacker-id original-damage damage-source))]
      (when-not allow?
        (with-damaged-player-owner
          player
          #(damage-hooks/run-attack-precheck-side-effects!
             player-id attacker-id original-damage damage-source)))
      {:player-id player-id
       :attacker-id attacker-id
       :original-damage original-damage
       :allow? allow?})))

(defn process-damage
  [player-id attacker-id original-damage damage-source]
  (damage-hooks/process-damage-interception
    player-id attacker-id original-damage damage-source))

(defn damage-process-result
  "Return shared mutable-damage result for player damage, or nil when the
  damaged entity is not a server player. Loader registration adapters and the
  Fabric mixin both consume this common result."
  [entity damage-source amount]
  (when (instance? ServerPlayer entity)
    (let [^ServerPlayer player entity
          player-id (str (.getUUID player))
          original-damage (double amount)
          attacker-id (attacker-id damage-source)
          next-damage (if (Double/isFinite original-damage)
                        (with-damaged-player-owner
                          player
                          #(process-damage
                             player-id attacker-id original-damage damage-source))
                        original-damage)
          next-damage (if (and (number? next-damage)
                               (Double/isFinite (double next-damage)))
                        (double next-damage)
                        original-damage)]
      {:player-id player-id
       :attacker-id attacker-id
       :original-damage original-damage
       :next-damage next-damage
       :changed? (not= next-damage original-damage)})))

(defn allow-attack?
  "Common attack-stage decision used by loader registration callbacks."
  [entity damage-source amount]
  (try
    (if-let [{:keys [allow?]} (attack-precheck-result entity damage-source amount)]
      (boolean allow?)
      true)
    (catch Exception e
      (log/warn "Damage interception precheck failed:" (ex-message e))
      true)))

(defn apply-attack-result!
  "Apply the common attack decision through a loader-provided cancellation
  setter. The loader owns only native event adaptation."
  [entity damage-source amount cancel-fn]
  (try
    (let [allow? (allow-attack? entity damage-source amount)]
      (when-not allow?
        (cancel-fn true))
      allow?)
    (catch Exception e
      (log/warn "Damage interception cancellation apply failed:" (ex-message e))
      true)))

(defn rewrite-damage
  "Return the common final pre-armor damage. Invalid handler output fails open
  to the original amount."
  [entity damage-source amount]
  (try
    (if-let [{:keys [next-damage]} (damage-process-result entity damage-source amount)]
      (float next-damage)
      (float amount))
    (catch Exception e
      (log/warn "Damage interception rewrite failed:" (ex-message e))
      (float amount))))

(defn apply-damage-result!
  "Apply common damage rewriting through a loader-provided amount setter."
  [entity damage-source amount set-amount-fn]
  (try
    (let [next-damage (rewrite-damage entity damage-source amount)
          changed? (not= (float amount) next-damage)]
      (when changed?
        (set-amount-fn next-damage))
      next-damage)
    (catch Exception e
      (log/warn "Damage interception amount apply failed:" (ex-message e))
      (float amount))))
