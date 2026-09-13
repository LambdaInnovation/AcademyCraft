(ns cn.li.neoforge262.gui.screen-impl-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcbase.gui.screen.impl :as screen-impl]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.platform.neutral.gui-runtime :as gui-runtime]))

(deftest resolve-image-size-test
  (testing "nil when no size override"
    (is (nil? (screen-impl/resolve-image-size {}))))
  (testing "delta adds to vanilla defaults (TechUI)"
    (is (= [207 186]
           (screen-impl/resolve-image-size {:size-dx 31 :size-dy 20}))))
  (testing "absolute image size takes precedence (developer centering)"
    (is (= [0 0]
           (screen-impl/resolve-image-size {:image-width 0 :image-height 0}))))
  (testing "partial absolute falls back missing axis to default"
    (is (= [0 166]
           (screen-impl/resolve-image-size {:image-width 0})))))

(deftest with-screen-client-owner-binds-runtime-owner-test
  (let [menu (Object.)
        container {:owner {:logical-side :client
                            :client-session-id :session-a
                            :player-uuid "player-a"}}
        captured (atom nil)]
    (with-redefs [gui-runtime/get-container-for-menu (fn [_] container)
                  gui-runtime/owner-from-container :owner]
      (screen-impl/with-screen-client-owner menu
        #(reset! captured {:client-session-id (runtime-hooks/client-session-id)
                           :owner (runtime-hooks/player-state-owner)})))
    (is (= :session-a (:client-session-id @captured)))
    (is (= "player-a" (:player-uuid (:owner @captured))))))

(deftest owner-for-screen-menu-requires-client-owner-test
  (let [menu (Object.)]
    (with-redefs [gui-runtime/get-container-for-menu
                  (fn [_] {:owner {:server-session-id [:server 1]
                                   :player-uuid "player-a"
                                   :logical-side :server}})
                  gui-runtime/owner-from-container :owner]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"client-owner contract violation"
                            (screen-impl/owner-for-screen-menu menu))))))
