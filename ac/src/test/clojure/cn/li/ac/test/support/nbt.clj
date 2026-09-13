(ns cn.li.ac.test.support.nbt
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.platform.structured-data :as sd]))

(def ^:private compound-marker ::atom-compound)
(def ^:private list-marker ::atom-list)

(defn- nk [k] (if (keyword? k) (name k) (str k)))

(defn- compound? [c] (= compound-marker (:type c)))
(defn- nbt-list-value? [c] (= list-marker (:type c)))
(defn- compound-data [c] (:data c))
(defn- list-data [c] (:data c))

(defn- new-compound [] {:type compound-marker :data (atom {})})
(defn- new-list [] {:type list-marker :data (atom [])})

(defn install-test-structured-data-ops!
  "Install atom-backed NBT ops into the current Framework atom. Ops live in
  the Framework, so tests that swap in a fresh Framework must call this again
  — guard on the live atom, never on a one-shot flag."
  []
  (when (and (fw/fw-atom) (not (sd/available?)))
    (sd/install-structured-data-ops!
      {:sd-set-int! (fn [c k v]
                       (when (compound? c)
                         (swap! (compound-data c) assoc (nk k) (int v)))
                       c)
       :sd-get-int (fn [c k]
                      (when (compound? c)
                        (int (get @(compound-data c) (nk k) 0))))
       :sd-set-string! (fn [c k v]
                          (when (compound? c)
                            (swap! (compound-data c) assoc (nk k) (str v)))
                          c)
       :sd-get-string (fn [c k]
                         (when (compound? c)
                           (str (get @(compound-data c) (nk k) ""))))
       :sd-set-boolean! (fn [c k v]
                           (when (compound? c)
                             (swap! (compound-data c) assoc (nk k) (boolean v)))
                           c)
       :sd-get-boolean (fn [c k]
                          (when (compound? c)
                            (boolean (get @(compound-data c) (nk k) false))))
       :sd-set-double! (fn [c k v]
                          (when (compound? c)
                            (swap! (compound-data c) assoc (nk k) (double v)))
                          c)
       :sd-get-double (fn [c k]
                         (when (compound? c)
                           (double (get @(compound-data c) (nk k) 0.0))))
       :sd-set-float! (fn [c k v]
                         (when (compound? c)
                           (swap! (compound-data c) assoc (nk k) (float v)))
                         c)
       :sd-get-float (fn [c k]
                        (when (compound? c)
                          (float (get @(compound-data c) (nk k) 0.0))))
       :sd-set-long! (fn [c k v]
                        (when (compound? c)
                          (swap! (compound-data c) assoc (nk k) (long v)))
                        c)
       :sd-get-long (fn [c k]
                       (when (compound? c)
                         (long (get @(compound-data c) (nk k) 0))))
       :sd-set-entry! (fn [c k v]
                       (when (compound? c)
                         (swap! (compound-data c) assoc (nk k) v))
                       c)
       :sd-get-entry (fn [c k]
                      (when (compound? c)
                        (get @(compound-data c) (nk k))))
       :sd-get-structured (fn [c k]
                           (when (compound? c)
                             (let [v (get @(compound-data c) (nk k))]
                               (when (compound? v) v))))
       :sd-get-list (fn [c k]
                      (when (compound? c)
                        (let [v (get @(compound-data c) (nk k))]
                          (when (nbt-list-value? v) v))))
       :sd-has-key? (fn [c k]
                       (when (compound? c)
                         (contains? @(compound-data c) (nk k))))
       :sd-append! (fn [lst el]
                      (when (nbt-list-value? lst)
                        (swap! (list-data lst) conj el))
                      lst)
       :sd-list-size (fn [lst]
                        (when (nbt-list-value? lst)
                          (count @(list-data lst))))
       :sd-list-get (fn [lst i]
                       (when (nbt-list-value? lst)
                         (get @(list-data lst) i)))
       :sd-list-get-structured (fn [lst i]
                                (when (nbt-list-value? lst)
                                  (let [v (get @(list-data lst) i)]
                                    (when (compound? v) v))))
       :create-structured new-compound
       :create-list new-list}
      "ac-test")))

(defn atom-compound
  []
  (install-test-structured-data-ops!)
  (new-compound))

(defn atom-list
  []
  (install-test-structured-data-ops!)
  (new-list))
