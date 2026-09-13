(ns cn.li.ac.gui.tech-ui-reactive
  "Reactive TechUI toolkit — replaces cn.li.ac.gui.tech-ui-common.
   Signal-driven: no on-frame polling, no per-widget atoms, no find-widget+set-texture!."
  (:require [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.dsl :as dsl]
            [cn.li.mcmod.ui.xml :as ui-xml]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.anim :as anim]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.util.log :as log]
            [clojure.string :as str]))

(def gui-width 172)
(def gui-height 187)

;; ============================================================================
;; Inventory page — load XML, attach signals
;; ============================================================================

(defn create-inventory-page
  "Create reactive inventory page. Loads page_inv.xml, sets UI texture by name.
   Returns {:id :window :runtime rt :page-node ^INode}"
  [name]
  (try
    (let [r (rt/create-runtime)
          spec (ui-xml/load-spec (modid/namespaced-path "guis/rework/new/page_inv.xml"))
          root-idx (rt/build! r spec)
          ;; Set ui_block texture via signal
          tex-path (sig/signal-o (modid/asset-path "textures" (str "guis/ui/ui_" name ".png")))]
      (ui/bind! r :ui_block :src tex-path)  ;; image src signal
      {:id "inv" :runtime r :page-node (rt/node-by-idx r root-idx)})
    (catch Exception e
      (log/stacktrace "Error creating reactive inventory page" e)
      {:id "inv" :runtime (rt/create-runtime) :page-node nil})))

(defn create-rework-page
  "Create a reactive TechUI page from XML resource."
  ([resource-path]
   (create-rework-page "inv" resource-path))
  ([page-id resource-path]
   (let [r (rt/create-runtime)
         spec (ui-xml/load-spec (if (str/starts-with? resource-path "assets/")
                                  resource-path
                                  (modid/namespaced-path (str "guis/" resource-path))))]
     (rt/build! r spec)
     {:id page-id :runtime r})))

;; ============================================================================
;; Info area helpers — return signal-backed specs
;; ============================================================================

(defn add-histogram
  "Create histogram nodes bound to buffer signals.
   hist-specs: [(hist-buffer val-fn max-fn)] or [(hist-energy ...) (hist-capacity ...)]
   Returns {:nodes [...] :y next-y :signals [...]}"
  [specs start-y]
  {:nodes (map-indexed (fn [i {:keys [label value-fn max-fn color]}]
                         (dsl/group {:id (str "hist-" i) :x 10 :y (+ start-y (* i 20)) :w 150 :h 18}
                           (dsl/text {:text (str label) :x 0 :y 0 :font-size 10 :color 0xFFCCCCCC})
                           (dsl/progress {:x 40 :y 6 :w 106 :h 8})))
                       specs)
   :y (+ start-y (* 20 (count specs)))})

(defn add-sepline
  "Add separator line."
  [label start-y]
  {:node (dsl/text {:text (str "-- " label " --") :x 10 :y start-y :font-size 10 :color 0xFF888888})
   :y (+ start-y 14)})

(defn add-property
  "Add property text node. For interactive info-area rows use info-area-reactive/add-property!."
  [label value-fn start-y]
  {:node (dsl/text {:text (str label ": ...") :x 10 :y start-y :font-size 10 :color 0xFFAAAAAA})
   :y (+ start-y 14)})

;; ============================================================================
;; Histogram constructors
;; ============================================================================

(defn hist-energy [color]
  {:label "Energy" :value-fn nil :max-fn nil :color (or color 0xFF4488CC)})

(defn hist-capacity [color]
  {:label "Capacity" :value-fn nil :max-fn nil :color (or color 0xFF44CC88)})

(defn hist-buffer [value-fn max-fn]
  {:label "Buffer" :value-fn value-fn :max-fn max-fn :color 0xFFCC8844})

;; ============================================================================
;; TechUI screen assembly
;; ============================================================================

(defn assemble-tech-ui-root
  "Assemble TechUI root from pages + container + info area specs.
   {:pages [...] :container container :build-info-area! (fn [specs] ...)}
   Returns {:runtime rt :root ^INode}"
  [{:keys [pages container build-info-area!]}]
  (let [r (rt/create-runtime)
        ;; Build root group
        spec (dsl/group {:id :root :w gui-width :h gui-height :align-w :center :align-h :middle})
        ;; TODO: tab composition via :list node
        _ (rt/build! r spec)]
    {:runtime r
     :root (rt/node-by-idx r 0)
     :container container}))

;; ============================================================================
;; Screen creation from root
;; ============================================================================

(defn create-tech-screen-from-root
  "Create screen wrapper — returns map with :runtime :root :size-dx :size-dy"
  [^cn.li.mcmod.uipojo.runtime.UiRt rt _current menu]
  {:runtime rt
   :root (rt/node-by-idx rt 0)
   :minecraft-container menu
   :size-dx 31
   :size-dy 20})

(defn create-tech-screen-container
  "Create container-backed TechUI screen.
   {:pages [...] :container container :minecraft-container menu :build-info-area! fn}
   Returns compatible map for gui-reg screen-fn."
  [{:keys [pages container minecraft-container build-info-area!]}]
  (let [assembled (assemble-tech-ui-root {:pages pages
                                           :container container
                                           :build-info-area! build-info-area!})]
    (assoc assembled
           :minecraft-container minecraft-container
           :size-dx 31
           :size-dy 20)))
