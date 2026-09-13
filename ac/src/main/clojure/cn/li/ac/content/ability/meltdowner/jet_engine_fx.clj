(ns cn.li.ac.content.ability.meltdowner.jet-engine-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :jet-engine
     :initial-state (fn [] {:fx-state {}})
     :channels {:start {:topic :jet-engine/fx-start}
                :update {:topic :jet-engine/fx-update}
                :end {:topic :jet-engine/fx-end}
                :trigger-start {:topic :jet-engine/fx-trigger-start}
                :trigger-update {:topic :jet-engine/fx-trigger-update}
                :trigger-end {:topic :jet-engine/fx-trigger-end}}}))

(arc-beam/def-arc-beam-fx :jet-engine)

;; Mirrors impl/jet_engine.clj's private trigger-ttl — the trigger phase's
;; fixed lifetime in ticks, used to normalize :ttl into a fade-out ratio.
(def ^:private trigger-ttl 20)

(defn flash-alpha
  "Screen-flash intensity (0-255, capped at 85) for `player-uuid`'s
  currently-triggering jet-engine phase, if any; 0 when idle or triggering
  for a different player. This used to be computed inline in build-plan as
  an inert {:type :screen-flash} op — the world-space :ops renderer only
  understands :kind (:line/:quad/:plasma-body), so it was silently dropped
  every frame. The real consumer is the 2D screen overlay
  (reactive-hud/build-snapshot -> reactive-overlay's :skill-flash-screen),
  which has no notion of :ops at all."
  [player-uuid]
  (let [states (vals (:fx-state (fx-snapshot)))
        matching (filter (fn [st]
                            (and (= :triggering (:phase st))
                                 (or (nil? player-uuid)
                                     (nil? (:source-player-id st))
                                     (= (str player-uuid) (str (:source-player-id st))))))
                          states)
        raw (->> matching
                 (map #(long (or (:ttl %) 0)))
                 (filter pos?)
                 (map #(int (* 220 (/ (double %) (double trigger-ttl)))))
                 (reduce max 0))]
    (min 85 raw)))