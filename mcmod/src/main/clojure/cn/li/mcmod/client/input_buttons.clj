(ns cn.li.mcmod.client.input-buttons
  "Platform-neutral button transition helper shared by loader input handlers.

   Keeping this tiny state machine out of AC prevents platform AOT entrypoints
   from pulling the AC client graph into their transitive compile closure.")

(defn initial-button-state
  []
  {:was-down false :down-at-ns nil})

(defn handle-button-state!
  "Advance a digital button state and run transition callbacks."
  [state-atom is-down {:keys [now-ns
                              short-press-threshold-ns
                              screen-open?
                              on-down
                              on-up
                              on-short-up]
                       :or {now-ns (System/nanoTime)
                            short-press-threshold-ns (* 300 1000 1000)}}]
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
