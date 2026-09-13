(ns cn.li.ac.recipe.crafting-recipes
  "Single source of truth for vanilla crafting/smelting recipe metadata.
   Recipes are ported from AcademyCraft 1.12 `default.recipe` and adapted for 1.20."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.schema.recipe :as recipe-schema]))

(defn- m
  [path]
  (str modid/MOD-ID ":" path))

(defn- item
  [id]
  {:item id})

(defn- tag
  [id]
  {:tag id})

(defn- shaped
  [id pattern key result]
  {:id id
   :type :shaped
   :pattern pattern
   :key key
   :result result})

(defn- shapeless
  [id ingredients result]
  {:id id
   :type :shapeless
   :ingredients ingredients
   :result result})

(defn- smelting
  [id ingredient result experience cooking-time]
  {:id id
   :type :smelting
   :ingredient ingredient
   :result result
   :experience experience
   :cooking-time cooking-time})

(defn get-all-recipes
  []
  (let [plate-iron (item (m "reinforced_iron_plate"))
        bed-tag (tag "minecraft:beds")]
    (recipe-schema/require-recipes! :ac-crafting-recipes
      [(shapeless "imag_silicon_piece_from_wafer"
       [(item (m "wafer"))]
       {:item (m "imag_silicon_piece") :count 2})
     (shaped "data_chip_from_plate_iron"
       ["RRR"
        " P "]
       {\R (item "minecraft:redstone")
        \P plate-iron}
       {:item (m "data_chip") :count 1})
     (shaped "data_chip_from_imag_silicon_piece"
       ["RRR"
        " I "]
       {\R (item "minecraft:redstone")
        \I (item (m "imag_silicon_piece"))}
       {:item (m "data_chip") :count 1})
     (shapeless "calc_chip_from_data_chip_and_quartz"
       [(item (m "data_chip"))
        (item "minecraft:quartz")
        (item "minecraft:quartz")]
       {:item (m "calc_chip") :count 1})
     (shapeless "calc_chip_from_data_chip_and_reso_crystal"
       [(item (m "data_chip"))
        (item (m "reso_crystal"))]
       {:item (m "calc_chip") :count 1})
     (shaped "reinforced_iron_plate_from_iron_ingot"
       ["I"
        "I"
        "I"]
       {\I (item "minecraft:iron_ingot")}
       {:item (m "reinforced_iron_plate") :count 2})
     (shaped "machine_frame"
       [" P "
        "PRP"
        " P "]
       {\P plate-iron
        \R (item "minecraft:redstone")}
       {:item (m "machine_frame") :count 1})
     (shaped "phase_gen"
       ["CMC"
        "U U"]
       {\C (item (m "crystal_low"))
        \M (item (m "machine_frame"))
        \U (item (m "matter_unit"))}
       {:item (m "phase_gen") :count 1})
     (shaped "solar_gen"
       ["GGG"
        " W "
        "EME"]
       {\G (item "minecraft:glass_pane")
        \W (item (m "wafer"))
        \E (item (m "energy_convert_component"))
        \M (item (m "machine_frame"))}
       {:item (m "solar_gen") :count 1})
     (shaped "windgen_base"
       ["C"
        "M"
        "E"]
       {\C (item (m "constraint_ingot"))
        \M (item (m "machine_frame"))
        \E (item (m "energy_convert_component"))}
       {:item (m "windgen_base") :count 1})
     (shaped "windgen_pillar"
       ["B"
        "R"
        "B"]
       {\B (item "minecraft:iron_bars")
        \R (item "minecraft:redstone")}
       {:item (m "windgen_pillar") :count 1})
     (shaped "windgen_main"
       [" M "
        "CEC"
        " M "]
       {\M (item (m "machine_frame"))
        \C (item (m "constraint_plate"))
        \E (item (m "energy_convert_component"))}
       {:item (m "windgen_main") :count 1})
     (shaped "windgen_fan"
       [" P "
        "PBP"
        " P "]
       {\P plate-iron
        \B (item "minecraft:iron_bars")}
       {:item (m "windgen_fan") :count 1})
     (shaped "node_basic"
       [" C "
        "IMI"
        "RLR"]
       {\C (item (m "calc_chip"))
        \I (item (m "constraint_ingot"))
        \M (item (m "machine_frame"))
        \R (item (m "crystal_low"))
        \L (item (m "reso_crystal"))}
       {:item (m "node_basic") :count 1})
     (shaped "node_standard"
       [" N "
        "CEC"
        " B "]
       {\N (item (m "crystal_normal"))
        \C (item (m "calc_chip"))
        \E (item (m "energy_convert_component"))
        \B (item (m "node_basic"))}
       {:item (m "node_standard") :count 1})
     (shaped "node_advanced"
       ["P"
        "R"
        "N"]
       {\P (item (m "crystal_pure"))
        \R (item (m "resonance_component"))
        \N (item (m "node_standard"))}
       {:item (m "node_advanced") :count 1})
     (shaped "matter_unit"
       [" C "
        "CGC"
        " C "]
       {\C (item (m "constraint_plate"))
        \G (item "minecraft:glass")}
       {:item (m "matter_unit") :count 4})
     (shaped "energy_unit_from_crystal_low"
       [" C "
        "CLC"
        " D "]
       {\C (item (m "constraint_plate"))
        \L (item (m "crystal_low"))
        \D (item (m "data_chip"))}
       {:item (m "energy_unit") :count 1})
     (shaped "energy_unit_from_crystal_normal"
       [" C "
        "CNC"
        " D "]
       {\C (item (m "constraint_plate"))
        \N (item (m "crystal_normal"))
        \D (item (m "data_chip"))}
       {:item (m "energy_unit") :count 2})
     (shaped "energy_unit_from_crystal_pure"
       [" C "
        "CPC"
        " D "]
       {\C (item (m "constraint_plate"))
        \P (item (m "crystal_pure"))
        \D (item (m "data_chip"))}
       {:item (m "energy_unit") :count 4})
     (shaped "constraint_plate_from_constraint_ingot"
       ["III"]
       {\I (item (m "constraint_ingot"))}
       {:item (m "constraint_plate") :count 2})
     (shaped "terminal_installer"
       ["DGD"
        "PBP"
        "IRI"]
       {\D (item (m "data_chip"))
        \G (item "minecraft:glass_pane")
        \P plate-iron
        \B (item (m "brain_component"))
        \I (item (m "info_component"))
        \R (item "minecraft:redstone_block")}
       {:item (m "terminal_installer") :count 1})
     (shaped "imag_fusor_variant_1"
       ["CLC"
        "DMD"
        "CUC"]
       {\C (item (m "constraint_plate"))
        \L (item (m "crystal_low"))
        \D (item (m "calc_chip"))
        \M (item (m "machine_frame"))
        \U (item (m "matter_unit"))}
       {:item (m "imag_fusor") :count 1})
     (shaped "imag_fusor_variant_2"
       [" L "
        "DME"
        " U "]
       {\L (item (m "crystal_low"))
        \D (item (m "calc_chip"))
        \M (item (m "machine_frame"))
        \E (item (m "energy_convert_component"))
        \U (item (m "matter_unit"))}
       {:item (m "imag_fusor") :count 1})
     (shaped "imag_fusor_variant_3"
       [" L "
        "EMD"
        " U "]
       {\L (item (m "crystal_low"))
        \D (item (m "calc_chip"))
        \M (item (m "machine_frame"))
        \E (item (m "energy_convert_component"))
        \U (item (m "matter_unit"))}
       {:item (m "imag_fusor") :count 1})
     (shaped "metal_former"
       [" S "
        "DMD"
        "CUC"]
       {\S (item "minecraft:shears")
        \D (item (m "calc_chip"))
        \M (item (m "machine_frame"))
        \C (item (m "constraint_plate"))
        \U (item (m "matter_unit"))}
       {:item (m "metal_former") :count 1})
     (shaped "matrix"
       [" R "
        "SFS"
        "DRD"]
       {\R (item (m "reso_crystal"))
        \S (item "minecraft:redstone")
        \F (item (m "machine_frame"))
        \D (item (m "data_chip"))}
       {:item (m "matrix") :count 1})
     (shaped "mat_core_0"
       [" L "
        "CRD"
        " E "]
       {\L (item (m "crystal_low"))
        \C (item (m "calc_chip"))
        \R (item (m "reso_crystal"))
        \D (item (m "data_chip"))
        \E (item (m "energy_convert_component"))}
       {:item (m "mat_core_0") :count 1})
     (shaped "mat_core_1"
       ["R"
        "N"
        "M"]
       {\R (item (m "reso_crystal"))
        \N (item (m "crystal_normal"))
        \M (item (m "mat_core_0"))}
       {:item (m "mat_core_1") :count 1})
     (shaped "mat_core_2"
       ["GGG"
        "RER"
        " M "]
       {\G (item "minecraft:glowstone_dust")
        \R (item (m "reso_crystal"))
        \E (item "minecraft:ender_pearl")
        \M (item (m "mat_core_1"))}
       {:item (m "mat_core_2") :count 1})
     (smelting "imag_silicon_ingot_from_imagsil_ore_smelting"
       (item (m "imagsil_ore"))
       {:item (m "imag_silicon_ingot") :count 1}
       0.8
       200)
     (smelting "constraint_ingot_from_constraint_metal_smelting"
       (item (m "constraint_metal"))
       {:item (m "constraint_ingot") :count 1}
       0.7
       200)
     (smelting "crystal_low_from_crystal_ore_smelting"
       (item (m "crystal_ore"))
       {:item (m "crystal_low") :count 1}
       0.8
       200)
     (shaped "info_component"
       ["G"
        "D"]
       {\G (item "minecraft:glowstone_dust")
        \D (item (m "data_chip"))}
       {:item (m "info_component") :count 1})
     (shaped "brain_component"
       [" G "
        "RCR"
        " G "]
       {\G (item "minecraft:gold_nugget")
        \R (item "minecraft:redstone")
        \C (item (m "calc_chip"))}
       {:item (m "brain_component") :count 1})
     (shaped "resonance_component"
       ["CRC"
        " S "]
       {\C (item (m "constraint_plate"))
        \R (item (m "reso_crystal"))
        \S (item "minecraft:redstone")}
       {:item (m "resonance_component") :count 1})
     (shaped "energy_convert_component"
       ["C"
        "E"
        "R"]
       {\C (item (m "calc_chip"))
        \E (item (m "energy_unit"))
        \R (item (m "reso_crystal"))}
       {:item (m "energy_convert_component") :count 1})
     (shaped "app_skill_tree"
       ["C"
        "D"
        "I"]
       {\C (item "minecraft:compass")
        \D (item (m "data_chip"))
        \I (item (m "info_component"))}
       {:item (m "app_skill_tree") :count 1})
     (shaped "app_media_player"
       ["NNN"
        " D "
        " I "]
       {\N (item "minecraft:note_block")
        \D (item (m "data_chip"))
        \I (item (m "info_component"))}
       {:item (m "app_media_player") :count 1})
     (shaped "app_freq_transmitter"
       ["R"
        "D"
        "I"]
       {\R (item (m "resonance_component"))
        \D (item (m "data_chip"))
        \I (item (m "info_component"))}
       {:item (m "app_freq_transmitter") :count 1})
     (shaped "mag_hook"
       [" P "
        "PPP"
        " P "]
       {\P plate-iron}
       {:item (m "mag_hook") :count 3})
     (shaped "developer_portable"
       ["DGC"
        "BIE"
        "PLP"]
       {\D (item (m "data_chip"))
        \G (item "minecraft:glass_pane")
        \C (item (m "calc_chip"))
        \B (item (m "brain_component"))
        \I (item (m "info_component"))
        \E (item (m "energy_convert_component"))
        \P (item (m "constraint_plate"))
        \L (item (m "crystal_low"))}
       {:item (m "developer_portable") :count 1})
     (shapeless "silbarn_from_imag_silicon_piece"
       [(item (m "imag_silicon_piece"))
        (item (m "imag_silicon_piece"))]
       {:item (m "silbarn") :count 1})
     (shaped "developer_normal_variant_1"
       [" P "
        "UBS"
        "CMR"]
       {\P (item (m "developer_portable"))
        \U (item (m "mat_core_0"))
        \B bed-tag
        \S (item "minecraft:piston")
        \C (item (m "crystal_normal"))
        \M (item (m "machine_frame"))
        \R (item "minecraft:redstone")}
        {:item (m "dev_normal") :count 1})
     (shaped "developer_normal_variant_2"
       ["BIE"
        "UPS"
        "CMR"]
       {\B (item (m "brain_component"))
        \I (item (m "info_component"))
        \E (item (m "energy_convert_component"))
        \U (item (m "mat_core_0"))
        \P bed-tag
        \S (item "minecraft:piston")
        \C (item (m "crystal_normal"))
        \M (item (m "machine_frame"))
        \R (item "minecraft:redstone")}
        {:item (m "dev_normal") :count 1})
     (shaped "developer_advanced"
       ["CCC"
        "GDG"
        "NPR"]
       {\C (item (m "constraint_plate"))
        \G (item "minecraft:glowstone")
        \D (item (m "dev_normal"))
        \N (item (m "node_standard"))
        \P (item (m "crystal_pure"))
        \R (item (m "reso_crystal"))}
        {:item (m "dev_advanced") :count 1})
     (shaped "ability_interferer"
       [" E "
        "BMN"
        " C "]
       {\E (item (m "energy_convert_component"))
        \B (item (m "brain_component"))
        \M (item (m "machine_frame"))
        \N (item "minecraft:note_block")
        \C (item (m "calc_chip"))}
       {:item (m "ability_interferer") :count 1})
     (shapeless "wafer_from_imag_silicon_ingot"
       [(item (m "imag_silicon_ingot"))]
       {:item (m "wafer") :count 1})
     (shapeless "tutorial"
       [(item "minecraft:book")
        (item (m "crystal_low"))]
       {:item (m "tutorial") :count 1})
     (shaped "magnetic_coil"
       ["CRC"
        "CRC"
        "PDP"]
       {\C (item (m "constraint_plate"))
        \R (item (m "reso_crystal"))
        \P plate-iron
        \D (item "minecraft:diamond")}
       {:item (m "magnetic_coil") :count 1})
     ;; ============================================================
     ;; Energy Converter (Energy Bridge) blocks
     ;; Ported from AcademyCraft 1.12 RFSupport / IC2Support recipes
     ;; ============================================================
      (shaped "ac_rf_input"
       ["abc"
        " d "]
       {\a (item (m "energy_unit"))
        \b (item (m "machine_frame"))
        \c (item (m "constraint_plate"))
        \d (item (m "energy_convert_component"))}
        {:item (m "ac_rf_input") :count 1})
      (shaped "ac_rf_output"
       ["abc"
        " d "]
       {\a (item (m "energy_unit"))
        \b (item (m "machine_frame"))
        \c (item (m "reso_crystal"))
        \d (item (m "energy_convert_component"))}
        {:item (m "ac_rf_output") :count 1})
      (shaped "eu_input"
       ["abc"
        " d "]
       {\a (item (m "energy_unit"))
        \b (item (m "machine_frame"))
        \c (item "minecraft:copper_ingot")
        \d (item (m "energy_convert_component"))}
        {:item (m "eu_input") :count 1})
      (shaped "eu_output"
       ["abc"
        " d "]
       {\a (item (m "crystal_low"))
        \b (item (m "machine_frame"))
        \c (item "minecraft:copper_ingot")
        \d (item (m "energy_convert_component"))}
        {:item (m "eu_output") :count 1})
     ;; Reversible Input <-> Output conversion (shapeless)
      (shapeless "ac_rf_input_from_output"
        [(item (m "ac_rf_output"))]
        {:item (m "ac_rf_input") :count 1})
      (shapeless "ac_rf_output_from_input"
        [(item (m "ac_rf_input"))]
        {:item (m "ac_rf_output") :count 1})
      (shapeless "eu_input_from_output"
        [(item (m "eu_output"))]
        {:item (m "eu_input") :count 1})
      (shapeless "eu_output_from_input"
        [(item (m "eu_input"))]
        {:item (m "eu_output") :count 1})])))
