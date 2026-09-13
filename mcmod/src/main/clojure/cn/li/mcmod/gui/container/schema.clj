(ns cn.li.mcmod.gui.container.schema
  "Generic schema-driven utilities for GUI container atom fields.

  Field schema entry:
    :key         - keyword, container key
    :init        - (fn [state]) -> initial value
    :sync?       - boolean, include in sync payload
    :payload-key - optional outgoing payload key
    :coerce      - (fn [v]) -> typed value when applying sync
    :close-reset - value used on close/reset")

(defn build-atoms
  "Build a map of {key (atom initial-value)} from field schema and state."
  [fields state]
  (into {}
        (map (fn [{:keys [key init]}]
               [key (atom (init state))])
             fields)))

(defn get-sync-data
  "Extract sync data from a container based on schema."
  [fields container]
  (into {}
        (for [{:keys [key sync?]} fields
              :when sync?]
          [key @(get container key)])))

(defn reset-atoms!
  "Reset all container atoms to their :close-reset values."
  [fields container]
  (doseq [{:keys [key close-reset]} fields]
    (when-let [a (get container key)]
      (reset! a close-reset))))

(defn apply-sync-data!
  "Apply sync data into container atoms, using each field's :coerce function."
  [fields container data]
  (doseq [{:keys [key coerce]} fields
          :when (contains? data key)]
    (when-let [a (get container key)]
      (reset! a (coerce (get data key))))))

(defn sync-field-mappings
  "Build mapping vector from schema sync fields.

  Emits :key when payload-key is same as key, otherwise emits [key payload-key]."
  [fields]
  (into []
        (for [{:keys [key payload-key sync?]} fields
              :when sync?]
          (let [pk (or payload-key key)]
            (if (= pk key)
              key
              [key pk])))))

(defn build-sync-packet-fields
  "Build synced fields payload using container atom values or :close-reset fallback."
  [fields container]
  (into {}
        (for [{:keys [key sync? close-reset]} fields
              :when sync?]
          [key (if-let [a (and container (get container key))]
                 @a
                 close-reset)])))

(defn build-sync-packet-payload
  "Build synced payload map keyed by :payload-key or :key.

  Value precedence:
  1) container atom value
  2) (init state)
  3) :close-reset"
  [fields container state]
  (into {}
        (for [{:keys [key init sync? close-reset payload-key]} fields
              :when sync?]
          (let [pk    (or payload-key key)
                value (if-let [a (and container (get container key))]
                        @a
                        (if (and state init)
                          (init state)
                          close-reset))]
            [pk value]))))

(defn sync-payload-dirty?
  "Return true when the current payload differs from the cached payload."
  [last-sent-atom has-sent?-atom payload]
  (or (not @has-sent?-atom)
      (not= payload @last-sent-atom)))

(defn cache-sync-payload!
  "Record a payload as the latest synced snapshot."
  [last-sent-atom has-sent?-atom payload]
  (reset! last-sent-atom payload)
  (reset! has-sent?-atom true)
  payload)
