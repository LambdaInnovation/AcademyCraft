(ns cn.li.mcmod.events.world-save-cache-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.events.world-save-cache :as save-cache]))

(use-fixtures :each
  (fn [f]
    (save-cache/reset-world-save-cache-for-test!)
    (f)
    (save-cache/reset-world-save-cache-for-test!)))

(defn- test-world
  [session-id world-id]
  {:server-session-id session-id
   :world-id world-id})

(deftest remember-and-consume-saved-data-per-world-test
  (let [world-a (test-world :session-a :overworld)
        world-b (test-world :session-a :nether)
        saved-a [{:k :a}]
        saved-b [{:k :b}]]
    (save-cache/remember-saved-data! world-a saved-a)
    (save-cache/remember-saved-data! world-b saved-b)
    (is (= saved-a (save-cache/consume-saved-data! world-a)))
    (is (nil? (save-cache/consume-saved-data! world-a)))
    (is (= saved-b (save-cache/consume-saved-data! world-b)))))

(deftest clear-session-saved-data-removes-only-target-session-test
  (let [world-a (test-world :session-a :overworld)
        world-a-nether (test-world :session-a :nether)
        world-b (test-world :session-b :overworld)]
    (save-cache/remember-saved-data! world-a [{:world :a}])
    (save-cache/remember-saved-data! world-a-nether [{:world :a-nether}])
    (save-cache/remember-saved-data! world-b [{:world :b}])
    (save-cache/clear-session-saved-data! :session-a)
    (is (nil? (save-cache/consume-saved-data! world-a)))
    (is (nil? (save-cache/consume-saved-data! world-a-nether)))
    (is (= [{:world :b}] (save-cache/consume-saved-data! world-b)))))

(deftest world-key-requires-explicit-owner-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"requires :server-session-id"
                        (save-cache/world-key :legacy-world)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"requires :world-id"
                        (save-cache/world-key {:server-session-id :session-a}))))
