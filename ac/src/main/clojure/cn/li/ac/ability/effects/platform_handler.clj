(ns cn.li.ac.ability.effects.platform-handler
  "Effect handler for :platform-call effects.

  Delegates to the platform-hooks registry.

  Effect shape:
    {:effect/type  :platform-call
     :fn-ref       keyword
     :args         [& args]}"
  (:require [cn.li.ac.ability.service.platform-hooks :as hooks]
            [cn.li.mcmod.util.log :as log]))

(defn execute-platform-call!
  [{:keys [fn-ref args]}]
  (if (hooks/platform-fn-registered? fn-ref)
    (apply (hooks/get-platform-fn fn-ref) args)
    (do
      (log/warn "platform-call effect: fn-ref not registered" fn-ref)
      nil)))