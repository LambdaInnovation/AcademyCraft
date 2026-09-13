(ns cn.li.ac.item.app-installers
  "Terminal app installer items migrated from original AcademyCraft."
  (:require [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.terminal.player :as term-player]
            [cn.li.ac.terminal.catalog :as term-catalog]
            [cn.li.mcmod.util.log :as log]))

(defn- install-app-for-player!
  [player app-id]
  (cond
    (not (term-player/terminal-installed? player))
    (do
      (log/info "Skip app install: terminal not installed" {:player (uuid/player-uuid player) :app app-id})
      {:consume? true})

    (not (term-catalog/app-exists? app-id))
    (do
      (log/warn "Skip app install: app not registered" {:player (uuid/player-uuid player) :app app-id})
      {:consume? true})

    :else
    (do
      (term-player/install-app! player app-id)
      (log/info "Installed terminal app from installer item" {:player (uuid/player-uuid player) :app app-id})
      {:consume? true})))

(defn- app-installer-handler
  [app-id]
  (fn [{:keys [player side]}]
    ;; App state lives on the server side.
    (when (= side :server)
      (install-app-for-player! player app-id))
    {:consume? true}))

(defn init-app-installers!
  []
  (install/framework-once! ::app-installers-installed?
  (fn []
    (idsl/register-item!
      (idsl/create-item-spec
        "app_freq_transmitter"
        {:max-stack-size 1
         :creative-tab :tools
         :properties {:tooltip ["Install terminal app: Frequency Transmitter"
                                "Requires terminal installation first"]
                      :model-texture "app_freq_transmitter"}
         :on-right-click (app-installer-handler :freq-transmitter)}))
    (idsl/register-item!
      (idsl/create-item-spec
        "app_media_player"
        {:max-stack-size 1
         :creative-tab :tools
         :properties {:tooltip ["Install terminal app: Media Player"
                                "Requires terminal installation first"]
                      :model-texture "app_media_player"}
         :on-right-click (app-installer-handler :media-player)}))
    (idsl/register-item!
      (idsl/create-item-spec
        "app_skill_tree"
        {:max-stack-size 1
         :creative-tab :tools
         :properties {:tooltip ["安装终端应用: 技能树"
                                "需要先安装终端"]
                      :model-texture "app_skill_tree"}
         :on-right-click (app-installer-handler :skill-tree)}))
    (log/debug "App installer items initialized: app_freq_transmitter, app_media_player, app_skill_tree"))))
