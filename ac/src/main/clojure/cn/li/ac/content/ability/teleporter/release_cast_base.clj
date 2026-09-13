(ns cn.li.ac.content.ability.teleporter.release-cast-base
  "Shared preview-hold-release state machine for teleporter :release-cast skills.

  Each skill supplies tick/up hooks; down/abort use defaults unless overridden.
  No Minecraft imports."
  (:require [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.mcmod.util.log :as log]))

(defn build-ops
  "Build {:down! :tick! :up! :abort!} from skill-specific hooks.

  Required: :tick!, :up!
  Optional: :initial-state (default {:hold-ticks 0})
            :require-cost-on-down? (default true)
            :down!, :abort! (full overrides)
            :abort-log-label (debug message for default abort)"
  [{:keys [initial-state require-cost-on-down? down! tick! up! abort!
           abort-log-label]
    :or {initial-state {:hold-ticks 0}
         require-cost-on-down? true}}]
  (when (or (nil? tick!) (nil? up!))
    (throw (ex-info "release-cast build-ops requires :tick! and :up!" {})))
  {:down! (or down!
              (fn [ctx-id _player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage _player-ref]
                (when (or (not require-cost-on-down?) cost-ok?)
                  (ctx-skill/replace-skill-state! ctx-id initial-state))))
   :tick! tick!
   :up! up!
   :abort! (or abort!
                (fn [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
                  (ctx-skill/clear-skill-state! ctx-id)
                  (when abort-log-label
                    (log/debug abort-log-label))))})

(defn down! [ops & args] (apply (:down! ops) args))
(defn tick! [ops & args] (apply (:tick! ops) args))
(defn up! [ops & args] (apply (:up! ops) args))
(defn abort! [ops & args] (apply (:abort! ops) args))

(defn preview-tick!
  "Store preview under :preview-key and optional extra keys while key is held."
  [{:keys [preview-key build-preview merge-extra on-preview]}
   ctx-id player-id _skill-id _exp _cost-ok? hold-ticks _cost-stage player-ref]
  (let [key (or preview-key :preview)
        preview (build-preview player-id player-ref ctx-id hold-ticks)
        state (merge (or merge-extra {})
                     {:hold-ticks (long hold-ticks)
                      key preview})]
    (ctx-skill/replace-skill-state! ctx-id state)
    (when (and preview on-preview)
      (on-preview ctx-id player-id hold-ticks preview preview-key))))

(defn cached-preview
  "Return preview from skill-state or rebuild via :build-preview."
  [{:keys [preview-key build-preview]}
   player-id player-ref ctx-id]
  (let [key (or preview-key :preview)
        ctx-data (ctx-skill/get-context ctx-id)]
    (or (get-in ctx-data [:skill-state key])
        (build-preview player-id player-ref ctx-id
                       (long (or (get-in ctx-data [:skill-state :hold-ticks]) 0))))))
