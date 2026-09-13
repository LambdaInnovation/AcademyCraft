(ns cn.li.ac.ability.integration.external-providers
  "Bridges third-party cn.li.acapi.ability.AbilityContentProvider
  implementations (discovered via java.util.ServiceLoader) into the ability
  registries.

  Must run after register-declared-skills! (in-tree defskill content) and
  strictly before the skill/category registries freeze — see
  content/ability.clj init-ability-content!.

  External skills are marked :external? true so registry/skill.clj's
  register-skill! trusts their :level/:controllable? as supplied (they have
  no skill-config/skill-definitions entry — see inject-configured-fields)."
  (:require [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.util.attack :as attack]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.ability AbilityContentProvider SkillDefinition SkillActionContext]
           [cn.li.acapi.ability SkillDefinition$ActionHandler SkillDefinition$CostFn
            SkillDefinition$CooldownFn SkillDefinition$Prerequisite]
           [java.util UUID ServiceLoader]))

;; ============================================================================
;; SkillActionContext — the whitelist third-party ActionHandlers run against.
;; ============================================================================

(defn- ->uuid-str
  [^UUID uuid]
  (some-> uuid .toString))

(defn- attack-data->java-map
  "Convert attack/resolve-attack-data's keyword-keyed result into the
  string-keyed shape documented on SkillActionContext.resolveAttack."
  [{:keys [world-id eye look hit-kind target-uuid impact]}]
  {"world-id" world-id
   "eye" eye
   "look" look
   "hit-kind" (name hit-kind)
   "target-uuid" (some-> target-uuid str)
   "impact" impact})

(defn- make-skill-action-context
  ^SkillActionContext
  [ctx-id player-id skill-id exp cost-ok? hold-ticks]
  (reify SkillActionContext
    (ctxId [_] ctx-id)
    (playerId [_] (UUID/fromString (str player-id)))
    (skillId [_] (name skill-id))
    (exp [_] (double (or exp 0.0)))
    (costOk [_] (boolean cost-ok?))
    (holdTicks [_] (long (or hold-ticks 0)))
    (addSkillExp [_ amount]
      (skill-effects/add-skill-exp! player-id skill-id (double amount))
      nil)
    (setMainCooldown [_ ticks]
      (skill-effects/set-main-cooldown! player-id skill-id (int ticks))
      nil)
    (sendFx [_ topic payload]
      (fx/send! ctx-id {:topic (keyword topic)} nil payload)
      nil)
    (damageEntity [_ target amount damage-type]
      (attack/damage-entity! (geom/world-id-of player-id) (->uuid-str target)
                             (double amount) (keyword damage-type))
      nil)
    (resolveAttack [_ range]
      (attack-data->java-map (attack/resolve-attack-data player-id (double range))))))

;; ============================================================================
;; SkillDefinition -> registry spec
;; ============================================================================

(defn- wrap-action
  "Adapt a Java ActionHandler to the in-tree 8-arg positional callback
  contract: [ctx-id player-id skill-id exp cost-ok? hold-ticks cost-stage player-ref]."
  [^SkillDefinition$ActionHandler handler]
  (fn [ctx-id player-id skill-id exp cost-ok? hold-ticks _cost-stage _player-ref]
    (.handle handler (make-skill-action-context ctx-id player-id skill-id exp cost-ok? hold-ticks))
    nil))

(defn- wrap-cost-fn
  "Adapt a Java CostFn to the 3-arg (player-id skill-id exp) shape
  service/skill-effects's resolve-val tries first."
  [^SkillDefinition$CostFn cost-fn]
  (fn [player-id skill-id exp]
    (.compute cost-fn (UUID/fromString (str player-id)) (name skill-id) (double (or exp 0.0)))))

(defn- costs->spec
  [^SkillDefinition sd]
  (let [costs (.costs sd)]
    (when (seq costs)
      (into {}
            (map (fn [[stage resources]]
                   [(keyword stage)
                    (into {}
                          (map (fn [[resource cost-fn]]
                                 [(keyword resource) (wrap-cost-fn cost-fn)]))
                          resources)]))
            costs))))

(defn- cooldown-ticks->spec
  [^SkillDefinition sd]
  (let [v (.cooldownTicks sd)]
    (cond
      (nil? v) nil
      (instance? Integer v) v
      :else (fn [player-id skill-id exp]
              (.compute ^SkillDefinition$CooldownFn v
                        (UUID/fromString (str player-id)) (name skill-id) (double (or exp 0.0)))))))

(defn- prerequisites->spec
  [^SkillDefinition sd]
  (mapv (fn [^SkillDefinition$Prerequisite p]
          {:skill-id (keyword (.skillId p)) :min-exp (.minExp p)})
        (.prerequisites sd)))

(defn- actions->spec
  [^SkillDefinition sd]
  (into {}
        (map (fn [[action-key handler]]
               [(keyword action-key) (wrap-action handler)]))
        (.actionHandlers sd)))

(defn- skill-definition->spec
  [^SkillDefinition sd]
  (cond-> {:id (keyword (.id sd))
           :category-id (keyword (.categoryId sd))
           :level (.level sd)
           :controllable? (.controllable sd)
           :name-key (.nameKey sd)
           :ctrl-id (keyword (.ctrlId sd))
           :pattern (keyword (.pattern sd))
           :ui-position [(.uiPositionX sd) (.uiPositionY sd)]
           :actions (actions->spec sd)
           :prerequisites (prerequisites->spec sd)
           :external? true}
    (.descriptionKey sd) (assoc :description-key (.descriptionKey sd))
    (.icon sd) (assoc :icon (.icon sd))
    (seq (.costs sd)) (assoc :cost (costs->spec sd))
    (some? (.cooldownTicks sd)) (assoc :cooldown-ticks (cooldown-ticks->spec sd))))

(defn- normalize-category-map
  [m]
  {:id (keyword (get m "id"))
   :name-key (get m "name-key")
   :icon (get m "icon")
   :color (vec (get m "color" [1.0 1.0 1.0 1.0]))
   :prog-incr-rate (double (get m "prog-incr-rate" 1.0))
   :enabled (boolean (get m "enabled" true))})

;; ============================================================================
;; ServiceLoader discovery
;; ============================================================================

(defn load-external-providers!
  "Discover AbilityContentProvider implementations via ServiceLoader and
  register their categories/skills. Call once, after register-declared-skills!
  and before the skill/category registries freeze."
  []
  (let [loader (ServiceLoader/load AbilityContentProvider
                                   (.getClassLoader AbilityContentProvider))]
    (doseq [^AbilityContentProvider provider (iterator-seq (.iterator loader))]
      (try
        (doseq [cat (.categories provider)]
          (category/register-category! (normalize-category-map cat)))
        (doseq [^SkillDefinition sd (.skills provider)]
          (skill-registry/register-skill! (skill-definition->spec sd)))
        (log/debug "Registered external ability provider" (.providerId provider))
        (catch Throwable t
          (log/warn "Failed to register external ability provider"
                    (.providerId provider) (ex-message t)))))))
