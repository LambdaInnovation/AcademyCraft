(ns cn.li.ac.datagen.worldgen
  "AC worldgen content registration for datagen.
  Registers ore and liquid pool descriptors into the neutral worldgen registry."
  (:require [cn.li.mcmod.worldgen :as mcmod-worldgen]
            [cn.li.ac.config.worldgen :as worldgen-cfg]))

(defn register-datagen-metadata!
  "Register AC worldgen content (ores and phase liquid) into the shared worldgen registry.
  Called during datagen initialization. Generation flags are read from AC config at this point."
  []
  (let [gen-ores? (boolean (try (worldgen-cfg/gen-ores-enabled?) (catch Exception _ true)))
        gen-phase? (boolean (try (worldgen-cfg/gen-phase-liquid-enabled?) (catch Exception _ true)))]
    (mcmod-worldgen/register-worldgen-ore! {:id "constraint_metal" :name "Constraint Metal Ore" :size 12 :count 8 :enabled? gen-ores?})
    (mcmod-worldgen/register-worldgen-ore! {:id "reso_ore" :name "Resonance Ore" :size 9 :count 8 :enabled? gen-ores?})
    (mcmod-worldgen/register-worldgen-ore! {:id "crystal_ore" :name "Crystal Ore" :size 12 :count 12 :enabled? gen-ores?})
    (mcmod-worldgen/register-worldgen-ore! {:id "imagsil_ore" :name "Imag Silicon Ore" :size 11 :count 8 :enabled? gen-ores?})
    ;; 1-in-3 (~33%) — upstream used 30% per chunk; rarity_filter takes integer 1/N, so 3 is closest.
    ;; min-y 5, max-y 34 — upstream: 5 + random.nextInt(30) = [5, 34]
    (mcmod-worldgen/register-worldgen-liquid! {:id "phase_liquid" :name "Phase Liquid Pool"
                                                :rarity 3 :min-y 5 :max-y 34 :enabled? gen-phase?}))
  nil)
