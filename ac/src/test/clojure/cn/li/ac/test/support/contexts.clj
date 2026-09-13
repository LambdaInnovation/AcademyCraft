(ns cn.li.ac.test.support.contexts
  (:require [cn.li.ac.ability.service.context-dispatcher :as ctx]))

(defn clean-contexts-fixture
  [f]
  (ctx/call-with-dispatcher-runtime
    (ctx/create-dispatcher-runtime)
    (fn []
      (ctx/reset-contexts-for-test!)
      (ctx/reset-route-fns-for-test!)
      (try
        (f)
        (finally
          (ctx/reset-contexts-for-test!)
          (ctx/reset-route-fns-for-test!))))))
