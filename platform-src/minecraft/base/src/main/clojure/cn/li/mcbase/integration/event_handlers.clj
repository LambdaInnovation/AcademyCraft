(ns cn.li.mcbase.integration.event-handlers
  "Shared event handler logic for block interactions.

  Platform-specific dispatchers and handlers are passed as function parameters
  to enable both Forge and Fabric to use identical core event processing logic."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.block-runtime :as bquery]
            [cn.li.mcbase.integration.event-feedback :as event-feedback]))

(defn- identify-block-id
  [block]
  (bquery/identify-block-from-full-name (str block)))

(defn- dispatch-block-event
  [event-data dispatcher-fn event-key log-prefix]
  (let [{:keys [x y z block]} event-data
        block-name (str block)
        block-id (identify-block-id block)]
    (log/debug (str log-prefix " Event at (" x "," y "," z ") block:" block-name))
    (log/debug (str log-prefix " Identified block-id:" block-id))
    (when block-id
      (dispatcher-fn (assoc event-data :block-id block-id :event-key event-key)))))

(defn handle-block-place
  "Shared block place handler. Dispatches :on-place to registered block handlers."
  [event-data dispatcher-fn log-prefix]
  (dispatch-block-event event-data dispatcher-fn :on-place log-prefix))

(defn handle-block-break
  "Shared block break handler. Dispatches :on-break to registered block handlers."
  [event-data dispatcher-fn log-prefix]
  (dispatch-block-event event-data dispatcher-fn :on-break log-prefix))

(defn handle-block-right-click
  "Shared right-click handler logic accepting platform-specific dispatcher.

  Note: no ability-mode gating lives here — upstream AcademyCraft gates
  vanilla interactions solely through ControlOverrider (our
  vanilla-input-control SPI), which suppresses the attack/use KeyMappings only
  for slots that have a skill delegate. Empty slots keep full vanilla
  behavior (spawn eggs, chests, block breaking) while ability mode is on.

  Args:
  - event-data: map with :x, :y, :z, :block keys (and other event info)
  - dispatcher-fn: function to call with (assoc event-data :block-id block-id) on dispatch
  - gui-result-pred: predicate to check if result is a GUI open result (can be nil for no check)
  - gui-opener-fn: function to call with (gui-id player world pos tile-entity) to open GUI (can be nil)
  - log-prefix: string prefix for logging (e.g. \"[RIGHT-CLICK]\" or empty string)

  Returns: result from dispatcher-fn, or nil if no handler"
  [event-data dispatcher-fn gui-result-pred gui-opener-fn log-prefix]
  (let [{:keys [x y z block]} event-data
        block-name (str block)
        block-id (identify-block-id block)]
    (log/debug (str log-prefix " Event at (" x "," y "," z ") block:" block-name))
    (log/debug (str log-prefix " Identified block-id:" block-id))

    (if block-id
      (if (or (bquery/has-block-event-handler? block-id :on-right-click)
              (bquery/is-part-block? block-id))
        (do
          (log/debug (str log-prefix " Block has registered handler (or is part block), dispatching..."))
          (let [ret (dispatcher-fn (assoc event-data :block-id block-id))]
            (log/debug (str log-prefix " Dispatcher returned gui-id=" (:gui-id ret)
                            " player=" (some-> (:player ret) (str))
                            " pos=" (:pos ret)))
            (event-feedback/emit-feedback! event-data ret)

            ;; Handle GUI opening if result indicates it
            (when (and gui-result-pred gui-opener-fn ret (gui-result-pred ret))
              (try
                (let [{:keys [gui-id player world pos]} ret]
                  (when (and gui-id player world pos)
                    (let [^net.minecraft.world.level.Level world world
                          ^net.minecraft.core.BlockPos pos pos
                          tile-entity (.getBlockEntity world pos)]
                      (when tile-entity
                        (log/debug (str log-prefix " GUI result received: gui-id=" gui-id))
                        (gui-opener-fn gui-id player world pos tile-entity)))))
                (catch Exception e
                  (log/stacktrace (str log-prefix " Failed to open GUI") e))))
            ret))
        (log/debug (str log-prefix " Block has no registered :on-right-click handler")))
      (log/debug (str log-prefix " Could not identify block-id from:" block-name)))))
