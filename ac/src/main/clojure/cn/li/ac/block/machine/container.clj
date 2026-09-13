(ns cn.li.ac.block.machine.container
  "Generate standard tile container function maps from declarative config."
  (:require [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.mcmod.platform.item :as pitem]))

(defn- stack-count [stack]
  (if (or (nil? stack)
          (try (boolean (pitem/empty? stack)) (catch Exception _ true)))
    0
    (try (int (pitem/stack-count stack)) (catch Exception _ 0))))

(defn make-inventory-container-fns
  "Build container fns for a fixed-size [:inventory slot] vector in customState.

  Options:
  - :default-state map
  - :slot-count int or (fn [] int) — thunk supported for lazy schema resolution
  - :inventory-key keyword (default :inventory)
  - :can-place? (fn [be slot item face] -> bool)
  - :can-take? (fn [be slot item face] -> bool)
  - :transform-state (fn [state] -> state) applied after inventory mutation
  - :slots-for-face optional (fn [be face] -> int[])"
  [{:keys [default-state slot-count inventory-key can-place? can-take? transform-state slots-for-face
           blockstate-updater]
    :or {inventory-key :inventory
         transform-state identity}}]
  (let [slot-count-fn (if (fn? slot-count) slot-count (fn [] (int (or slot-count 0))))
        assoc-slot (fn [state slot item]
                     (assoc-in state [inventory-key slot] item))
        apply-transform (fn [state]
                          (transform-state state))
        commit! (fn [be state']
                  ;; Inventory mutation is player-interaction-driven, not tick-driven —
                  ;; always sync-client here is cheap and preserves visuals for TESRs
                  ;; that read counts/amounts derived from slot contents (e.g. matrix
                  ;; shields, phase-gen liquid level).
                  (machine-runtime/commit-transform!
                    be default-state (constantly state')
                    :blockstate-updater blockstate-updater
                    :sync-client? true))]
    {:get-size (fn [_be] (slot-count-fn))
     :get-item (fn [be slot]
                 (get-in (machine-runtime/state-or-default be default-state) [inventory-key slot]))
     :set-item! (fn [be slot item]
                  (let [state (machine-runtime/state-or-default be default-state)
                        state' (apply-transform (assoc-slot state slot item))]
                    (commit! be state')))
     :remove-item (fn [be slot amount]
                    (let [state (machine-runtime/state-or-default be default-state)
                          item (get-in state [inventory-key slot])]
                      (when item
                        (let [cnt (stack-count item)]
                          (if (<= cnt amount)
                            (do (commit! be (apply-transform (assoc-slot state slot nil)))
                                item)
                            (pitem/split item amount))))))
     :remove-item-no-update (fn [be slot]
                              (let [state (machine-runtime/state-or-default be default-state)
                                    item (get-in state [inventory-key slot])]
                                (commit! be (apply-transform (assoc-slot state slot nil)))
                                item))
     :clear! (fn [be]
               (commit! be
                        (apply-transform
                          (assoc (machine-runtime/state-or-default be default-state)
                                 inventory-key (vec (repeat (slot-count-fn) nil))))))
     :still-valid (fn [_be _player] true)
     :slots-for-face (or slots-for-face (fn [_be _face] (int-array (range (slot-count-fn)))))
     :can-place (or can-place? (fn [_be _slot _item _face] false))
     :can-take (or can-take? (fn [_be _slot _item _face] false))}))
