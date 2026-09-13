(ns cn.li.mcbase.runtime.item-handler-core
  "Shared Minecraft-side item runtime helpers (no loader API imports)."
  (:require [cn.li.mcbase.runtime.event.item-use :as item-use]
            [clojure.string :as str]
            [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.platform.neutral.client-runtime :as client-bridge]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.platform.neutral.hooks :as hooks-core]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcbase.entity IScriptedEffectEntity]
           [cn.li.mcver EntityClasses McAccess]
           [net.minecraft.core.registries BuiltInRegistries]
           [net.minecraft.world InteractionHand]
           [net.minecraft.world.entity Entity]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.item ItemStack]))

(defn- same-effect-type?
  [expected-id ^Entity effect]
  (when-let [registry-id (.getKey BuiltInRegistries/ENTITY_TYPE (.getType effect))]
    (let [expected (str expected-id)
          actual (str registry-id)]
      (or (= expected actual)
          (= expected (.getPath registry-id))))))

(defn- unique-effect-active?
  [^Player player plan side]
  (when (= side :server)
    (some
      (fn [action]
        (when (and (= :spawn-scripted-effect (:kind action))
                   (:unique-per-owner? action))
          (let [owner-uuid (.getUUID player)
                effect-class (EntityClasses/scriptedEffectEntity)
                effects (.getEntitiesOfClass
                          (.level player)
                          effect-class
                          (.inflate (.getBoundingBox player) 32.0))]
            (boolean
              (some (fn [effect]
                      (and (instance? IScriptedEffectEntity effect)
                           (= owner-uuid (.getOwnerUuid ^IScriptedEffectEntity effect))
                           (same-effect-type? (:entity-id action) effect)))
                    effects)))))
      (:server-actions plan))))

(defn get-item-id
  [^ItemStack stack]
  (when stack
    (try
      (let [item (.getItem stack)
            registry-name (.getKey BuiltInRegistries/ITEM item)]
        (when registry-name
          (str (.getNamespace registry-name) ":" (.getPath registry-name))))
      (catch Exception e
        (log/warn "Failed to get item ID:" (ex-message e))
        nil))))

(defn resolve-dsl-item-spec
  "Resolve DSL item spec from runtime item id (`namespace:path`)."
  [item-id]
  (when-let [[_namespace registry-path] (some-> item-id (str/split #":" 2))]
    (some (fn [dsl-id]
            (when (= registry-path (registry-metadata/get-item-registry-name dsl-id))
              (registry-metadata/get-item-spec dsl-id)))
          (registry-metadata/get-all-item-ids))))

(defn dispatch-dsl-item-use!
  [^Player player item-id hand stack side]
  (when-let [item-spec (resolve-dsl-item-spec item-id)]
    (idsl/handle-use item-spec {:player player
                                :item-id item-id
                                :item-spec item-spec
                                :item-stack stack
                                :hand hand
                                :side side})))

(defn dispatch-dsl-item-right-click-consume?
  [^Player player item-id side hand stack]
  (when-let [item-spec (resolve-dsl-item-spec item-id)]
    (let [ret (idsl/handle-right-click item-spec {:player player
                                                  :item-id item-id
                                                  :item-spec item-spec
                                                  :item-stack stack
                                                  :hand hand
                                                  :side side})]
      (boolean (:consume? ret)))))

(defn dispatch-dsl-item-finish-using!
  [^Player player stack side]
  (let [item-id (get-item-id stack)]
    (when-let [item-spec (resolve-dsl-item-spec item-id)]
      (idsl/handle-finish-using item-spec {:player player
                                           :item-id item-id
                                           :item-spec item-spec
                                           :item-stack stack
                                           :side side}))
    item-id))

(defn- game-time-ms
  "Game-time in milliseconds (pauses when game pauses). Falls back to wall-clock."
  [^Player player]
  (if-let [server (McAccess/serverOf player)]
    (* (McAccess/serverTickCount server) 50)
    (if-let [level (some-> player .level)]
      (* (McAccess/gameTime level) 50)
      (System/currentTimeMillis))))

(defn- run-plan-actions!
  [^Player player hand ^ItemStack stack side player-uuid plan _opts]
  (when plan
    ;; Client-actions notify the *local* client's UI and require a bound client
    ;; session -- only present on the client thread. In singleplayer the
    ;; client-thread item-use event fires alongside this one and dispatches them
    ;; there; skip on the server side like :consume-item / :spawn-scripted-effect.
    (when (= side :client)
      (doseq [action (:client-actions plan)]
        (case (:kind action)
          :notify-local-effect
          (hooks-core/client-notify-visual-event!
            (or (:event-key action) (:effect-key action) :local-effect)
            (merge {:player-uuid player-uuid :now-ms (game-time-ms player)}
                   (or (:payload action) {})))

          :open-screen
          (client-bridge/open-screen!
            (:screen-key action)
            (merge {:player-uuid player-uuid}
                   (or (:payload action) {})))

          nil)))

    (doseq [action (:server-actions plan)]
      (case (:kind action)
        :consume-item
        (when (and (= side :server)
                   (or (not (:unless-instabuild? action))
                       (not (.. player (getAbilities) instabuild))))
          (when (and stack (not (.isEmpty stack)))
            (.shrink stack (int (or (:count action) 1)))
            (when (.isEmpty stack)
              (.setItemInHand player hand ItemStack/EMPTY))))

        :domain-action
        (hooks-core/on-runtime-item-action! (:action action) player-uuid (:payload action))

        :spawn-scripted-effect
        (when (= side :server)
          (entity/player-spawn-entity-by-id! player
                                             (str (:entity-id action))
                                             (double (or (:speed action) 0.0))))

        nil))))

(defn process-item-use!
  "Shared main-hand runtime item-use flow.

  Returns {:consume? boolean :item-id string|nil :player-uuid string :plan map|nil}."
  [^Player player hand stack side opts]
  (if (not= hand InteractionHand/MAIN_HAND)
    {:consume? false
     :item-id nil
     :player-uuid nil
     :plan nil}
    (let [player-uuid (str (.getUUID player))
          item-id (get-item-id stack)
          runtime-activated? (hooks-core/runtime-activated? player-uuid)
          requested-plan (hooks-core/build-item-use-plan player-uuid item-id runtime-activated? side)
          ;; ItemCoin refuses a second throw while this player's first coin is
          ;; still airborne. Preflight before consume/domain/spawn keeps that
          ;; behavior atomic on the authoritative server.
          plan (if (unique-effect-active? player requested-plan side)
                 nil
                 requested-plan)]
      (dispatch-dsl-item-use! player item-id hand stack side)
      (run-plan-actions! player hand stack side player-uuid plan opts)
      {:consume? (or (:consume? plan)
                     (dispatch-dsl-item-right-click-consume? player item-id side hand stack))
       :item-id item-id
       :player-uuid player-uuid
       :plan plan})))

(item-use/install-item-core!
  {:dispatch-dsl-item-finish-using! dispatch-dsl-item-finish-using!
   :process-item-use! process-item-use!})
