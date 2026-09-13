(ns cn.li.mcmod.protocol.core
  "Shared registry abstraction for atom-backed metadata registries.

  Expected map keys:
  - :snapshot (fn [] -> state)
  - :swap-state! (fn [f] -> new-state)
  - :reset-state! (fn [new-state] -> new-state)")

(defn atom-registry
  [initial-state]
  (let [state-ref (atom initial-state)]
    {:snapshot (fn [] @state-ref)
     :swap-state! (fn [f] (swap! state-ref f))
     :reset-state! (fn [new-state] (reset! state-ref new-state))}))

(defn var-root-registry
  [state-var]
  (let [lock (Object.)]
    {:snapshot (fn [] (var-get state-var))
     :swap-state! (fn [f]
                    (locking lock
                      (let [next-state (f (var-get state-var))]
                        (alter-var-root state-var (constantly next-state))
                        next-state)))
     :reset-state! (fn [new-state]
                     (locking lock
                       (alter-var-root state-var (constantly new-state))
                       new-state))}))

(defn lookup
  ([registry k]
   (get ((:snapshot registry)) k))
  ([registry k default]
   (get ((:snapshot registry)) k default)))

(defn lookup-in
  [registry path]
  (get-in ((:snapshot registry)) path))
