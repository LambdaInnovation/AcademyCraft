(ns cn.li.ac.gui.info-area-reactive
  "Reactive TechUI info-area — replaces tech-ui-common add-property/add-sepline/add-button."
  (:require
            [clojure.string :as str]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.slot-write :as slot-write]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.events :as events])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

(def ^:private row-h 10.0)
;; Upstream TechUI numbers (expectWidth 100): rows are (100-10)=90 wide at
;; x=6, keyLength=40 labels, valueArea size(40,8) at x=40 — the editable
;; value's visible window is exactly 40px (5 chars @ font 8) before scrolling.
(def ^:private label-w 40.0)
(def ^:private row-w 90.0)
(def ^:private value-w 40.0)
(def ^:private idle-color 0xFFFFFFFF)
(def ^:private edit-color 0xFF2180D8)

(defn ensure-shell!
  "Ensure :info-area group exists on runtime (sibling to machine panel).
   Background uses :nine-slice matching upstream BlendQuad (blend_quad.png 3×3 + line.png borders)."
  [^UiRt rt]
  (when-not (ui/node rt :info-area)
    (let [root (rt/node-by-idx rt 0)]
      (rt/build-child! rt
        {:kind :nine-slice
         :props {:id :info-area-bg :x 179.0 :y 5.0 :w 100.0 :h 177.0
                 :margin 4.0
                 :src (modid/asset-path "textures" "guis/blend_quad")
                 :line-tex (modid/asset-path "textures" "guis/line")}}
        root)
      (rt/build-child! rt
        {:kind :group
         :props {:id :info-area :x 179.0 :y 5.0 :w 100.0 :h 177.0 :clip? true}}
        root)))
  nil)

(defn- area-node [^UiRt rt]
  (or (ui/node rt :info-area)
      (throw (ex-info "info-area node missing — call ensure-shell! first" {}))))

(defn clear-area!
  "Clear info-area children. Returns fresh build context."
  [^UiRt rt]
  (ensure-shell! rt)
  (rt/clear-children! rt (area-node rt))
  {:rt rt :seq (atom 0) :y (atom 0.0)})

(defn- next-id! [ctx] (keyword (str "ia-" (swap! (:seq ctx) inc))))

(defn- advance! [ctx & [dy]] (swap! (:y ctx) + (or dy row-h)) @(:y ctx))

(def ^:private gui-label-sections ["prop" "sep" "hist"])

(defn- translate-label
  "Resolve a short info-area label to its gui.academy.common.<section>.<tail>
  translation key and localize it. Callers pass key tails (\"owner\", \"Info\",
  \"Energy\", \"mode\") matching the datagen keys; labels without a key fall
  through unchanged."
  [label]
  (let [tail (-> (str label) str/trim str/lower-case
                 (str/replace #"[\s-]+" "_"))
        key (some (fn [section]
                    (let [k (str "gui.academy.common." section "." tail)]
                      (when (not= (i18n/translate k) k) k)))
                  gui-label-sections)]
    (if key (i18n/translate key) (str label))))

(defn add-blank!
  "Insert a blank row (upstream InfoPage.blank(n) — e.g. spacing between the
  INIT password field and the INIT button)."
  [ctx & [rows]]
  (advance! ctx (* row-h (double (or rows 1))))
  nil)

(defn add-sepline!
  [ctx label]
  (let [^UiRt rt (:rt ctx)
        y @(:y ctx)
        id (next-id! ctx)
        ;; Upstream sepline: (expectWidth-3, 8) at pos(3,0).
        spec {:kind :text
              :props {:id id :x 3.0 :y y :w (- 100.0 3.0) :h 8.0
                      :text (str "-- " (translate-label label) " --")
                      :font-size 6.0 :color 0x99FFFFFF}}]
    (rt/build-child! rt spec (area-node rt))
    (advance! ctx 11.0)))

(defn- wire-editable-value!
  [^UiRt rt ^INode value-n editable? on-change color-change?]
  (when editable?
    (let [last-confirmed (volatile! nil)]
      ;; Upstream TechUI InfoPage.property submits the editCallback ONLY on
      ;; ConfirmInputEvent (Enter); ChangeContentEvent just tints the field
      ;; edit-color and blur does nothing. Remove the lost-focus auto-confirm
      ;; — matrix/node ssid+password edits apply on Enter, not on blur.
      (rt/register-event! rt (.getIdx value-n) :confirm-input
        (fn [_ _ evt]
          (let [v (:value evt)]
            (when (and on-change (not= v @last-confirmed))
              (vreset! last-confirmed v)
              (on-change v)))
          (when color-change?
            (ui/set-node-prop! rt value-n :color idle-color))))
      ;; Restore edit-color when the field gains focus (re-click after blur)
      (when (and editable? color-change?)
        (rt/register-event! rt (.getIdx value-n) :gain-focus
          (fn [_ _ _]
            (ui/set-node-prop! rt value-n :color edit-color))))
      (when (and editable? color-change?)
        (rt/register-event! rt (.getIdx value-n) :change-content
          (fn [_ _ _]
            (ui/set-node-prop! rt value-n :color edit-color)))))))

(defn add-property!
  "Add label + value row. value may be string or (fn [] string) for live updates.
   Returns {:value-node n} when :editable? true."
  [ctx label value & {:keys [editable? masked? on-change color-change?]
                       :or {color-change? true}}]
  (let [^UiRt rt (:rt ctx)
        y @(:y ctx)
        row-id (next-id! ctx)
        label-id (keyword (str (name row-id) "-label"))
        value-id (keyword (str (name row-id) "-value"))
        value-color (if editable? edit-color idle-color)
        ;; AcademyCraft-style [ ] brackets flanking the editable value
        ;; (upstream: box("[").pos(-4,0), box("]").pos(valueArea.width + 2, 0)
        ;; — children of the 40px valueArea at x=40, so [ lands at 36 and
        ;; ] at 82 row-local; both stay inside the 90-wide row).
        brackets (when editable?
                   [{:kind :text
                     :props {:id (keyword (str (name row-id) "-lb"))
                             :x (- label-w 4.0) :y 0.0 :w 10.0 :h row-h
                             :text "[" :font-size 8.0 :color idle-color}}
                    {:kind :text
                     :props {:id (keyword (str (name row-id) "-rb"))
                             :x (+ label-w value-w 2.0) :y 0.0 :w 10.0 :h row-h
                             :text "]" :font-size 8.0 :color idle-color}}])
        row-spec {:kind :group
                  :props {:id row-id :x 6.0 :y y :w row-w :h row-h}
                  :children (vec
                              (concat
                                [{:kind :text
                                  :props {:id label-id :x 0.0 :y 0.0 :w label-w :h row-h
                                          :text (translate-label label) :font-size 8.0 :color 0xFFAAAAAA}}
                                 {:kind :text
                                  :props (cond-> {:id value-id
                                                  :x label-w :y 0.0 :w value-w :h row-h
                                                  :text (if (fn? value) (value) (str value))
                                                  :font-size 8.0 :color value-color
                                                  :editable? (boolean editable?)}
                                           masked? (assoc :masked? true))}]
                                (or brackets ())))}
        ^INode row (rt/build-child! rt row-spec (area-node rt))
        ^INode value-n (ui/item-node row value-id)]
    (when (fn? value)
      (let [writer (slot-write/resolve-sig-writer (get node/kinds :text) :text)
            live (sig/computed-o [(rt/clock-ms-sig rt)]
                    (fn [_] (str (value))))
            b (sig/bind! live value-n writer (rt/get-dirty-bindings-q rt))]
        (rt/register-binding! rt (.getIdx value-n) b)))
    (wire-editable-value! rt value-n editable? on-change color-change?)
    (advance! ctx)
    (when editable? {:value-node value-n})))

(defn add-button!
  [ctx label on-click]
  (let [^UiRt rt (:rt ctx)
        y @(:y ctx)
        btn-id (next-id! ctx)
        spec {:kind :text
              :props {:id btn-id :x 6.0 :y y :w 98.0 :h row-h
                      :text label :font-size 8.0 :color 0xFF88CCFF}}]
    (rt/build-child! rt spec (area-node rt))
    (events/on! rt btn-id :left-click (fn [_ _ _] (on-click)))
    (advance! ctx 12.0)))

(def ^:private histogram-tex (modid/asset-path "textures" "guis/histogram.png"))

(defn- write-box-height!
  "Binding writer: set a box's height to full-h × pct (source ISigD 0..1). With
   :align-h :bottom the box grows UPWARD from its parent's bottom edge — a
   vertical fill bar."
  [^double full-h ^INode node source]
  (let [pct (max 0.0 (min 1.0 (double (.dGet ^cn.li.mcmod.uipojo.signal.ISigD source))))
        h (* full-h pct)]
    (when-not (== h (.getH node))
      (.setH node h)
      (.setFlag node node/FLAG-LAYOUT-DIRTY))))

(defn add-histogram!
  "AcademyCraft-exact histogram (TechUI.histogram + histProperty): the
   guis/histogram.png frame scaled 0.4, one vertical fill-up bar per elem (bar
   value clamped to [0.03,1] like upstream), then a per-elem property row of
   label + colored icon + live value text.
   elems: [{:label :color :value-fn (fn->raw) :max :desc-fn (fn->string)}]."
  [ctx elems]
  (let [^UiRt rt (:rt ctx)
        y @(:y ctx)
        scale 0.4
        bar-h 120.0
        hist-id (next-id! ctx)
        fill-id (fn [idx] (keyword (str (name hist-id) "-b" (long idx))))
        bars (vec (mapcat
                    (fn [idx elem]
                      [{:kind :group
                        :props {:id (keyword (str (name hist-id) "-r" (long idx)))
                                :x (+ 56.0 (* (double idx) 40.0)) :y 78.0 :w 16.0 :h bar-h}
                        :children [{:kind :box
                                    :props {:id (fill-id idx) :x 0.0 :y 0.0 :w 16.0 :h 0.0
                                            :fill (:color elem) :align-h :bottom}}]}])
                    (range) elems))
        ;; Upstream InfoPage.histogram runs blank(-30) BEFORE element(), but
        ;; reset() starts elemY at 10 — so the first frame's page offset is
        ;; 10 - 30 = -20 (its top 20px clipped, bars at frame-y 78×0.4 ≈ 31
        ;; land near the page top). Our y cursor starts at 0, so the pull-up
        ;; is -20, not -30 — a -30 frame renders the bars 10px higher and
        ;; clips the top further than upstream.
        frame-spec {:kind :group
                    :props {:id hist-id :x 8.0 :y (- y 20.0) :w 210.0 :h 210.0 :scale scale}
                    :children (into [{:kind :image
                                      :props {:id (keyword (str (name hist-id) "-frame"))
                                              :x 0.0 :y 0.0 :w 210.0 :h 210.0 :src histogram-tex}}]
                                    bars)}]
    (rt/build-child! rt frame-spec (area-node rt))
    (doseq [[idx elem] (map-indexed vector elems)]
      (when-let [^INode bar (rt/node-by-id rt (fill-id idx))]
        (let [vf (:value-fn elem)
              mf (:max-fn elem)
              ;; The bar ratio divides by a max. A static :max is captured once
              ;; here; a :max-fn is evaluated every frame like :value-fn. Live
              ;; max matters when the real maximum arrives asynchronously (e.g.
              ;; the node's :max-capacity DataSlot syncs after the info-area is
              ;; built) — a stale cap of 1.0 renders the bar at 100% while the
              ;; text row already shows the correct "2/8".
              static-cap (when-not mf (max 1.0 (double (:max elem))))
              pct-sig (sig/computed-d [(rt/clock-ms-sig rt)]
                        (fn [_]
                          (let [cap (or static-cap (max 1.0 (double (mf))))
                                pct (/ (double (vf)) cap)]
                            (max 0.03 (min 1.0 pct)))))]
          (rt/register-binding! rt (.getIdx bar) (sig/bind! pct-sig bar (partial write-box-height! bar-h) (rt/get-dirty-bindings-q rt))))))
    ;; Upstream element() advances by height × scale (84), but the frame was
    ;; pulled up 20px — its bottom sits at cursor+64, and the property rows
    ;; must start there (a plain 84 advance leaves a 20px gap between the
    ;; frame and the text, exactly the upstream offset).
    (advance! ctx (- (* 210.0 scale) 20.0))
    ;; per-elem property row: label + colored icon + live value text (histProperty)
    (doseq [elem elems]
      (let [ry @(:y ctx)
            rid (next-id! ctx)
            val-id (keyword (str (name rid) "-v"))
            df (:desc-fn elem)
            row-spec {:kind :group :props {:id rid :x 6.0 :y ry :w 98.0 :h 8.0}
                      :children
                      [{:kind :text :props {:id (keyword (str (name rid) "-l"))
                                            :x 0.0 :y 0.0 :w 30.0 :h 8.0
                                            :text (translate-label (:label elem)) :font-size 8.0 :color 0xFFCCCCCC}}
                       {:kind :box :props {:id (keyword (str (name rid) "-i"))
                                           :x 33.0 :y 1.0 :w 6.0 :h 6.0 :fill (:color elem)}}
                       {:kind :text :props {:id val-id :x 43.0 :y 0.0 :w 55.0 :h 8.0
                                            :text (str (df)) :font-size 8.0 :color 0xFFFFFFFF}}]}
            ^INode row (rt/build-child! rt row-spec (area-node rt))
            ^INode val-n (ui/item-node row val-id)
            writer (slot-write/resolve-sig-writer (get node/kinds :text) :text)
            live (sig/computed-o [(rt/clock-ms-sig rt)] (fn [_] (str (df))))
            b (sig/bind! live val-n writer (rt/get-dirty-bindings-q rt))]
        (rt/register-binding! rt (.getIdx val-n) b)
        (advance! ctx 10.0)))))
