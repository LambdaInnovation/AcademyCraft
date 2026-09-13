(ns cn.li.mcmod.schema.core
  "Pure-Clojure schema validation — zero external dependencies.

  clojure.spec.alpha was evaluated but rejected: s/or, s/and, s/coll-of, s/map-of
  are all macros — they cannot be applied dynamically at runtime to convert
  Malli-style schema vectors.  Hand-rolled predicates are the only AOT-safe path.

  Keep this namespace platform-neutral. Callers should compile validators once
  near immutable schemas and call `explain` only on failure."
  (:require [clojure.string :as str]))

;; ============================================================================
;; Internal helpers
;; ============================================================================

(defn- ^:private resolve-sym
  [x resolve-ns]
  (if (symbol? x)
    (let [v (ns-resolve resolve-ns x)]
      (if v @v (throw (IllegalArgumentException.
                       (str "schema.core: unresolved symbol " x " in " (ns-name resolve-ns))))))
    x))

(defn- ^:private schema-label
  "Human-readable label for a schema, used in explain output."
  [schema]
  (cond
    (fn? schema) (or (:name (meta schema)) 'fn?)
    (keyword? schema) schema
    (vector? schema) (first schema)
    :else (pr-str schema)))

;; ============================================================================
;; Core compiler — every validator returns true or {:path .. :expected .. :actual ..}
;; ============================================================================

(declare compile-schema*)

(defn- ^:private compile-or
  [args resolve-ns]
  (let [subs (mapv #(compile-schema* % resolve-ns) args)]
    (fn
      ([v]
       (boolean (some #(true? (% v [])) subs)))
      ([v path]
       (loop [[sub & rest-subs] subs
              last-failure nil]
         (if (nil? sub)
           (or last-failure
               {:path (vec path) :expected (mapv schema-label subs) :actual v})
           (let [r (sub v (conj (vec path) (schema-label sub)))]
             (if (true? r) r
                 (recur rest-subs (or last-failure r))))))))))

(defn- ^:private compile-and
  [args resolve-ns]
  (let [subs (mapv #(compile-schema* % resolve-ns) args)]
    (fn
      ([v]
       (boolean (every? #(true? (% v [])) subs)))
      ([v path]
       (reduce (fn [_ sub]
                 (let [r (sub v path)]
                   (if (true? r) true (reduced r))))
               true
               subs)))))

(defn- ^:private compile-enum
  [args]
  (let [allowed (set args)]
    (fn
      ([v] (contains? allowed v))
      ([v path] (if (contains? allowed v) true
                  {:path (vec path) :expected (vec args) :actual v})))))

(defn- ^:private compile-map-entries
  [entries resolve-ns]
  (let [compiled (mapv (fn [entry]
                         (let [k (first entry)
                               tail (rest entry)
                               [opts spec] (if (and (map? (first tail))
                                                    (contains? (first tail) :optional))
                                             [(first tail) (second tail)]
                                             [nil (first tail)])
                               optional? (boolean (:optional opts))
                               pred (compile-schema* spec resolve-ns)]
                           [k optional? pred]))
                       entries)]
    (fn
      ([v]
       (and (map? v)
            (every? (fn [[k optional? pred]]
                      (if (contains? v k)
                        (pred (get v k))
                        optional?))
                    compiled)))
      ([v path]
       (if-not (map? v)
         {:path (vec path) :expected 'map? :actual (type v)}
         (loop [[[k optional? pred] & rest] compiled]
           (if (nil? k)
             true
             (let [val (get v k ::absent)]
               (if (= val ::absent)
                 (if optional?
                   (recur rest)
                   {:path (conj (vec path) k) :expected 'present :actual 'absent})
                 (let [r (pred val (conj (vec path) k))]
                   (if (true? r) (recur rest) r)))))))))))

(defn- ^:private compile-map-of
  [[k-spec v-spec] resolve-ns]
  (let [k-pred (compile-schema* k-spec resolve-ns)
        v-pred (compile-schema* v-spec resolve-ns)]
    (fn
      ([v]
       (and (map? v)
            (reduce-kv (fn [_ k val]
                         (and (true? (k-pred k))
                              (true? (v-pred val))))
                       true v)))
      ([v path]
       (if-not (map? v)
         {:path (vec path) :expected 'map? :actual (type v)}
         (reduce-kv (fn [_ k val]
                      (let [kr (k-pred k (conj (vec path) :key k))]
                        (if-not (true? kr) (reduced kr)
                            (let [vr (v-pred val (conj (vec path) :val k))]
                              (if (true? vr) true (reduced vr))))))
                    true
                    v))))))

(defn- ^:private compile-coll-of
  [kind item-pred]
  (fn
    ([v]
     (and (kind v) (boolean (every? #(true? (item-pred % [])) v))))
    ([v path]
     (cond
       (not (kind v))
       {:path (vec path) :expected (pr-str kind) :actual (type v)}
       :else
       (reduce-kv (fn [_ idx item]
                    (let [r (item-pred item (conj (vec path) idx))]
                      (if (true? r) true (reduced r))))
                  true
                  (vec v))))))

(defn- ^:private compile-schema*
  [schema resolve-ns]
  (cond
    ;; predicate function
    (fn? schema)
    (fn
      ([v] (boolean (schema v)))
      ([v path] (if (schema v) true
                  {:path (vec path) :expected (schema-label schema) :actual v})))

    ;; symbol → resolve
    (symbol? schema)
    (compile-schema* (resolve-sym schema resolve-ns) resolve-ns)

    ;; var → deref
    (var? schema)
    (compile-schema* @schema resolve-ns)

    ;; bare keyword → enum singleton
    (keyword? schema)
    (fn
      ([v] (= schema v))
      ([v path] (if (= schema v) true
                  {:path (vec path) :expected schema :actual v})))

    ;; structured schemas
    (vector? schema)
    (let [[tag & args] schema]
      (case tag
        :or   (compile-or args resolve-ns)
        :and  (compile-and args resolve-ns)
        :enum (compile-enum args)

        :map  (compile-map-entries args resolve-ns)

        :map-of
        (compile-map-of args resolve-ns)

        :set
        (compile-coll-of set? (compile-schema* (first args) resolve-ns))

        :vector
        (compile-coll-of vector? (compile-schema* (first args) resolve-ns))

        :sequential
        (compile-coll-of sequential? (compile-schema* (first args) resolve-ns))

        :re
        (let [raw (first args)
              pat (resolve-sym raw resolve-ns)]
          (fn
            ([v] (and (string? v) (boolean (re-matches pat v))))
            ([v path]
             (cond (not (string? v))
                   {:path (vec path) :expected 'string? :actual (type v)}
                   (not (re-matches pat v))
                   {:path (vec path) :expected ['re pat] :actual v}
                   :else true))))

        :>=
        (let [n (resolve-sym (first args) resolve-ns)]
          (fn
            ([v] (and (number? v) (>= v n)))
            ([v path]
             (cond (not (number? v))
                   {:path (vec path) :expected 'number? :actual (type v)}
                   (< v n)
                   {:path (vec path) :expected ['>= n] :actual v}
                   :else true))))

        :<=
        (let [n (resolve-sym (first args) resolve-ns)]
          (fn ([v] (and (number? v) (<= v n)))
              ([v path] (cond (not (number? v))
                             {:path (vec path) :expected 'number? :actual (type v)}
                             (> v n)
                             {:path (vec path) :expected ['<= n] :actual v}
                             :else true))))

        :>
        (let [n (resolve-sym (first args) resolve-ns)]
          (fn ([v] (and (number? v) (> v n)))
              ([v path] (cond (not (number? v))
                             {:path (vec path) :expected 'number? :actual (type v)}
                             (<= v n)
                             {:path (vec path) :expected ['> n] :actual v}
                             :else true))))

        :<
        (let [n (resolve-sym (first args) resolve-ns)]
          (fn ([v] (and (number? v) (< v n)))
              ([v path] (cond (not (number? v))
                             {:path (vec path) :expected 'number? :actual (type v)}
                             (>= v n)
                             {:path (vec path) :expected ['< n] :actual v}
                             :else true))))

        :=
        (let [expected (resolve-sym (first args) resolve-ns)]
          (fn ([v] (= expected v))
              ([v path] (if (= expected v) true
                          {:path (vec path) :expected expected :actual v}))))

        :fn
        (let [f (resolve-sym (first args) resolve-ns)]
          (if (fn? f)
            (fn ([v] (boolean (f v)))
                ([v path] (if (f v) true
                            {:path (vec path) :expected (pr-str f) :actual v})))
            (throw (IllegalArgumentException.
                    (str "schema.core: :fn requires a function, got " f)))))

        ;; unknown tag
        (throw (IllegalArgumentException.
                (str "schema.core: unknown schema tag " tag)))))

    ;; bare map predicate
    (= schema map?)
    (fn ([v] (map? v))
        ([v path] (if (map? v) true
                    {:path (vec path) :expected 'map? :actual (type v)})))

    ;; identity
    :else
    (fn ([_] true) ([_ _path] true))))

;; ============================================================================
;; Public API
;; ============================================================================

(defn validator
  "Compile a schema into a reusable spec object.  The returned object is a
  function with two arities:
    (spec value)        → boolean
    (spec value [path]) → true or {:path .. :expected .. :actual ..}

  Supports :or, :and, :enum, :=, :>=, :<=, :>, :<,
  :map, :map-of, :set, :vector, :sequential, :re, :fn,
  predicate functions, and nested schema references via symbols or vars."
  ([schema]
   (compile-schema* schema *ns*))
  ([schema resolve-ns]
   (compile-schema* schema resolve-ns)))

(defn valid?
  "Return true when compiled validator accepts value."
  [compiled-validator value]
  (boolean (true? (compiled-validator value))))

(defn explain
  "Return structured explanation data for a failed value, or nil if valid.
  Returns {:path [...], :expected ..., :actual ...} on failure."
  [schema value]
  (let [spec (validator schema)]
    (let [result (spec value [])]
      (when (not (true? result))
        result))))

(defn contract-ex-info
  [contract value explain-data]
  (ex-info (str contract " contract violation")
           (cond-> {:contract contract :value value}
             explain-data (assoc :explain explain-data))))

(defn require-valid
  "Validate value with a compiled validator; throw standardized ex-info on failure."
  [schema compiled-spec contract value]
  (if (valid? compiled-spec value)
    value
    (throw (contract-ex-info contract value (explain schema value)))))

;; ============================================================================
;; Lazy validator — AOT-safe deferred compilation
;; ============================================================================

(defn lazy-validator
  "Returns a 0-arg fn that compiles `schema` on first call and caches the result.
  Captures `*ns*` for correct symbol resolution.  Uses `atom` for caching — never
  `delay` (macro, leaks into downstream eval contexts under AOT)."
  [schema]
  (let [cache (atom nil)
        resolve-ns *ns*]
    (fn []
      (if-let [c @cache]
        c
        (let [v (validator schema resolve-ns)]
          (reset! cache v)
          v)))))
