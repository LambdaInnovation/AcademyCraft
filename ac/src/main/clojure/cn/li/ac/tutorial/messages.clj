(ns cn.li.ac.tutorial.messages
  "Pure tutorial network message identifiers shared by client GUI and server
  handlers.  Follows terminal/messages.clj pattern.")

(def message-ids
  {:tutorial/sync                 "tutorial:sync"                 ;; server → client: full state
   :tutorial/request-sync         "tutorial:request-sync"         ;; client → server: request sync
   :tutorial/mark-first-open-done "tutorial:mark-first-open-done"}) ;; client → server: animation played

(defn msg-id
  [action]
  (or (get message-ids action)
      (throw (ex-info "Unknown tutorial message action" {:action action}))))
