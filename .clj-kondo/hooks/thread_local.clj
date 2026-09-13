(ns hooks.thread-local
  "clj-kondo hook: prevent ThreadLocal usage outside of hooks.core.
  Enforces the ThreadLocal calling convention — only cn.li.mcmod.hooks.core
  may construct ThreadLocal instances."
  (:require [clj-kondo.hooks-api :as api]))

(def allowed-ns
  "Namespaces allowed to use ThreadLocal."
  #{"cn.li.mcmod.hooks.core"})

(defn thread-local-ctor
  "Check (ThreadLocal.) calls. Warns when called outside allowed namespaces."
  [{:keys [node]}]
  (let [current-ns (some-> node :ns .name str)]
    (when (and current-ns (not (contains? allowed-ns current-ns)))
      (api/reg-finding!
        (assoc (meta node)
               :message (str "ThreadLocal constructor called outside allowed namespace ("
                             current-ns "). "
                             "Use with-client-ctx-fn in cn.li.mcmod.hooks.core instead.")
               :type :thread-local-isolation)))
    node))
