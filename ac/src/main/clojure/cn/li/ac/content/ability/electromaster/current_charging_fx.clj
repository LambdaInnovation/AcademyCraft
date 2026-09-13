(ns cn.li.ac.content.ability.electromaster.current-charging-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.skill-config :as skill-config]))

(declare fx-snapshot)

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :current-charging
     :runtime :level
     :level-initial-state (fn [] {:states {}})
     :channels {:start {:topic :current-charging/fx-start :mode :start :targets [:level]}
                :update {:topic :current-charging/fx-update :mode :update :targets [:level]}
                :end {:topic :current-charging/fx-end :mode :end :targets [:level]}}}))

(arc-beam/def-arc-beam-fx :current-charging)

(defn- visual-max-ticks []
  (max 1 (int (or (skill-config/tunable-int :current-charging :charge.visual-max-ticks) 40))))

(def ^:private default-state
  {:active? false :blending? false :is-item false :good? false
   :charge-ticks 0 :charge-ratio 0.0 :target nil :block-pos nil
   :charged 0.0 :started-at-ms 0 :ending-at-ms 0})

(defn- current-store []
  (let [store (fx-snapshot)]
    (cond
      (contains? store :states)
      store

      (and (map? store) (or (:hand store) (:level store)))
      {:states (merge (get-in store [:level :states] {})
                      (get-in store [:hand :states] {}))}

      :else
      (arc-beam/initial-state :current-charging))))

(defn- live-state?
  [st]
  ;; The active state is owned by the skill context and ends explicitly with
  ;; :current-charging/fx-end, matching upstream's held EntityArc lifetime.
  (boolean (:active? st)))

(defn- state-for-selector [store selector]
  (let [states (:states store)]
    (or (cond
          (vector? selector)
          (let [st (get states selector)]
            (when (live-state? st) st))
          (some? selector)
          (some (fn [[_ st]]
                  (when (and (:source-player-id st)
                             (= (str selector) (str (:source-player-id st)))
                             (live-state? st))
                    st))
                states)
          :else
          (or (some (fn [[_ st]] (when (:active? st) st)) states)
              (some (fn [[_ st]] (when (live-state? st) st)) states)))
        default-state)))

(defn current-state [selector]
  (state-for-selector (current-store) selector))
