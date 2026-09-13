(ns cn.li.mcmod.gui.container.sync-routing-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.gui.container.sync-routing :as sync-routing]
            [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.platform.entity :as entity]))

(deftest require-open-container-validates-container-id-test
  (testing "missing container-id fails contract"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"sync-routing contract violation"
                          (sync-routing/require-open-container! {} :player))))

  (testing "resolved container returned when menu matches"
    (let [container {:container-type :test :tile-entity :tile}
          menu (reify Object
                 (toString [_] "menu"))]
      (with-redefs [entity/player-get-container-menu (fn [_] menu)
                    container-state/get-menu-container-id (fn [_] 9)
                    container-state/get-container-for-menu (fn [_] container)]
        (is (= container
               (sync-routing/require-open-container! {:container-id 9} :player))))))

  (testing "container-id mismatch fails"
    (with-redefs [entity/player-get-container-menu (fn [_] (reify Object))
                  container-state/get-menu-container-id (fn [_] 1)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Container id mismatch"
                            (sync-routing/require-open-container! {:container-id 2} :player))))))
