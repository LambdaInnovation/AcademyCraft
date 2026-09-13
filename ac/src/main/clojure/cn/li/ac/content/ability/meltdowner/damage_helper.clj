(ns cn.li.ac.content.ability.meltdowner.damage-helper
  "Meltdowner damage helper: mark targets and amplify incoming damage while marked."
  (:require [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.player-runtime-commands :as prt-cmd]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.server.damage.runtime :as damage-runtime]
            [cn.li.mcmod.hooks.core :as hooks]
            [cn.li.ac.content.ability.meltdowner.rad-intensify :as rad]))

(def ^:private rad-mark-fx-channel :rad-intensify/fx-mark)

(def ^:private last-radiation-tick-id* (atom nil))

;; Singleton command constant — reused across all players every tick.
;; Eliminates O(N) per-player map allocations in tick-marks!.
(def ^:private tick-radiation-command
  {:command :tick-radiation-marks})

(defn- normalize-id
  [id]
  (when id (str id)))

(defn- current-server-tick-id
  []
  (some-> (hooks/player-state-owner) :server-tick-id))

(defn clear-all-marks!
  []
  (doseq [holder (prt-cmd/radiation-mark-holders)]
    (prt-cmd/run-for-player!
     holder
     {:command :clear-radiation-marks :clear-all? true}))
  (reset! last-radiation-tick-id* nil)
  nil)

(defn on-server-stop!
  "Drop the whole session's derived radiation-mark index in one shot instead
   of iterating players (the store's per-player state is already gone by the
   time this runs, so per-player clear commands would be pure ghost-state
   recreation)."
  [session-id]
  (prt-cmd/clear-radiation-index-session! session-id)
  (reset! last-radiation-tick-id* nil)
  nil)

(defn reset-marks-for-test!
  ([]
   (reset-marks-for-test! {}))
  ([snapshot]
   (clear-all-marks!)
   (doseq [[target-id mark] snapshot]
     (when-let [source-id (:source-player-id mark)]
       (prt-cmd/run-for-player!
        source-id
        {:command :mark-radiation-target
         :target-id target-id
         :mark mark})))
   nil))

(defn marks-snapshot
  []
  (prt-cmd/radiation-marks-snapshot))

(defn clear-mark!
  ([target-id]
   (when-let [target-key (normalize-id target-id)]
     (doseq [source-uuid (prt-cmd/radiation-mark-sources-for-target target-key)]
       (prt-cmd/run-for-player!
        source-uuid
        {:command :clear-radiation-marks :target-id target-key})))
   nil)
  ([_source-player-id target-id]
   (clear-mark! target-id)))

(defn clear-target-mark!
  [target-id]
  (clear-mark! target-id)
  nil)

(defn clear-target-marks!
  [target-id]
  (clear-target-mark! target-id))

(defn clear-source-marks!
  [source-player-id]
  (when source-player-id
    (prt-cmd/run-for-player!
     source-player-id
     {:command :clear-radiation-marks :source-player-id source-player-id}))
  nil)

(defn clear-expired-marks!
  []
  (doseq [holder (prt-cmd/radiation-mark-holders)]
    (prt-cmd/run-for-player!
     holder
     {:command :clear-radiation-marks :clear-expired? true}))
  nil)

(defn tick-marks!
  "Drive radiation markers globally once per unique server tick.
   Optimized: only iterates players who currently hold at least one
   outgoing mark (via the derived radiation-mark index), instead of every
   online player — a server with no active marks emits zero commands."
  []
  (let [tick-id (current-server-tick-id)]
    (when (or (nil? tick-id) (not= tick-id @last-radiation-tick-id*))
      (when tick-id
        (reset! last-radiation-tick-id* tick-id))
      (let [session-id (prt-cmd/session-id)]
        (doseq [holder (prt-cmd/radiation-mark-holders)]
          (if (store/get-player-state session-id holder)
            (prt-cmd/run-for-player! holder tick-radiation-command)
            ;; Ghost index entry (backing player state already gone) — drop
            ;; it directly. Never use a get-or-create path here, or a
            ;; departed player's state gets silently resurrected.
            (prt-cmd/drop-radiation-index-source! holder))))))
  nil)

(defn- learned-rad-intensify?
  [player-id]
  (boolean
    (when-let [state (skill-effects/get-player-state player-id)]
      (adata/is-learned? (:ability-data state) :rad-intensify))))

(defn- emit-mark-fx!
  [source-id {:keys [ctx-id target-pos mark]}]
  (when (and source-id ctx-id mark)
    (let [payload (cond-> {:mode :mark
                           :target-id (:target-id mark)
                           :ticks-left (long (or (:ticks-left mark) 0))
                           :rate (double (or (:rate mark) 1.0))
                           :source-player-id source-id}
                    (map? target-pos)
                    (merge {:x (double (or (:x target-pos) 0.0))
                            :y (double (or (:y target-pos) 0.0))
                            :z (double (or (:z target-pos) 0.0))}))]
      (ctx-mgr/push-channel-to-player! source-id ctx-id rad-mark-fx-channel payload)
      (ctx-mgr/push-channel-to-nearby-players! source-id ctx-id rad-mark-fx-channel payload))))

(defn mark-target!
  ([attacker-id target-id]
   (mark-target! attacker-id target-id nil))
  ([attacker-id target-id {:keys [ctx-id target-pos] :as _fx-context}]
   (let [source-id (normalize-id attacker-id)
         marked-target-id (normalize-id target-id)]
     (when (and source-id marked-target-id (learned-rad-intensify? source-id))
       ;; Original stores one mark directly on the target entity. A later
       ;; Meltdowner hit therefore replaces both the previous source/rate and
       ;; its duration instead of keeping concurrent per-caster marks.
       ;;
       ;; Its unusual max(60, getMarkTick(player)) expression reads the
       ;; ATTACKER's incoming mark duration, not the victim's current mark.
       (let [source-ticks (long (or (:ticks-left (prt-cmd/radiation-marks-for-target source-id)) 0))
             mark-ticks (long (max 60 (rad/mark-duration-ticks) source-ticks))
             mark-rate (rad/rate source-id)
             mark {:source-player-id source-id
                   :target-id marked-target-id
                   :ticks-left mark-ticks
                   :rate mark-rate
                   :updated-at-tick (current-server-tick-id)}]
         (clear-mark! marked-target-id)
         (prt-cmd/run-for-player!
          source-id
          {:command :mark-radiation-target
           :target-id marked-target-id
           :mark mark})
         (emit-mark-fx! source-id {:ctx-id ctx-id
                                   :target-pos target-pos
                                   :mark mark}))))))

(defn- active-mark
  [target-id]
  (let [target-key (normalize-id target-id)]
    (when-let [mark (prt-cmd/radiation-marks-for-target target-key)]
      (if (pos? (long (or (:ticks-left mark) 0)))
        mark
        (do
          (clear-mark! target-id)
          nil)))))

(defn- damage-handler
  [player-id attacker-id damage _damage-source]
  (if-let [{:keys [source-player-id target-id rate]} (active-mark player-id)]
    (let [mark-rate (double rate)]
      [(* (double damage) mark-rate)
       {:handler :meltdowner/rad-intensify
        :source-player-id source-player-id
        :target-id target-id
        :rate mark-rate}])
    [(double damage) nil]))

(defn ensure-damage-handler!
  []
  (damage-runtime/register-damage-handler!
    :meltdowner/rad-intensify
    damage-handler
    90))

(defn init!
  []
  (ensure-damage-handler!)
  ;; Matches original's Skill.expCustomized: rad-intensify's exp is always
  ;; the synthetic max-CP-ratio formula, not a stored/accumulated value —
  ;; wire it into the generic exp accessor so UI display etc. sees the same
  ;; value the mark-rate calculation uses.
  (skill-effects/register-custom-skill-exp! :rad-intensify rad/skill-exp)
  nil)
