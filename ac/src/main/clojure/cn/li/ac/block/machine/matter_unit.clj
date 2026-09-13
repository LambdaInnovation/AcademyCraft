(ns cn.li.ac.block.machine.matter-unit
  "Matter unit item helpers shared by phase-gen and imag-fusor."
  (:require [cn.li.ac.block.machine.inventory-stack :as inv]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.structured-data :as sd]))

(defn matter-unit-kind
  [stack matter-unit-item-id]
  (when (and (not (inv/stack-empty? stack))
             (= (inv/stack-id stack) matter-unit-item-id))
    (let [tag (try (pitem/custom-data stack) (catch Exception _ nil))
          tag-kind (when tag (try (sd/get-string tag "matterKind") (catch Exception _ nil)))]
      (or (case (some-> tag-kind str)
            "none" :none
            "phase-liquid" :phase-liquid
            nil)
          (case (int (try (pitem/damage stack) (catch Exception _ -1)))
            0 :none
            1 :phase-liquid
            nil)))))

(defn phase-liquid-unit?
  [stack matter-unit-item-id]
  (= :phase-liquid (matter-unit-kind stack matter-unit-item-id)))

(defn empty-matter-unit?
  [stack matter-unit-item-id]
  (= :none (matter-unit-kind stack matter-unit-item-id)))

(defn make-empty-matter-unit
  [matter-unit-item-id none-meta count]
  (let [stack (pitem/stack-by-id matter-unit-item-id (int count))]
    (when stack
      ;; Empty unit: NO matterKind NBT (damage 0 means empty). A "none" tag
      ;; made machine-produced empties NBT-incompatible with plain ones, so
      ;; two empty stacks refused to merge (they swapped on drag instead).
      (try
        (pitem/set-damage! stack none-meta)
        (catch Exception _ nil))
      stack)))

(defn can-output-empty-unit?
  [output-unit matter-unit-item-id max-stack]
  (or (inv/stack-empty? output-unit)
      (and (empty-matter-unit? output-unit matter-unit-item-id)
           (< (inv/stack-count output-unit) (int max-stack)))))

(defn convert-phase-unit-state
  "Pure state step: consume one phase-liquid unit from input slot, add liquid, output empty unit."
  [state {:keys [liquid-in-slot liquid-out-slot liquid-per-unit tank-size
                matter-unit-item-id max-output-stack]}]
  (let [in-unit (get-in state [:inventory liquid-in-slot])
        out-unit (get-in state [:inventory liquid-out-slot])
        liquid (int (get state :liquid-amount 0))
        tank-size (int (get state :tank-size tank-size))
        can-consume? (and (phase-liquid-unit? in-unit matter-unit-item-id)
                          (pos? (inv/stack-count in-unit))
                          (<= (+ liquid liquid-per-unit) tank-size))
        can-output? (can-output-empty-unit? out-unit matter-unit-item-id max-output-stack)]
    (if (and can-consume? can-output?)
      (let [new-input (inv/consume-stack in-unit 1)
            add-count (if (inv/stack-empty? out-unit) 1 (inc (inv/stack-count out-unit)))
            new-output (or (when-not (inv/stack-empty? out-unit)
                             (inv/rebuild-stack out-unit add-count))
                           (make-empty-matter-unit matter-unit-item-id 0 add-count))]
        (-> state
            (assoc :liquid-amount (+ liquid liquid-per-unit))
            (assoc-in [:inventory liquid-in-slot] new-input)
            (assoc-in [:inventory liquid-out-slot] new-output)))
      state)))
