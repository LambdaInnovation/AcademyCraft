(ns cn.li.neoforge262.gui.menu-proxy-quick-move-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcbase.gui.menu.proxy :as menu-proxy]
            [cn.li.platform.neutral.tabbed-gui :as tabbed]))

(deftest quick-move-allowed-policy-test
  (testing "non-tabbed containers always allow quick-move"
    (with-redefs [tabbed/tabbed-container? (fn [_] false)
                  tabbed/slots-active-for-menu? (fn [_ _] false)]
      (is (true? (menu-proxy/quick-move-allowed? :menu :container)))))

  (testing "tabbed containers require active inventory slots"
    (with-redefs [tabbed/tabbed-container? (fn [_] true)
                  tabbed/slots-active-for-menu? (fn [_ _] true)]
      (is (true? (menu-proxy/quick-move-allowed? :menu :container))))
    (with-redefs [tabbed/tabbed-container? (fn [_] true)
                  tabbed/slots-active-for-menu? (fn [_ _] false)]
      (is (false? (menu-proxy/quick-move-allowed? :menu :container))))))
