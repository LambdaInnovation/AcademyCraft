(ns cn.li.ac.block.machine-wireless-coverage-test
  "Every machine's wireless capability wiring, checked against upstream's class
  hierarchy. The whole wireless system (network transfer, GUI linking) and
  CurrentCharging's \"is this a chargeable energy block\" test all resolve
  through cn.li.ac.wireless.core.capability-lookup, which only looks at the
  tile spec's :capability-keys — a machine missing its key is invisible to all
  of them, silently. That is exactly how the metal former ended up unable to
  receive energy from anything.

  Upstream mapping (AcademyCraft cn.academy.block.tileentity):
    TileNode                  implements IWirelessNode      -> :wireless-node
    TileMatrix                implements IWirelessMatrix    -> :wireless-matrix
    TileReceiverBase subclasses  (IWirelessReceiver)         -> :wireless-receiver
      TileDeveloper, TileImagFusor, TileMetalFormer, AbilityInterferer
    TileGeneratorBase subclasses (IWirelessGenerator)        -> :wireless-generator
      TileSolarGen, TileWindGenBase, TilePhaseGen, TileCatEngine
    TileWindGenMain (TileInventory) / TileWindGenPillar / TileImagPhase
      hold no wireless interface -> no key."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.block.ability-interferer.block :as interferer]
            [cn.li.ac.block.cat-engine.block :as cat-engine]
            [cn.li.ac.block.developer.block :as developer]
            [cn.li.ac.block.imag-fusor.block :as imag-fusor]
            [cn.li.ac.block.metal-former.block :as metal-former]
            [cn.li.ac.block.phase-gen.block :as phase-gen]
            [cn.li.ac.block.solar-gen.block :as solar-gen]
            [cn.li.ac.block.wind-gen.block :as wind-gen]
            [cn.li.ac.block.wireless-matrix.block :as wireless-matrix]
            [cn.li.ac.block.wireless-node.block :as wireless-node]
            [cn.li.mcmod.block.tile-dsl :as tdsl]))

(def ^:private expected-capability-key
  {"wireless-node-basic"    :wireless-node
   "wireless-node-standard" :wireless-node
   "wireless-node-advanced" :wireless-node
   "wireless-matrix"        :wireless-matrix
   "developer-normal"       :wireless-receiver
   "developer-advanced"     :wireless-receiver
   "imag-fusor"             :wireless-receiver
   "metal-former"           :wireless-receiver
   "ability-interferer"     :wireless-receiver
   "solar-gen"              :wireless-generator
   "wind-gen-base"          :wireless-generator
   "phase-gen"              :wireless-generator
   "cat-engine"             :wireless-generator})

(defn- init-all-machines! []
  (interferer/init-ability-interferer!)
  (cat-engine/init-cat-engine!)
  (developer/init-developer!)
  (imag-fusor/init-imag-fusor!)
  (metal-former/init-metal-former!)
  (phase-gen/init-phase-gen!)
  (solar-gen/init-solar-gen!)
  (wind-gen/init-wind-gen!)
  (wireless-matrix/init-wireless-matrix!)
  (wireless-node/init-wireless-nodes!))

(deftest every-wireless-machine-declares-its-capability-key-test
  (init-all-machines!)
  (doseq [[tile-id cap-key] expected-capability-key]
    (let [spec (tdsl/get-tile tile-id)]
      (is (some? spec) (str tile-id " tile spec must be registered"))
      (is (contains? (:capability-keys spec #{}) cap-key)
          (str tile-id " must declare " cap-key
               " — without it capability-lookup resolves nothing, so the"
               " wireless network cannot reach it and CurrentCharging treats"
               " it as a non-energy block")))))

(deftest non-wireless-tiles-declare-no-wireless-capability-test
  (init-all-machines!)
  ;; The wind gen's rotor head and pillar carry no energy upstream (only the
  ;; base does), so granting them a generator capability would let the network
  ;; pull from parts that hold nothing.
  (doseq [tile-id ["wind-gen-main" "wind-gen-pillar"]]
    (when-let [spec (tdsl/get-tile tile-id)]
      (is (not (contains? (:capability-keys spec #{}) :wireless-generator))
          (str tile-id " is not the energy-bearing tile upstream")))))
