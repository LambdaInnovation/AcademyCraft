(ns cn.li.ac.ability.client.fx-spec
  "Declarative client-side ability FX registration.

  Skills register level/hand runtimes and channel handlers through `register!`
  instead of hand-written `fx-registry` case blocks."
  (:require [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.level-effects :as level-effects]))

(def ^:private meta-keys
  [:effect-instance-id :source-player-id :world-id])

(defn default-owner-key
  ([ctx-id]
   (default-owner-key ctx-id nil))
  ([ctx-id payload]
   (cond
     (and (map? payload) (:effect-instance-id payload))
     [:effect-instance (:effect-instance-id payload)]

     ctx-id
     [:ctx ctx-id]

     (and (map? payload) (:source-player-id payload))
     [:source-player (:source-player-id payload)]

     :else
     [:ctx ctx-id])))

(defn select-meta
  [payload]
  (select-keys (or payload {}) meta-keys))

(defn- merge-payload
  [mode payload* custom level-extra hand-extra]
  ;; :mode is merged LAST so the channel-spec mode stays authoritative — a
  ;; server payload carrying its own :mode (e.g. electron-bomb's
  ;; {:mode :perform} settle payload) must not override the channel's
  ;; :spawn/:beam/:end routing.
  (merge (select-meta payload*)
         custom
         payload*
         level-extra
         hand-extra
         (when mode {:mode mode})))

(defn- normalize-targets [targets]
  (vec (or targets [:level])))

(defn- register-runtime!
  [effect-id {:keys [level hand]}]
  (when level
    (level-effects/register-level-effect!
      effect-id
      (select-keys level [:initial-state :enqueue-state-fn :tick-state-fn :build-plan-fn :empty-state? :fov-offset-fn :clear-owner-fn])))
  (when hand
    (hand-effects/register-hand-effect!
      effect-id
      (select-keys hand [:initial-state :enqueue-state-fn :tick-state-fn :transform-fn])))
  nil)

(defn- channel-handler
  [effect-id channel-spec]
  (let [{:keys [mode targets payload handler level-payload hand-payload immediate-fn]}
        channel-spec
        targets* (normalize-targets targets)]
    (fn [ctx-id channel payload]
      (if handler
        (handler ctx-id channel payload)
        (let [payload* (or payload {})
              mode* (or mode (:mode payload*))
              owner-key (default-owner-key ctx-id payload*)
              custom (when (fn? payload) (payload ctx-id channel payload*))
              level-extra (when (fn? level-payload) (level-payload ctx-id channel payload*))
              hand-extra (when (fn? hand-payload) (hand-payload ctx-id channel payload*))
              merged (merge-payload mode* payload* custom level-extra hand-extra)
              hand-merged (merge (select-meta payload*)
                                 (when mode* {:mode mode*})
                                 custom
                                 payload*
                                 hand-extra)]
          (doseq [target targets*]
            (case target
              :immediate (when immediate-fn (immediate-fn ctx-id channel payload*))
              :level (level-effects/enqueue-level-effect!
                       effect-id ctx-id channel merged :owner-key owner-key)
              :hand (hand-effects/enqueue-hand-effect!
                      effect-id ctx-id channel
                      (dissoc hand-merged :affected-blocks :broken-blocks)
                      :owner-key owner-key)
              nil)))))))

(defn register!
  "Register one ability FX spec.

  `spec` keys:
    :id       effect keyword
    :level    optional level runtime map
    :hand     optional hand runtime map
    :channels map of channel-key -> {:topic kw :mode kw :targets [...] ...}"
  [{:keys [id level hand channels]}]
  (when-not (and (keyword? id) (map? channels))
    (throw (IllegalArgumentException. "register-fx-spec!: id must be keyword, channels must be map")))
  (register-runtime! id {:level level :hand hand})
  (doseq [[_channel-key channel-spec] channels]
    (when-let [topic (:topic channel-spec)]
      (fx-registry/register-fx-channel!
        topic
        (channel-handler id channel-spec))))
  nil)

;; ---------------------------------------------------------------------------
;; Owner-state helpers (default shape {:states {owner-key state}})
;; ---------------------------------------------------------------------------

(defn states-store
  [initial-state]
  {:states (or (:states initial-state) {})})

(defn state-values
  [store]
  (vals (:states (or store {}))))

(defn get-owner-state
  [store owner-key default-state]
  (get-in (or store {}) [:states owner-key] default-state))

(defn update-owner-state
  [store owner-key f & args]
  (update (or store {}) :states
          (fn [states]
            (let [current (get states owner-key)
                  next-state (apply f current args)]
              (if (some? next-state)
                (assoc states owner-key next-state)
                (dissoc states owner-key))))))

(defn clear-owner-in-store
  [store owner-key]
  (update (or store {}) :states dissoc owner-key))

(defn matching-active-owner
  "Find first owner state matching `pred`, optionally filtered by source-player-id."
  [store pred hand-center-pos]
  (some (fn [st]
          (when (and (pred st)
                     (or (nil? (:source-player-id st))
                         (nil? (:player-uuid hand-center-pos))
                         (= (str (:source-player-id st))
                            (str (:player-uuid hand-center-pos)))))
            st))
        (state-values store)))
