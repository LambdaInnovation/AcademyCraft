(ns cn.li.ac.item.terminal-installer-handler
  "Server-driven terminal installer right-click handler matching original
   ItemTerminalInstaller.onItemRightClick (AcademyCraft Forge 1.12)."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.terminal.messages :as terminal-messages]
            [cn.li.ac.terminal.player :as terminal-player]
            [cn.li.ac.achievement.dispatcher :as achievement-dispatcher]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.server.platform-bridge :as server-bridge]))

(defn- send-chat-message!
  [player-uuid message args translate?]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :player-feedback :send-player-feedback!
                            player-uuid
                            {:mode :chat
                             :message message
                             :args (vec (or args []))
                             :translate? (boolean translate?)}))))

(defn handle-right-click
  "Server-side handler for terminal_installer right-click.
   Matching original ItemTerminalInstaller.onItemRightClick:
   - Already installed → message, always consume item (original SUCCESS)
   - Not installed → install + achievement + push install-effect
   - Consume item unless creative mode"
  [player]
  (let [uuid-str (uuid/player-uuid player)
        installed? (boolean (terminal-player/terminal-installed? player))]
    (if installed?
      ;; Already installed: always consume (matching original SUCCESS)
      (do
        (send-chat-message! uuid-str (str "terminal." modid/MOD-ID ".alrdy_installed") [] true)
        {:consume? true})
      ;; Not installed: install, trigger achievement, push effect to client
      (do
        (terminal-player/install-terminal! player)
        (try
          (achievement-dispatcher/trigger-custom-event! uuid-str "terminal_installed")
          (catch Throwable _ nil))
        (try
          (server-bridge/send-to-client! uuid-str
            (terminal-messages/msg-id :terminal-install-effect) {})
          (catch Throwable _ nil))
        ;; Matching original: consume unless creative mode
        {:consume? (not (boolean (entity/player-creative? player)))}))))
