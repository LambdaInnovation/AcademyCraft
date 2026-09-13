(ns cn.li.mcmod.gui.container-state-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.gui.container-state :as state]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.framework :as fw]))

(defn- with-framework [f]
  (let [prev-fw fw/framework]
    (try
      (when-let [fw-inst (fw/create-framework)]
        (alter-var-root #'fw/framework (constantly fw-inst))
        (f))
      (finally
        (alter-var-root #'fw/framework (constantly prev-fw))))))

(defn- test-fixture [f]
  (with-framework
    (fn []
      (state/clear-all!)
      (f))))

(use-fixtures :each test-fixture)

(deftest register-and-resolve-menu-container-test
  (testing "register-menu-container! stores container and get-container-for-menu retrieves it"
    (let [menu (Object.)
          container {:gui-type :developer :id 13}]
      (is (nil? (state/get-container-for-menu menu)))
      (is (nil? (state/resolve-container-for-menu menu)))
      (state/register-menu-container! menu container)
      (is (= container (state/get-container-for-menu menu)))
      (is (= container (state/resolve-container-for-menu menu)))
      (state/unregister-menu-container! menu)
      (is (nil? (state/get-container-for-menu menu)))
      (is (nil? (state/resolve-container-for-menu menu))))))

(deftest clear-all-clears-menu-containers-test
  (testing "clear-all! removes all menu→container mappings"
    (let [menu (Object.)
          container {:gui-type :developer :id 13}]
      (state/register-menu-container! menu container)
      (is (= container (state/get-container-for-menu menu)))
      (state/clear-all!)
      (is (nil? (state/get-container-for-menu menu))))))

(deftest get-menu-container-id-uses-platform-protocol-test
  (testing "get-menu-container-id reads via platform entity ops"
    (entity/install-entity-ops!
      {:menu-get-container-id (fn [menu] (:container-id menu))}
      "container-state-test")
    (let [menu {:container-id 42}]
      (is (= 42 (state/get-menu-container-id menu)))
      (is (nil? (state/get-menu-container-id nil))))))

(deftest container-state-snapshot-tracks-menu-containers-test
  (testing "container-state-snapshot exposes menu-containers map"
    (let [menu (Object.)
          container {:gui-type :developer :id 13}]
      (state/register-menu-container! menu container)
      (let [snap (state/container-state-snapshot)]
        (is (map? (:menu-containers snap)))
        (is (= container (get (:menu-containers snap) menu)))))))

(deftest client-container-side-channel-stays-absent-test
  (testing "the legacy global client-container API does not exist"
    (is (nil? (ns-resolve 'cn.li.mcmod.gui.container-state 'client-container)))
    (is (nil? (ns-resolve 'cn.li.mcmod.gui.container-state 'set-client-container!)))
    (is (nil? (ns-resolve 'cn.li.mcmod.gui.container-state 'get-client-container)))
    (is (nil? (ns-resolve 'cn.li.mcmod.gui.container-state 'clear-client-container!)))
    (is (nil? (ns-resolve 'cn.li.mcmod.gui.container-state 'set-tab-index-by-container-id!)))
    (is (nil? (when-let [tabbed-ns (find-ns 'cn.li.mcmod.gui.tabbed-gui)]
               (ns-resolve tabbed-ns 'set-tab-index-by-container-id!))))))
