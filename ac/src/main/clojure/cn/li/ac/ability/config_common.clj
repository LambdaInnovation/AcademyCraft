(ns cn.li.ac.ability.config-common)

(defn finite-double
  [value default]
  (try
    (let [d (cond
              (number? value) (double value)
              (string? value) (Double/parseDouble value)
              :else (double default))]
      (if (or (Double/isNaN d) (Double/isInfinite d))
        (double default)
        d))
    (catch Exception _
      (double default))))

(defn list-like?
  [value]
  (and (not (string? value))
       (seqable? value)))

(defn boolean-value
  [value default]
  (cond
    (instance? Boolean value) value
    (string? value) (Boolean/parseBoolean value)
    :else (boolean default)))
