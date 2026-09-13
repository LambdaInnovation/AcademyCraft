(ns cn.li.ac.block.solar-gen.logic
  (:require [cn.li.ac.block.machine.container :as machine-container]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.solar-gen.config :as solar-config]
            [cn.li.ac.block.solar-gen.schema :as solar-schema]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessNode]))

(def ^:private solar-rt
  (machine-runtime/schema-runtime solar-schema/unified-solar-schema))

(def solar-default-state (:default-state solar-rt))
(def solar-scripted-load-fn (:load-fn solar-rt))
(def solar-scripted-save-fn (:save-fn solar-rt))

(defn- can-generate? [level pos]
  (when (and level pos)
    (let [time (rem (long (world/day-time level)) 24000)
          day? (<= time (solar-config/daytime-threshold-ticks))]
      (and day?
           (world/can-see-sky? level
             (pos/create-block-pos (pos/pos-x pos) (inc (pos/pos-y pos)) (pos/pos-z pos)))))))

(defn solar-tick-state
  [state level pos _block-state _be]
  (let [generating? (can-generate? level pos)
        raining? (world/raining? level)
        status (cond (not generating?) "STOPPED"
                     raining? "WEAK"
                     :else "STRONG")
        bright (if generating? 1.0 0.0)
        bright* (if (and (> bright 0) raining?) (* bright (solar-config/rain-multiplier)) bright)
        gen (* bright* (solar-config/generation-rate))
        current (double (get state :energy 0.0))
        max-energy (solar-config/max-energy)
        new-energy (min max-energy (+ current gen))
        changed? (and (> gen 0) (not= new-energy current))
        state' (cond-> (assoc state :status status :max-energy max-energy :gen-speed (double gen))
                 changed? (assoc :energy new-energy))]
    ;; Charge energy into the battery/item in the output slot
    (let [stack (get-in state' [:inventory 0])
          cur (double (get state' :energy 0.0))]
      (if (and stack (energy/is-energy-item-supported? stack) (pos? cur))
        (let [item-cur (double (energy/get-item-energy stack))
              item-max (double (energy/get-item-max-energy stack))
              need (max 0.0 (- item-max item-cur))
              amount (min cur need)
              leftover (double (energy/charge-energy-to-item stack amount false))
              accepted (max 0.0 (- amount leftover))]
          (if (pos? accepted)
            (assoc state' :energy (- cur accepted))
            state'))
        state'))))

(def solar-tick-fn
  (machine-runtime/make-tick-fn
    {:default-state solar-default-state
     :tick-state solar-tick-state
     :mark-changed? (fn [old-state new-state]
                      (and (pos? (get new-state :gen-speed 0.0))
                           (not= (:energy old-state) (:energy new-state))))}))

(def solar-container-fns
  (machine-container/make-inventory-container-fns
    {:default-state solar-default-state
     :slot-count (constantly 1)
     :can-place? (fn [_be _slot item _face]
                   (energy/is-energy-item-supported? item))
     :can-take? (fn [_be _slot _item _face] true)}))

(def open-solar-gui!
  (machine-runtime/make-open-gui-handler :solar))

(defn get-linked-node ^IWirelessNode [tile]
  (let [gen-pos (try (pos/block-pos tile) (catch Exception _ nil))
        pos-str (when gen-pos (str (pos/pos-x gen-pos) "," (pos/pos-y gen-pos) "," (pos/pos-z gen-pos)))]
    (if-let [conn (try (wireless-api/get-node-conn-by-generator tile)
                       (catch Exception e
                         (log/debug "[get-linked-node] exception:" (ex-message e))
                         nil))]
      (if-let [node (try (node-conn/get-node conn (platform-be/be-get-world-safe tile))
                       (catch Exception e
                         (log/debug "[get-linked-node] get-node exception:" (ex-message e))
                         nil))]
        node
        (do
          (log/debug "[get-linked-node] connection found but node tile not resolved for gen at" pos-str)
          nil))
      (do
        (log/debug "[get-linked-node] no connection found for generator at" pos-str)
        nil))))
