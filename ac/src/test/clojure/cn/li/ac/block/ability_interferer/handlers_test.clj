(ns cn.li.ac.block.ability-interferer.handlers-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.ability-interferer.handlers :as handlers]
            [cn.li.ac.block.ability-interferer.logic :as interferer-logic]
            [cn.li.ac.block.machine.handlers :as machine-handlers]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.test.support.gui-payload :as gui-payload]
            [cn.li.ac.test.support.network :as network-support]
            [cn.li.mcmod.platform.be :as platform-be]))

(def ^:private payload (gui-payload/machine-payload 1))

(defn- with-saved-state
  [initial-state f]
  (let [saved (atom nil)]
    (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)
                  platform-be/get-custom-state (fn [_] initial-state)
                  machine-runtime/commit-transform!
                  (fn [_ _ transform & _]
                    (reset! saved (transform (interferer-logic/interferer-default-state))))]
      (f saved))))

(deftest handle-set-whitelist-normalizes-and-marks-changed-test
  (with-saved-state {:whitelist ["Old"]}
    (fn [saved]
      (is (= {:success true}
             (#'handlers/handle-set-whitelist (assoc payload :whitelist [" Bob " "" "Alice" "Bob"])
                                              :player)))
      (is (= ["Alice" "Bob"] (:whitelist @saved))))))

(deftest handle-add-to-whitelist-marks-changed-and-normalizes-test
  (with-saved-state {:whitelist ["Bob"]}
    (fn [saved]
      (is (= {:success true}
             (#'handlers/handle-add-to-whitelist (assoc payload :player-name " Alice ")
                                                   :player)))
      (is (= ["Alice" "Bob"] (:whitelist @saved))))))

(deftest handle-remove-from-whitelist-marks-changed-test
  (with-saved-state {:whitelist ["Alice" "Bob"]}
    (fn [saved]
      (is (= {:success true}
             (#'handlers/handle-remove-from-whitelist (assoc payload :player-name "Bob")
                                                      :player)))
      (is (= ["Alice"] (:whitelist @saved))))))

(deftest handle-add-remove-whitelist-guards-invalid-input-test
  (testing "blank add name is rejected"
    (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)]
      (is (= {:success false}
             (#'handlers/handle-add-to-whitelist (assoc payload :player-name "   ")
                                                 :player)))))
  (testing "blank remove name is rejected"
    (with-redefs [machine-handlers/open-container-tile (network-support/open-tile-mock :tile)]
      (is (= {:success false}
             (#'handlers/handle-remove-from-whitelist (assoc payload :player-name "")
                                                      :player))))))
