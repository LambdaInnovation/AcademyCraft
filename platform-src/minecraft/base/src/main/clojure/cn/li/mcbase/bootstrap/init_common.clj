(ns cn.li.mcbase.bootstrap.init-common
  "Shared platform init orchestration for Java entrypoints."
  (:require [cn.li.mcmod.aot :as aot]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.platform.dispatch :as platform-dispatch]
            [cn.li.mcmod.platform.position :as platform-position]
            [cn.li.mcmod.platform.structured-data :as platform-sd]
            [cn.li.mcmod.platform.item :as platform-item]
            [cn.li.mcmod.util.log :as log]))

(defn set-platform-version!
  [platform-key]
  (platform-dispatch/install-platform-version! platform-key "mc1201-bootstrap")
  (log/debug "Set platform dispatch to" platform-key))

(defn assert-platform-ready!
  [platform-key]
  (let [fw-atom (fw/fw-atom)
        checks [{:k :resource :ok (boolean (get (platform/get-adapter fw-atom :resource) :factory))}
                {:k :position :ok (platform-position/factory-initialized?)}
                {:k :structured-data :ok (platform-sd/factory-initialized?)}
                {:k :item :ok (platform-item/available?)}]
        missing (->> checks (remove #(get % :ok)) (map #(get % :k)) vec)]
    (when (seq missing)
      (throw (ex-info "Platform bootstrap incomplete - init-platform! must run before init-from-java"
                      {:platform platform-key
                       :missing missing})))))

(defn init-from-java!
  [platform-key on-runtime-init]
  (let [ctx (aot/compile-context)]
    (log/debug "[BOOTSTRAP_TRACE_INIT] init-from-java enter"
              {:platform platform-key
               :compile-context ctx})
    (if (aot/compiling?)
      (log/debug "[BOOTSTRAP_TRACE_INIT] skip content init during compilation/check" {:platform platform-key})
      (do
        (assert-platform-ready! platform-key)
        (set-platform-version! platform-key)
        (when (fn? on-runtime-init)
          (on-runtime-init))
        (log/debug "Platform adapter initialized" {:platform platform-key})))))
