(ns cn.li.mcmod.events.world-lifecycle
  "Thread-confined world lifecycle dispatch. Registration is mutable during
  bootstrap and frozen before gameplay; tick dispatch reads only ArrayLists."
  (:import [java.util ArrayList HashMap HashSet Iterator]))

(definterface ILifecycleEntry
  (^Object entryId [])
  (^clojure.lang.IFn entryFn []))

(deftype LifecycleEntry [id ^clojure.lang.IFn handler]
  ILifecycleEntry
  (entryId [_] id)
  (entryFn [_] handler))

(definterface IWorldLifecycleRuntime
  (^java.util.ArrayList loadHandlers [])
  (^java.util.ArrayList unloadHandlers [])
  (^java.util.ArrayList saveHandlers [])
  (^java.util.ArrayList tickHandlers [])
  (^java.util.HashMap handlerIndexes [])
  (^booleans frozenCell []))

(deftype WorldLifecycleRuntime
  [^ArrayList load ^ArrayList unload ^ArrayList save ^ArrayList tick
   ^HashMap indexes ^booleans frozen]
  IWorldLifecycleRuntime
  (loadHandlers [_] load)
  (unloadHandlers [_] unload)
  (saveHandlers [_] save)
  (tickHandlers [_] tick)
  (handlerIndexes [_] indexes)
  (frozenCell [_] frozen))

(defn create-world-lifecycle-runtime
  ^WorldLifecycleRuntime []
  (WorldLifecycleRuntime. (ArrayList.) (ArrayList.) (ArrayList.) (ArrayList.)
                          (HashMap.) (boolean-array 1)))

(defonce ^:private current-runtime-cell
  (object-array [(create-world-lifecycle-runtime)]))

(defn- current-runtime ^WorldLifecycleRuntime []
  (aget ^objects current-runtime-cell 0))

(defn call-with-world-lifecycle-runtime
  "Test/bootstrap scope helper. Gameplay owns one frozen runtime and never
  switches it."
  [runtime f]
  (let [previous (current-runtime)]
    (aset ^objects current-runtime-cell 0 runtime)
    (try (f) (finally (aset ^objects current-runtime-cell 0 previous)))))

(defn- report-handler-error! [phase ^Throwable t]
  (let [^java.io.Writer err *err*
        ^java.io.PrintWriter pw (if (instance? java.io.PrintWriter err)
                                  ^java.io.PrintWriter err
                                  (java.io.PrintWriter. err))]
    (.println pw (str "Error in world " (name phase) " handler: " (ex-message t)))
    (.printStackTrace t pw)))

(defn- phase-handlers
  ^ArrayList [^WorldLifecycleRuntime runtime phase]
  (case phase
    :load (.loadHandlers runtime)
    :unload (.unloadHandlers runtime)
    :save (.saveHandlers runtime)
    :tick (.tickHandlers runtime)))

(defn- assert-not-frozen! [^WorldLifecycleRuntime runtime]
  (when (aget (.frozenCell runtime) 0)
    (throw (ex-info "World lifecycle handlers are frozen" {}))))

(defn- register-handler-entry!
  [^WorldLifecycleRuntime runtime phase id ^clojure.lang.IFn handler]
  (let [^HashMap indexes (.handlerIndexes runtime)
        phase-key [phase id]
        ^LifecycleEntry existing (.get indexes phase-key)]
    (if existing
      (when-not (identical? handler (.entryFn existing))
        (throw (ex-info "Conflicting world lifecycle handler id" {:id id})))
      (let [entry (LifecycleEntry. id handler)]
        (.put indexes phase-key entry)
        (.add (phase-handlers runtime phase) entry))))
  nil)

(defn register-world-lifecycle-handler!
  [handler-map]
  (let [runtime (current-runtime)
        id (or (:id handler-map) handler-map)]
    (assert-not-frozen! runtime)
    (when-let [handler (:on-load handler-map)]
      (register-handler-entry! runtime :load id handler))
    (when-let [handler (:on-unload handler-map)]
      (register-handler-entry! runtime :unload id handler))
    (when-let [handler (:on-save handler-map)]
      (register-handler-entry! runtime :save id handler))
    (when-let [handler (:on-tick handler-map)]
      (register-handler-entry! runtime :tick id handler)))
  nil)

(defn freeze-world-lifecycle-handlers! []
  (aset-boolean (.frozenCell (current-runtime)) 0 true)
  nil)

(defn reset-world-lifecycle-handlers-for-test! []
  (aset ^objects current-runtime-cell 0 (create-world-lifecycle-runtime))
  nil)

(defn- entries-snapshot [^ArrayList entries]
  (mapv (fn [^LifecycleEntry entry]
          {:id (.entryId entry) :fn (.entryFn entry)})
        entries))

(defn lifecycle-handlers-snapshot []
  (let [runtime (current-runtime)]
    {:load (entries-snapshot (.loadHandlers runtime))
     :unload (entries-snapshot (.unloadHandlers runtime))
     :save (entries-snapshot (.saveHandlers runtime))
     :tick (entries-snapshot (.tickHandlers runtime))
     :frozen? (aget (.frozenCell runtime) 0)}))

(defn dispatch-world-load [world saved-data]
  (let [^ArrayList handlers (.loadHandlers (current-runtime))
        handler-ids (HashSet.)
        ^Iterator id-it (.iterator handlers)]
    (while (.hasNext id-it)
      (.add handler-ids (.entryId ^LifecycleEntry (.next id-it))))
    (let [by-id? (and (map? saved-data)
                      (boolean (some #(.contains handler-ids %) (keys saved-data))))
          ^Iterator it (.iterator handlers)]
      (while (.hasNext it)
        (let [^LifecycleEntry entry (.next it)
              id (.entryId entry)]
          (try
            ((.entryFn entry) world (if by-id? (get saved-data id) saved-data))
            (catch Throwable t (report-handler-error! :load t)))))))
  nil)

(defn dispatch-world-unload [world]
  (let [^Iterator it (.iterator (.unloadHandlers (current-runtime)))]
    (while (.hasNext it)
      (try
        ((.entryFn ^LifecycleEntry (.next it)) world)
        (catch Throwable t (report-handler-error! :unload t)))))
  nil)

(defn dispatch-world-save [world]
  (let [result (transient {})
        ^Iterator it (.iterator (.saveHandlers (current-runtime)))]
    (while (.hasNext it)
      (let [^LifecycleEntry entry (.next it)]
        (try
          (when-let [data ((.entryFn entry) world)]
            (assoc! result (.entryId entry) data))
          (catch Throwable t (report-handler-error! :save t)))))
    (persistent! result)))

(defn dispatch-world-tick [world]
  (let [^Iterator it (.iterator (.tickHandlers (current-runtime)))]
    (while (.hasNext it)
      (try
        ((.entryFn ^LifecycleEntry (.next it)) world)
        (catch Throwable t (report-handler-error! :tick t)))))
  nil)
