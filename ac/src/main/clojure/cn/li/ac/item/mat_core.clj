(ns cn.li.ac.item.mat-core
  "Matrix Core - main component for Wireless Matrix

  Different damage values represent different tiers:
  - Damage 0: Tier 1 (Basic)
  - Damage 1: Tier 2 (Improved)
  - Damage 2: Tier 3 (Advanced)
  - Damage 3: Tier 4 (Ultimate)"
  (:require [cn.li.ac.block.wireless-matrix.stats :as matrix-stats]
            [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.platform.item :as item]
            [clojure.string :as str]))

;; ============================================================================
;; Matrix Core Tiers
;; ============================================================================

(def core-tiers
  {:tier-1 {:damage 0
            :name "基础矩阵核心"}
   :tier-2 {:damage 1
            :name "改进矩阵核心"}
   :tier-3 {:damage 2
            :name "高级矩阵核心"}
   :tier-4 {:damage 3
            :name "终极矩阵核心"}})


(def ^:private core-item-ids ["mat_core_0" "mat_core_1" "mat_core_2"])
(def ^:private full-matrix-plate-count 3)

(defn- format-stat
  [value]
  (let [d (double value)
        rounded (Math/round d)]
    (if (< (Math/abs (- d (double rounded))) 1.0e-9)
      (str rounded)
      (format "%.1f" d))))

(defn matrix-core-tooltip
  "Build config-backed tooltip text for a full Wireless Matrix using `level` core."
  [level]
  (let [{:keys [capacity bandwidth range]}
        (matrix-stats/stats-for-counts full-matrix-plate-count level full-matrix-plate-count)]
    [(str "等级: " (int level))
     (str "容量倍率: " (format-stat capacity))
     (str "带宽倍率: " (format-stat bandwidth))
     (str "范围倍率: " (format-stat range))]))

;; Tier 4 disabled - missing mat_core_3.png texture
;; (idsl/defitem mat-core-tier-4 ...)

;; ============================================================================
;; Helper Functions
;; ============================================================================

(defn is-mat-core?
  "Check whether a platform ItemStack is a matrix core."
  [item-stack]
  (when item-stack
    (let [item-obj (try (item/object item-stack) (catch Throwable _ nil))
          registry-name (when item-obj
                          (try (item/registry-name item-obj) (catch Throwable _ nil)))
          desc (when item-obj
                 (try (str (item/description-id item-obj)) (catch Throwable _ nil)))
          item-id (or registry-name (when desc (last (str/split desc #"\\."))))
          result (boolean
                   (some (fn [spec-id]
                           (or (= item-id spec-id)
                               (and (string? desc) (str/includes? desc spec-id))))
                         core-item-ids))]
      ;; (try
      ;;   (log/debug "is-mat-core?" {:stack-class (class item-stack)
      ;;                                :desc desc
      ;;                                :derived-id item-id
      ;;                                :result result})
      ;;   (catch Throwable _))
      result)))

(defn get-core-level
  "Get core level from ItemStack (1-3)
  Returns 0 if not a core or empty"
  [item-stack]
  (if (and item-stack (is-mat-core? item-stack))
    (+ 1 (item/damage item-stack))
    0))

(defn init-mat-cores! []
  (install/framework-once! ::mat-cores-installed?
  (fn []
    (idsl/register-item!
      (idsl/create-item-spec
        "mat_core_0"
        {:max-stack-size 1
         :creative-tab :misc
         :max-damage 0
         :properties {:tooltip (matrix-core-tooltip 1)
                      :display-name (get-in core-tiers [:tier-1 :name])
                      :model-texture "mat_core_0"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "mat_core_1"
        {:max-stack-size 1
         :creative-tab :misc
         :max-damage 1
         :properties {:tooltip (matrix-core-tooltip 2)
                      :display-name (get-in core-tiers [:tier-2 :name])
                      :model-texture "mat_core_1"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "mat_core_2"
        {:max-stack-size 1
         :creative-tab :misc
         :max-damage 2
         :properties {:tooltip (matrix-core-tooltip 3)
                      :display-name (get-in core-tiers [:tier-3 :name])
                      :model-texture "mat_core_2"}}))
    (log/debug "Matrix Cores initialized: 3 tiers"))))
