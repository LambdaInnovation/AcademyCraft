(ns cn.li.ac.block.machine-wireless-handlers-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.machine.wireless-handlers :as wireless-handlers]
            [cn.li.ac.test.support.gui-payload :as gui-payload]
            [cn.li.ac.test.support.network :as network-support]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.wireless.gui.sync.handler :as net-helpers]
            [cn.li.mcmod.gui.container.sync-routing :as sync-routing]
            [cn.li.mcmod.network.server :as net-server]
            [cn.li.mcmod.platform.position :as pos]))

(def ^:private base-payload (gui-payload/machine-payload 1))

(defn- register-handlers!
  [spec]
  (let [handlers (atom {})]
    (with-redefs [net-server/register-handler (fn [msg-id handler]
                                                (swap! handlers assoc (peek msg-id) handler))
                  msg-registry/msg (fn [_domain action] [:test-domain action])]
      (wireless-handlers/register-link-handlers! spec)
      @handlers)))

(deftest link-handlers-list-nodes-safe-defaults-test
  (let [handlers (register-handlers!
                   {:message-domain :test
                    :get-linked-node (fn [_] nil)
                    :link! (fn [& _] true)
                    :unlink! (fn [_] nil)})]
    (with-redefs [sync-routing/require-open-container! (network-support/require-open-container-mock :tile)
                  net-helpers/get-world (fn [_] :world)
                  pos/block-pos (fn [_] :tile-pos)
                  wireless-api/get-nodes-in-range (fn [_ _] [])]
      (is (= {:success true :linked nil :avail []}
             ((:list-nodes handlers) base-payload :player))))))

(deftest link-handlers-connect-and-disconnect-test
  (testing "connect success path"
    (let [link-calls (atom [])
          handlers (register-handlers!
                     {:message-domain :test
                      :get-linked-node (fn [_] nil)
                      :link! (fn [recv node pass need-auth?]
                               (swap! link-calls conj [recv node pass need-auth?])
                               {:success true})
                      :unlink! (fn [_] nil)})]
      (with-redefs [sync-routing/require-open-container!
                    (fn [payload _player]
                      (if (contains? payload :node-x)
                        {:tile-entity :node}
                        {:tile-entity :recv}))
                    net-helpers/get-world (fn [_] :world)
                    net-helpers/get-tile-at (fn [_ _] :recv)
                    pos/block-pos identity
                    wireless-api/get-nodes-in-range (fn [_ _] [])]
        (let [result ((:connect handlers)
                      (merge base-payload
                             {:node-x 9 :node-y 8 :node-z 7
                              :password "pw"
                              :need-auth? false})
                      :player)]
          (is (true? (:success result)))
          (is (= [[:node :recv "pw" false]] @link-calls))))))

  (testing "connect failure without node payload"
    (let [handlers (register-handlers!
                     {:message-domain :test
                      :get-linked-node (fn [_] nil)
                      :link! (fn [& _] true)
                      :unlink! (fn [_] nil)})]
      (with-redefs [sync-routing/require-open-container! (network-support/require-open-container-mock :recv)
                    net-helpers/get-world (fn [_] :world)]
        (is (false? (:success ((:connect handlers) base-payload :player)))))))

  (testing "disconnect success path"
    (let [unlink-calls (atom 0)
          handlers (register-handlers!
                     {:message-domain :test
                      :get-linked-node (fn [_] nil)
                      :link! (fn [& _] true)
                      :unlink! (fn [_] (swap! unlink-calls inc))})]
      (with-redefs [sync-routing/require-open-container! (network-support/require-open-container-mock :recv)
                    net-helpers/get-world (fn [_] nil)
                    pos/block-pos identity]
        (is (true? (:success ((:disconnect handlers) base-payload :player))))
        (is (= 1 @unlink-calls))))))
