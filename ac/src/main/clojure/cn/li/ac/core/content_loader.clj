(ns cn.li.ac.core.content-loader
  "AC runtime content loading orchestration."
  (:require [cn.li.ac.gui.platform-adapter :as platform-gui]
            [cn.li.ac.registry.content-namespaces :as content-ns]
            [cn.li.ac.registry.hooks :as hooks]
            [cn.li.ac.wireless.gui.sync.handler :as wireless-sync-handler]
            [cn.li.ac.wireless.gui.screen-factory :as screen-factory]
            [cn.li.mcmod.block.state-schema :as state-schema]
            [cn.li.mcmod.gui.registry :as gui-registry]
            [cn.li.mcmod.gui.tabbed-gui :as tabbed-gui]
            [cn.li.ac.gui.reactive.register :as reactive-register]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(defn- register-network-edit-helpers!
  []
  (state-schema/register-network-helper-fns!
    {:get-world wireless-sync-handler/get-world
     :get-tile-at wireless-sync-handler/get-tile-at}))

(defn- load-runtime-content-once!
  []
  (platform-gui/install-into-mcmod!)
  (register-network-edit-helpers!)
  (content-ns/load-all!)
  (let [gui-ids (gui-registry/get-all-gui-ids)]
    (log/info "Registering screen factories for GUI IDs:" gui-ids)
    (doseq [gui-id gui-ids]
      (when-let [gui-type (gui-registry/get-gui-type gui-id)]
        (let [declared-screen-fn-kw (gui-registry/get-screen-factory-fn-kw gui-id)
              screen-fn-kw (or declared-screen-fn-kw
                               (keyword (str "create-" (name gui-type) "-screen")))]
          (gui-registry/register-screen-factory!
            screen-fn-kw
            (partial screen-factory/create-screen gui-type))
          (log/debug "Registered screen factory" screen-fn-kw "for GUI ID" gui-id
                     "gui-type=" gui-type "declared?" (boolean declared-screen-fn-kw))))))
  (hooks/call-all-network-handlers!)
  (tabbed-gui/register-set-tab-handler!)
  ;; Install the client bridge used by registered reactive GUI factories.
  (reactive-register/install-bridge!)
  nil)

(defn activate-runtime-content!
  "Load and initialize AC runtime content once per Framework lifetime.
  Safe to call repeatedly."
  []
  (install/framework-once! ::runtime-content-loaded (fn [] (load-runtime-content-once!)))
  nil)
