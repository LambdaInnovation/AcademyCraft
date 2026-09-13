(ns cn.li.ac.tutorial.client.preview-reactive
  "Complete reactive replacement for tutorial/client/preview.clj.
   ViewGroup × SubView navigation data/state (build-view-groups and all
   pure navigation functions) is reused verbatim. Only the CGUI widget
   builders (create-icon-preview, create-recipe-preview, load-recipe-widget,
   etc.) are replaced with native node specs.

   :block-3d and :item-3d views render through the :preview-3d kind, which
   reproduces upstream's perspective preview camera (FOV 50, Y turntable)
   rather than flattening them into 2D item icons."
  (:require [clojure.string :as str]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.terminal.catalog :as terminal-catalog]
            [cn.li.mcmod.client.platform-bridge :as platform-bridge]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Localized display text — upstream GuiTutorial ViewGroup.getDisplayText
;; resolves via LocalHelper (ac.tutorial.crafting + stack display name) and
;; item names; the port uses the same tutorial.* keys through the platform
;; i18n layer, falling back to the previous English literals when a lang
;; entry is missing (tests run without a translate fn).
;; ============================================================================

(defn- translate-or-fallback
  "Translate k; nil when the platform echoes the key back (no lang entry)."
  [k]
  (let [v (i18n/translate k)]
    (when (and (string? v) (not= v k) (not (str/blank? v)))
      v)))

(defn- registry-path
  "Strip the namespace from an id string ('academy:wafer' -> 'wafer')."
  [item-id]
  (let [s (str item-id)
        i (str/index-of s ":")]
    (if i (subs s (inc i)) s)))

(defn- localized-name
  "Localized display name for an item/block id via the standard
   item/block.<MOD-ID>.<path> lang keys (upstream stack.getDisplayName), or
   nil when neither entry exists."
  [item-id]
  (let [path (registry-path item-id)]
    (some translate-or-fallback
          [(str "item." modid/MOD-ID "." path)
           (str "block." modid/MOD-ID "." path)])))

(defn- craft-text
  "Localized 'Crafting: %s' for a display name — upstream localCraft() uses
   the ac.tutorial.crafting key formatted with the stack display name.
   Returns nil for a nil name (callers fall back to an English literal)."
  [display-name]
  (let [k (str "tutorial." modid/MOD-ID ".crafting")]
    (cond
      (nil? display-name) nil
      (translate-or-fallback k) (i18n/translate k display-name)
      :else (str "Crafting: " display-name))))

(defn- craft-display-text
  "Localized craft tooltip for an item/block id, falling back to the
   previous English literal when no item/block lang entry exists."
  [item-id fallback]
  (or (craft-text (localized-name item-id))
      (craft-text fallback)))

(defn- view-text
  "Localized tag tooltip for a :view group — tutorial.<MOD-ID>.view.<slug>.
   Falls back to the English literal when the lang entry is missing (e.g.
   stale generated lang files) instead of echoing the raw key."
  [slug fallback]
  (or (translate-or-fallback (str "tutorial." modid/MOD-ID ".view." (name slug)))
      fallback))

(defn- app-display-name
  "Localized app name. Upstream AppRegistry lang entries use the bare
   app.<MOD-ID>.<id> key; freq_transmitter's sits at a .name suffix."
  [app]
  (let [base (str "app." modid/MOD-ID "."
                  (str/replace (name (:id app)) "-" "_"))]
    (or (translate-or-fallback base)
        (translate-or-fallback (str base ".name"))
        (:name app))))

;; ============================================================================
;; ViewGroup data — reused verbatim from preview.clj
;; ============================================================================

(defn- query-recipes? [item-id]
  (try
    (platform-bridge/has-recipes? item-id)
    (catch Throwable e
      (log/warn "Recipe query failed for" item-id (ex-message e))
      false)))

(defn- recipe-kind->query-kind [kind]
  (case kind
    "ImagFusor" :imag-fusor
    "MetalFormer" :metal-former
    "Smelting" :smelting
    "Crafting" :crafting
    :crafting))

(def ^:private recipe-kinds
  "Query kind → display kind, in upstream RecipeHandler.recipeOfStack order:
   vanilla crafting, then ImagFusor, then MetalFormer, then smelting."
  [[:crafting "Crafting"]
   [:imag-fusor "ImagFusor"]
   [:metal-former "MetalFormer"]
   [:smelting "Smelting"]])

(defn- all-recipe-views
  "Every recipe producing `item-id`, one entry per recipe, ACROSS all kinds.

   Upstream RecipeHandler.recipeOfStack concatenates the vanilla crafting,
   ImagFusor, MetalFormer and smelting hits into one Widget[] — an item made
   both in a Metal Former and on a crafting table gets two sub-views, which is
   what gives the preview's left/right arrows something to page through.

   Returns [[kind-name {:input [item-id...] :count n}] ...]. `find-recipes` is
   asked first purely to skip the kinds that have nothing, so the common
   one-machine item costs two queries rather than four."
  [item-id]
  (try
    (let [available (or (platform-bridge/find-recipes item-id) {})]
      (into []
            (mapcat (fn [[query-kind kind-name]]
                      (when (seq (get available query-kind))
                        (map (fn [recipe] [kind-name recipe])
                             (platform-bridge/all-recipes-for item-id query-kind)))))
            recipe-kinds))
    (catch Throwable e
      (log/warn "Recipe query failed for" item-id (ex-message e))
      nil)))

(defn- expand-recipe-sub-views
  "For a view group that has :recipe or :crafting-grid sub-views, query all
   matching recipes and create one sub-view per recipe variant.  Multiple
   recipes stay as sub-views within the same view group — the left/right
   arrows switch between them (matching upstream AcademyCraft's
   RecipeHandler.recipeOfStack() → ViewGroup.getSubViews() array).

   The :recipe-kind declared in build-view-groups is only a guess at which
   machine makes the item; each sub-view takes the kind of the recipe it
   actually shows, so a group can mix e.g. a MetalFormer and a Crafting page."
  [vg]
  (let [svs (:sub-views vg)
        sv (first svs)
        sv-type (:type sv)]
    (if (and (= 1 (count svs))
             (or (= :recipe sv-type) (= :crafting-grid sv-type)))
      (if-let [found (seq (all-recipe-views (:item-id sv)))]
        (assoc vg :sub-views
               (mapv (fn [[kind-name recipe]]
                       (assoc sv :type :recipe
                                 :recipe-kind kind-name
                                 :recipe-input (:input recipe)
                                 :recipe-count (:count recipe)))
                     found))
        ;; No recipe found → fall back to 3D item view
        (assoc vg :sub-views [(assoc sv :type :item-3d)]))
      ;; Non-recipe groups pass through unchanged
      vg)))

(defn build-view-groups
  "Build the ViewGroup list for a tutorial, matching original AcademyCraft
   TutorialInit definitions. Each group = {:tag :view|:craft :display-text ...
   :sub-views [{:type ...}]}"
  [tut-id]
  (let [raw (case tut-id
    :ores
    [{:tag :view :display-text (view-text :constraint_metal "Constraint Metal Ore")
      :sub-views [{:type :block-3d :block-id (modid/namespaced-path "constraint_metal")}]}
     {:tag :view :display-text (view-text :imagsil_ore "Imag Silicon Ore")
      :sub-views [{:type :block-3d :block-id (modid/namespaced-path "imagsil_ore")}]}
     {:tag :view :display-text (view-text :crystal_ore "Crystal Ore")
      :sub-views [{:type :block-3d :block-id (modid/namespaced-path "crystal_ore")}]}
     {:tag :view :display-text (view-text :reso_ore "Resonant Crystal Ore")
      :sub-views [{:type :block-3d :block-id (modid/namespaced-path "reso_ore")}]}
     {:tag :view :display-text (view-text :phase_liquid "Phase Liquid")
      :sub-views [{:type :icon
                   :texture (modid/asset-path "textures/item" "phase_liquid_mat.png")
                   :tag :view}]}
     {:tag :craft :display-text (craft-display-text (modid/namespaced-path "constraint_plate") "Constraint Plate")
      :sub-views [{:type :recipe :recipe-kind "MetalFormer"
                  :item-id (modid/namespaced-path "constraint_plate")}]}
     {:tag :craft :display-text (craft-display-text (modid/namespaced-path "imag_silicon_ingot") "Imag Silicon Ingot")
      :sub-views [{:type :recipe :recipe-kind "MetalFormer"
                  :item-id (modid/namespaced-path "imag_silicon_ingot")}]}
     {:tag :craft :display-text (craft-display-text (modid/namespaced-path "wafer") "Wafer")
      :sub-views [{:type :recipe :recipe-kind "MetalFormer"
                  :item-id (modid/namespaced-path "wafer")}]}
     {:tag :craft :display-text (craft-display-text (modid/namespaced-path "imag_silicon_piece") "Imag Silicon Piece")
      :sub-views [{:type :recipe :recipe-kind "MetalFormer"
                  :item-id (modid/namespaced-path "imag_silicon_piece")}]}]

    :phase_generator
    [{:tag :craft :display-text (craft-display-text (modid/namespaced-path "phase_gen") "Phase Generator")
      :sub-views [(if (query-recipes? (modid/namespaced-path "phase_gen"))
                    {:type :recipe :recipe-kind "ImagFusor" :item-id (modid/namespaced-path "phase_gen")}
                    {:type :item-3d :item-id (modid/namespaced-path "phase_gen")})]}]

    :solar_generator
    [{:tag :craft :display-text (craft-display-text (modid/namespaced-path "solar_gen") "Solar Generator")
      :sub-views [(if (query-recipes? (modid/namespaced-path "solar_gen"))
                    {:type :recipe :recipe-kind "Smelting" :item-id (modid/namespaced-path "solar_gen")}
                    {:type :item-3d :item-id (modid/namespaced-path "solar_gen")})]}]

    :wind_generator
    ;; Display names come from the blocks' own lang keys
    ;; (block.<MOD-ID>.windgen_*), which the datagen supplies from the
    ;; corresponding block registrations; the literals below are only the
    ;; English fallback when a lang entry is missing.
    (vec (for [[item-id fallback]
               [[(modid/namespaced-path "windgen_base") "Wind Generator Pillar Base"]
                [(modid/namespaced-path "windgen_pillar") "Wind Generator Pillar"]
                [(modid/namespaced-path "windgen_main") "Wind Generator"]
                [(modid/namespaced-path "windgen_fan") "Wind Generator Fan"]]]
           {:tag :craft :display-text (craft-display-text item-id fallback)
            :sub-views [{:type :recipe :recipe-kind "MetalFormer"
                        :item-id item-id}]}))

    :imag_fusor
    [{:tag :craft :display-text (craft-display-text (modid/namespaced-path "imag_fusor") "Imag Fusor")
      :sub-views [{:type :recipe :recipe-kind "ImagFusor"
                  :item-id (modid/namespaced-path "imag_fusor")}]}]

    :metal_former
    [{:tag :craft :display-text (craft-display-text (modid/namespaced-path "metal_former") "Metal Former")
      :sub-views [{:type :recipe :recipe-kind "MetalFormer"
                  :item-id (modid/namespaced-path "metal_former")}]}]

    :terminal
    (let [base [{:tag :craft :display-text (craft-display-text (modid/namespaced-path "terminal_installer") "Terminal Installer")
                 :sub-views [{:type :recipe :recipe-kind "Smelting"
                             :item-id (modid/namespaced-path "terminal_installer")}]}]
          apps (try (terminal-catalog/ordered-apps)
                    (catch Throwable e
                      (log/warn "Failed to load terminal catalog; app tutorial entries may be missing:" (ex-message e))
                      nil))
          app-groups (mapv (fn [app]
                            (let [app-installer-id (modid/namespaced-path (str "app_"
                                                       (str/replace (name (:id app)) "-" "_")))
                                  has-recipe? (query-recipes? app-installer-id)
                                  name (app-display-name app)]
                              {:tag (if has-recipe? :craft :view)
                               :display-text (if has-recipe? (craft-text name) name)
                               :sub-views [(if has-recipe?
                                             {:type :recipe
                                              :recipe-kind "Crafting"
                                              :item-id app-installer-id}
                                             {:type :icon
                                              :texture (:icon app)
                                              :item-id (name (:id app))})]}))
                          (remove :pre-installed? (or apps [])))]
      (into base app-groups))

    :ability_developer
    [{:tag :craft :display-text (craft-display-text (modid/namespaced-path "developer_portable") "Portable Developer")
      :sub-views [{:type :recipe :recipe-kind "MetalFormer"
                  :item-id (modid/namespaced-path "developer_portable")}]}
      {:tag :craft :display-text (craft-display-text (modid/namespaced-path "dev_normal") "Normal Developer")
       :sub-views [{:type :recipe :recipe-kind "MetalFormer"
                    :item-id (modid/namespaced-path "dev_normal")}]}
     {:tag :craft :display-text (craft-display-text (modid/namespaced-path "dev_advanced") "Advanced Developer")
      :sub-views [{:type :recipe :recipe-kind "MetalFormer"
                   :item-id (modid/namespaced-path "dev_advanced")}]}]

    [])
      expanded (mapv expand-recipe-sub-views raw)]
    expanded))

;; ============================================================================
;; Preview node specs — native replacement for build-preview-widget.
;; All positioned to exactly fill the 134×134 "area" node (abs computed by
;; the caller — see tutorial_reactive.clj's showWindow layout), matching the
;; CENTER/CENTER + scale math from the original XML/tutorial_windows.xml.
;; ============================================================================

(def ^:private recipe-bg
  {"ImagFusor" (modid/asset-path "textures/guis" "tutorial_fusor.png")
   "MetalFormer" (modid/asset-path "textures/guis" "tutorial_metalformer.png")
   "Smelting" (modid/asset-path "textures/guis" "tutorial_smelting.png")})

;; [x y w h scale] within the 134×134 area, CENTER/CENTER-aligned raw texture
;; size from tutorial_windows.xml (ImagFusor 196×128×0.6, MetalFormer
;; 192×192×0.5, Smelting 192×128×0.6).
(def ^:private recipe-geom
  {"ImagFusor" [8.2 28.6 196.0 128.0 0.6]
   "MetalFormer" [19.0 19.0 192.0 192.0 0.5]
   "Smelting" [9.4 28.6 192.0 128.0 0.6]})

(def ^:private crafting-grid-bg
  (modid/asset-path "textures/guis" "tutorial/crafting_grid.png"))

;; ============================================================================
;; Recipe slot layout — slot positions within the 134×134 preview area,
;; hand-derived from tutorial_windows.xml slot coords × scale + recipe-geom offset.
;; ============================================================================

(def ^:private recipe-slots
  "Per recipe-kind: list of {:id :x :y :w :h :role} for each slot within the
   134×134 preview area.  :role is :input or :output."
  {"ImagFusor"    [{:id :slot-in  :x 19.6  :y 66.1  :w 19.2 :h 18.6 :role :input}
                   {:id :slot-out :x 96.4  :y 66.1  :w 19.2 :h 18.6 :role :output}]
   "MetalFormer"  [{:id :slot-in  :x 24.65 :y 63.25 :w 12.5 :h 12.5 :role :input}
                   {:id :slot-out :x 96.65 :y 63.25 :w 12.5 :h 12.5 :role :output}]
   "Smelting"     [{:id :slot-in  :x 27.4  :y 54.52 :w 19.2 :h 19.2 :role :input}
                   {:id :slot-out :x 83.38 :y 54.52 :w 19.2 :h 19.2 :role :output}]})

(defn- recipe-preview-spec
  "Build a recipe preview spec: machine background + item renders in slots.
   Uses recipe data from the sub-view (pre-queried by expand-recipe-sub-views)
   or falls back to platform-bridge/first-recipe-for for single-recipe views."
  [view id]
  (let [kind (:recipe-kind view)
        item-id (:item-id view)
        input-items (or (:recipe-input view)
                        (try (when-let [r (platform-bridge/first-recipe-for
                                           item-id (recipe-kind->query-kind kind))]
                               (:input r))
                             (catch Throwable _ nil))
                        [])
        output-item (or (:item-id view) item-id)]
    (if (= kind "Crafting")
      ;; Crafting table: 196×128 widget scaled 0.6 placed at ImagFusor offset.
      ;; Slot coords from upstream RecipeHandler.CraftingGridDisplay:
      ;;   STEP=43, input pos(5+col*43, 5+row*43), StackDisplay 32×34
      ;;   output pos(153, 49), StackDisplay 32×34
      ;; Scaled by 0.6 + offset [8.2, 28.6] into 134×134 preview.
      (let [step (* 43.0 0.6)      ;; 25.8
            base-x (+ 8.2 (* 5.0 0.6))  ;; 11.2
            base-y (+ 28.6 (* 5.0 0.6)) ;; 31.6
            slot-w (* 32.0 0.6)   ;; 19.2
            slot-h (* 34.0 0.6)   ;; 20.4
            out-x (+ 8.2 (* 153.0 0.6)) ;; 100.0
            out-y (+ 28.6 (* 49.0 0.6)) ;; 58.0
            slot-children
            (into []
              (keep-indexed
                (fn [i input-id]
                  (when input-id
                    (let [col (mod i 3) row (quot i 3)]
                      {:kind :preview-item
                       :props {:id (keyword (str (name id) "-in-" i))
                               :x (+ base-x (* col step))
                               :y (+ base-y (* row step))
                               :w slot-w :h slot-h
                               :item-id (str input-id)}})))
                input-items))]
        {:kind :group :props {:id id :x 0.0 :y 0.0 :w 134.0 :h 134.0}
         :children (into [{:kind :image :props {:id (keyword (str (name id) "-bg"))
                                                 :x 8.2 :y 28.6 :w 196.0 :h 128.0 :scale 0.6
                                                 :src crafting-grid-bg}}]
                         (conj slot-children
                               (when output-item
                                 {:kind :preview-item
                                  :props {:id (keyword (str (name id) "-out"))
                                          :x out-x :y out-y :w slot-w :h slot-h
                                          :item-id (str output-item)}})))})
      ;; Machine GUI: background image + items in defined slot positions
      (let [[x y w h scale] (get recipe-geom kind [0.0 0.0 134.0 134.0 1.0])
            bg-src (get recipe-bg kind)
            slots (get recipe-slots kind [])
            slot-children
            (into []
              (keep (fn [slot]
                      (let [slot-item (case (:role slot)
                                        :input (first input-items)
                                        :output output-item
                                        nil)]
                        (when slot-item
                          {:kind :preview-item
                           :props {:id (keyword (str (name id) "-" (name (:id slot))))
                                   :x (:x slot) :y (:y slot) :w (:w slot) :h (:h slot)
                                   :item-id (str slot-item)}}))))
                  slots)]
        {:kind :group :props {:id id :x 0.0 :y 0.0 :w 134.0 :h 134.0}
         :children (into [{:kind :image :props {:id (keyword (str (name id) "-bg"))
                                                 :x x :y y :w w :h h :scale scale
                                                 :src bg-src}}]
                         slot-children)}))))

(defn build-preview-spec
  "Build a native node spec for a single sub-view, positioned relative to
   the 134×134 preview \"area\" origin (i.e. x/y are area-local, caller adds
   the area's own absolute offset when placing this as a child)."
  [view id]
  (case (:type view)
    :icon
    ;; Original renders a 1×1 textured quad centered with scale=1 in a 0.75×
    ;; perspective viewport — ~75% of preview area.  Reactive uses orthographic
    ;; so render at 64×64 centered in the 134×134 area.
    (let [icon-size 64.0
          offset (/ (- 134.0 icon-size) 2.0)]
      {:kind :image :props {:id id :x offset :y offset :w icon-size :h icon-size
                             :src (:texture view)}})

    :recipe
    (recipe-preview-spec view id)

    :crafting-grid
    (recipe-preview-spec view id)

    :block-3d
    {:kind :preview-3d :props {:id id :x 0.0 :y 0.0 :w 134.0 :h 134.0
                                :render-type :block
                                :block-id (str (:block-id view))
                                :rotation-speed 1.0 :scale 0.8 :y-offset 0.0}}

    :item-3d
    {:kind :preview-3d :props {:id id :x 0.0 :y 0.0 :w 134.0 :h 134.0
                                :render-type :item
                                :item-id (str (:item-id view))
                                :rotation-speed 0.0 :scale 1.0 :y-offset 0.0}}

    {:kind :box :props {:id id :x 0.0 :y 0.0 :w 134.0 :h 134.0 :fill 0x00000000}}))

;; ============================================================================
;; Navigation state — two-level (ViewGroup × SubView), reused verbatim
;; ============================================================================

(defn create-preview-state [tut-id]
  (let [groups (build-view-groups tut-id)]
    {:view-groups groups :group-index 0 :sub-indices {}}))

(defn current-view-group [state*]
  (let [{:keys [view-groups group-index]} @state*
        idx (or group-index 0)
        i (max 0 (min (dec (max 1 (count view-groups))) idx))]
    (when (seq view-groups)
      (nth view-groups i nil))))

(defn- current-group-index [state*]
  (or (:group-index @state*) 0))

(defn current-sub-view [state*]
  (when-let [vg (current-view-group state*)]
    (let [svs (or (:sub-views vg) [])
          idx (get (:sub-indices @state*) (current-group-index state*) 0)
          i (max 0 (min (dec (max 1 (count svs))) idx))]
      (when (seq svs)
        (nth svs i nil)))))

(defn cycle-sub-view!
  "Advance the CURRENT view-group's own remembered sub-view index (wrapping).
   Upstream GuiTutorial: each ViewGroup's PreviewInfo.viewIndex is independent
   and persists across tag switches for the life of the open tutorial entry —
   only a fresh tutorial entry resets it. Tracked per group-index here so
   switch-view-group! (a tag click) never disturbs another group's position."
  [state* direction]
  (when-let [vg (current-view-group state*)]
    (let [cnt (count (or (:sub-views vg) []))]
      (if (zero? cnt)
        0
        (let [gi (current-group-index state*)
              cur (get (:sub-indices @state*) gi 0)
              delta (if (= direction :next) 1 -1)
              new-idx (mod (+ cur delta) cnt)]
          (swap! state* assoc-in [:sub-indices gi] new-idx)
          new-idx)))))

(defn switch-view-group!
  "Change which view-group (tag) is active. Does NOT reset :sub-indices —
   each group keeps its own remembered sub-view position (see cycle-sub-view!)."
  [state* group-index]
  (swap! state* assoc :group-index group-index)
  group-index)

(def tag-textures
  {:craft (modid/asset-path "textures/guis" "icons/icon_craft.png")
   :smelt (modid/asset-path "textures/guis" "icons/icon_smelt.png")
   :view  (modid/asset-path "textures/guis" "icons/icon_view.png")})

(defn current-tag [state*]
  (or (:tag (current-view-group state*)) :view))

(defn display-text [state*]
  (or (:display-text (current-view-group state*)) ""))
