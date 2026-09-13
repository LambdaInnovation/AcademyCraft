(ns cn.li.ac.ability.server.service.context-transport-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.context-transport :as transport]))

(defn- reset-transport-fixture [f]
  (transport/reset-context-transport-for-test!)
  (try
    (f)
    (finally
      (transport/reset-context-transport-for-test!))))

(use-fixtures :each reset-transport-fixture)

(deftest registered-send-fns-dispatch-test
  (let [client-calls (atom [])
        server-calls (atom [])]
    (transport/register-send-fns!
     {:to-client (fn [player-uuid msg-id payload]
                   (swap! client-calls conj [player-uuid msg-id payload]))
      :to-server (fn [msg-id payload]
                   (swap! server-calls conj [msg-id payload]))})
    (transport/send-to-client! "p1" :msg/a {:x 1})
    (transport/send-to-server! :msg/b {:y 2})
    (is (= [["p1" :msg/a {:x 1}]] @client-calls))
    (is (= [[:msg/b {:y 2}]] @server-calls))
    (is (fn? (:to-client (transport/context-transport-snapshot))))))

(deftest context-transport-freeze-policy-test
  (transport/freeze-context-transport!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Context transport is frozen"
                        (transport/register-send-fns! {:to-client identity
                                                       :to-server identity}))))
