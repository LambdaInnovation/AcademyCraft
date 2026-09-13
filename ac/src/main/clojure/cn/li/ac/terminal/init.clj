(ns cn.li.ac.terminal.init
  "Terminal system initialization."
  (:require [cn.li.ac.media.network :as media-network]
            [cn.li.ac.registry.hooks :as hooks]
            [cn.li.ac.terminal.freq-network :as freq-network]
            [cn.li.ac.terminal.network :as network]
            [cn.li.mcmod.util.log :as log]))

(defn init-terminal!
  "Initialize the terminal system (network handlers only)."
  []
  (log/debug "Initializing terminal system...")
  (hooks/register-network-handler! network/register-handlers!)
  (hooks/register-network-handler! freq-network/register-handlers!)
  (hooks/register-network-handler! media-network/register-handlers!)
  (log/debug "Terminal system initialized successfully"))
