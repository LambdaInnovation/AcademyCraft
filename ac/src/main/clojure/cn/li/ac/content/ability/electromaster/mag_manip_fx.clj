(ns cn.li.ac.content.ability.electromaster.mag-manip-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]))

(def ^:private mag-manip-effect-id :mag-manip)

(defn- on-fx-hold [ctx-id channel payload]
  (when-let [mode (:mode payload)]
    (hand-effects/enqueue-hand-effect! mag-manip-effect-id ctx-id channel
      (assoc (or payload {}) :mode mode))))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :mag-manip
     :runtime :hand
     :initial-state (fn [] {:states {}})
     :channels {:hold {:topic :mag-manip/fx-hold :targets [:hand] :handler on-fx-hold}
                :throw {:topic :mag-manip/fx-throw :mode :throw :targets [:hand]}
                :end {:topic :mag-manip/fx-end :mode :end :targets [:hand]}}}))

(arc-beam/def-arc-beam-fx :mag-manip)

(def ^:private default-state
  {:active? false :focus nil :block-id nil :ticks 0})

(defn current-state [selector]
  (let [{:keys [states]} (fx-snapshot)]
    (or (get states selector) default-state)))
