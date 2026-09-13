(ns cn.li.ac.ability.service.radiation-mark-index
  "Derived read index for meltdowner radiation marks.

  Authoritative data lives in per-player state
  ([:runtime :meltdowner :radiation-marks]); this index is a derived cache
  maintained via :radiation-index-sync effects emitted by the reducer
  (full replacement per source-player on every mark/clear/tick command).

  Tests must not seed radiation-marks via seed-player-state!/reset-store!
  directly — always go through commands so the index stays in sync."
  (:import [java.util HashMap ArrayList]))

(defonce ^:private ^HashMap sessions (HashMap.))

(defn- session-index
  ^HashMap [session-id create?]
  (or (.get sessions session-id)
      (when create?
        (let [idx (HashMap.)]
          (.put idx :by-source (HashMap.))
          (.put idx :by-target (HashMap.))
          (.put sessions session-id idx)
          idx))))

;; @index shape:
;; {server-session-id
;;  {:by-source {"caster-uuid" {"target-id" mark}}    ; mirrors each holder's
;;                                                     ; authoritative marks map
;;   :by-target {"target-id" {"caster-uuid" mark}}}}  ; reverse index for O(1)
;;                                                     ; per-target lookup

(defn apply-source-marks
  "Pure: replace source-id's entries inside one session index.
  Removes stale by-target reverse-entries and adds fresh ones; drops empty
  maps entirely rather than leaving empty-map residue."
  [session-idx source-id new-marks]
  (let [old-marks (get-in session-idx [:by-source source-id] {})
        removed (remove #(contains? new-marks %) (keys old-marks))
        idx (reduce (fn [i t]
                      (let [m (not-empty (dissoc (get-in i [:by-target t]) source-id))]
                        (if m
                          (assoc-in i [:by-target t] m)
                          (update i :by-target #(not-empty (dissoc % t))))))
                    session-idx removed)
        idx (reduce-kv (fn [i t mark] (assoc-in i [:by-target t source-id] mark))
                        idx new-marks)]
    (if (seq new-marks)
      (assoc-in idx [:by-source source-id] new-marks)
      (update idx :by-source #(not-empty (dissoc % source-id))))))

(defn sync-source-marks!
  "Idempotent full replacement of one player's mark entries in the index."
  [session-id source-id marks]
  (let [source-id (str source-id)
        ^HashMap idx (session-index session-id true)
        ^HashMap by-source (.get idx :by-source)
        ^HashMap by-target (.get idx :by-target)
        old-marks (.get by-source source-id)
        new-marks (or marks {})]
    (when old-marks
      (doseq [target-id (keys old-marks)]
        (when-not (contains? new-marks target-id)
          (when-let [^HashMap sources (.get by-target (str target-id))]
            (.remove sources source-id)
            (when (.isEmpty sources)
              (.remove by-target (str target-id)))))))
    (doseq [[target-id mark] new-marks]
      (let [target-id (str target-id)
            ^HashMap sources (or (.get by-target target-id)
                                 (let [m (HashMap.)]
                                   (.put by-target target-id m)
                                   m))]
        (.put sources source-id mark)))
    (if (seq new-marks)
      (.put by-source source-id new-marks)
      (.remove by-source source-id))
    (when (and (.isEmpty by-source) (.isEmpty by-target))
      (.remove sessions session-id)))
  nil)

(defn execute-index-sync!
  "Interpreter handler entry point for the :radiation-index-sync effect."
  [session-id {:keys [player-uuid marks]}]
  (sync-source-marks! session-id player-uuid marks))

(defn mark-holders
  "Player-uuid strings that currently hold at least one outgoing mark."
  [session-id]
  (if-let [^HashMap idx (session-index session-id false)]
    (ArrayList. (.keySet ^HashMap (.get idx :by-source)))
    ()))

(defn sources-for-target
  [session-id target-id]
  (if-let [^HashMap idx (session-index session-id false)]
    (if-let [^HashMap sources (.get ^HashMap (.get idx :by-target) (str target-id))]
      (ArrayList. (.keySet sources))
      ())
    ()))

(defn strongest-mark-for-target
  "Deterministic: the mark with the greatest :ticks-left among all sources
  currently marking target-id (matches mark-target!'s refresh-to-max semantics)."
  [session-id target-id]
  (when-let [^HashMap idx (session-index session-id false)]
    (when-let [^HashMap by-src (.get ^HashMap (.get idx :by-target) (str target-id))]
      (when-not (.isEmpty by-src)
        (reduce (fn [best mark]
                  (if (or (nil? best)
                          (> (long (or (:ticks-left mark) 0))
                             (long (or (:ticks-left best) 0))))
                    mark best))
                nil
                (.values by-src))))))

(defn snapshot-by-target
  [session-id]
  (if-let [^HashMap idx (session-index session-id false)]
    (reduce (fn [acc entry]
              (let [target-id (.getKey ^java.util.Map$Entry entry)]
                (assoc acc target-id (strongest-mark-for-target session-id target-id))))
            {}
            (.entrySet ^HashMap (.get idx :by-target)))
    {}))

(defn clear-session!
  [session-id]
  (.remove sessions session-id)
  nil)

(defn reset-for-test!
  []
  (.clear sessions)
  nil)
