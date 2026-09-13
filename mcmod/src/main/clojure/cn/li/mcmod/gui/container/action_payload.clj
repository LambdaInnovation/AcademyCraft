(ns cn.li.mcmod.gui.container.action-payload
  "Helpers for building validated C2S GUI action payloads."
  (:require [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.util.log :as log]))

(defn menu-container-id
  [container]
  (when container
    (or (:container-id container)
        (when-let [menu (or (:menu container) (:minecraft-container container))]
          (container-state/get-menu-container-id menu)))))

(defn action-payload
  "Merge optional base map with required :container-id from container/menu."
  [container base]
  (let [cid (menu-container-id container)]
    (when-not (integer? cid)
      (log/error "GUI action payload missing container-id=" cid)
      (throw (ex-info "GUI action requires open menu container-id"
                      {:container-keys (keys container)})))
    (merge (or base {})
           {:container-id (int cid)})))
