(ns cn.li.ac.block.phase-gen.logic
  "Phase Generator business logic: tick, inventory helpers, GUI open."
  (:require [cn.li.ac.block.machine.container :as machine-container]
            [cn.li.ac.block.machine.matter-unit :as matter-unit]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.phase-gen.config :as phase-config]
            [cn.li.ac.block.phase-gen.schema :as phase-schema]
            [cn.li.ac.energy.operations :as energy]))

(def ^:private phase-rt
  (machine-runtime/schema-runtime phase-schema/phase-gen-schema :server-only? true))

(def phase-state-schema (:server-schema phase-rt))
(def phase-default-state (:default-state phase-rt))
(def phase-scripted-load-fn (:load-fn phase-rt))
(def phase-scripted-save-fn (:save-fn phase-rt))

(defn- maybe-charge-output-item [state]
  (let [slot phase-config/output-slot
        stack (get-in state [:inventory slot])
        cur (double (get state :energy 0.0))]
    (if (and stack (energy/is-energy-item-supported? stack) (pos? cur))
      (let [item-cur (double (energy/get-item-energy stack))
            item-max (double (energy/get-item-max-energy stack))
            need (max 0.0 (- item-max item-cur))
            ;; TileGeneratorBase.tryChargeStack caps per-tick charging at the
            ;; generator's bandwidth (LATENCY_MK1 = 50 IF/tick).
            amount (min cur need (double phase-config/default-bandwidth))
            leftover (double (energy/charge-energy-to-item stack amount false))
            accepted (max 0.0 (- amount leftover))]
        (if (pos? accepted)
          (assoc state :energy (- cur accepted))
          state))
      state)))

(defn- calc-generation [state]
  (let [liquid (double (max 0 (int (get state :liquid-amount 0))))
        current-energy (double (get state :energy 0.0))
        max-energy (double (get state :max-energy (phase-config/max-energy)))
        energy-room (max 0.0 (- max-energy current-energy))
        gen-per-mb (double (phase-config/gen-per-mb))
        max-drain-by-config (double (phase-config/liquid-consume-per-tick))
        max-drain-by-energy (if (pos? gen-per-mb)
                              (/ energy-room gen-per-mb)
                              0.0)
        drain (int (Math/floor (max 0.0 (min liquid max-drain-by-config max-drain-by-energy))))
        gen (* (double drain) gen-per-mb)]
    {:drain drain :gen gen}))

(defn phase-tick-state
  [state _level _pos _block-state _be]
  ;; Original AcademyCraft order (TileGeneratorBase.update → TilePhaseGen.update):
  ;; 1) Drain liquid → generate energy (this tick uses liquid from previous round)
  ;; 2) Consume matter unit → add liquid (available for NEXT tick)
  ;; 3) Charge output energy item
  (machine-runtime/advance-tick! state)
  (let [state-prep (-> state
                       (assoc :tank-size (int (phase-config/tank-size))
                              :max-energy (double (phase-config/max-energy))))
        ;; Step 1: Generate energy from liquid currently in tank
        {:keys [drain gen]} (calc-generation state-prep)
        cur-energy (double (get state-prep :energy 0.0))
        liquid-before (int (get state-prep :liquid-amount 0))
        state-gen (-> state-prep
                      (assoc :energy (+ cur-energy gen))
                      (assoc :liquid-amount (max 0 (- liquid-before drain)))
                      (assoc :gen-speed (double gen))
                      (assoc :status (cond
                                       (<= liquid-before 0) "NO_LIQUID"
                                       (pos? gen) "GENERATING"
                                       :else "IDLE")))
        ;; Step 2: Consume phase-liquid matter unit → add liquid for next tick
        state-post (matter-unit/convert-phase-unit-state
                     state-gen
                     {:liquid-in-slot phase-config/liquid-in-slot
                      :liquid-out-slot phase-config/liquid-out-slot
                      :liquid-per-unit (phase-config/liquid-per-unit)
                      :tank-size (phase-config/tank-size)
                      :matter-unit-item-id phase-config/matter-unit-item-id
                      :max-output-stack 16})]
    ;; Step 3: Charge energy to output slot item
    (maybe-charge-output-item state-post)))

(def phase-tick-fn
  (machine-runtime/make-tick-fn
    {:default-state phase-default-state
     :tick-state phase-tick-state
     ;; render.clj reads :liquid-amount directly (texture frame 0-4).
     :sync-client? true}))

(defn- can-place? [_be ^long slot item _face]
  (case slot
    0 (matter-unit/phase-liquid-unit? item phase-config/matter-unit-item-id)
    2 (energy/is-energy-item-supported? item)
    false))

(defn- can-take? [_be ^long slot _item _face]
  (or (= slot phase-config/liquid-out-slot)
      (= slot phase-config/output-slot)))

(def phase-container-fns
  (machine-container/make-inventory-container-fns
    {:default-state phase-default-state
     :slot-count phase-config/total-slots
     :can-place? can-place?
     :can-take? can-take?}))

(def open-phase-gen-gui!
  (machine-runtime/make-open-gui-handler :phase-gen))
