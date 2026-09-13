(ns cn.li.mcmod.platform.be
  "Block entity operations via Framework function map.

   BE ops stored at [:platform :be-ops] as {:be-get-level fn, :be-get-world fn, ...}."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.platform.world :as world]))

(def be-ops-keys
  #{:be-get-level :be-get-world :be-get-custom-state :be-set-custom-state!
    :be-get-block-id :be-get-tile-id :be-set-changed! :be-get-fluid-height
    :be-sync-to-client!})

(defn install-be-ops!
  [ops-map _label]
  (if-let [fw-atom (fw/fw-atom)]
    (let [missing (seq (remove (set (keys ops-map)) be-ops-keys))]
      (swap! fw-atom assoc-in [:platform :be-ops] ops-map)
      (log/debug "BeOps installed:" (pr-str (keys ops-map)))
      (when missing
        (log/error "BeOps MISSING required keys:" (pr-str missing))))
    (log/error "BeOps install FAILED: Framework atom nil")))

(defn available? [] (boolean (get-in @(fw/fw-atom) [:platform :be-ops])))

(defn- call-log [level & xs]
  (case level
    :error (apply log/error xs)
    :warn  (apply log/warn xs)
    :info  (apply log/info xs)
    nil))

(defn- be-op [k & args]
  (when-let [f (get-in @(fw/fw-atom) [:platform :be-ops k])]
    (apply f args)))

(defn be-get-level [be] (be-op :be-get-level be))

(defn be-get-world-safe [be]
  (let [level (try (be-op :be-get-level be) (catch Exception e (log/stacktrace "be-get-level failed" e) nil))
        world (try (be-op :be-get-world be) (catch Exception e (log/stacktrace "be-get-world failed" e) nil))
        result (or level world)]
    (when (and be (nil? result))
      (log/warn "be-get-world-safe returned nil — be-op :be-get-level =" (some? level) ":be-get-world =" (some? world)))
    result))

(defn get-block-entity [w block-pos]
  (try
    (world/get-tile-entity w block-pos)
    (catch Exception e
      (call-log :warn "get-block-entity failed:" (ex-message e))
      (log/stacktrace "get-block-entity failed" e)
      nil)))

(defn get-custom-state [be]
  (when be
    (try (be-op :be-get-custom-state be)
         (catch Exception e
           (call-log :warn "get-custom-state failed:" (ex-message e))
           (log/stacktrace "get-custom-state failed" e) nil))))

(defn set-custom-state! [be state]
  (when be
    (try
      (be-op :be-set-custom-state! be state)
      (catch Exception e
        (call-log :error "set-custom-state! failed:" (ex-message e))
        (log/stacktrace "set-custom-state! failed" e)))))

(defn sync-to-client! [be]
  (when be
    (try (be-op :be-sync-to-client! be)
         (catch Exception e
           (call-log :warn "sync-to-client! failed:" (ex-message e))
           (log/stacktrace "sync-to-client! failed" e)))))

(defn get-block-id [be]
  (when be
    (try (be-op :be-get-block-id be)
         (catch Exception e
           (call-log :warn "be-get-block-id failed:" (ex-message e))
           (log/stacktrace "be-get-block-id failed" e) nil))))

(defn get-tile-id [be]
  (when be
    (try (be-op :be-get-tile-id be)
         (catch Exception e
           (call-log :warn "get-tile-id failed:" (ex-message e))
           (log/stacktrace "get-tile-id failed" e) nil))))

(defn set-changed! [be]
  (when be
    (try (be-op :be-set-changed! be)
         (catch Exception e
           (call-log :warn "set-changed! failed:" (ex-message e))
           (log/stacktrace "set-changed! failed" e) nil))))

(defn get-fluid-height [be]
  (when be
    (try (or (be-op :be-get-fluid-height be) 0.0)
         (catch Exception e (log/stacktrace "be-get-fluid-height failed" e) 0.0))))
