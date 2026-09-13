(ns cn.li.ac.ability.application.contracts
  "Runtime contracts for ability commands/events/effects.

  Use assert-* at shell boundaries to fail fast on malformed payloads."
  )

(defn command?
  [value]
  (and (map? value)
       (keyword? (:command value))))

(defn event?
  [value]
  (and (map? value)
       (keyword? (:event/type value))))

(defn effect?
  [value]
  (and (map? value)
       (keyword? (:effect/type value))))

(defn reducer-result?
  [value]
  (and (map? value)
       (contains? value :state)
  (vector? (:events value))
  (vector? (:effects value))
  (every? event? (:events value))
  (every? effect? (:effects value))))

(defn- describe
  [label value]
  (str label " invalid: " (pr-str value)))

(defn assert-command!
  [command]
  (when-not (command? command)
    (throw (ex-info (describe "command" command)
                    {:type ::invalid-command
                     :command command})))
  command)

(defn assert-reducer-result!
  [result]
  (when-not (reducer-result? result)
    (throw (ex-info (describe "reducer result" result)
                    {:type ::invalid-reducer-result
                     :result result})))
  result)

(defn trim-command-meta
  "Drop optional debug-only command metadata keys.
  Keep this helper pure so callers can normalize before reducer dispatch."
  [command]
  (apply dissoc command [:trace-id :trace/span :debug/source]))
