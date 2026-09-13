(ns cn.li.ac.gui.block-gui-reactive
  "Shared reactive block GUI helper — single migration path for all ~10 block GUIs.
   Replaces the tech-ui-common + assemble-tech-ui-root + per-GUI boilerplate pattern.

   Usage per block GUI (e.g. solar_gen):
     (require '[cn.li.ac.gui.block-gui-reactive :as bgui])
     (defn create-screen [container menu player]
       (bgui/create-screen
         {:page-xml \"guis/rework/new/page_solar.xml\"
          :texture-name \"solar\"
          :container container
          :menu menu
          :histograms [(bgui/hist-buffer energy-fn max-energy-fn)]
          :properties {:gen_speed speed-fn :status status-fn}
          :wireless? true
          :wireless-role :generator}))"
  (:require
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.gui.tech-ui-tabs-reactive :as tech-tabs]
            [cn.li.ac.wireless.gui.tab-reactive :as wireless-tab]
            [cn.li.ac.gui.info-area-reactive :as info-area]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.xml :as ui-xml]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcmod.ui.node INode]))

;; ============================================================================
;; Config API (replaces tech-ui-common constructors)
;; ============================================================================

(defn hist-buffer
  "Generic value bar. Optional :label/:color/:desc-fn override the defaults
   (upstream TechUI.histBuffer defaults)."
  ([value-fn max-fn] (hist-buffer value-fn max-fn {}))
  ([value-fn max-fn {:keys [label color desc-fn]}]
   {:type :buffer :value-fn value-fn :max-fn max-fn
    :color (or color 0xFFCC8844) :label label :desc-fn desc-fn}))

(defn hist-energy [color]
  {:type :energy :color (or color 0xFF4488CC)})

(defn hist-capacity [color]
  {:type :capacity :color (or color 0xFF44CC88)})

(defn- inv-page-xml [page-xml]
  (or page-xml "guis/rework/new/page_inv.xml"))

(defn- wireless-pages [page-xml]
  [{:id tech-tabs/inv-tab-id :xml (inv-page-xml page-xml)}
   {:id "wireless" :xml "guis/rework/new/page_wireless.xml"}])

(defn update-signals!
  "Read container atoms, sset signals. Called each frame by screen host.
   :properties text is rendered via attach-histograms-and-properties!'s own
   live-bound rows (see create-screen) — no longer duplicated here."
  [{:keys [signals container]}]
  (let [safe-val #(some-> % deref)]
    (sig/sset-d! (:energy signals) (double (or (safe-val (:energy container)) 0.0)))
    (sig/sset-d! (:max-energy signals) (max 1.0 (double (or (safe-val (:max-energy container)) 1.0))))
    (sig/sset-o! (:status signals) (or (safe-val (:status container)) "IDLE"))
    (sig/sset-o! (:gen-speed signals)
                 (format "%.2fIF/T" (double (or (safe-val (:gen-speed container)) 0.0))))
    (sig/sset-d! (:progress signals) (double (or (safe-val (:progress container)) 0.0)))))

(defn- screen-config [r signals container menu properties histograms
                      current-tab-atom tech-ui custom-bind! update-fn]
  {:type :reactive-container-screen
   :runtime r
   :signals signals
   :container container
   :menu menu
   :properties properties
   :histograms histograms
   :size-dx 31   ;; AcademyCraft ContainerUI: xSize += 31 (176+31=207 total)
   :size-dy 21   ;; 187 (screen-root h) - 166 (vanilla default)
   :update-fn update-fn
   :current-tab-atom current-tab-atom
   :tech-ui tech-ui
   :custom-bind! custom-bind!})

;; ============================================================================
;; Screen creation (replaces tech-ui/assemble-tech-ui-root)
;; ============================================================================

(defn- wrap-with-info-area [child-spec]
  {:kind :group
   :props {:id :screen-root :w 290.0 :h 187.0}
   :children [;; Left-anchor the machine page so the background's slot-holes line
              ;; up with the vanilla slots (drawn at guiLeft + schema-x). A raw XML
              ;; page's <Ui>/main nodes are center-aligned; without an anchor they
              ;; center inside the 290-wide screen-root (the info-area adds ~114px
              ;; on the right), shifting the background ~57px right of the slots.
              ;; Width 172 (not 176) matches the wireless tabbed-root's page-group
              ;; exactly: a 176-wide page centered in 172 lands at x=-2, the same
              ;; origin node/phase/solar render at; anchoring at 176 would leave
              ;; the page 2px right of those (and of the slots).
              {:kind :group
               :props {:id :page-anchor :x 0.0 :y 0.0 :w 172.0 :h 187.0}
               :children [child-spec]}
              {:kind :nine-slice
               :props {:id :info-area-bg :x 179.0 :y 5.0 :w 100.0 :h 177.0
                       :margin 4.0
                       :src (modid/asset-path "textures" "guis/blend_quad")
                       :line-tex (modid/asset-path "textures" "guis/line")}}
              {:kind :group
               :props {:id :info-area :x 179.0 :y 5.0 :w 100.0 :h 177.0 :clip? true}}]})

;; ============================================================================
;; Histogram/property rendering — was previously accepted as config and
;; silently discarded (histograms: never consumed anywhere at all; properties:
;; called each frame by update-signals! but the return value was thrown away).
;; Renders into the auto-built :info-area sidebar via the already-live
;; info-area-reactive.clj rows (each row binds its own per-frame Binding, so
;; it actually refreshes — unlike a bare put-user-signal! computed).
;; ============================================================================

(defn- histogram-elems
  "Build the add-histogram! elems for one histogram config."
  [container h]
  (case (:type h)
    :buffer (let [default-desc (fn [] (format "%.0f" (double ((:value-fn h)))))]
              [{:label (or (:label h) "Value")
                :color (or (:color h) 0xFF25C4FF)
                :value-fn (:value-fn h)
                ;; :max-fn not :max — evaluated per frame like :value-fn, so
                ;; a max that syncs in late (DataSlot) still scales the bar
                ;; correctly instead of freezing the ratio at attach time.
                :max-fn (:max-fn h)
                :desc-fn (or (:desc-fn h) default-desc)}])
    :energy (let [value-fn (fn [] (double (or @(:energy container) 0.0)))]
              [{:label "Energy"
                :color 0xFF25C4FF
                :value-fn value-fn
                :max-fn (fn [] (max 1.0 (double (or @(:max-energy container) 1.0))))
                :desc-fn (fn [] (format "%.0f IF" (double (value-fn))))}])
    :capacity (let [load-fn (fn [] (double (or @(:load container) 0.0)))
                    max-fn (fn [] (max 1.0 (double (or @(:max-capacity container) 1.0))))]
                [{:label "Load"
                  :color 0xFFFF6C00
                  :value-fn load-fn
                  :max-fn max-fn
                  :desc-fn (fn [] (str (long (load-fn)) "/" (long (max-fn))))}])
    []))

(defn- attach-histograms-and-properties!
  [r histograms properties container]
  (let [ctx (info-area/clear-area! r)]
    ;; Upstream TechUI.infoPage.histogram(elems*): ONE 210×210×0.4 frame with
    ;; bars side by side (bar x = 56 + idx*40), then one property row per elem
    ;; — the phase generator and imag fusor show TWO bars (energy + liquid)
    ;; in that single frame. A per-histogram add-histogram! call stacked
    ;; separate frames vertically and a second frame (88px + rows ≈ 98px)
    ;; overflowed the 177px info area, clipping the second bar.
    (info-area/add-histogram!
      ctx
      (mapcat (partial histogram-elems container) histograms))
    (when (and (seq histograms) (seq properties)) (info-area/add-sepline! ctx "Info"))
    (doseq [[k f] properties] (info-area/add-property! ctx (name k) f))))

(defn create-screen
  "Create reactive tech-ui screen config.
   Options:
   :page-xml      - resource path to XML layout
   :texture-name  - ui_block texture suffix
   :container     - container atom map
   :menu          - Minecraft container menu
   :player        - opening player (for info-area auth)
   :info-area?    - attach side info panel (matrix/node)
   :histograms    - [(hist-buffer ...) ...]
   :properties    - {:key value-fn ...}
   :wireless?     - include wireless tab
   :wireless-role - :generator or :machine
   :update-fn     - per-frame update override (default update-signals!) — a
                    GUI that needs per-frame work (e.g. re-asserting an
                    editable input's focus) must wrap it here; an orphan
                    computed signal is never evaluated."
  [{:keys [page-xml texture-name container menu player histograms properties wireless? wireless-role
            info-area? custom-bind! update-fn]}]
  (let [r (rt/create-runtime)
        pages (when wireless? (wireless-pages page-xml))
        base-spec (if wireless?
                    (tech-tabs/build-tabbed-root-spec pages)
                    (ui-xml/load-spec (modid/namespaced-path (inv-page-xml page-xml))))
        ;; Auto-build the info-area sidebar when the caller supplied histograms/
        ;; properties but didn't already request one (matrix/node build+populate
        ;; their own via custom-bind!, so don't double-populate for those).
        auto-info-area? (and (not info-area?) (or (seq histograms) (seq properties)))
        spec (if (or info-area? auto-info-area?) (wrap-with-info-area base-spec) base-spec)
        _ (rt/build! r spec)
        _ (when auto-info-area? (attach-histograms-and-properties! r histograms properties container))
        ;; :ui_block exists in page_inv.xml (background texture swapped per block
        ;; type); page_wireless.xml and other custom layouts may omit it.
        ;; Direct set (not bind!): the texture is static, and bind! only fires on
        ;; a signal *change* — a static signal-o never pushes its initial value,
        ;; so a bound src is never written and ui_block would keep its XML default
        ;; (ui_phasegen.png). set-prop! writes immediately.
        _ (when (rt/node-by-id r :ui_block)
            (ui/set-prop! r :ui_block :src
              (modid/asset-path "textures"
                (str "guis/ui/ui_" (or texture-name "inv") ".png"))))
        signals {:energy (sig/signal-d 0.0)
                 :max-energy (sig/signal-d 1.0)
                 :status (sig/signal-o "IDLE")
                 :gen-speed (sig/signal-o "0IF/T")
                 :progress (sig/signal-d 0.0)}
        _ (doseq [[k s] signals] (rt/put-user-signal! r k s))
        current-tab-atom (when wireless? (atom tech-tabs/inv-tab-id))
        wireless-attached? (when wireless? (atom false))
        tech-ui (when wireless?
                  (tech-tabs/attach-tab-ui! r pages current-tab-atom container menu
                    {:on-switch
                     (fn [tab-id]
                       (when (and (= tab-id "wireless")
                                  (compare-and-set! wireless-attached? false true))
                         (wireless-tab/attach-panel!
                           r {:role wireless-role
                              :container container
                              :menu menu
                              :defer-initial-rebuild? false})))}))
        _ (when custom-bind! (custom-bind! r container menu player signals))
        _ (when wireless? (rt/put-user-signal! r :wireless-role (sig/signal-o (name wireless-role))))]
    (screen-config r signals container menu properties histograms
             current-tab-atom tech-ui custom-bind!
             (or update-fn update-signals!))))

;; ============================================================================
;; Open screen (replaces create-screen-fn + registration)
;; ============================================================================

(defn open!
  "Open reactive block GUI screen via bridge."
  [{:keys [runtime]} title]
  (bridge/open-reactive-screen! runtime (or title "Machine")))
