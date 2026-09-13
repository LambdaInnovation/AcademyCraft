(ns cn.li.ac.ability.service.player-runtime-commands
  "Reducer-backed player runtime command helpers (delayed projectiles, marks, vecmanip)."
  (:require [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.radiation-mark-index :as rad-index]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn session-id
  []
  (runtime-hooks/require-player-state-session-id "player-runtime-commands"))

(defn run-for-player!
  ([player-uuid command]
   (run-for-player! player-uuid command {}))
  ([player-uuid command opts]
   (command-rt/run-command-in-session! (session-id) player-uuid command opts)))

(defn player-state
  [player-uuid]
  (skill-effects/get-player-state-in-session! (session-id) player-uuid))

(defn radiation-marks-for-target
  "O(1) lookup via the derived radiation-mark index (see radiation-mark-index.clj)."
  [target-id]
  (rad-index/strongest-mark-for-target (session-id) (str target-id)))

(defn radiation-marks-snapshot
  []
  (rad-index/snapshot-by-target (session-id)))

(defn radiation-mark-holders
  "Player-uuid strings that currently hold at least one outgoing radiation mark."
  []
  (rad-index/mark-holders (session-id)))

(defn radiation-mark-sources-for-target
  [target-id]
  (rad-index/sources-for-target (session-id) target-id))

(defn drop-radiation-index-source!
  "Clear source-uuid's index entries without touching player state (used to
  reap ghost index entries whose backing player state no longer exists)."
  [source-uuid]
  (rad-index/sync-source-marks! (session-id) (str source-uuid) {}))

(defn clear-radiation-index-session!
  [session-id]
  (rad-index/clear-session! session-id))

(defn projectile-claims
  [player-uuid]
  (or (get-in (player-state player-uuid) [:runtime :vecmanip :projectile-claims])
      {:tick -1 :owners {}}))

(defn vec-reflection-state
  [player-uuid]
  (or (get-in (player-state player-uuid) [:runtime :vecmanip :reflection])
      {:reflecting-pairs [] :reflection-depths {}}))

(defn reset-content-runtime-for-player!
  [player-uuid]
  (run-for-player! player-uuid {:command :clear-radiation-marks :clear-all? true})
  (run-for-player! player-uuid {:command :clear-player-projectile-claims})
  (run-for-player! player-uuid {:command :reset-vec-reflection-runtime})
  nil)

(defn reset-all-content-runtimes!
  []
  (let [sid (session-id)]
    (doseq [player-uuid (store/list-players sid)]
      (reset-content-runtime-for-player! player-uuid)))
  nil)
