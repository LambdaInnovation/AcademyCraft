(ns cn.li.mcmod.gui.tabbed-gui-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.gui.tabbed-gui :as tabbed]))

(use-fixtures
  :each
  (fn [f]
    (try
      (f)
      (finally
        (container-state/clear-all!)))))

(deftest slots-active-uses-container-tab-index-test
  (let [container {:tab-index (atom 0)}]
    (is (true? (tabbed/slots-active? container)))
    (reset! (:tab-index container) 1)
    (is (false? (tabbed/slots-active? container)))))

(deftest attach-tab-sync-skips-network-send-when-owner-and-runtime-session-missing-test
  (let [pages [{:id "inv"} {:id "wireless"}]
        tech-ui {:current (atom "inv")}
        container {:tab-index (atom 0)}
        sent-calls (atom [])]
    (runtime-hooks/with-client-ctx-fn {:session-id nil} (fn [] (with-redefs [tabbed/send-set-tab! (fn [& args]
                                           (swap! sent-calls conj args))]
        (tabbed/attach-tab-sync! pages tech-ui container 17)
        (reset! (:current tech-ui) "wireless"))))
    (is (empty? @sent-calls))))

(deftest attach-tab-sync-sends-scoped-owner-when-available-test
  (let [owner {:client-session-id :session-a :player-uuid "player-a"}
        pages [{:id "inv"} {:id "wireless"}]
        tech-ui {:current (atom "inv")}
        container {:tab-index (atom 0)
                   :owner owner}
        sent-calls (atom [])]
    (with-redefs [tabbed/send-set-tab! (fn [& args]
                                         (swap! sent-calls conj args))]
      (tabbed/attach-tab-sync! pages tech-ui container 7)
      (reset! (:current tech-ui) "wireless"))
    (is (= [[owner 0 7]
            [owner 1 7]]
           @sent-calls))))
