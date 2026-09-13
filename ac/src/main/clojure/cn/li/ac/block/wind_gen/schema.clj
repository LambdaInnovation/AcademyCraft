(ns cn.li.ac.block.wind-gen.schema
  "Wind Generator state schemas aligned with AcademyCraft windgen behavior."
  (:require [cn.li.mcmod.block.inventory-helpers :as inv-helpers]))

;; ============================================================================
;; Wind Generator Main Schema
;; ============================================================================

(def wind-gen-main-schema
  "Main block stores fan inventory + structure/obstacle state."
  [{:key :inventory
    :nbt-key "Inventory"
    :type :inventory
    :default [nil]
    :persist? true
    :client-sync? true
    :gui-sync? false
    :load-fn inv-helpers/load-inventory
    :save-fn inv-helpers/save-inventory}

   {:key :complete
    :nbt-key "Complete"
    :type :boolean
    :default false
    :persist? true
    :gui-sync? true
    :gui-coerce boolean
    ;; render.clj draws the fan from the CLIENT custom-state — without
    ;; :client-sync? the tick never calls sync-to-client, so the client BE
    ;; keeps its default false and the fan never renders.
    :client-sync? true}

   {:key :no-obstacle
    :nbt-key "NoObstacle"
    :type :boolean
    :default false
    :persist? true
    :gui-sync? true
    :gui-coerce boolean
    :client-sync? true}

   {:key :fan-installed
    :nbt-key "FanInstalled"
    :type :boolean
    :default false
    :persist? true
    :gui-sync? true
    :gui-coerce boolean
    :client-sync? true}

   {:key :status
    :nbt-key "Status"
    :type :string
    :default "STOPPED"
    :persist? true
    :gui-sync? true
    :gui-coerce str
    :gui-data-slot? true
    :gui-data-slot-status-codes ["STOPPED" "WEAK" "STRONG"]}

   {:key :wind-change-ticker
    :type :int
    :default 0
    :persist? false}

   {:key :structure-valid
    :type :boolean
    :default false
    :persist? false
    :gui-sync? true
    :gui-coerce boolean}])

;; ============================================================================
;; Wind Generator Base Schema
;; ============================================================================

(def wind-gen-base-schema
  "Base block stores energy, battery slot, and tower completeness state."
  [{:key :energy
    :nbt-key "Energy"
    :type :double
    :default 0.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}

   {:key :max-energy
    :nbt-key "MaxEnergy"
    :type :double
    :default 20000.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}

     {:key :gen-speed
    :nbt-key "GenSpeed"
    :type :double
    :default 0.0
    :persist? true
    :gui-sync? true
    :gui-coerce double
    :gui-data-slot-scale 100}

     {:key :inventory
    :nbt-key "Inventory"
    :type :inventory
    :default [nil]
    :persist? true
    :gui-sync? false
    :load-fn inv-helpers/load-inventory
    :save-fn inv-helpers/save-inventory}

     {:key :completeness
    :nbt-key "Completeness"
    :type :string
    :default "BASE_ONLY"
    :persist? true
    :gui-sync? true
    ;; The base GUI structure icons read this field: it must ride the vanilla
    ;; DataSlot path (gui-data-slot? false + no codec left the client at the
    ;; default "BASE_ONLY" forever). Codes must match the values base-tick-state
    ;; actually stores — (name completeness-kw), lower-case with hyphens
    ;; ("complete"/"no-top"/"base-only") — not the upper-case status strings.
    :gui-coerce str
    :gui-data-slot-status-codes ["base-only" "no-top" "complete" "complete-not-working"]}

   {:key :status
    :nbt-key "Status"
    :type :string
    :default "IDLE"
    :persist? true
    :gui-sync? true
    ;; Same DataSlot fix: the previous codes ("STOPPED"/"WEAK"/"STRONG") never
    ;; matched the actual status values, so any encoded status fell back to 0.
    :gui-coerce str
    :gui-data-slot-status-codes ["IDLE" "BASE_ONLY" "NO_TOP" "COMPLETE" "COMPLETE_NOT_WORKING"]}

   {:key :structure-valid
    :type :boolean
    :default false
    :persist? false
    :gui-sync? true
    :gui-coerce boolean}

   {:key :main-pos-x
    :nbt-key "MainPosX"
    :type :int
    :default 0
    :persist? true}

   {:key :main-pos-y
    :nbt-key "MainPosY"
    :type :int
    :default 0
    :persist? true}

   {:key :main-pos-z
    :nbt-key "MainPosZ"
    :type :int
    :default 0
    :persist? true}])

;; ============================================================================
;; Wind Generator Pillar Schema
;; ============================================================================

(def wind-gen-pillar-schema
  "Pillar block only tracks lightweight structure-check state."
  [{:key :structure-valid
    :type :boolean
    :default false
    :persist? false}

   ])
