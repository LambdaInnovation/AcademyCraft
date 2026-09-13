(ns cn.li.ac.tutorial.registry
  "Immutable tutorial registry — 13 entries exactly matching original AcademyCraft
  TutorialInit definitions (registration order preserved).

  Each entry is a map:
    :id                  keyword    ; tutorial id
    :default-installed?  boolean    ; true → always visible (unconditional)
    :conditions          [cond]     ; empty when default-installed?

  A condition is a map:
    {:type   :item-obtained        ; :item-crafted | :item-smelted | :item-pickup
     :item-id string}              ; runtime item id e.g. \"academy:constraint_metal\"

  Note: energy_bridge.md exists in resources but was NEVER registered in the
  original AcademyCraft TutorialInit.java — we preserve that decision by NOT
  including it here.

  Pattern follows catalog.clj `apps` vector style."
  (:require [cn.li.ac.config.modid :as modid]))

(defn- item [suffix]
  (modid/namespaced-path suffix))

;; --- Tutorial entries ---

(def tutorials
  "Ordered vector of all 13 registered tutorials, matching original
  AcademyCraft TutorialInit.java registration order exactly."
  [{:id :welcome
    :default-installed? true
    :conditions []}

   {:id :ores
    :default-installed? false
    :conditions [{:type :item-obtained :item-id (item "constraint_metal")}
                 {:type :item-obtained :item-id (item "imagsil_ore")}
                 {:type :item-obtained :item-id (item "crystal_ore")}
                 {:type :item-obtained :item-id (item "reso_ore")}]}

   {:id :phase_generator
    :default-installed? false
    :conditions [{:type :item-obtained :item-id (item "phase_gen")}]}

   {:id :solar_generator
    :default-installed? false
    :conditions [{:type :item-obtained :item-id (item "solar_gen")}]}

   {:id :wind_generator
    :default-installed? false
    :conditions [{:type :item-obtained :item-id (item "windgen_base")}
                 {:type :item-obtained :item-id (item "windgen_fan")}
                 {:type :item-obtained :item-id (item "windgen_main")}
                 {:type :item-obtained :item-id (item "windgen_pillar")}]}

   {:id :metal_former
    :default-installed? false
    :conditions [{:type :item-obtained :item-id (item "metal_former")}]}

   {:id :imag_fusor
    :default-installed? false
    :conditions [{:type :item-obtained :item-id (item "imag_fusor")}]}

   {:id :terminal
    :default-installed? false
    :conditions [{:type :item-obtained :item-id (item "terminal_installer")}
                 ;; Dynamically extended in original AC with non-pre-installed
                 ;; app installer items.  Those conditions are added by
                 ;; conditions.clj during initialization (Phase 5).
                 ]}

   {:id :ability_developer
    :default-installed? false
    :conditions [{:type :item-obtained :item-id (item "developer_portable")}
                 {:type :item-obtained :item-id (item "dev_normal")}
                 {:type :item-obtained :item-id (item "dev_advanced")}]}

   {:id :ability_basis
    :default-installed? true
    :conditions []}

   {:id :misc
    :default-installed? true
    :conditions []}

   {:id :develop_ability
    :default-installed? true
    :conditions []}

   {:id :wireless_network
    :default-installed? true
    :conditions []}])

;; --- Queries ---

(def ^:dynamic *show-all?*
  "Debug flag: when true, all tutorials are treated as learned."
  false)

(defn all-tutorials
  "Return the ordered vector of all tutorial entries."
  []
  tutorials)

(defn tutorial-by-id
  "Look up a single tutorial entry by keyword id.  Returns nil when not found."
  [tut-id]
  (some (fn [t] (when (= (:id t) (keyword tut-id)) t))
        tutorials))

(defn group-by-activated
  "Split tutorials into {:learned [...] :unlearned [...]} based on player state.

  A tutorial is considered 'learned' (visible/activated) when:
    - *show-all?* is true (debug mode), OR
    - its :default-installed? is true, OR
    - its :id is present in tutorial-state's :activated-tuts set.

  `tutorial-state` is a map from tutorial/model.clj (fresh-state shape)."
  [tutorial-state]
  (if *show-all?*
    {:learned tutorials :unlearned []}
    (let [activated (set (:activated-tuts tutorial-state))]
    (reduce (fn [acc tut]
              (if (or (:default-installed? tut)
                      (contains? activated (:id tut)))
                (update acc :learned conj tut)
                (update acc :unlearned conj tut)))
            {:learned [] :unlearned []}
            tutorials))))
