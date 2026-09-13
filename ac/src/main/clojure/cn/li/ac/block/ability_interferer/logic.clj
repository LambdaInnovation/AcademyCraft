(ns cn.li.ac.block.ability-interferer.logic
  "Ability Interferer business logic: state schema, player detection, tick, GUI, container."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cn.li.ac.ability.model.resource :as rd]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.block.ability-interferer.config :as interferer-config]
            [cn.li.ac.block.ability-interferer.schema :as interferer-schema]
            [cn.li.ac.block.energy-converter.wireless-impl :as wireless-impl]
            [cn.li.ac.block.machine.container :as machine-container]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.ac.item.item-energy-base :as energy-base]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.mcmod.block.state-schema :as state-schema]
            [cn.li.mcmod.events.world-lifecycle :as world-lifecycle]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap HashSet IdentityHashMap Iterator Map$Entry]))

(def ^:private interferer-rt
  (machine-runtime/schema-runtime interferer-schema/ability-interferer-schema :server-only? true))

(def interferer-state-schema (:server-schema interferer-rt))
(def interferer-default-state (:default-state interferer-rt))
(def interferer-scripted-load-fn (:load-fn interferer-rt))
(def interferer-scripted-save-fn (:save-fn interferer-rt))

(def interferer-block-state-properties
  (:block-state-properties interferer-rt))

(def interferer-blockstate-updater
  (:blockstate-updater interferer-rt))

(def battery-slot 0)
(def total-slots 1)

(def clamp-range interferer-config/clamp-range)

(defn source-id [level pos]
	(let [dim-id (or (try (world/dimension-id level) (catch Exception _ nil)) "unknown")]
		(str "interferer@" dim-id "/" (pos/pos-x pos) "," (pos/pos-y pos) "," (pos/pos-z pos))))

(defn- player-name [player]
	(let [n (try (entity/player-get-name player) (catch Exception _ nil))]
		(if (and n (not (str/blank? (str n)))) (str n) "")))

(defn- player-creative? [player]
	(try (boolean (entity/player-creative? player)) (catch Exception _ false)))

(defn- player-spectator? [player]
	(try (boolean (entity/player-spectator? player)) (catch Exception _ false)))

(defn- raw-level-players [level]
	(or (try (world/players level) (catch Exception _ nil)) []))

(defn- player-in-aabb?
  "True when player is non-creative, non-spectator, and inside the AABB bounds.
  Top-level to avoid runtime class generation in tick-adjacent filter (Iron Rule 13)."
  [min-x max-x min-y max-y min-z max-z p]
  (and p
       (not (player-creative? p))
       (not (player-spectator? p))
       (let [x (entity/entity-get-x p)
             y (entity/entity-get-y p)
             z (entity/entity-get-z p)]
         (and (<= min-x x max-x)
              (<= min-y y max-y)
              (<= min-z z max-z)))))

(defn- find-players-in-range
  "Find players within a cubic AABB centered on the block (matching original AcademyCraft behavior)."
  [level pos range]
  (let [cx (+ 0.5 (double (pos/pos-x pos)))
        cy (+ 0.5 (double (pos/pos-y pos)))
        cz (+ 0.5 (double (pos/pos-z pos)))
        r (double range)
        min-x (- cx r) max-x (+ cx r)
        min-y (- cy r) max-y (+ cy r)
        min-z (- cz r) max-z (+ cz r)
        all (raw-level-players level)]
    (->> all
         (filter (partial player-in-aabb? min-x max-x min-y max-y min-z max-z))
         vec)))

(defn- session-id-for-level
	"Server ability-session id for a level — resolves [:server <server-hash>]
	from the level's MinecraftServer, the SAME key the ability runtime binds
	around server ticks (lifecycle-core server-owner). The block-entity tick has
	no player-state ThreadLocal, so the previous
	require-player-state-server-session-id always threw and interference was
	never written — skills stayed usable."
	[level]
	(when level
		(try (world/server-session-id level) (catch Exception _ nil))))

(defn- apply-interference-effect! [level player src-id]
	(when-let [uuid (uuid/player-uuid player)]
		(try
			(let [session-id (session-id-for-level level)
						state (store/get-player-state session-id uuid)
						resource-data (some-> state :resource-data (rd/add-interference src-id))]
				(when resource-data
					(command-rt/run-command-in-session!
					 session-id
					 uuid
					 {:command :hydrate-player-state
						:resource-data resource-data})))
			true
			(catch Exception e
				(log/warn "Failed to add interference for" uuid ":" (ex-message e))
				false))))

(defn- remove-interference-effect-by-uuid! [level uuid src-id]
	(when (and uuid src-id)
		(try
			(let [session-id (session-id-for-level level)
						state (store/get-player-state session-id uuid)
						resource-data (some-> state :resource-data (rd/remove-interference src-id))]
				(when resource-data
					(command-rt/run-command-in-session!
					 session-id
					 uuid
					 {:command :hydrate-player-state
						:resource-data resource-data})))
			true
			(catch Exception e
				(log/warn "Failed to remove interference for" uuid ":" (ex-message e))
				false))))

(defn clear-interference-by-uuids! [level uuids src-id]
	(doseq [uuid uuids]
		(remove-interference-effect-by-uuid! level uuid src-id)))

;; ============================================================================
;; Global registry of active interferers (for tile invalidation detection)
;; Keyed by server-session (level identity hash) so state from one
;; integrated-server world can never leak into a different world loaded
;; later in the same client JVM session — see docs/dev/AGENT_AND_TOOLING.md P5.
;; ============================================================================

(definterface IInterfererSession
  (^java.util.HashMap activeInterferers [])
  (^longs tickCell []))

(deftype InterfererSession [^HashMap active ^longs tick]
  IInterfererSession
  (activeInterferers [_] active)
  (tickCell [_] tick))

(definterface IActiveInterferer
  (^Object activeLevel [])
  (^Object activePos [])
  (^java.util.HashSet affectedPlayers []))

(deftype ActiveInterferer [level pos ^HashSet affected]
  IActiveInterferer
  (activeLevel [_] level)
  (activePos [_] pos)
  (affectedPlayers [_] affected))

(defonce ^:private ^IdentityHashMap interferer-sessions (IdentityHashMap.))

(defn- session-runtime
  ^InterfererSession [level create?]
  (or (.get interferer-sessions level)
      (when create?
        (let [runtime (InterfererSession. (HashMap.) (long-array 1))]
          (.put interferer-sessions level runtime)
          runtime))))

(defn- affected-set
  ^HashSet [uuids]
  (let [result (HashSet.)]
    (.addAll result ^java.util.Collection uuids)
    result))

(defn- register-active-interferer! [src-id level pos uuids]
  (let [^InterfererSession runtime (session-runtime level true)]
    (.put (.activeInterferers runtime) src-id
          (ActiveInterferer. level pos (affected-set uuids))))
  nil)

(defn- unregister-active-interferer! [level src-id]
  (when-let [^InterfererSession runtime (session-runtime level false)]
    (.remove (.activeInterferers runtime) src-id))
  nil)

(defn- update-active-interferer-uuids! [level src-id uuids]
  (when-let [^InterfererSession runtime (session-runtime level false)]
    (when-let [^ActiveInterferer active (.get (.activeInterferers runtime) src-id)]
      (let [^HashSet affected (.affectedPlayers active)]
        (.clear affected)
        (.addAll affected ^java.util.Collection uuids))))
  nil)

(defn- active-interferer? [level src-id]
  (if-let [^InterfererSession runtime (session-runtime level false)]
    (.containsKey (.activeInterferers runtime) src-id)
    false))

(defn- dispose-world-interferers! [level]
  (when-let [^InterfererSession runtime (.remove interferer-sessions level)]
    (let [^Iterator it (.iterator (.values (.activeInterferers runtime)))]
      (while (.hasNext it)
        (let [^ActiveInterferer active (.next it)]
          (clear-interference-by-uuids!
            level
            (.affectedPlayers active)
            (source-id (.activeLevel active) (.activePos active)))))))
  nil)

(defn- cleanup-stale-interferers!
  "World-tick handler: check all registered interferers for chunk unload.
   Matching original AcademyCraft's !tile.isInvalid per-tick check."
  [world]
  (when-let [^InterfererSession runtime (session-runtime world false)]
    (let [^longs tick-cell (.tickCell runtime)
          tick (unchecked-inc (aget tick-cell 0))]
      (aset-long tick-cell 0 tick)
      (when (zero? (rem tick 40))
        (let [^Iterator it (.iterator (.entrySet (.activeInterferers runtime)))]
          (while (.hasNext it)
            (let [^Map$Entry entry (.next it)
                  src-id (.getKey entry)
                  ^ActiveInterferer active (.getValue entry)
                  level (.activeLevel active)
                  block-pos (.activePos active)
                  ^HashSet affected (.affectedPlayers active)]
              (when (and level block-pos)
                (try
                  (when-not (world/chunk-loaded?
                              level
                              (world/block-to-chunk-coord (pos/pos-x block-pos))
                              (world/block-to-chunk-coord (pos/pos-z block-pos)))
                    (log/warn "[Interferer] Chunk unloaded for" src-id "- cleaning up" (.size affected) "players")
                    (clear-interference-by-uuids! level affected src-id)
                    (.remove it))
                  (catch Exception e
                    (log/warn "[Interferer] Error checking chunk for" src-id ":" (ex-message e)))))))))))
  nil)

;; ============================================================================
;; Scan and tick
;; ============================================================================

(defn- scan-affected-uuids
  [level pos state]
  (let [range (clamp-range (:range state (interferer-config/default-range)))
        whitelist (set (map str (:whitelist state [])))
        players (find-players-in-range level pos range)]
    {:range range
     :uuids (vec (set (keep uuid/player-uuid
                            (remove #(contains? whitelist (player-name %)) players))))}))

(defn interferer-tick-state
  "Pure state step; ability I/O runs in interferer-after-commit!."
  [base-state level pos _block-state _be]
  (let [ticker (machine-runtime/advance-tick! base-state)
        state0 (assoc base-state
                      :max-energy (double (interferer-config/max-energy))
                      :range (clamp-range (:range base-state)))
        battery-item (get-in state0 [:inventory battery-slot])
        state1 (if (and battery-item (energy/is-energy-item-supported? battery-item))
                 (let [cur-energy (double (:energy state0 0.0))
                       max-energy (double (:max-energy state0 (interferer-config/max-energy)))
                       needed (max 0.0 (- max-energy cur-energy))
                       wanted (min needed (interferer-config/battery-pull-per-tick))
                       pulled (double (energy/pull-energy-from-item battery-item wanted false))]
                   (if (pos? pulled)
                     (assoc state0 :energy (+ cur-energy (min pulled wanted)))
                     state0))
                 state0)
        state2 (if (not (:enabled state1 false))
                 (assoc state1 :affected-player-count 0 :affected-player-uuids [])
                 state1)]
    (if (and (:enabled state2 false)
             (zero? (mod ticker (interferer-config/check-interval))))
      (let [{:keys [range uuids]} (scan-affected-uuids level pos state2)
            energy-cost (interferer-config/calculate-energy-cost range)
            current-energy (double (:energy state2 0.0))]
        (if (>= current-energy energy-cost)
          (assoc state2
                 :range range
                 :energy (- current-energy energy-cost)
                 :affected-player-count (count uuids)
                 :affected-player-uuids uuids)
          (assoc state2
                 :enabled false
                 :affected-player-count 0
                 :affected-player-uuids [])))
      state2)))

(defn interferer-after-commit!
  "1.20-idiomatic: cheap in-memory :enabled comparison first; only touch world
   BlockState when the visual property actually changes. Ability interference
   effects (player registration) are handled after the BlockState check."
  [_be level pos old-state new-state]
  (when (and level pos (not= old-state new-state))
    ;; BlockState update (1.20 pattern: cheap comparison before world access)
    (when (not= (:enabled old-state) (:enabled new-state))
      (interferer-blockstate-updater new-state level pos))
    ;; Ability interference effects
    (let [src-id (source-id level pos)
          prev (set (:affected-player-uuids old-state []))
          next (set (:affected-player-uuids new-state []))
          removed (set/difference prev next)
          added (set/difference next prev)]
      ;; Remove interference from players who left range
      (doseq [u removed]
        (remove-interference-effect-by-uuid! level u src-id))
      ;; Add interference to new players in range
      (when (seq added)
        (let [range (clamp-range (:range new-state (interferer-config/default-range)))
              whitelist (set (map str (:whitelist new-state [])))
              players (find-players-in-range level pos range)]
          (doseq [player players
                  :let [uid (uuid/player-uuid player)]
                  :when (and uid (contains? added uid)
                             (not (contains? whitelist (player-name player))))]
            (apply-interference-effect! level player src-id))))
      ;; Update the global registry for tile invalidation tracking
      (if (seq next)
        (if (active-interferer? level src-id)
          (update-active-interferer-uuids! level src-id next)
          (register-active-interferer! src-id level pos next))
        (unregister-active-interferer! level src-id)))))

(def interferer-tick-fn
  "BlockState updates follow the vanilla 1.20 pattern:
   :after-commit! does a cheap in-memory :enabled comparison first;
   :blockstate-updater is intentionally omitted — only touches world
   BlockState when :enabled actually changes (rare: energy depletion or
   owner toggle). Handlers pass blockstate-updater explicitly for
   immediate visual feedback."
  (machine-runtime/make-tick-fn
    {:default-state interferer-default-state
     :tick-state interferer-tick-state
     :after-commit! interferer-after-commit!}))

;; ============================================================================
;; Container functions
;; ============================================================================

(defn- can-place-battery? [_be slot item _face]
  "Only accept energy_unit items in the battery slot (matching original AcademyCraft)."
  (and (= slot battery-slot)
       (= :energy-unit (energy-base/get-energy-item-type item))))

(defn- can-take-battery? [_be _slot _item _face]
  "Block automated extraction (matching original AcademyCraft)."
  false)

(def interferer-container-fns
  (machine-container/make-inventory-container-fns
    {:default-state interferer-default-state
     :slot-count total-slots
     :can-place? can-place-battery?
     :can-take? can-take-battery?}))

(def open-interferer-gui!
  (machine-runtime/make-open-gui-handler :ability-interferer))

;; ============================================================================
;; Block lifecycle: place, break
;; ============================================================================

(defn on-interferer-placed! [player world pos _block-id]
	(when (and player world pos)
		(let [tile (world/get-tile-entity world pos)]
			(when tile
				(let [state (or (platform-be/get-custom-state tile) interferer-default-state)
							existing-placer (str (get state :placer-name ""))
							placer-name (try (entity/player-get-name player) (catch Exception _ ""))
							placer-name (if (and placer-name (not (str/blank? (str placer-name)))) (str placer-name) "")
							should-init? (str/blank? existing-placer)
							whitelist (vec (or (:whitelist state) []))
							whitelist' (if (or (str/blank? placer-name)
																 (some #(= % placer-name) whitelist))
													 whitelist
													 (conj whitelist placer-name))
							state' (if should-init?
											 (assoc state :placer-name placer-name
																		:whitelist whitelist')
											 state)]
          (machine-runtime/commit-state! tile world pos state state'
                                         :blockstate-updater interferer-blockstate-updater))))))

(defn on-interferer-break! [world pos _block-id]
	(when (and world pos)
		(let [tile (world/get-tile-entity world pos)]
			(when tile
				(let [state (or (platform-be/get-custom-state tile) interferer-default-state)
							uuids (set (:affected-player-uuids state []))
							src-id (source-id world pos)]
					(when (seq uuids)
						(clear-interference-by-uuids! world uuids src-id))
          ;; Remove from global registry
          (unregister-active-interferer! world src-id))))))

;; ============================================================================
;; IWirelessReceiver capability (matching original TileReceiverBase)
;; ============================================================================

(defn create-interferer-wireless-receiver
  "Create IWirelessReceiver implementation for the ability interferer.
   Wireless energy goes into the same :energy pool as battery pull,
   matching original AcademyCraft TileReceiverBase behavior."
  [be]
  (wireless-impl/create-wireless-receiver
    be
    (fn [] (machine-runtime/state-or-default be interferer-default-state))
    (fn [st] (machine-runtime/commit-from-tile! be interferer-default-state st))
    {:max-energy (fn [] (interferer-config/max-energy))
     :bandwidth (fn [] (interferer-config/battery-pull-per-tick))}))

;; ============================================================================
;; World-tick cleanup registration
;; ============================================================================

(defn ensure-world-tick-cleanup!
  "Register the stale interferer cleanup handler if not already registered."
  []
  (install/process-once! ::world-tick-cleanup-registered
    (fn []
      (world-lifecycle/register-world-lifecycle-handler!
        {:id :ac/ability-interferer
         :on-tick cleanup-stale-interferers!
         :on-unload dispose-world-interferers!})
      (log/debug "Ability Interferer world-tick cleanup handler registered")))
  nil)
