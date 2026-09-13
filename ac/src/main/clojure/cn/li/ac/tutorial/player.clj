(ns cn.li.ac.tutorial.player
  "Server-side tutorial player state — persisted directly to player NBT.

  Mirrors upstream AcademyCraft TutorialData: each domain writes its own NBT key
  independently, instead of going through a centralized runtime-store.

  Uses player.getPersistentData() for storage — Minecraft handles save/load
  automatically with the player entity."
  (:require [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.tutorial.conditions :as conditions]
            [cn.li.ac.tutorial.model :as model]
            [cn.li.ac.persistence.nbt-collections :as nbt-coll]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.util.log :as log]))

(def ^:private nbt-key "ac_tutorial_v2")
(def ^:private schema-version 2)

(defn- player-persistent-data
  [player]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :player-persistent-data :get! player)))

;; --- NBT helpers ---

(defn- load-state
  [tag]
  (when (sd/has-key-safe? tag nbt-key)
    (let [root (sd/get-structured tag nbt-key)]
      (when (= schema-version (sd/get-int root "schema"))
        {:activated-tuts (nbt-coll/read-keyword-set root "activated")
         :condition-flags (nbt-coll/read-int-set root "conditions")
         :misaka-id (when (sd/get-boolean root "has_misaka")
                      (sd/get-int root "misaka"))
         :first-open? (sd/get-boolean root "first_open")
         :dirty? false}))))

(defn- save-state!
  [tag state]
  (let [root (sd/create-structured)
        misaka-id (:misaka-id state)]
    (sd/set-int! root "schema" schema-version)
    (nbt-coll/write-keyword-set! root "activated" (:activated-tuts state))
    (nbt-coll/write-int-set! root "conditions" (:condition-flags state))
    (sd/set-boolean! root "has_misaka" (some? misaka-id))
    (when (some? misaka-id)
      (sd/set-int! root "misaka" (int misaka-id)))
    (sd/set-boolean! root "first_open" (boolean (:first-open? state)))
    (sd/set-entry! tag nbt-key root)))

;; --- Public API ---

(defn state
  "Read tutorial state for a player. Returns fresh-state when not yet initialized."
  [player]
  (or (load-state (player-persistent-data player))
      (model/fresh-state)))

(defn update-state!
  "Apply f to the player's tutorial state and write back to NBT.
  f receives the current state (or fresh-state) and should return the new state."
  [player f & args]
  (let [tag (player-persistent-data player)
        current (or (load-state tag) (model/fresh-state))
        new-state (apply f current args)]
    (save-state! tag new-state)
    new-state))

;; --- Activation ---

(defn is-activated?
  "True when `tut-id` has been explicitly activated (or is default-installed)."
  [player tut-id]
  (model/is-activated? (state player) (keyword tut-id)))

(defn activate-tutorial!
  "Mark a tutorial as activated. Idempotent."
  [player tut-id]
  (log/info "Activating tutorial" (name tut-id) "for" (uuid/player-uuid player))
  (update-state! player model/activate-tutorial (keyword tut-id)))

;; --- Misaka ID ---

(defn get-misaka-id
  "Return the player's random Misaka No. (1000–19000). Lazy-initializes on first access."
  [player]
  (let [current (:misaka-id (state player))]
    (if current
      current
      (do (update-state! player model/ensure-misaka-id)
          (:misaka-id (state player))))))

;; --- First-open flag ---

(defn first-open?
  "True when the first-open animation has not been played yet."
  [player]
  (model/first-open? (state player)))

(defn mark-first-open-done!
  "Record that the first-open animation has played."
  [player]
  (update-state! player model/mark-first-open-done!))

;; --- Condition checking (called by events.clj) ---

(defn mark-conditions-dirty
  "Mark condition flags and set the dirty flag — pure state transform.
  (Regression: the previous `->` threading inserted the state as the reduce
  FUNCTION, so a map got called as a fn and returned the last condition
  index; mark-dirty! then assoc'd onto a Long.)"
  [s matching-conditions]
  (model/mark-dirty! (reduce model/mark-condition! s matching-conditions)))

(defn mark-conditions-and-set-dirty!
  "Mark condition flags and set dirty flag in one update."
  [player matching-conditions]
  (update-state! player #(mark-conditions-dirty % matching-conditions)))

(defn process-pending!
  "If state is dirty, check for new activations and clear dirty flag.
  Returns [new-activations]."
  [player cond-map]
  (let [s (state player)]
    (when (model/dirty? s)
      (let [acts (conditions/check-new-activations s cond-map)
            _ (doseq [tut-id acts]
                (activate-tutorial! player tut-id))]
        (update-state! player model/clear-dirty!)
        acts))))
