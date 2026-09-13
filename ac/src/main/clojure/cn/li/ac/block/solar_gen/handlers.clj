(ns cn.li.ac.block.solar-gen.handlers
  (:require [cn.li.ac.block.machine.wireless-handlers :as wireless-handlers]
            [cn.li.ac.block.solar-gen.logic :as solar-logic]
            [cn.li.ac.wireless.api :as wireless-api]))

(defn register-network-handlers! []
  (wireless-handlers/register-link-handlers!
    {:message-domain :generator
     :get-linked-node solar-logic/get-linked-node
     :link! wireless-api/link-generator-to-node!
     :unlink! wireless-api/unlink-generator-from-node!
     :log-label "Solar Generator network handlers registered"}))
