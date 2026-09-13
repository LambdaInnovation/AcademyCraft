(ns hooks.send-to-server
  "clj-kondo hook: detect ambiguous 3-arg calls to net-client/send-to-server.

  send-to-server has these arities:
    [msg-id payload]              ;; 2-arg: no owner, no callback
    [msg-id payload callback]     ;; 3-arg: no owner, WITH callback (ambigous!)
    [owner msg-id payload callback] ;; 4-arg: WITH owner, WITH callback

  A 3-arg call `(send-to-server a b c)` dispatches as
  `[msg-id payload callback]` — there is NO owner. If the caller
  intended `[owner msg-id payload]` it silently misroutes and
  the first arg becomes msg-id.

  This hook warns on every 3-arg call and suggests passing nil as
  the 4th arg if the intent was [owner msg-id payload], or using
  the 2-arg form if no callback is needed."
  (:require [clj-kondo.hooks-api :as api]))

(defn send-to-server-hook
  [{:keys [node]}]
  (let [children (:children node)
        arg-count (count (filter (comp not :children) children))]
    (when (= 3 arg-count)
      (api/reg-finding!
        (assoc (meta node)
               :message (str "send-to-server called with 3 args. "
                             "This dispatches as [msg-id payload callback] — NO owner is passed. "
                             "If you need an owner, pass 4 args: "
                             "(send-to-server owner msg-id payload nil). "
                             "If no callback is needed and no owner, use 2 args: "
                             "(send-to-server msg-id payload).")
               :type :send-to-server-arity))))
  node)
