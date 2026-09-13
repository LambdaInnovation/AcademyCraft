(ns cn.li.mcmod.gui.spec
  "Shared GUI spec builders and validation."
  (:require [clojure.string :as str]
            [cn.li.mcmod.gui.registry :as gui-registry]
            [cn.li.mcmod.gui.registry-contract :as registry-contract]
            [cn.li.mcmod.gui.slot-schema :as slot-schema]))

(defn- validate-gui-spec!
  [gui-spec]
  (when-let [screen-contract (:screen-contract gui-spec)]
    (registry-contract/validate-screen-contract! screen-contract))
  (when-not (and (:id gui-spec) (string? (:id gui-spec)) (not (str/blank? (:id gui-spec))))
    (throw (ex-info "GUI must have a non-empty string :id" {:id (:id gui-spec) :spec gui-spec})))

  (when (some? (:gui-id gui-spec))
    (let [registration (:registration gui-spec)]
      (when-not (integer? (:gui-id gui-spec))
        (throw (ex-info "GUI :gui-id must be an integer when provided"
                        {:gui-id (:gui-id gui-spec) :id (:id gui-spec)})))
      (when-not (and (string? (:registry-name registration))
                     (not (str/blank? (:registry-name registration))))
        (throw (ex-info "GUI :registry-name must be a non-empty string when :gui-id is present"
                        {:id (:id gui-spec)
                         :gui-id (:gui-id gui-spec)
                         :registry-name (:registry-name registration)})))
      (when-not (keyword? (:gui-type registration))
        (throw (ex-info "GUI :gui-type must be a keyword when :gui-id is present"
                        {:id (:id gui-spec)
                         :gui-id (:gui-id gui-spec)
                         :gui-type (:gui-type registration)})))
      (when-not (keyword? (:screen-factory-fn-kw registration))
        (throw (ex-info "GUI :screen-factory-fn-kw must be a keyword when :gui-id is present"
                        {:id (:id gui-spec)
                         :gui-id (:gui-id gui-spec)
                         :screen-factory-fn-kw (:screen-factory-fn-kw registration)})))))

  (let [layout (:layout gui-spec)]
    (doseq [slot (:slots layout)]
      (when-not (number? (:index slot))
        (throw (ex-info "Slot must have an :index number" {:slot slot}))))
    (doseq [button (:buttons layout)]
      (when-not (number? (:id button))
        (throw (ex-info "Button must have an :id number" {:button button})))))
  gui-spec)

(defn- parse-slot [slot-map]
  {:index (:index slot-map)
   :x (:x slot-map 0)
   :y (:y slot-map 0)
   :filter (or (:filter slot-map) (constantly true))
   :on-change (or (:on-change slot-map) (constantly nil))})

(defn- parse-button [button-map]
  {:id (:id button-map)
   :x (:x button-map 0)
   :y (:y button-map 0)
   :width (or (:width button-map) 60)
   :height (or (:height button-map) 20)
   :text (:text button-map "Button")
   :on-click (or (:on-click button-map) (constantly nil))})

(defn- parse-label [label-map]
  {:x (:x label-map 0)
   :y (:y label-map 0)
   :text (:text label-map "")
   :color (or (:color label-map) 0x404040)})

(defn create-gui-spec
  [gui-id options]
  (let [registration-opts (:registration options)
        lifecycle-opts (:lifecycle options)
        operations-opts (:operations options)
        slot-operations-opts (:slot-operations options)
        layout-opts (:layout options)
        slots-vec (mapv parse-slot (or (:slots layout-opts)
                                       (when (vector? (:slots options)) (:slots options))
                                       []))
        buttons-vec (mapv parse-button (or (:buttons layout-opts) (:buttons options) []))
        labels-vec (mapv parse-label (or (:labels layout-opts) (:labels options) []))
        spec {:id gui-id
              :screen-contract (registry-contract/normalize-screen-contract
                                 (or (:screen-contract options)
                                     (registry-contract/default-client-screen-contract)))
              :gui-id (:gui-id options)
              :registration {:display-name (:display-name registration-opts)
                             :gui-type (:gui-type registration-opts)
                             :registry-name (:registry-name registration-opts)
                             :screen-factory-fn-kw (:screen-factory-fn-kw registration-opts)
                             :slot-layout (:slot-layout registration-opts)}
              :lifecycle {:container-fn (:container-fn lifecycle-opts)
                          :container-predicate (:container-predicate lifecycle-opts)
                          :screen-fn (:screen-fn lifecycle-opts)
                          :server-menu-sync-fn (:server-menu-sync-fn lifecycle-opts)}
              :operations {:validate-fn (:validate-fn operations-opts)
                           :close-fn (:close-fn operations-opts)
                           :button-click-fn (:button-click-fn operations-opts)
                           :text-input-fn (:text-input-fn operations-opts)}
              :slot-operations {:slot-count-fn (:slot-count-fn slot-operations-opts)
                                :slot-get-fn (:slot-get-fn slot-operations-opts)
                                :slot-set-fn (:slot-set-fn slot-operations-opts)
                                :slot-can-place-fn (:slot-can-place-fn slot-operations-opts)
                                :slot-changed-fn (:slot-changed-fn slot-operations-opts)
                                :quick-move-fn (:quick-move-fn slot-operations-opts)}
              :layout {:title (or (:title layout-opts) (:title options) "GUI")
                       :width (or (:width layout-opts) (:width options) 176)
                       :height (or (:height layout-opts) (:height options) 166)
                       :slots slots-vec
                       :buttons buttons-vec
                       :labels labels-vec
                       :background (or (:background layout-opts) (:background options) :default)}}]
    (validate-gui-spec! spec)))

(defn slot-operations
  [{:keys [slot-count-fn slot-get-fn slot-set-fn slot-can-place-fn slot-changed-fn quick-move-fn]}]
  {:slot-count-fn slot-count-fn
   :slot-get-fn slot-get-fn
   :slot-set-fn slot-set-fn
   :slot-can-place-fn slot-can-place-fn
   :slot-changed-fn slot-changed-fn
   :quick-move-fn quick-move-fn})

(defn create-block-gui-spec
  [gui-name opts]
  (let [layout (or (:slot-layout opts)
                   (when (:slot-schema-id opts)
                     (slot-schema/get-slot-layout (:slot-schema-id opts))))]
    (create-gui-spec
      gui-name
      {:gui-id (:gui-id opts)
       :registration {:display-name (:display-name opts)
                      :gui-type (:gui-type opts)
                      :registry-name (:registry-name opts)
                      :screen-factory-fn-kw (:screen-factory-fn-kw opts)
                      :slot-layout layout}
       :lifecycle {:container-predicate (:container-predicate opts)
                   :container-fn (:container-fn opts)
                   :screen-fn (:screen-fn opts)
                   :server-menu-sync-fn (:server-menu-sync-fn opts)}
       :operations {:validate-fn (:validate-fn opts)
                    :close-fn (:close-fn opts)
                    :button-click-fn (:button-click-fn opts)}
       :slot-operations (slot-operations opts)})))

(defn register-block-gui!
  [gui-name opts]
  (gui-registry/register-gui! (create-block-gui-spec gui-name opts)))
