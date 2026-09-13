(ns cn.li.ac.ability.client.effects.sounds
  "Sound effect commands for ability system (AC layer - no Minecraft imports)."
  (:require [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.client.effects.queue-infra :as queue-infra])
  (:import [java.util ArrayDeque]))

;; Sound effect command structure
;; {:type :sound
;;  :sound-id string (e.g., "minecraft:entity.experience_orb.pickup")
;;  :source keyword (optional, defaults to :players)
;;  :volume float
;;  :pitch float
;;  :x :y :z position (optional, defaults to player position)}

(defn make-skill-activation-sound
  "Create sound effect for skill activation."
  [skill-category]
  (let [sound-id (case skill-category
                  :electromaster "minecraft:entity.lightning_bolt.thunder"
                  :teleporter "minecraft:entity.enderman.teleport"
                  :meltdowner "minecraft:entity.blaze.shoot"
                  :vecmanip "minecraft:block.beacon.activate"
                  "minecraft:entity.player.levelup")]
    {:type :sound
     :sound-id sound-id
     :volume 0.5
     :pitch 1.0}))

(defn make-cp-consumption-sound
  "Create sound effect for CP consumption."
  []
  {:type :sound
   :sound-id "minecraft:block.enchantment_table.use"
   :volume 0.3
   :pitch 1.5})

(defn make-overload-sound
  "Create sound effect for overload state."
  []
  {:type :sound
   :sound-id "minecraft:entity.generic.hurt"
   :volume 0.7
   :pitch 0.8})

(defn make-level-up-sound
  "Create sound effect for level up."
  []
  {:type :sound
   :sound-id "minecraft:entity.player.levelup"
   :volume 1.0
   :pitch 1.0})

(defn make-skill-learn-sound
  "Create sound effect for learning a skill."
  []
  {:type :sound
   :sound-id "minecraft:entity.experience_orb.pickup"
   :volume 0.8
   :pitch 1.2})

;; Client-thread-confined bounded sound queue.

(defonce ^:private fallback-sound-queue (ArrayDeque. 1024))

(defn- sound-queue ^ArrayDeque
  []
  fallback-sound-queue)

(defn- normalize-session-id
  [owner-or-session]
  (queue-infra/normalize-session-id "sound" owner-or-session))

(defn current-effect-owner
  []
  (queue-infra/current-effect-owner "sound"))

(defn queue-sound-effect!
  "Queue a sound effect to be played."
  ([sound-cmd]
   (queue-sound-effect! nil sound-cmd))
  ([owner-or-session sound-cmd]
  (queue-infra/queue-effect! (sound-queue) "sound" owner-or-session sound-cmd)
   nil))

(defn queue-current-sound-effect!
  [sound-cmd]
  (queue-sound-effect! (current-effect-owner) sound-cmd))

(defn poll-sound-effects!
  "Poll and clear all queued sound effects. Called by forge layer."
  ([]
   (poll-sound-effects! nil))
  ([owner-or-session]
   (queue-infra/poll-effects! (sound-queue) "sound" owner-or-session)))

(defn clear-session-sound-effects!
  "Remove all queued sound effects for a specific session from the
  lock-free queue."
  ([]
   (clear-session-sound-effects! nil))
  ([owner-or-session]
   (let [session-id (normalize-session-id owner-or-session)
         ^ArrayDeque q (sound-queue)]
     (.removeIf q (reify java.util.function.Predicate
                    (test [_ entry]
                      (= session-id (first entry))))))
   nil))

(defn clear-owner-sound-effects!
  [owner]
  (clear-session-sound-effects! owner))

(defn sound-queue-snapshot
  "Snapshot all queued sounds (for debug/introspection)."
  ([]
   (vec (.toArray (sound-queue))))
  ([owner-or-session]
   (let [session-id (normalize-session-id owner-or-session)]
     (->> (.toArray (sound-queue))
          (filter #(= session-id (first %)))
          (mapv second)))))

(defn reset-sound-queue-for-test!
  "Reset the bounded sound queue for testing."
  ([]
   (reset-sound-queue-for-test! {}))
  ([queues]
   (.clear (sound-queue))
   ;; Re-populate from test data: queues = {session-id [sound-cmd ...]}
   (doseq [[session-id sounds] (or queues {})]
     (doseq [sound-cmd sounds]
       (queue-infra/queue-effect! (sound-queue) "sound" session-id sound-cmd)))
   nil))

;; Event listeners for automatic sound playing
(defn on-skill-activation
  "Handle skill activation event."
  [event]
  (when-let [category (:category event)]
    (queue-sound-effect! (make-skill-activation-sound category))))

(defn on-overload
  "Handle overload event."
  [_event]
  (queue-sound-effect! (make-overload-sound)))

(defn on-level-up
  "Handle level up event."
  [_event]
  (queue-sound-effect! (make-level-up-sound)))

(defn on-skill-learn
  "Handle skill learn event."
  [_event]
  (queue-sound-effect! (make-skill-learn-sound)))

(defn init!
  "Initialize sound effect system. Wire event listeners."
  []
  (evt/subscribe-ability-event!
   evt/EVT-ABILITY-ACTIVATE
   (fn [event] (on-skill-activation event)))
  (evt/subscribe-ability-event!
   evt/EVT-LEVEL-CHANGE
   (fn [_event] (on-level-up _event)))
  (evt/subscribe-ability-event!
   evt/EVT-SKILL-LEARN
   (fn [_event] (on-skill-learn _event)))
  nil)
