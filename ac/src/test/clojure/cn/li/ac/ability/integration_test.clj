(ns cn.li.ac.ability.integration-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.reducer :as reducer]
            [cn.li.ac.ability.service.runtime-store :as runtime-store]
            [cn.li.ac.test.support.player-state :as ps-fix]))

(use-fixtures :each ps-fix/clean-player-states-fixture)

(deftest store-plus-reducer-command-flow-test
  (let [session-id :test-session
        player-uuid "int-player"]
    (runtime-store/create-session! session-id)
    (runtime-store/get-or-create-player-state! session-id player-uuid)

    (let [activate-result (reducer/apply-command
                            (runtime-store/get-player-state session-id player-uuid)
                            {:command :set-activated
                             :player-uuid player-uuid
                             :activated true})]
      (runtime-store/apply-reducer-result! session-id player-uuid activate-result))

    (let [consume-result (reducer/apply-command
                           (runtime-store/get-player-state session-id player-uuid)
                           {:command :consume-resource
                            :player-uuid player-uuid
                            :cp 8.0
                            :overload 2.0
                            :creative? false})]
      (runtime-store/apply-reducer-result! session-id player-uuid consume-result)
      (is (true? (:success? consume-result))))

    (let [updated (runtime-store/get-player-state session-id player-uuid)]
      (is (true? (get-in updated [:resource-data :activated])))
      (is (< (get-in updated [:resource-data :cur-cp])
             (get-in updated [:resource-data :max-cp]))))))
