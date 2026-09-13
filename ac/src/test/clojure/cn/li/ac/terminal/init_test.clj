(ns cn.li.ac.terminal.init-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.registry.hooks :as hooks]
            [cn.li.ac.terminal.freq-network :as freq-network]
            [cn.li.ac.terminal.init :as terminal-init]
            [cn.li.ac.media.network :as media-network]
            [cn.li.ac.terminal.network :as network]))

(deftest init-terminal-registers-network-handlers-test
  (let [registered (atom [])]
    (with-redefs [hooks/register-network-handler! (fn [f] (swap! registered conj f))]
      (terminal-init/init-terminal!)
      ;; Terminal RPC handlers, freq-transmitter wireless handlers, media handlers.
      (is (= [network/register-handlers!
              freq-network/register-handlers!
              media-network/register-handlers!]
             @registered)))))
