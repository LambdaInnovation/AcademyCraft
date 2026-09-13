(ns cn.li.mcmod.client.ui.registry
  "Client UI widget factory registry.

  Widget ids and payload schemas are content-owned; mcmod only stores and invokes
  registered factories.

  State stored in Framework [:registry :widget-factories]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(def ^:private widgets-path [:registry :widget-factories])

(defn- widget-factories-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom widgets-path {})
    {}))

(defn register-widget-factory!
  [widget-key factory-fn]
  (when-not (keyword? widget-key)
    (throw (ex-info "Widget key must be a keyword" {:widget-key widget-key})))
  (when-not (fn? factory-fn)
    (throw (ex-info "Widget factory must be a function" {:widget-key widget-key :factory factory-fn})))
  (let [prev (get (widget-factories-snapshot) widget-key)]
    (when (and (some? prev) (not= prev factory-fn))
      (throw (ex-info "Duplicate UI widget factory registration"
                      {:widget-key widget-key :previous prev :incoming factory-fn}))))
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in (conj widgets-path widget-key) factory-fn))
  nil)

(defn reset-widget-factory-registry!
  "Test-only reset of widget factory registry."
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in widgets-path {}))
  nil)

(defn register-widget-factories!
  [factory-map]
  (doseq [[widget-key factory-fn] factory-map]
    (register-widget-factory! widget-key factory-fn))
  nil)

(defn create-widget
  [widget-key payload]
  (let [factory-fn (or (get (widget-factories-snapshot) widget-key)
                       (throw (ex-info "UI widget factory not registered"
                                       {:widget-key widget-key})))]
    (factory-fn payload)))
