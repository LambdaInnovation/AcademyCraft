(ns cn.li.ac.terminal.messages
  "Pure terminal network message identifiers shared by client GUI and server handlers.")

(def message-ids
  {:install-terminal         "terminal:install-terminal"
   :install-app              "terminal:install-app"
   :uninstall-app            "terminal:uninstall-app"
   :get-state                "terminal:get-state"
   :terminal-install-effect  "terminal:install-effect"})

(defn msg-id
  [action]
  (or (get message-ids action)
      (throw (ex-info "Unknown terminal message action" {:action action}))))