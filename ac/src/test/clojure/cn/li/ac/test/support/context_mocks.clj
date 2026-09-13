(ns cn.li.ac.test.support.context-mocks
  "In-memory mocks for context reads and reducer-backed skill-state writes in unit tests."
  (:require [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]))

(defn make-context-mocks
  "Returns a map of mock fns. Bind with `bind-context-mocks!`."
  [initial-ctx]
  (let [ctx* (atom initial-ctx)
        terminate-calls* (atom [])
        get-context (fn [& _] @ctx*)
        assoc-skill-state! (fn [_ctx-id k v]
                             (swap! ctx* update :skill-state
                                    (fn [ss]
                                      (let [key-path (if (vector? k) k [k])]
                                        (assoc-in (or ss {}) key-path v)))))
        update-skill-state-root! (fn [_ctx-id f & args]
                                   (swap! ctx* update :skill-state
                                          (fn [ss] (apply f (or ss {}) args))))
        clear-skill-state! (fn [_ctx-id]
                             (swap! ctx* assoc :skill-state {}))
        terminate-context! (fn [ctx-id & _]
                             (swap! terminate-calls* conj ctx-id))]
    {:ctx* ctx*
     :terminate-calls* terminate-calls*
     :get-context get-context
     :assoc-skill-state! assoc-skill-state!
     :update-skill-state-root! update-skill-state-root!
     :clear-skill-state! clear-skill-state!
     :terminate-context! terminate-context!}))

(defn bind-context-mocks!
  "Macro-free helper: pass mocks from `make-context-mocks` and a thunk."
  [{:keys [get-context assoc-skill-state! update-skill-state-root! clear-skill-state!
           terminate-context!]} f]
  (with-redefs [ctx/get-context get-context
                ctx/terminate-context! terminate-context!
                ctx-skill/assoc-skill-state! assoc-skill-state!
                ctx-skill/update-skill-state-root! update-skill-state-root!
                ctx-skill/clear-skill-state! clear-skill-state!]
    (f)))
