(ns cn.li.ac.ability.runtime-registry)

(defn- runtime-tag
  [runtime]
  (or (:runtime runtime)
      (some (fn [[k v]]
              (when (and (keyword? k)
                         (= "runtime" (name k)))
                v))
            runtime)))

(defn assert-runtime!
  [runtime expected-runtime-key error-message]
  (when-not (and (map? runtime)
                 (= expected-runtime-key (runtime-tag runtime))
                 (some? (:state* runtime)))
    (throw (ex-info error-message {:runtime runtime})))
  runtime)

(defn state-atom
  [runtime]
  (:state* runtime))

(defn snapshot
  [runtime]
  @(state-atom runtime))

(defn update-state!
  [runtime f & args]
  (apply swap! (state-atom runtime) f args))

(defn reset-state!
  [runtime new-state]
  (reset! (state-atom runtime) new-state))

(defn assert-open!
  [runtime freeze-key error-message]
  (when (get (snapshot runtime) freeze-key)
    (throw (ex-info error-message {}))))

(defn freeze!
  [runtime freeze-key]
  (update-state! runtime assoc freeze-key true)
  nil)
