(ns cn.li.ac.terminal.client.apps-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.terminal.client.apps :as client-apps]
            [cn.li.ac.terminal.catalog :as catalog]))

(deftest launchers-cover-catalog-test
  (is (= (set (catalog/app-ids))
         (set (keys client-apps/launchers)))))

(deftest unknown-app-launch-fails-test
  (is (false? (client-apps/launch! :missing-app :player))))
