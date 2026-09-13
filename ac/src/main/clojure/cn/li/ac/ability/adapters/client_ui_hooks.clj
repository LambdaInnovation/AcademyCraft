(ns cn.li.ac.ability.adapters.client-ui-hooks
  "Client HUD/screen/context hook composition for AC ability platform bridge."
  (:require
            [cn.li.ac.ability.service.command-runtime :as command-rt]
[cn.li.ac.ability.service.runtime-store :as store]
[cn.li.ac.ability.client.api :as client-api]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.client.combat-notice :as combat-notice]
            [cn.li.ac.ability.client.delegate-state :as delegate-state]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.hud :as hud-renderer]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.keybinds :as client-keybinds]
            [cn.li.ac.ability.client.managed-screens :as managed-screens]
            [cn.li.ac.ability.client.reactive-hud :as reactive-hud]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.client.fx-templates.arc-beam.impl.plasma-cannon :as plasma-cannon-fx]
            [cn.li.ac.content.ability.electromaster.current-charging-fx :as current-charging-fx]
            [cn.li.ac.content.ability.teleporter.location-teleport-reactive :as location-teleport-reactive]
            [cn.li.ac.ability.client.screens.preset-editor :as preset-editor-screen]
            [cn.li.ac.ability.client.screens.preset-editor-reactive :as preset-editor-reactive]
            [cn.li.ac.ability.client.screens.skill-tree :as skill-tree-screen]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.model.preset :as preset-data]
            [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.util.resource-check :as resource-check]
            [cn.li.ac.config.gameplay :as gameplay]
            [cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.ability.messages :as catalog]
            [cn.li.ac.client.toast :as toast]
            [cn.li.ac.ability.client.debug-overlay :as debug-overlay]
             [cn.li.ac.tutorial.client.notification :as tutorial-notification]
             [cn.li.ac.terminal.client.apps.freq-transmitter-reactive :as freq-tx]
             [cn.li.ac.terminal.client.apps.media-reactive :as media-player]
             [cn.li.ac.terminal.client.install-effect-reactive :as install-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.client.content-actions :as content-actions]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.mcmod.runtime.owner :as owner]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap]))

(defonce ^:private ^HashMap slot-context-ids (HashMap.))
(defonce ^:private ^HashMap slot-key-tick-ms (HashMap.))
(defonce ^:private ^HashMap charge-coin-state (HashMap.))
(defonce ^:private ^HashMap overlay-skill-shape-cache (HashMap.))
(defonce ^:private ^HashMap overlay-context-cache (HashMap.))
(defonce ^:private push-handlers-registered (boolean-array 1))

(defn create-client-ui-runtime []
  {::runtime ::client-ui})

(defn call-with-client-ui-runtime
  [_runtime f]
  (f))

(defn- slot-context-ids-snapshot
  []
  slot-context-ids)

(defn- slot-key-tick-ms-snapshot
  []
  slot-key-tick-ms)

(defn- charge-coin-state-snapshot
  []
  charge-coin-state)

(defn- overlay-skill-shape-cache-snapshot
  []
  overlay-skill-shape-cache)

(defn- overlay-context-cache-snapshot
  []
  overlay-context-cache)

(defn- current-client-session-id
  []
  (or client-keybinds/*client-session-id* (runtime-hooks/client-session-id)))

(defn- require-client-owner-value
  [owner label value]
  (if (some? value)
    value
    (throw (ex-info (format "Client UI owner requires %s" label)
                    {:owner owner
                     :required label}))))

(defn client-ui-owner-key
  [owner]
  (let [owner-map (cond
                    (vector? owner) owner
                    (map? owner) owner
                    (some? owner) {:player-uuid owner}
                    :else {})]
    (if (vector? owner-map)
      owner-map
       (let [session-id (or (:client-session-id owner-map)
                            (current-client-session-id))
             player-uuid (some-> (or (:player-uuid owner-map)
                                     (:uuid owner-map))
                                 str)]
         [(require-client-owner-value owner ":client-session-id" session-id)
          (require-client-owner-value owner ":player-uuid" player-uuid)]))))

(defn- with-client-owner-bindings
  [owner f]
  (let [[session-id player-uuid] (client-ui-owner-key owner)]
    (runtime-hooks/with-client-ctx-fn {:session-id session-id} (fn [] (runtime-hooks/with-player-state-owner-fn
        {:client-session-id session-id
         :player-uuid player-uuid}
        f)))))

(defn- with-client-player-state-owner
  [player-uuid f]
  (let [player-uuid* (require-client-owner-value {:player-uuid player-uuid}
                                                 ":player-uuid"
                                                 (some-> player-uuid str))]
    (let [session-id (require-client-owner-value {} ":client-session-id" (current-client-session-id))]
      (with-client-owner-bindings {:client-session-id session-id
                                   :player-uuid player-uuid*}
        #(f session-id player-uuid*)))))

(defn- client-ui-read-owner-key
  [player-uuid]
  (let [session-id (require-client-owner-value {} ":client-session-id" (current-client-session-id))
        player-uuid* (require-client-owner-value {:player-uuid player-uuid}
                                                 ":player-uuid"
                                                 (some-> player-uuid str))]
    [session-id :client-ui-hooks player-uuid*]))

(defn- get-client-player-state
  [player-uuid]
  (read-model/get-player-state (client-ui-read-owner-key player-uuid)))

(defn- ensure-client-player-state!
  [player-uuid]
  (read-model/ensure-player-state! (client-ui-read-owner-key player-uuid)))

(defn- update-client-ability-data!
  [player-uuid ability-data]
  (with-client-player-state-owner player-uuid
    (fn [session-id player-uuid*]
      (command-rt/run-command-in-session!
       session-id
       player-uuid*
       {:command :hydrate-player-state
        :ability-data ability-data}))))

(defn- update-client-resource-data!
  [player-uuid resource-data]
  (with-client-player-state-owner player-uuid
    (fn [session-id player-uuid*]
      (command-rt/run-command-in-session!
       session-id
       player-uuid*
       {:command :hydrate-player-state
        :resource-data resource-data}))))

(defn- update-client-cooldown-data!
  [player-uuid cooldown-data]
  (with-client-player-state-owner player-uuid
    (fn [session-id player-uuid*]
      (command-rt/run-command-in-session!
       session-id
       player-uuid*
       {:command :hydrate-player-state
        :cooldown-data cooldown-data}))))

(defn- update-client-preset-data!
  [player-uuid preset-data]
  (with-client-player-state-owner player-uuid
    (fn [session-id player-uuid*]
      (command-rt/run-command-in-session!
       session-id
       player-uuid*
       {:command :hydrate-player-state
        :preset-data preset-data}))))

(declare runtime-sync-resets-input?
         resource-sync-disables-input?
         abort-all-slot-contexts-for-owner!)

(defn- apply-client-runtime-v2!
  [{:keys [version opcode uuid revision dirty-mask] :as payload}]
  (when (= 1 opcode)
    (log/debug "[SYNC-TRACE][CLIENT] full sync recv"
              {:uuid uuid
               :revision revision
               :dirty-mask dirty-mask
               :category-id (get-in payload [:ability-data :category-id])
               :session-id (current-client-session-id)}))
  (when (and (= 2 version) (or (= 1 opcode) (= 2 opcode)) uuid
             (integer? revision) (integer? dirty-mask))
    (let [old-state (get-client-player-state uuid)
          old-revision (long (get old-state :sync-revision -1))
          mask (long dirty-mask)]
      (when (<= (long revision) old-revision)
        (log/debug "[SYNC-TRACE][CLIENT] skip stale sync"
                  {:opcode opcode :revision revision :old-revision old-revision}))
      (when (> (long revision) old-revision)
        (when (and (not (zero? (bit-and mask store/resource-data-mask)))
                   (not= (boolean (get-in old-state [:resource-data :activated]))
                         (boolean (get-in payload [:resource-data :activated]))))
          (log/debug "[SYNC-TRACE][CLIENT] activated sync"
                    {:from (boolean (get-in old-state [:resource-data :activated]))
                     :to (boolean (get-in payload [:resource-data :activated]))
                     :revision revision}))
        (let [command (cond-> {:command :hydrate-player-state
                               :sync-revision (long revision)}
                        (not (zero? (bit-and mask store/ability-data-mask)))
                        (assoc :ability-data (:ability-data payload))
                        (not (zero? (bit-and mask store/resource-data-mask)))
                        (assoc :resource-data (:resource-data payload))
                        (not (zero? (bit-and mask store/cooldown-data-mask)))
                        (assoc :cooldown-data (:cooldown-data payload))
                        (not (zero? (bit-and mask store/preset-data-mask)))
                        (assoc :preset-data (:preset-data payload))
                        (not (zero? (bit-and mask store/develop-data-mask)))
                        (assoc :develop-data (:develop-data payload)))]
          (with-client-player-state-owner uuid
            (fn [session-id player-uuid]
              (command-rt/run-command-in-session! session-id player-uuid command
                                                  {:mark-dirty? false})))
          (when (and (not (zero? (bit-and mask store/ability-data-mask)))
                     (runtime-sync-resets-input? (:ability-data old-state)
                                                 (:ability-data payload)))
            (abort-all-slot-contexts-for-owner! uuid)
            (client-keybinds/clear-client-keybind-state! uuid)
            (client-keybinds/clear-key-group! :default))
          (when (and (not (zero? (bit-and mask store/resource-data-mask)))
                     (resource-sync-disables-input? (:resource-data old-state)
                                                    (:resource-data payload)))
            (abort-all-slot-contexts-for-owner! uuid)
            (client-keybinds/clear-client-keybind-state! uuid))
          (when-not (zero? (bit-and mask store/preset-data-mask))
            (client-keybinds/update-default-group! uuid)
            (preset-editor-reactive/refresh-active-screen! uuid)))))))

(defn- client-context-owner
  [player-uuid]
  (let [[session-id player-uuid*] (client-ui-owner-key {:player-uuid player-uuid})]
    {:logical-side :client
     :client-session-id session-id
     :player-uuid player-uuid*}))

(defn- client-context-owner-from-owner
  [owner]
  (let [[session-id player-uuid] (client-ui-owner-key owner)]
    {:logical-side :client
     :client-session-id session-id
     :player-uuid player-uuid}))

(defn- player-contexts
  [player-uuid]
  (read-model/get-player-contexts-for-player (str player-uuid)
                                             (current-client-session-id)
                                             :client-ui-hooks))

(defn- with-client-context-owner
  [player-uuid f]
  (f (client-context-owner player-uuid)))

(defn- slot-context-key [player-uuid key-idx]
  (conj (client-ui-owner-key player-uuid) key-idx))

(defn- slot-key-owner
  [slot-key]
  (subvec (vec slot-key) 0 2))

(defn client-ui-state-snapshot
  ([]
   {:vm-wave-circles {}
    :vm-wave-last-spawn-ms {}
    :slot-context-ids (into {} slot-context-ids)
    :slot-key-tick-ms (into {} slot-key-tick-ms)
    :charge-coin-state (into {} charge-coin-state)
    :push-handlers-registered? (aget ^booleans push-handlers-registered 0)})
  ([owner]
   (let [owner-key (client-ui-owner-key owner)]
     {:vm-wave-circles []
      :vm-wave-last-spawn-ms 0
      :slot-context-ids (reduce (fn [result entry]
                                  (let [slot-key (.getKey ^java.util.Map$Entry entry)]
                                    (if (= owner-key (slot-key-owner slot-key))
                                      (assoc result slot-key (.getValue ^java.util.Map$Entry entry))
                                      result)))
                                {} (.entrySet slot-context-ids))
      :slot-key-tick-ms (reduce (fn [result entry]
                                  (let [slot-key (.getKey ^java.util.Map$Entry entry)]
                                    (if (= owner-key (slot-key-owner slot-key))
                                      (assoc result slot-key (.getValue ^java.util.Map$Entry entry))
                                      result)))
                                {} (.entrySet slot-key-tick-ms))
      :charge-coin-state (.get charge-coin-state owner-key)})))

(defn clear-client-ui-state!
  [owner]
  (let [owner-key (client-ui-owner-key owner)]
    (doseq [^HashMap cache [slot-context-ids slot-key-tick-ms]]
      (let [iterator (.iterator (.keySet cache))]
        (while (.hasNext iterator)
          (when (= owner-key (slot-key-owner (.next iterator)))
            (.remove iterator)))))
    (.remove charge-coin-state owner-key)
    (.remove overlay-skill-shape-cache owner-key)
    (.remove overlay-context-cache owner-key)
    (reactive-hud/clear-vm-wave-for-owner! (read-model/owner-key owner nil))
    (reactive-hud/clear-charging-arcs-for-owner! (read-model/owner-key owner nil))
    nil))

(defn- clear-client-player-state!
  [owner]
  (with-client-owner-bindings owner
    (fn []
      (let [[session-id player-uuid] (client-ui-owner-key owner)]
        (store/remove-player-state! session-id player-uuid))))
  nil)

(defn- clear-managed-screen-state!
  [owner]
  (skill-tree-screen/close-screen! owner)
  (preset-editor-screen/close-screen! owner)
  (location-teleport-reactive/close-screen! owner)
  nil)

(defn- clear-client-owned-runtime-state!
  [owner]
  (clear-managed-screen-state! owner)
  (ctx/clear-owner-contexts! (client-context-owner-from-owner owner))
  (clear-client-ui-state! owner)
  (client-keybinds/clear-client-keybind-state! owner)
  (client-particles/clear-owner-particle-effects! owner)
  (client-sounds/clear-owner-sound-effects! owner)
  (hand-effects/clear-owner-camera-pitch-deltas! owner)
  (clear-client-player-state! owner)
  nil)

(defn reset-client-ui-state-for-test!
  []
  (.clear slot-context-ids)
  (.clear slot-key-tick-ms)
  (.clear charge-coin-state)
  (.clear overlay-skill-shape-cache)
  (.clear overlay-context-cache)
  (aset-boolean ^booleans push-handlers-registered 0 false)
  (managed-screens/reset-managed-screen-state-for-test!)
  nil)

(defn- mark-client-push-handlers-registered!
  "Atomically check-and-set :push-handlers-registered? in client UI runtime state.
  Returns true only for the caller whose CAS committed the flag transition.
  Pure swap function — no side effects inside swap! (swap-vals! guarantees the
  old/new comparison is drawn from the committed values, not a retried attempt)."
  []
  (if (aget ^booleans push-handlers-registered 0)
    false
    (do (aset-boolean ^booleans push-handlers-registered 0 true) true)))

(defn set-slot-context-for-test!
  [player-uuid key-idx ctx-id]
  (.put slot-context-ids (slot-context-key player-uuid key-idx) ctx-id)
  nil)

(defn seed-vm-wave-state-for-test!
  ([owner circles]
   (seed-vm-wave-state-for-test! owner circles 0))
  ([owner circles last-spawn-ms]
   (reactive-hud/seed-vm-wave-state-for-test! owner circles last-spawn-ms)))

;; Client input descriptors are now managed solely through mcmod.protocol.keyboard-input
;; (System A). See cn.li.ac.input-ids for keybinding configuration and handler registration.
;; The old register-client-input-descriptor! / emit-client-input! path (System B) was dead
;; code — emit-client-input! was never called from any platform event loop.

(defn- railgun-charge-item-max-ticks []
  (skill-config/tunable-int :railgun :charge.item-charge-ticks))

(defn- railgun-coin-active-threshold []
  (skill-config/tunable-double :railgun :qte.coin-active-threshold))

(defn- railgun-coin-window-ms []
  (skill-config/tunable-int :railgun :qte.coin-window-ms))

(def ^:private coin-flight-init-vel 0.92)
(def ^:private coin-flight-gravity 0.06)
(def ^:private coin-flight-end-ms
  ;; Full flight back to launch height: t = 2·v0/g ticks → ms.
  (* 50.0 (/ (* 2.0 coin-flight-init-vel) coin-flight-gravity)))

(defn- coin-flight-progress
  "Analytic port of upstream EntityCoinThrowing#getProgress for the port's
  coin (vertical-ballistic, init-vel 0.92 / gravity 0.06, entities/all.clj
  entity_coin_throwing :hook-params). Rising half 0→0.5 to the apex
  (t = v0/g ≈ 767 ms), falling half 0.5→1.0 back to launch height
  (t = 2·v0/g ≈ 1533 ms). Mirrors the server-side judge
  (railgun.clj read-coin-qte-status) so the client window and the fire check
  share one clock — a linear time window expired before the perform phase
  (progress > 0.7 ≈ 1.25 s) ever started."
  [elapsed-ms]
  (let [v0 (double coin-flight-init-vel)
        g  (double coin-flight-gravity)
        apex-ms (* 50.0 (/ v0 g))
        t (double (max 0.0 elapsed-ms))]
    (if (<= t apex-ms)
      (* 0.5 (/ t apex-ms))
      (let [fall-ratio (/ (- t apex-ms) apex-ms)]
        (min 1.0 (+ 0.5 (* 0.5 fall-ratio fall-ratio)))))))

(defn- notify-charge-coin-throw!
  [player-uuid payload-now-ms]
  ;; Window timestamps use GAME time (the item handler's :now-ms = ticks×50 +
  ;; partial tick, matching the coin entity's tick-driven flight and the
  ;; server-side motion-progress judge). The overlay's per-frame now-ms is
  ;; wall clock, so charge-coin-visual-state converts it to game time via
  ;; client-bridge/game-time-ms; using wall time here made the displayed
  ;; progress drift from the coin's real flight and the QTE miss at the
  ;; perform boundary.
  (let [window-ms (max 1 (long (railgun-coin-window-ms)))]
    (.put charge-coin-state
          (client-ui-owner-key player-uuid)
          {:start-ms (long (or payload-now-ms (client-bridge/game-time-ms)))
           :window-ms window-ms})
    nil))

(defn- charge-coin-visual-state
  [player-uuid now-ms]
  (let [contexts (player-contexts player-uuid)
        railgun-ctx (some (fn [ctx-data]
                            (when (and (= :railgun (:skill-id ctx-data))
                                       (ctx/active-context? ctx-data))
                              ctx-data))
                          contexts)
        skill-state (:skill-state railgun-ctx)
        mode (:mode skill-state)
        charge-ticks (max 0 (int (or (:charge-ticks skill-state) 0)))
        max-charge-ticks (max 1 (int (railgun-charge-item-max-ticks)))]
    (if (= mode :item-charge)
      {:active? true
       :charge-ticks charge-ticks
       :coin-active? false
       :coin-progress 0.0
       :charge-start-ms nil
       :charge-ratio (max 0.0 (min 1.0 (- 1.0 (/ (double charge-ticks) max-charge-ticks))))}
      (let [owner-key (client-ui-owner-key player-uuid)
            {:keys [start-ms window-ms]} (get (charge-coin-state-snapshot) owner-key)
            ;; now-ms is required to compute the window; paths without a clock
            ;; (e.g. :client-slot-visual-state) degrade to the inactive branch.
            has-window? (and now-ms start-ms window-ms)
            ;; The overlay passes wall-clock now-ms, but the window is
            ;; game-time (start-ms from the item handler) and the coin's
            ;; flight is tick-driven — compute the elapsed on the game clock
            ;; (sub-tick precision via partial ticks) so the displayed
            ;; progress tracks the server-side motion-progress judge.
            elapsed (if has-window?
                      (- (long (or (try (client-bridge/game-time-ms)
                                        (catch Exception _ nil))
                                   now-ms))
                         (long start-ms))
                      0)
            ;; The window must stay open until the coin's perform phase ends —
            ;; a persisted qte.coin-window-ms shorter than the flight (e.g. the
            ;; old 1000 ms default) would close the window before progress 0.7
            ;; (≈1250 ms) ever arrived and made the QTE unfireable. Floor the
            ;; limit at the flight time; the config only ever extends it.
            window-limit (if has-window?
                           (max (long (Math/ceil coin-flight-end-ms))
                                (max 1 (long window-ms)))
                           1)
            progress (if has-window?
                       (coin-flight-progress (max 0 (min elapsed window-limit)))
                       0.0)
            active-window? (and has-window? (<= elapsed window-limit))
            ratio (max 0.0 (min 1.0 progress))
            coin-active? (and active-window? (>= ratio (railgun-coin-active-threshold)))]
        (when (and has-window? (not active-window?))
          (.remove charge-coin-state owner-key))
        {:active? (boolean active-window?)
         :charge-ticks 0
         :charge-start-ms start-ms
         :coin-active? (boolean coin-active?)
         :coin-progress ratio
         :charge-ratio ratio}))))

(defn- find-player-context
  [player-uuid skill-id]
  (some (fn [ctx-data]
          (when (and (= skill-id (:skill-id ctx-data))
                     (ctx/active-context? ctx-data))
            ctx-data))
  (player-contexts player-uuid)))

(defn- hold-ticks-from-context
  [ctx-data]
  (max 0 (long (or (get-in ctx-data [:skill-state :hold-ticks])
                   (:hold-ticks ctx-data)
                   0))))

(defn- body-intensify-visual-state
  [player-uuid]
  (let [ctx-data (find-player-context player-uuid :body-intensify)
        hold-ticks (hold-ticks-from-context ctx-data)
        max-ticks (max 1 (long (skill-config/tunable-int :body-intensify :charge.max-ticks)))]
    {:active? (boolean ctx-data)
     :charge-ticks hold-ticks
     :charge-ratio (max 0.0 (min 1.0 (/ (double hold-ticks) (double max-ticks))))}))

(defn- coin-qte-overlay-elements
  "Build golden coin-QTE timing window overlay elements.
   Rendered when railgun is in coin-QTE mode with an active timing window.
   Shows a ring/bar at screen center indicating window progress and the
   coin-active threshold marker, matching original AcademyCraft QTE UI."
  [player-uuid screen-width screen-height now-ms]
  (let [coin-state (charge-coin-visual-state player-uuid now-ms)]
    (when (and (:active? coin-state) (pos? (:coin-progress coin-state)))
      (let [cx (int (/ screen-width 2))
            cy (int (/ screen-height 2))
            progress (double (:coin-progress coin-state))
            coin-active? (boolean (:coin-active? coin-state))
            threshold (double (railgun-coin-active-threshold))
            ;; Ring geometry
            ring-radius 24
            ring-thickness 3
            segments 48
            dot-count 12
            ;; Colors
            window-color (if coin-active?
                          {:r 255 :g 215 :b 0 :a 220}   ;; gold when in active zone
                          {:r 180 :g 150 :b 50 :a 160}) ;; dim amber outside zone
            threshold-color {:r 255 :g 220 :b 80 :a 240}
            bg-color {:r 20 :g 18 :b 10 :a 100}]
        (concat
          ;; Background disc
          [{:kind :fill
            :x (- cx ring-radius) :y (- cy ring-radius)
            :w (* 2 ring-radius) :h (* 2 ring-radius)
            :color bg-color}]
          ;; Progress arc — dots around the ring showing window progress
          (for [i (range dot-count)
                :let [angle (* 2.0 Math/PI (/ i dot-count))
                      dot-active? (< (/ i dot-count) progress)
                      dx (int (* ring-radius (Math/cos angle)))
                      dy (int (* ring-radius (Math/sin angle)))
                      dot-size 3]]
            {:kind :fill
             :x (+ cx dx (- dot-size)) :y (+ cy dy (- dot-size))
             :w (* 2 dot-size) :h (* 2 dot-size)
             :color (if dot-active?
                     (update window-color :a #(int (* % (if coin-active? 1.0 0.6))))
                     (assoc window-color :a 40))})
          ;; Threshold marker — small bright dot at the threshold angle
          (let [threshold-angle (* 2.0 Math/PI threshold)
                tx (int (* ring-radius (Math/cos threshold-angle)))
                ty (int (* ring-radius (Math/sin threshold-angle)))
                marker-size 2]
            [{:kind :fill
              :x (+ cx tx (- marker-size)) :y (+ cy ty (- marker-size))
              :w (* 2 marker-size) :h (* 2 marker-size)
              :color threshold-color}])
          ;; Center text: progress percentage
          [{:kind :text
            :x (- cx 14) :y (- cy 4)
            :text (str (int (* 100.0 progress)) "%")
            :color (if coin-active?
                    {:r 255 :g 215 :b 0 :a 255}
                    {:r 180 :g 150 :b 50 :a 200})}])))))

(defn- preset-switch-state-for-overlay
  [player-uuid]
  (client-keybinds/get-preset-switch-state player-uuid))

(defn- remove-slot-context! [ctx-id]
  (let [iterator (.iterator (.entrySet slot-context-ids))]
    (while (.hasNext iterator)
      (when (= ctx-id (.getValue ^java.util.Map$Entry (.next iterator)))
        (.remove iterator))))
  nil)

(defn- context-id-for-slot!
  [player-uuid key-idx skill-id]
  (let [slot-key (slot-context-key player-uuid key-idx)]
    (or (get (slot-context-ids-snapshot) slot-key)
        (let [ctx-map (with-client-context-owner
                        player-uuid
                        (fn [owner]
                          (ctx-mgr/activate-context! owner player-uuid skill-id)))
              ctx-id (:id ctx-map)]
          (.put slot-context-ids slot-key ctx-id)
          ctx-id))))

(defn- send-with-client-owner!
  [player-uuid msg-id payload & [callback]]
  (net-client/send-to-server (client-context-owner player-uuid)
                             msg-id
                             payload
                             callback))

(defn- send-slot-key-message!
  [msg-id player-uuid key-idx]
  (if-let [skill-id (client-keybinds/get-skill-id-for-slot-public player-uuid key-idx)]
    (when-let [ctx-id (context-id-for-slot! player-uuid key-idx skill-id)]
      (log/debug "Slot key message sent"
                {:msg-id msg-id :key-idx key-idx :skill-id skill-id :ctx-id ctx-id})
      (send-with-client-owner! player-uuid msg-id {:ctx-id ctx-id
                                                   :skill-id skill-id
                                                   :key-idx key-idx})
      ctx-id)
    (log/debug "Slot key pressed but no skill bound to slot"
              {:msg-id msg-id :key-idx key-idx :player-uuid player-uuid})))

(defn- send-slot-keepalive!
  [player-uuid key-idx]
  (let [slot-key (slot-context-key player-uuid key-idx)]
    (when-let [ctx-id (get (slot-context-ids-snapshot) slot-key)]
      (send-with-client-owner! player-uuid catalog/MSG-CTX-KEEPALIVE {:ctx-id ctx-id})
      ctx-id)))

(defn- send-slot-key-up-message!
  [player-uuid key-idx]
  (let [slot-key (slot-context-key player-uuid key-idx)]
    (when-let [ctx-id (get (slot-context-ids-snapshot) slot-key)]
      (send-with-client-owner! player-uuid catalog/MSG-SLOT-KEY-UP {:ctx-id ctx-id
                                                                   :key-idx key-idx})
      (.remove slot-context-ids slot-key)
      ctx-id)))

(defn- abort-slot-context!
  [player-uuid key-idx]
  (let [slot-key (slot-context-key player-uuid key-idx)]
    (.remove slot-key-tick-ms slot-key)
    (when-let [ctx-id (get (slot-context-ids-snapshot) slot-key)]
      (send-with-client-owner! player-uuid catalog/MSG-SLOT-KEY-ABORT {:ctx-id ctx-id
                                                                       :key-idx key-idx})
      (.remove slot-context-ids slot-key)
      (with-client-context-owner player-uuid
        (fn [_owner]
          (ctx/with-context-owner (client-context-owner player-uuid)
            (ctx/terminate-context! ctx-id nil)
            (level-effects/clear-effect-owner! [:ctx ctx-id]))))
      ctx-id)))

(defn- clear-slot-key-ticks!
  [slot-key-pred]
  (let [iterator (.iterator (.keySet slot-key-tick-ms))]
    (while (.hasNext iterator)
      (when (slot-key-pred (.next iterator))
        (.remove iterator))))
  nil)

(defn- abort-slot-keys!
  [slot-keys]
  (doseq [slot-key slot-keys]
    (let [[_session-id player-uuid key-idx] slot-key]
      (abort-slot-context! player-uuid key-idx))))

(defn- abort-all-slot-contexts-for-owner!
  [owner]
  (let [owner-key (client-ui-owner-key owner)
        abort-slots (into []
                          (filter #(= owner-key (slot-key-owner %)))
                          (.keySet slot-context-ids))]
    (abort-slot-keys! abort-slots)
    (clear-slot-key-ticks! #(= owner-key (slot-key-owner %)))))

(defn- abort-all-slot-contexts-for-session!
  [session-id]
  (let [abort-slots (into []
                          (filter #(= session-id (first %)))
                          (.keySet slot-context-ids))]
    (abort-slot-keys! abort-slots)
    (clear-slot-key-ticks! #(= session-id (first %)))))

(defn- runtime-sync-resets-input?
  [old-ability-data new-ability-data]
  (let [old-category (:category-id old-ability-data)
        new-category (:category-id new-ability-data)
        old-learned (or (:learned-skills old-ability-data) [])
        new-learned (or (:learned-skills new-ability-data) [])]
    (or (not= old-category new-category)
        (and (seq old-learned)
             (empty? new-learned)))))

(defn- resource-sync-disables-input?
  [old-resource-data new-resource-data]
  (let [old-activated (boolean (:activated old-resource-data))
        new-activated (boolean (:activated new-resource-data))
        old-usable (resource-check/can-use-resource-data? old-resource-data)
        new-usable (resource-check/can-use-resource-data? new-resource-data)]
    (or (and old-activated (not new-activated))
        (and old-usable (not new-usable)))))

(defn- flush-buffered-context-message!
  [player-uuid ctx-id {:keys [channel payload]}]
  (send-with-client-owner! player-uuid catalog/MSG-CTX-CHANNEL {:ctx-id ctx-id
                                                                :channel channel
                                                                :payload payload}))

(defn- active-context-ids-for-skill
  [player-uuid skill-id]
  ;; Scan the CLIENT context registry, not the slot map: the slot entry is
  ;; cleared at key-up, so keep-active contexts (storm-wing / flashing) are
  ;; never found there — their movement sub-keys would silently go dead
  ;; (float with no way to steer) and the HUD hint column would never show.
  (->> (ctx/get-all-contexts)
       (keep (fn [[_key ctx-data]]
               (when (and (= (str player-uuid) (:player-uuid ctx-data))
                          (= skill-id (:skill-id ctx-data))
                          (ctx/active-context? ctx-data))
                 (:id ctx-data))))
       distinct
       vec))

(defn- send-active-wheel-message!
  "Mouse-wheel distance control (upstream PenetrateTeleport
  onPlayerUseWheel): route the raw wheel delta to every ACTIVE
  penetrate-teleport context. The loader input listener only knows the
  player, not which slot is bound to penetrate — resolve the contexts from
  the client context registry (the slot map entry is cleared at key-up, so
  keep-active contexts are never found there).

  Returns true when at least one context consumed the wheel — the loader
  input listener then cancels the scroll (hotbar stays put, matching
  upstream's wheel-as-distance-control while the skill key is held)."
  [player-uuid delta]
  (when (and (gameplay/use-mouse-wheel-enabled?)
             (number? delta)
             (not (zero? (double delta))))
    (let [ctx-ids (active-context-ids-for-skill player-uuid :penetrate-teleport)]
      (doseq [ctx-id ctx-ids]
        (send-with-client-owner! player-uuid catalog/MSG-CTX-CHANNEL
                                 {:ctx-id ctx-id
                                  :channel :penetrate-tp/set-distance
                                  :payload {:delta (double delta)}})
        ;; The server side updates its skill-state for the release cost, but
        ;; the client's tick! recomputes the preview from the CLIENT
        ;; skill-state — dispatch the same channel locally so the marker
        ;; follows the wheel (upstream l_updateMark reads curDist directly
        ;; on the client). The wheel callback only binds the session, so
        ;; scope the context owner for the dispatch (the ctx-on! handler
        ;; calls update-skill-state-root! with the bound owner).
        (ctx/with-context-owner (client-context-owner player-uuid)
          (ctx/ctx-send-to-local! ctx-id :penetrate-tp/set-distance
                                  {:delta (double delta)})))
      (boolean (seq ctx-ids)))))

(def ^:private movement-skill-channel-maps
  "Keep-active skills that steer via WASD movement sub-keys (upstream
  KEY_GROUP) -> per-transition context channel."
  {:flashing {:down :flashing/move-down
              :tick :flashing/move-tick
              :up :flashing/move-up}
   :storm-wing {:down :storm-wing/move-down
                :tick :storm-wing/move-tick
                :up :storm-wing/move-up}})

(defn- active-movement-contexts
  "One registry pass collecting [skill-id ctx-id] for every alive
  keep-active movement context of `player-uuid`. A single get-all-contexts
  instead of one full snapshot+projection per skill — the movement tick
  fires per frame while a WASD key is held, and the scan is the dominant
  cost, so it must not scale with the number of keep-active skills."
  [player-uuid]
  (->> (ctx/get-all-contexts)
       (reduce-kv (fn [acc _k ctx-data]
                    (if (and (contains? movement-skill-channel-maps (:skill-id ctx-data))
                             (= (str player-uuid) (:player-uuid ctx-data))
                             (ctx/active-context? ctx-data))
                      (conj acc [(:skill-id ctx-data) (:id ctx-data)])
                      acc))
                  [])
       distinct
       vec))

(defn- send-movement-message!
  [player-uuid transition movement-key]
  (doseq [[skill-id ctx-id] (active-movement-contexts player-uuid)
          :let [channel (get-in movement-skill-channel-maps [skill-id transition])]
          :when channel]
    (send-with-client-owner!
     player-uuid
     catalog/MSG-CTX-CHANNEL
     {:ctx-id ctx-id
      :channel channel
      :payload {:key movement-key}})))

(defn- scan-vm-contexts
  "Single-pass context walk: returns reflection-active?, deviation-active?, and
   crosshair-intensity in one traversal. Replaces three separate get-all-contexts calls.

   Active when the server context carries the toggle OR the client-side fx
   state says the effect is up (fx-start received, no fx-end yet) — the fx
   signal works on any connection topology, while the server-context
   projection only sees server contexts when client and server share one
   runtime (single-player)."
  [player-uuid]
  (let [fx-reflection (reactive-hud/fx-effect-active? :vec-reflection)
        fx-deviation (reactive-hud/fx-effect-active? :vec-deviation)]
    (reduce
      (fn [acc [_ctx-id ctx-data :as _entry]]
        (if (and (= (:player-uuid ctx-data) player-uuid)
                 (ctx/active-context? ctx-data))
          (cond-> acc
            (or (toggle/is-toggle-active? ctx-data :vec-reflection) fx-reflection)
            (-> (assoc :reflection-active? true)
                (assoc :reflection-intensity
                       (let [ticks (long (or (get-in ctx-data [:skill-state :toggle :vec-reflection :total-ticks]) 0))
                             fx-ticks (reactive-hud/fx-effect-ticks :vec-reflection)]
                         (double (min 1.0 (/ (max ticks fx-ticks) 20.0))))))
            (or (toggle/is-toggle-active? ctx-data :vec-deviation) fx-deviation)
            (assoc :deviation-active? true))
          acc))
      {:reflection-active? fx-reflection
       :deviation-active? fx-deviation
       :reflection-intensity 0.0}
      (ctx/get-all-contexts))))

(defn- vm-wave-elements [player-uuid now-ms tint]
  (reactive-hud/build-vm-wave-overlay-elements player-uuid now-ms tint))

(defn- build-hud-model-from-state [player-state activated-override]
  (when player-state
    (let [resource-data (:resource-data player-state)
          ability-data (:ability-data player-state)
          preset-data-map (:preset-data player-state)
          activated (if (contains? activated-override :value)
                      (:value activated-override)
                      (boolean (:activated resource-data)))
          category-id (:category-id ability-data)
          cat (when category-id (category/get-category category-id))]
      {:cp {:cur (double (or (:cur-cp resource-data) 0.0))
            :max (double (or (:max-cp resource-data) 1.0))}
       :overload {:cur (double (or (:cur-overload resource-data) 0.0))
                  :max (double (or (:max-overload resource-data) 1.0))
                  :fine (boolean (get resource-data :overload-fine true))}
       :active-slots (vec (preset-data/get-active-slots preset-data-map))
       :activated activated
       :category-id category-id
       :category-color (:color cat)
       :category-icon (:icon cat)
       :interfered? (boolean (seq (:interferences resource-data)))})))

(defn- hud-render-data->overlay-elements [hud-render-data screen-width screen-height]
  (let [cp-bar (some-> (:cp-bar hud-render-data) (assoc :kind :bar) (dissoc :type))
        overload-bar (some-> (:overload-bar hud-render-data) (assoc :kind :bar) (dissoc :type))
        activation-indicator (some-> (:activation-indicator hud-render-data)
                                     (assoc :kind :activation-indicator
                                            :x (int (/ screen-width 2)))
                                     (dissoc :type))
      combat-notice (some-> (:combat-notice hud-render-data)
            (assoc :kind :text
                   :x (int (/ screen-width 2)))
            (dissoc :type))
         skill-slots (mapv (fn [slot]
              (-> slot
                  (assoc :kind :content-slot
                    :content-icon (:skill-icon slot)
                    :content-label (:skill-name slot)
                    :disabled? (:in-cooldown slot)
                    :status-seconds (:cooldown-seconds slot)
                    :timer-total (:cooldown-total slot)
                    :timer-remaining (:cooldown-remaining slot))
                  (dissoc :type :skill-id :skill-icon :skill-name :in-cooldown :cooldown-seconds
                          :cooldown-total :cooldown-remaining)))
                          (or (:skill-slots hud-render-data) []))
        preset-indicators (mapv (fn [p]
                        (-> p
                            (assoc :kind :selection-indicator
                                   :x (int (/ screen-width 2))
                                   :y (- screen-height 45))
                            (dissoc :type)))
                      (or (:preset-indicators hud-render-data) []))
        overload-pulse (when-let [ol-bar (:overload-bar hud-render-data)]
                         (let [pct (double (or (:percent ol-bar) 0.0))]
                           (when (> pct 0.8)
                             {:kind :overload-pulse
                              :intensity (* (- pct 0.8) 5.0)})))
        numbers-texts (or (:numbers-texts hud-render-data) [])
        movement-hints (some-> (:movement-hints hud-render-data) (dissoc :type))]
                (persistent!
                  (let [out (transient [])]
                    (doseq [x (keep identity [cp-bar overload-bar activation-indicator combat-notice overload-pulse])]
                      (conj! out x))
                    (doseq [x preset-indicators] (conj! out x))
                    (doseq [x skill-slots] (conj! out x))
                    (when movement-hints (conj! out movement-hints))
                    (doseq [x numbers-texts] (conj! out x))
                    out))))

(defn- tutorial-notification-elements [screen-width screen-height now-ms]
  (try
    (tutorial-notification/build-notification-elements! screen-width screen-height now-ms)
    (catch Throwable _ [])))

(defn- active-skill-cp-cost-from-contexts
  "Compute consumption-hint CP cost for the first active skill with a computable cost,
   given an already-fetched contexts collection. Iterates over active (non-terminated)
   contexts, trying common cost paths (tick, down, up, release) in order. Returns nil
   if no active skill has a cost.

   Replaces the railgun-specific coin-QTE logic with a general approach that
   works for all skills matching original AcademyCraft CPBar consumption-hint behavior."
  [contexts]
  (let [active-ctxs (filter ctx/active-context? contexts)]
    (some
      (fn [ctx-data]
        (let [skill-id (:skill-id ctx-data)
              exp (double (or (:exp ctx-data) 0.0))]
          (some
            (fn [cost-path]
              (try
                (let [cost (skill-config/lerp-double skill-id cost-path exp)]
                  (when (pos? cost) (double cost)))
                (catch Throwable _ nil)))
            [:cost.tick.cp :cost.down.cp :cost.up.cp :cost.release.cp :cost.attack.cp])))
      active-ctxs)))

(defn- cached-skill-slot-shapes
  "Cache A: skill-slot shape (icon/name/key-label/position), keyed on preset-data
   identity. Returns cached shapes when preset-data is unchanged, else rebuilds
   via hud-renderer/build-skill-slot-shape and writes the cache."
  [owner-key hud-model screen-width screen-height preset-data]
  (let [cache (get (overlay-skill-shape-cache-snapshot) owner-key)]
    (if (and cache (identical? preset-data (:last-preset-data cache)))
      (:shapes cache)
      (let [shapes (hud-renderer/build-skill-slot-shape hud-model screen-width screen-height)]
        (.put overlay-skill-shape-cache owner-key
              {:last-preset-data preset-data :shapes shapes})
        shapes))))

(defn- cached-context-data
  "Cache B: active-contexts + consumption-hint, keyed on a context-registry
   snapshot token. Both the skill-slot delegate-state patch and the
   consumption-hint computation share this single (allocating) context read
   instead of each scanning contexts independently every frame."
  [owner-key player-uuid]
  (let [token (ctx/contexts-version-token)
        cache (get (overlay-context-cache-snapshot) owner-key)]
    (if (and cache (identical? token (:last-contexts-token cache)))
      cache
      (let [contexts (player-contexts player-uuid)
            hint (active-skill-cp-cost-from-contexts contexts)
            entry {:last-contexts-token token
                   :active-contexts contexts
                   :consumption-hint hint}]
        (.put overlay-context-cache owner-key entry)
        entry))))

(defn reactive-overlay-snapshot
  "Signal-oriented HUD snapshot for reactive overlay updates.
   Reuses the same state reads as build-client-overlay-plan without building element vectors."
  [player-uuid overlay-state]
  (let [player-state (get-client-player-state player-uuid)
        resource-data (:resource-data player-state)
        activated-override {:value (if (some? (:activated-override overlay-state))
                                     (boolean (:activated-override overlay-state))
                                     (boolean (get resource-data :activated false)))}
        hud-model (build-hud-model-from-state player-state activated-override)
        now-ms (long (or (:now-ms overlay-state) (System/currentTimeMillis)))]
    (when hud-model
      (let [cp-bar (when (:activated hud-model) (hud-renderer/build-cp-bar-render-data hud-model))
            overload-bar (when (:activated hud-model)
                           (hud-renderer/build-overload-bar-render-data hud-model now-ms))]
        {:activated (:activated hud-model)
         :cp-percent (double (or (:percent cp-bar) 0.0))
         :cp-hint-percent (some-> cp-bar :hint-percent double)
         :cp-full-glow? (boolean (:full-glow? cp-bar))
         :overload-percent (double (or (:percent overload-bar) 0.0))
         :overload-scroll (double (or (:scroll-offset overload-bar) 0.0))
         :overloaded? (boolean (:overloaded overload-bar))
         :interfered? (:interfered? hud-model)}))))

(defn build-client-overlay-plan [player-uuid screen-width screen-height overlay-state]
  ;; When an overlay app is active, skip normal HUD and render overlay app elements.
  (if-let [app-kw (:active-overlay-app overlay-state)]
    (case app-kw
      :freq-tx {:elements (vec (freq-tx/build-overlay-elements player-uuid screen-width screen-height))
                :background-mask {:r 0.0 :g 0.0 :b 0.0 :a 0.45}
                :interfered? false}
      :install-fx {:elements (vec (install-fx/build-overlay-elements player-uuid screen-width screen-height))
                   :background-mask {:r 0.0 :g 0.0 :b 0.0 :a 0.4}
                   :interfered? false}
      {:elements [{:kind :text :text (str "Unknown overlay app: " (name app-kw)) :x 20 :y 20 :color 0xFFFF0000}]
       :background-mask {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
       :interfered? false})
    ;; Normal HUD rendering
    (let [player-state (get-client-player-state player-uuid)
        resource-data (:resource-data player-state)
        ability-data (:ability-data player-state)
        activated-override {:value (if (some? (:activated-override overlay-state))
                                     (boolean (:activated-override overlay-state))
                                     (boolean (get resource-data :activated false)))}
        ;; BackgroundMask: compute target color from category / overload state
        category-id (:category-id ability-data)
        cat (when category-id (category/get-category category-id))
        cat-color (:color cat)
        overloaded? (not (get resource-data :overload-fine true))
        activated? (boolean (get resource-data :activated false))
        bg-mask (cond
                  overloaded? {:r 0.82 :g 0.08 :b 0.08 :a 0.65}   ;; red, original CRL_OVERRIDE
                  (and activated? cat-color) {:r (double (nth cat-color 0))
                                              :g (double (nth cat-color 1))
                                              :b (double (nth cat-color 2))
                                              :a 0.35}              ;; subtle category tint
                  :else {:r 0.0 :g 0.0 :b 0.0 :a 0.0})            ;; invisible
        ;; Interference detection
        interfered? (boolean (seq (:interferences resource-data)))
        ;; Numbers display state (was previously dropped)
        showing-numbers? (boolean (:showing-numbers? overlay-state false))
        last-show-value-change-ms (long (or (:last-show-value-change-ms overlay-state) 0))
        hud-model (build-hud-model-from-state player-state activated-override)
        now-ms (long (or (:now-ms overlay-state) (System/currentTimeMillis)))
        charge-state (charge-coin-visual-state player-uuid now-ms)
        owner-key (client-ui-owner-key player-uuid)
        preset-data (:preset-data player-state)
        {:keys [active-contexts consumption-hint]} (cached-context-data owner-key player-uuid)
        hud-model (if consumption-hint
                    (assoc hud-model :consumption-hint consumption-hint)
                    hud-model)
        cooldown-data (:cooldown-data player-state)
        skill-exps (get-in player-state [:ability-data :skill-exps])
        activate-hint (client-keybinds/get-activate-hint player-uuid)
        preset-state (preset-switch-state-for-overlay player-uuid)
        ;; Reactive assembly (Cache A/B, see docs/dev plan "Overlay/HUD 响应式重构"):
        ;; cp-bar/overload-bar/activation-indicator/preset-indicators/numbers-texts stay
        ;; unconditional per-frame calls (cheap, no registry/context lookups). Skill-slot
        ;; shape (registry lookups) is Cache A; delegate-state/consumption-hint share the
        ;; single Cache B context read above; cooldown fields are patched fresh every frame.
        preset-indicators (hud-renderer/build-preset-indicators-data preset-state now-ms)
        preset-indicator (last preset-indicators)
        numbers-texts (hud-renderer/build-numbers-texts-data hud-model showing-numbers?
                                                             last-show-value-change-ms now-ms)
        skill-slots (when (:activated hud-model)
                      (-> (cached-skill-slot-shapes owner-key hud-model screen-width screen-height preset-data)
                          (hud-renderer/patch-skill-slot-cooldown cooldown-data {:player-id player-uuid
                                                     :skill-exps skill-exps})
                          (hud-renderer/patch-skill-slot-visual
                           (reactive-hud/with-fx-active-contexts active-contexts)
                           player-uuid now-ms)))
        movement-hints (reactive-hud/build-movement-hints-data
                        player-uuid active-contexts screen-width screen-height)
        hud-render-data (when (or (:activated hud-model) preset-indicator showing-numbers?
                                  (pos? last-show-value-change-ms))
                          {:cp-bar (when (:activated hud-model) (hud-renderer/build-cp-bar-render-data hud-model))
                           :overload-bar (when (:activated hud-model)
                                           (hud-renderer/build-overload-bar-render-data hud-model now-ms))
                           :skill-slots skill-slots
                           :movement-hints movement-hints
                           :activation-indicator (when (:activated hud-model)
                                                    (hud-renderer/build-activation-indicator-data hud-model activate-hint))
                           :combat-notice nil
                           :preset-indicator preset-indicator
                           :preset-indicators preset-indicators
                           :numbers-texts numbers-texts})
         base-elements (hud-render-data->overlay-elements hud-render-data screen-width screen-height)
         coin-qte-elements (coin-qte-overlay-elements player-uuid screen-width screen-height now-ms)
         media-elements (media-player/build-aux-overlay-elements screen-width screen-height)
        vm (scan-vm-contexts player-uuid)
        reflection-active? (:reflection-active? vm)
        deviation-active? (:deviation-active? vm)
        vec-reflection-intensity (:reflection-intensity vm)
        vm-wave-active? (or reflection-active? deviation-active?)
        phase (double (/ (mod now-ms 1200) 1200.0))
        vm-wave-tint (cond
                       (and reflection-active? deviation-active?) [0.4 0.7 1.0]  ;; both: cyan-blue blend
                       reflection-active? [0.3 0.6 1.0]                           ;; VecReflection: blue
                       deviation-active? [0.3 1.0 0.6]                            ;; VecDeviation: green
                       :else [1.0 1.0 1.0])                                       ;; fallback: white
        vm-wave (vm-wave-elements player-uuid now-ms vm-wave-tint)
        crosshair (when reflection-active?
              {:kind :content-crosshair
                     :x (int (/ screen-width 2))
                     :y (int (/ screen-height 2))
                     :phase phase
                     :intensity (double (or vec-reflection-intensity 1.0))})]
    {:elements (persistent!
                 (let [out (transient [])]
                    (doseq [coll [base-elements coin-qte-elements media-elements vm-wave]]
                     (doseq [x coll] (conj! out x)))
                   (when crosshair (conj! out crosshair))
                   ;; Lazy: only build toast elements when toasts are active
                   (when (seq (toast/active-toasts-snapshot))
                     (doseq [x (toast/build-toast-elements screen-width screen-height now-ms)]
                       (conj! out x)))
                   ;; Lazy: only build notification elements when notifications are active
                   (when (seq (tutorial-notification/active-snapshot))
                     (doseq [x (tutorial-notification-elements screen-width screen-height now-ms)]
                       (conj! out x)))
                   ;; Lazy: only build debug elements when debug state is active
                   (when (not= :none (debug-overlay/current-state))
                     (doseq [x (debug-overlay/build-debug-overlay-elements player-state)]
                       (conj! out x)))
                   out))
     :background-mask bg-mask
     :interfered? interfered?})))

(defn- on-context-channel-push! [{:keys [ctx-id channel payload]}]
  (fx-registry/dispatch-fx-channel! ctx-id channel payload)
  (when (= channel :location-teleport/ui-open)
    (when-let [owner (managed-screens/active-owner location-teleport-reactive/screen-id)]
      (location-teleport-reactive/apply-server-payload! owner payload))
    (client-bridge/open-screen! :ac/saved-position payload))
  (ctx/ctx-send-to-local! ctx-id channel payload))

(defn register-client-push-handlers!
  []
  (when (mark-client-push-handlers-registered!)
    ;; Register reactive screen widget factories.
    (skill-tree-screen/install-widget-factory!)
    (preset-editor-screen/install-widget-factory!)
    (location-teleport-reactive/init!)
    (net-client/register-push-handler! catalog/MSG-SYNC-V2 apply-client-runtime-v2!)
            ;; the player-state hash — no push-refresh needed.
    (net-client/register-push-handler! catalog/MSG-CTX-ESTABLISH
      (fn [{:keys [ctx-id server-id]}]
        (when-let [owner (runtime-hooks/current-player-state-owner)]
          (ctx/with-context-owner owner
            (ctx/transition-to-alive! owner ctx-id server-id
                                      (fn [msg]
                                        (flush-buffered-context-message!
                                         (:player-uuid owner) ctx-id msg)))))))
    (net-client/register-push-handler! catalog/MSG-CTX-TERMINATE
      (fn [{:keys [ctx-id]}]
        (remove-slot-context! ctx-id)
        (ctx/terminate-context! ctx-id nil)
        ;; Externally aborted contexts (overload stun, death, category change)
        ;; never get their skill's :end channel — release the fx state so
        ;; persistent effects (storm-wing wings/loop sound) stop rendering.
        (level-effects/clear-effect-owner! [:ctx ctx-id])))
    (net-client/register-push-handler! catalog/MSG-CTX-TERMINATED
      (fn [{:keys [ctx-id]}]
        (remove-slot-context! ctx-id)
        (ctx/terminate-context! ctx-id nil)
        (level-effects/clear-effect-owner! [:ctx ctx-id])))
    (net-client/register-push-handler! catalog/MSG-CTX-CHANNEL on-context-channel-push!)
    ;; Side-effect cleanup moved out of render path into tick hooks
    (content-actions/register-client-tick-hook!
      (fn tick-vm-wave-circles []
        (when-let [player (client-bridge/get-client-player)]
          (let [player-uuid (uuid/player-uuid player)
                now-ms (client-bridge/game-time-ms)
                [screen-w screen-h] (client-bridge/get-window-size)
                {:keys [reflection-active? deviation-active?]} (scan-vm-contexts player-uuid)]
            (reactive-hud/tick-vm-wave! player-uuid
                                        (cond-> #{}
                                          reflection-active? (conj :vec-reflection)
                                          deviation-active? (conj :vec-deviation))
                                        (int screen-w) (int screen-h) now-ms)))))
    (content-actions/register-client-tick-hook!
      (fn tick-charging-arc-particles []
        (when-let [player (client-bridge/get-client-player)]
          (reactive-hud/tick-charging-arcs! (uuid/player-uuid player)))))
    (content-actions/register-client-tick-hook!
      (fn tick-cleanup-overlay []
        (toast/cleanup-expired!)
        (tutorial-notification/cleanup-expired!)))
    (log/debug "Ability client push handlers registered")))

(defn runtime-client-ui-hooks
  []
  (let [combat-notice-component (combat-notice/create-combat-notice-component
                                  {:now-ms-fn #(client-bridge/game-time-ms)})]
    {:client-get-skill-by-controllable
     (fn [cat-id ctrl-id]
       (skill-query/get-skill-by-controllable cat-id ctrl-id))

     :client-new-context
     (fn [player-uuid skill-id]
       (with-client-context-owner player-uuid (fn [owner] (ctx/new-context player-uuid skill-id owner))))

     :client-register-context!
     (fn [ctx-map]
       (ctx/register-context! ctx-map))

     :client-get-context
     (fn [ctx-id]
       (ctx/get-context ctx-id))

     :client-terminate-context!
     (fn [ctx-id _reason]
       (remove-slot-context! ctx-id)
       (ctx/terminate-context! ctx-id nil)
       (level-effects/clear-effect-owner! [:ctx ctx-id]))

     :client-transition-to-alive!
     (fn [ctx-id server-id payload]
       (ctx/transition-to-alive! ctx-id server-id payload))

     :client-send-context-local!
     (fn [ctx-id channel payload]
       (ctx/ctx-send-to-local! ctx-id channel payload))

     :client-on-slot-key-down!
     (fn [player-uuid key-idx]
       ;; Reset keepalive throttle so the first hold refresh after key-down is never suppressed.
       (.remove slot-key-tick-ms (slot-context-key player-uuid key-idx))
       (send-slot-key-message! catalog/MSG-SLOT-KEY-DOWN player-uuid key-idx))

     :client-on-slot-key-tick!
     (fn [player-uuid key-idx]
       (let [slot-key (slot-context-key player-uuid key-idx)
             now-ms   (System/currentTimeMillis)
             last-ms  (get (slot-key-tick-ms-snapshot) slot-key 0)]
         (when (>= (- now-ms last-ms) 100)
           (.put slot-key-tick-ms slot-key now-ms)
           ;; Active contexts are advanced authoritatively by
           ;; context-manager/tick-player-contexts! once per server tick.
           ;; Sending MSG-SLOT-KEY-TICK here as well double-dispatched costs
           ;; and effects (20 server ticks + client network ticks).
           (send-slot-keepalive! player-uuid key-idx))))

     :client-on-slot-key-up!
     (fn [player-uuid key-idx]
       (.remove slot-key-tick-ms (slot-context-key player-uuid key-idx))
       (send-slot-key-up-message! player-uuid key-idx))

     :client-on-slot-key-abort!
     (fn [player-uuid key-idx]
       (abort-slot-context! player-uuid key-idx))

     :client-on-movement-key-down!
     (fn [player-uuid movement-key]
       (send-movement-message! player-uuid :down movement-key))

     :client-on-movement-key-tick!
     (fn [player-uuid movement-key]
       (send-movement-message! player-uuid :tick movement-key))

     :client-on-movement-key-up!
     (fn [player-uuid movement-key]
       (send-movement-message! player-uuid :up movement-key))

     :client-on-slot-wheel!
     (fn [player-uuid _key-idx delta]
       (send-active-wheel-message! player-uuid delta))

     :client-clear-owner-state!
     (fn [owner]
       (when-let [session-id (or (:client-session-id (when (map? owner) owner))
                                 (current-client-session-id))]
         (combat-notice/clear-session! combat-notice-component session-id))
       (clear-client-owned-runtime-state! owner))

     :client-abort-all!
     (fn []
       (abort-all-slot-contexts-for-session!
        (require-client-owner-value {} ":client-session-id" (current-client-session-id))))

     :client-update-ability-data!
     (fn [player-uuid ability-data]
       (ensure-client-player-state! player-uuid)
       (update-client-ability-data! player-uuid ability-data))

     :client-update-resource-data!
     (fn [player-uuid resource-data]
       (ensure-client-player-state! player-uuid)
       (update-client-resource-data! player-uuid resource-data))

     :client-update-cooldown-data!
     (fn [player-uuid cooldown-data]
       (ensure-client-player-state! player-uuid)
       (update-client-cooldown-data! player-uuid cooldown-data))

     :client-update-preset-data!
     (fn [player-uuid preset-data]
       (ensure-client-player-state! player-uuid)
       (update-client-preset-data! player-uuid preset-data))

     :client-show-combat-notice!
     (fn [notice-id payload]
       (when-let [session-id (current-client-session-id)]
         (combat-notice/show-notice! combat-notice-component session-id notice-id payload)))

     :client-slot-visual-state
     (fn [player-uuid key-idx]
       (let [active-ctxs (player-contexts player-uuid)
             skill-id (client-keybinds/get-skill-id-for-slot-public player-uuid key-idx)]
         (:state (delegate-state/delegate-state-for-slot active-ctxs skill-id player-uuid))))

     :client-build-overlay-plan
     (fn [player-uuid screen-width screen-height overlay-state]
       (build-client-overlay-plan player-uuid screen-width screen-height overlay-state))

     :client-req-learn-skill!
     (fn [player-uuid skill-id extra callback]
       (client-api/req-learn-skill! (client-context-owner player-uuid) skill-id extra callback))

     :client-req-level-up!
     (fn [player-uuid callback]
       (client-api/req-level-up! (client-context-owner player-uuid) callback))

     :client-req-set-activated!
     (fn [player-uuid activated callback]
       (client-api/req-set-activated! (client-context-owner player-uuid) activated callback))

     :client-req-set-preset-slot!
     (fn [player-uuid preset-idx key-idx cat-id ctrl-id callback]
       (client-api/req-set-preset-slot! (client-context-owner player-uuid)
                                        preset-idx key-idx cat-id ctrl-id callback))

     :client-req-switch-preset!
     (fn [player-uuid preset-idx callback]
       (client-api/req-switch-preset! (client-context-owner player-uuid) preset-idx callback))

     :client-register-push-handlers!
     (fn []
       (register-client-push-handlers!))

     :client-notify-visual-event!
     (fn [event-key payload]
       (case event-key
         :ac/charge-coin-throw (notify-charge-coin-throw! (:player-uuid payload) (:now-ms payload))
         nil))

     :client-visual-state
     (fn [state-key payload]
       (case state-key
         :ac/charge-coin (charge-coin-visual-state (:player-uuid payload) (:now-ms payload))
         :ac/body-intensify-charge (body-intensify-visual-state (:player-uuid payload))
         :ac/current-charging (current-charging-fx/current-state (:player-uuid payload))
         :ac.delegate-state/railgun
         (let [{:keys [active? coin-active?]}
               (charge-coin-visual-state (:player-uuid payload) (:now-ms payload))]
           (when active? (if coin-active? :active :charge)))
         ;; Upstream PlasmaCannonContext implements IStateProvider: CHARGE
         ;; until the charge completes, ACTIVE after. Nothing else ever puts a
         ;; context into the :charge input-state, so without this override the
         ;; slot goes straight to the blue ACTIVE glow on key-down.
         :ac.delegate-state/plasma-cannon
         (plasma-cannon-fx/charge-visual-state (:player-uuid payload))
         nil))

     :client-trigger-mode-switch!
     (fn [player-uuid]
       (client-keybinds/trigger-mode-switch! player-uuid))

     :client-trigger-preset-switch!
     (fn [player-uuid]
       (client-keybinds/switch-preset! player-uuid))}))
