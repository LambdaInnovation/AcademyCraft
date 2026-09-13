(ns cn.li.forge1201.client.key-input-fixtures
  "Test-only key input runtime fixtures (migrated from deprecated production key-input)."
  (:require [cn.li.mcmod.util.log :as log]))

(defn create-key-input-runtime
  ([] (create-key-input-runtime {}))
  ([initial-state]
   {:state (atom (merge {:slot-keys []
                         :screen-keys {}
                         :raw-v-state {}
                         :key-scheme :alternative
                         :override-active? {}}
                        initial-state))}))

(def ^:private _key-input-runtime (delay (create-key-input-runtime)))

(def ^:dynamic *key-input-runtime* nil)

(defn current-key-input-runtime []
  (or *key-input-runtime*
      @_key-input-runtime))

(defmacro with-key-input-runtime [runtime & body]
  `(binding [*key-input-runtime* ~runtime]
     ~@body))

(defn call-with-key-input-runtime [runtime f]
  (binding [*key-input-runtime* runtime]
    (f)))

(defn- key-input-runtime-state-atom []
  (:state (current-key-input-runtime)))

(defn key-input-runtime-state-snapshot []
  @(key-input-runtime-state-atom))

(defn- update-key-input-runtime! [f & args]
  (apply swap! (key-input-runtime-state-atom) f args))

(defn set-key-scheme! [scheme]
  (update-key-input-runtime! assoc :key-scheme scheme)
  (log/debug "Key scheme set" {:scheme scheme}))

(defn get-key-scheme []
  (:key-scheme (key-input-runtime-state-snapshot)))

(defn get-slot-keys []
  (:slot-keys (key-input-runtime-state-snapshot)))

(defn get-screen-keys []
  (vals (:screen-keys (key-input-runtime-state-snapshot))))

(defn input-state-snapshot []
  (let [{:keys [raw-v-state override-active?]} (key-input-runtime-state-snapshot)]
    {:raw-v-state raw-v-state
     :override-active? override-active?}))

(defn reset-input-state-for-test!
  ([] (reset-input-state-for-test! {}))
  ([{:keys [raw-v-state-map override-active-map key-scheme-value slot-keys-list screen-keys-map]
     :or {raw-v-state-map {}
          override-active-map {}
          key-scheme-value :alternative
          slot-keys-list []
          screen-keys-map {}}}]
   (update-key-input-runtime! merge
                              {:raw-v-state raw-v-state-map
                               :override-active? override-active-map
                               :key-scheme key-scheme-value
                               :slot-keys slot-keys-list
                               :screen-keys screen-keys-map})
   nil))

(defn- owner-key [owner]
  [(:client-session-id owner) (:player-uuid owner)])

(defn clear-owner-input-state! [owner]
  (let [k (owner-key owner)]
    (update-key-input-runtime!
     (fn [state]
       (-> state
           (update :raw-v-state dissoc k)
           (update :override-active? dissoc k))))
    nil))

(defn clear-client-input-session! [session-id]
  (update-key-input-runtime!
   (fn [state]
     (let [match? (fn [[sid _uuid]] (= sid session-id))]
       (-> state
           (update :raw-v-state #(into {} (remove (comp match? key) %)))
           (update :override-active? #(into {} (remove (comp match? key) %)))))))
  nil)
