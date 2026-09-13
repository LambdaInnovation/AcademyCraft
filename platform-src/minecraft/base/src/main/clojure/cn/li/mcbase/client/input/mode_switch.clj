(ns cn.li.mcbase.client.input.mode-switch
  "Shared client mode-switch key state machine (Minecraft 1.20.1).")

(def ^:private default-short-press-threshold-ns (* 300 1000 1000))

(defn initial-state
  []
  {:was-down false :down-at-ns nil})

(defn handle-button-state!
  "Advance a digital button state and fire callbacks on transitions.

  Options:
  - :now-ns                  current nanoTime (defaults to System/nanoTime)
  - :short-press-threshold-ns  threshold for short press detection (default 300ms)
  - :screen-open?            boolean; when true, short-up callback is skipped
  - :on-down                 (fn []) callback on up->down transition
  - :on-up                   (fn []) callback on down->up transition
  - :on-short-up             (fn []) callback on short down->up transition while screen closed"
  [state-atom is-down {:keys [now-ns
                              short-press-threshold-ns
                              screen-open?
                              on-down
                              on-up
                              on-short-up]
                       :or {now-ns (System/nanoTime)
                            short-press-threshold-ns default-short-press-threshold-ns}}]
  (let [{:keys [was-down down-at-ns]} @state-atom]
    (cond
      (and (not was-down) is-down)
      (do
        (swap! state-atom assoc :was-down true :down-at-ns now-ns)
        (when on-down (on-down)))

      (and was-down (not is-down))
      (let [held-ns (if down-at-ns (- now-ns down-at-ns) Long/MAX_VALUE)
            short-press? (< held-ns short-press-threshold-ns)]
        (swap! state-atom assoc :was-down false :down-at-ns nil)
        (when on-up (on-up))
        (when (and short-press?
                   (not (boolean screen-open?))
                   on-short-up)
          (on-short-up)))

      :else nil)))
