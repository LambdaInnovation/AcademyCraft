(ns cn.li.ac.ability.client.managed-screens
  "Client-thread-confined owner/state registry for managed screens."
  (:import [java.util HashMap Map$Entry]))

(definterface IManagedScreenRuntime
  (^java.util.HashMap activeOwners [])
  (^java.util.HashMap screenStates []))

(deftype ManagedScreenRuntime [^HashMap active ^HashMap states]
  IManagedScreenRuntime
  (activeOwners [_] active)
  (screenStates [_] states))

(defn create-managed-screen-runtime []
  (ManagedScreenRuntime. (HashMap.) (HashMap.)))

(defonce ^:private runtime-slot
  (object-array [(create-managed-screen-runtime)]))

(defn- current-runtime ^ManagedScreenRuntime []
  (aget ^objects runtime-slot 0))

(defn call-with-managed-screen-runtime [runtime f]
  (let [previous (current-runtime)]
    (aset ^objects runtime-slot 0 runtime)
    (try (f) (finally (aset ^objects runtime-slot 0 previous)))))

(defn managed-screen-state-snapshot []
  (let [runtime (current-runtime)]
    {:active-owners (into {} (.activeOwners runtime))
     :states (into {}
                   (map (fn [^Map$Entry entry]
                          [(.getKey entry) (into {} ^HashMap (.getValue entry))]))
                   (.entrySet (.screenStates runtime)))}))

(defn reset-managed-screen-state-for-test! []
  (.clear (.activeOwners (current-runtime)))
  (.clear (.screenStates (current-runtime)))
  nil)

(defn set-active-owner! [screen-id owner-key]
  (.put (.activeOwners (current-runtime)) screen-id owner-key)
  owner-key)

(defn active-owner [screen-id]
  (.get (.activeOwners (current-runtime)) screen-id))

(defn- owner-states
  ^HashMap [screen-id create?]
  (let [^HashMap screens (.screenStates (current-runtime))]
    (or (.get screens screen-id)
        (when create?
          (let [created (HashMap.)]
            (.put screens screen-id created)
            created)))))

(defn screen-state [screen-id owner-key default-state]
  (if-let [^HashMap states (owner-states screen-id false)]
    (or (.get states owner-key) default-state)
    default-state))

(defn update-screen-state! [screen-id owner-key default-state f & args]
  (let [^HashMap states (owner-states screen-id true)
        current (or (.get states owner-key) default-state)
        next-state (apply f current args)]
    (.put states owner-key next-state)
    next-state))

(defn clear-screen-state! [screen-id owner-key]
  (when-let [^HashMap states (owner-states screen-id false)]
    (.remove states owner-key)
    (when (.isEmpty states)
      (.remove (.screenStates (current-runtime)) screen-id)))
  (when (= owner-key (active-owner screen-id))
    (.remove (.activeOwners (current-runtime)) screen-id))
  nil)
