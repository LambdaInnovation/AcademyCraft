(ns cn.li.ac.ability.rules.resource-rules
  "Pure business logic for resource consumption, recovery, and growth.
  
  These are pure functions—no atoms, no event firing, no side effects.
  Callers (reducer layer) combine these with event generation.
  
  All functions take ResourceData maps and return updated maps (or
  {:data updated :success? bool} tuples for guard checks).
  
  Reference: cn.li.ac.ability.model.resource for the data schema."
  (:require [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.config :as cfg]))

;; ============================================================================
;; Activation Toggle
;; ============================================================================

(defn set-activated
  "Set activation state. Returns {:data updated :events-needed [...]}.
  
  Events needed: [:activate] or [:deactivate] if state changed, else []."
  [res-data v]
  (let [was (rdata/is-activated? res-data)
        now (boolean v)]
    (if (= was now)
      {:data res-data :events-needed []}
      {:data          (rdata/set-activated res-data now)
       :events-needed [(if now :activate :deactivate)]})))

;; ============================================================================
;; Max-value Recalculation (for passive skill modifiers)
;; ============================================================================

(defn recalc-max-for-level
  "Recalculate max-cp/max-overload when level changes.
  
  Returns {:data updated :events-needed [:recalc-max-cp :recalc-max-overload]}.
  
  Note: Event effect layer will fire CALC-MAX-CP and CALC-MAX-OVERLOAD
  to allow passive skills to modify. Reducers should capture new values
  from that event system and re-run this function if modifiers changed."
  [res-data level]
  (let [base (rdata/recalc-max-values res-data level)]
    {:data          (assoc base
                           :cur-cp       (min (:cur-cp base) (:max-cp base))
                           :cur-overload (min (:cur-overload base) (:max-overload base)))
     :events-needed [:recalc-max-cp :recalc-max-overload]}))

;; ============================================================================
;; Consumption Guard
;; ============================================================================

(defn can-perform?
  "Pure guard check: can player perform skill with given overload + cp cost?
  
  Args:
    res-data   – ResourceData map
    overload   – float, desired overload to add
    cp         – float, desired CP to consume
    creative?  – bool, skip CP check if true
  
  Returns: bool"
  [res-data overload cp creative?]
  (rdata/can-perform? res-data overload cp creative?))

;; ============================================================================
;; Consumption (Pure)
;; ============================================================================

(defn perform-resource
  "Attempt to consume (overload, cp).
  
  Returns: {:data updated :success? bool :events-needed [...]}.
  
  Events needed:
    [:cp-consumed]        – if CP was consumed
    [:overload-added]     – if overload was added
    [:overload-cap-hit]   – if overload hit max
    [:growth-cp]          – if max-cp grew
    [:growth-overload]    – if max-overload grew
  
  Args:
    res-data   – ResourceData map
    overload   – float, desired overload to add
    cp         – float, desired CP to consume
    creative?  – bool, skip CP check (creative mode)
    level      – int, player ability level (for growth caps)
    cp-incr-rate – float, multiplier for cp growth
    ol-incr-rate – float, multiplier for overload growth"
  [res-data overload cp creative? level cp-incr-rate ol-incr-rate]
  (if-not (can-perform? res-data overload cp creative?)
    {:data res-data :success? false :events-needed []}
    (let [events-needed (transient [])
          
          ;; Apply CP consumption
          [data1 cp-consumed?] (if (and (pos? cp) (not creative?))
                                 [(rdata/consume-cp res-data cp (cfg/cp-recover-cooldown))
                                  true]
                                 [res-data false])
          _ (when cp-consumed? (conj! events-needed :cp-consumed))
          
          ;; Apply overload
          [data2 ol-added? hit-cap?] (if (pos? overload)
                                       (let [[d hit?] (rdata/add-overload data1 overload
                                                                           (cfg/overload-recover-cooldown))]
                                         [d true hit?])
                                       [data1 false false])
          _ (when ol-added? (conj! events-needed :overload-added))
          _ (when hit-cap? (conj! events-needed :overload-cap-hit))
          
          ;; Growth: add-max-cp grows by consumed CP × rate
          [data3 cp-grew?] (if (and (pos? cp) (not creative?))
                             (let [d (rdata/grow-max-cp data2 cp cp-incr-rate level)]
                               [(assoc d :add-max-cp (:add-max-cp d))
                                (> (:add-max-cp d) (:add-max-cp data2))])
                             [data2 false])
          _ (when cp-grew? (conj! events-needed :growth-cp))
          
          ;; Growth: add-max-overload grows by overload × rate
          [data4 ol-grew?] (if (pos? overload)
                             (let [d (rdata/grow-max-overload data3 overload ol-incr-rate level)]
                               [d (> (:add-max-overload d) (:add-max-overload data3))])
                             [data3 false])
          _ (when ol-grew? (conj! events-needed :growth-overload))]
      
      {:data          data4
       :success?      true
       :events-needed (persistent! events-needed)})))

;; ============================================================================
;; Recovery
;; ============================================================================

(defn server-tick-recovery
  "Single server tick: recover CP and overload.
  
  Returns: {:data updated :events-needed [...]}.
  
  Events needed:
    [:cp-recovered]       – if CP recovery advanced
    [:overload-recovered] – if overload recovery advanced
  
  Args:
    res-data    – ResourceData map
    cp-speed    – float, CP recovery amount per tick
    ol-speed    – float, overload recovery amount per tick"
  [res-data cp-speed ol-speed]
  (let [events-needed (transient [])
        data1 (rdata/tick-cp-recovery res-data cp-speed)
        _ (when (not= (:cur-cp data1) (:cur-cp res-data))
            (conj! events-needed :cp-recovered))
        
        data2 (rdata/tick-overload-recovery data1 ol-speed)
        _ (when (not= (:cur-overload data2) (:cur-overload data1))
            (conj! events-needed :overload-recovered))]
    {:data          data2
     :events-needed (persistent! events-needed)}))

;; ============================================================================
;; Interference Management
;; ============================================================================

(defn add-interference
  "Add an interference source (blocks ability use).
  Returns: {:data updated :added? bool}."
  [res-data src-id]
  (let [before-size (count (:interferences res-data))
        data (rdata/add-interference res-data src-id)
        after-size (count (:interferences data))
        added? (> after-size before-size)]
    {:data added? :added? added?}))

(defn remove-interference
  "Remove an interference source.
  Returns: {:data updated :removed? bool}."
  [res-data src-id]
  (let [before-size (count (:interferences res-data))
        data (rdata/remove-interference res-data src-id)
        after-size (count (:interferences data))
        removed? (< after-size before-size)]
    {:data data :removed? removed?}))

;; ============================================================================
;; Queries (Pure)
;; ============================================================================

(defn is-activated?
  "Check if resource is currently activated (ability usable)."
  [res-data]
  (rdata/is-activated? res-data))

(defn is-in-overload-recovery?
  "Check if currently overload recovering (cannot use ability)."
  [res-data]
  (not (:overload-fine res-data)))

(defn get-cp-percent
  "Get CP as percentage of max-cp."
  [res-data]
  (if (zero? (:max-cp res-data))
    100.0
    (* 100.0 (/ (:cur-cp res-data) (:max-cp res-data)))))

(defn get-overload-percent
  "Get overload as percentage of max-overload."
  [res-data]
  (if (zero? (:max-overload res-data))
    0.0
    (* 100.0 (/ (:cur-overload res-data) (:max-overload res-data)))))

(defn has-interference?
  "Check if any interference is active."
  [res-data]
  (not-empty (:interferences res-data)))
