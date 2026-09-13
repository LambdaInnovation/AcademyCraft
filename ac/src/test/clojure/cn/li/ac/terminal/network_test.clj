(ns cn.li.ac.terminal.network-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.terminal.messages :as terminal-messages]
            [cn.li.ac.terminal.network :as network]
            [cn.li.mcmod.gui.registry-contract :as registry-contract]
            [cn.li.mcmod.network.server :as net-server]))

(defn- reset-handlers-fixture [f]
  (net-server/reset-handlers-for-test!)
  (try
    (f)
    (finally
      (net-server/reset-handlers-for-test!))))

(use-fixtures :each reset-handlers-fixture)

(deftest terminal-handlers-use-none-payload-routing-test
  ;; Regression: default GUI contract is :sync-routing and requires
  ;; :container-id. Terminal overlay RPCs send {} (e.g. Left-Alt get-state).
  (network/register-handlers!)
  (let [handlers (:handlers (net-server/handlers-snapshot))
        get-state-id (terminal-messages/msg-id :get-state)
        entry (get handlers get-state-id)
        contract (registry-contract/registered-handler-contract entry)
        responded (atom nil)]
    (is (= :none (:payload-routing contract)))
    (net-server/handle-request get-state-id 1 {} :fake-player
                               (fn [request-id response]
                                 (reset! responded [request-id response])))
    (is (= 1 (first @responded)))
    (is (map? (second @responded)))
    (is (not= "sync-routing contract violation"
              (:error (second @responded))))))
