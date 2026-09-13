(ns cn.li.ac.terminal.client.apps.settings-reactive
  "Reactive Settings UI aligned with AcademyCraft SettingsUI / settings.xml."
  (:require [cn.li.ac.ability.config :as ability-config]
            [cn.li.ac.config.common :as config-common]
            [cn.li.ac.tutorial.config :as tutorial-config]
            [cn.li.ac.config.gameplay :as gameplay-config]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.terminal.client.apps.ui-customize-reactive :as ui-customize]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.config.registry :as config-reg]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.spi.keybinding-registry :as kb-registry]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.slot-write :as slot-write]
            [cn.li.mcmod.ui.xml :as ui-xml])
  (:import [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.runtime UiRt]))

(def ^:private check-tex-true
  (modid/asset-path "textures/guis" "check_true.png"))

(def ^:private check-tex-false
  (modid/asset-path "textures/guis" "check_false.png"))

;; Upstream t_* templates are all height 60 (settings.xml).
(def ^:private row-h 60.0)
(def ^:private visible-h 720.0)
;; Upstream placeholder between categories: Widget size (10, 20).
(def ^:private category-gap 20.0)

(def ^:private main-w 742.0)
(def ^:private main-h 923.0)
(def ^:private fit-margin 0.92)

;; Scrollbar thumb travel — VerticalDragBar y0=119, y1=760 in settings.xml.
(def ^:private thumb-min-y 119.0)
(def ^:private thumb-max-y 760.0)
(def ^:private thumb-travel (- thumb-max-y thumb-min-y))

;; EditKey colors (PropertyElements.EditKey): idle (200,200,200,200), edit (251,133,37,200).
(def ^:private key-color-idle 0xC8C8C8C8)
(def ^:private key-color-edit 0xC8FB8525)
;; Vanilla Options > Controls marks a conflicting binding red.
(def ^:private key-color-conflict 0xC8FF5555)

(def ^:private props
  ;; :default mirrors AcademyCraft SettingsUI / domain defaults.
  [{:key :attack-player  :prop-id "attackPlayer"  :category "generic" :get ability-config/attack-player-enabled?    :domain config-common/ability-domain  :sp-only? true  :default true}
   {:key :destroy-blocks :prop-id "destroyBlocks" :category "generic" :get ability-config/destroy-blocks-enabled?   :domain config-common/ability-domain  :sp-only? true  :default true}
   {:key :heads-or-tails :prop-id "headsOrTails"  :category "generic" :get tutorial-config/heads-or-tails-enabled?  :domain config-common/tutorial-domain :sp-only? false :default false}
   {:key :use-mouse-wheel :prop-id "useMouseWheel" :category "generic" :get gameplay-config/use-mouse-wheel-enabled? :domain config-common/gameplay-domain :sp-only? false :default false}])

(def ^:private upstream-key-rows
  ;; All rows are :bridge — every AC binding is a vanilla KeyMapping, so the
  ;; Settings app and Options > Controls share the same instance and a rebind
  ;; on either side shows up on the other. :config-key? rows are the
  ;; config-seeded slots (ability keys + preset editor).
  [{:source :bridge :input-id :ability-key-0 :prop-id "ability_0" :default-code -100 :config-key? true}
   {:source :bridge :input-id :ability-key-1 :prop-id "ability_1" :default-code -99 :config-key? true}
   {:source :bridge :input-id :ability-key-2 :prop-id "ability_2" :default-code 82 :config-key? true}
   {:source :bridge :input-id :ability-key-3 :prop-id "ability_3" :default-code 70 :config-key? true}
   {:source :bridge :input-id :content/cycle-selection :prop-id "switch_preset" :default-code 67}
   {:source :bridge :input-id :edit-preset-key :prop-id "edit_preset" :default-code 78 :config-key? true}
   {:source :bridge :input-id :content/toggle-primary-state :prop-id "ability_activation" :default-code 86}
   {:source :bridge :input-id :content/toggle-debug-overlay :prop-id "debug_console" :default-code 293}
   {:source :bridge :input-id :content/toggle-terminal :prop-id "open_data_terminal" :default-code 342}])

(def ^:private glfw-key-names
  {-100 "MOUSE 1" -99 "MOUSE 2"
   67 "C" 70 "F" 78 "N" 82 "R" 86 "V" 293 "F4" 342 "Left Alt"})

(defn- persist! [domain key value]
  (config-reg/set-config-value! domain key value)
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :config-persist :persist! domain key value)))

(defn- toggle-config! [p]
  (let [v (not ((:get p)))]
    (persist! (:domain p) (:key p) v)
    v))

(defn- settings-i18n [suffix]
  (str "settings." modid/MOD-ID "." suffix))

(defn- checkbox-text [p]
  (or (i18n/translate (settings-i18n (str "prop." (:prop-id p))))
      (:prop-id p)))

(defn- cathead-text [cat]
  (or (i18n/translate (settings-i18n (str "cat." cat)))
      cat))

(defn- prop-label [prop-id]
  (or (i18n/translate (settings-i18n (str "prop." prop-id))) prop-id))

(defn- hide-row-sections! [^INode item]
  (doseq [id [:cathead-line :cathead-text :checkbox-row :key-row :callback-row]]
    (when-let [^INode n (ui/item-node item id)]
      (.setVisible n false))))

(defn- show-only! [^INode item section-id]
  (hide-row-sections! item)
  (when-let [^INode n (ui/item-node item section-id)]
    (.setVisible n true)))

(defn- update-checkbox-item! [r item p checked?]
  (show-only! item :checkbox-row)
  (let [text-n (ui/item-node item :text)
        box-n (ui/item-node item :box)]
    (ui/set-node-prop! r text-n :text (checkbox-text p))
    (ui/set-node-prop! r box-n :src (if checked? check-tex-true check-tex-false))))

(defn- wire-checkbox-click! [r item p]
  (let [^INode box (ui/item-node item :box)]
    (rt/register-event! r (.getIdx box) :left-click
      (fn [_ _ _]
        (let [new-val (toggle-config! p)]
          (update-checkbox-item! r item p new-val))))))

(defn- wire-cathead-item! [r item cat]
  (hide-row-sections! item)
  (ui/set-node-prop! r (ui/item-node item :cathead-text) :text (cathead-text cat))
  (when-let [^INode text (ui/item-node item :cathead-text)]
    (.setVisible text true))
  (when-let [^INode line (ui/item-node item :cathead-line)]
    (.setVisible line true)))

(defn- wire-spacer-item! [^INode item]
  (hide-row-sections! item))

(defn- rebind-supported? []
  (boolean (bridge/call-adapter :keybind-rebind-supported?)))

(defn- default-key-name [key-code]
  (or (bridge/call-adapter :settings-key-name key-code)
      (get glfw-key-names key-code)
      (str "KEY_" key-code)))

(defn- current-key-code [{:keys [input-id default-code]}]
  (or (bridge/call-adapter :keybind-get-key-code input-id) default-code))

(defn- current-key-name [{:keys [input-id] :as row}]
  (or (bridge/call-adapter :keybind-get-key-name input-id)
      (default-key-name (current-key-code row))))

(defn- binding-editable? [_]
  (rebind-supported?))

(defn- binding-conflict?
  "True when the row's current KeyMapping shares its key with another
   vanilla/mod mapping — shown in red like Options > Controls."
  [{:keys [input-id]}]
  (boolean (bridge/call-adapter :keybind-conflict? input-id)))

(defn- refresh-key-display!
  "Update a key row's label text and color; conflicting bindings render red
   (vanilla Options > Controls convention)."
  [r key-val key-text row]
  (let [conflict? (binding-conflict? row)
        n (str (or (current-key-name row) ""))]
    (sig/sset-o! key-text n)
    (ui/set-node-prop! r key-val :text n)
    (ui/set-node-prop! r key-val :color
                       (if conflict? key-color-conflict key-color-idle))))

(defn- persist-binding! [{:keys [input-id]} key-code]
  (bridge/call-adapter :keybind-set-key! input-id (int key-code)))

(defn- visible-key-rows []
  (let [registered (kb-registry/get-all-keybinding-configs)]
    (filter (fn [{:keys [input-id config-key?]}]
              (or config-key? (contains? registered input-id)))
            upstream-key-rows)))

(defn- reset-all-to-defaults!
  "Restore every Settings checkbox and key binding to its declared default."
  []
  (doseq [p props]
    (persist! (:domain p) (:key p) (:default p)))
  (doseq [row (visible-key-rows)]
    (when (binding-editable? row)
      (persist-binding! row (:default-code row)))))

(defn- wire-key-binding-item! [r item {:keys [prop-id] :as row}]
  (show-only! item :key-row)
  (let [editable? (binding-editable? row)
        key-text (sig/signal-o (str (or (current-key-name row) "")))
        recording? (atom false)
        ^INode key-val (ui/item-node item :key-value)
        ^INode key-hit (or (ui/item-node item :key-hit) key-val)
        writer (slot-write/resolve-sig-writer (get node/kinds :text) :text)
        b (sig/bind! key-text key-val writer (rt/get-dirty-bindings-q r))
        finish! (fn [key-code]
                  (reset! recording? false)
                  ;; ESC (256) abandons without write — PropertyElements.EditKey.
                  (when-not (= 256 (int key-code))
                    (persist-binding! row key-code))
                  ;; Re-check conflict after the rebind: the new key may
                  ;; conflict with a vanilla/mod mapping (or the previous one
                  ;; may have been freed).
                  (refresh-key-display! r key-val key-text row))
        cancel! (fn []
                  (when @recording?
                    (finish! 256)))
        start! (fn []
                 (reset! recording? true)
                 ;; Key events only reach the focused node. Click may hit the
                 ;; outer :key-hit box; force focus onto :key-value which owns
                 ;; the :key handler (and also register :key on both below).
                 (events/gain-focus! r (.getIdx key-val))
                 (ui/set-node-prop! r key-val :color key-color-edit)
                 (sig/sset-o! key-text "PRESS")
                 (ui/set-node-prop! r key-val :text "PRESS"))
        on-click! (fn [_ _ evt]
                    (if @recording?
                      (finish! (- (int (:button evt)) 100))
                      (start!)))
        ;; Host always dispatches keyPressed with action=0; do NOT require
        ;; (not= 0 action) or keyboard rebinding never commits.
        on-key! (fn [_ _ evt]
                  (when @recording?
                    (finish! (:key-code evt))))
        targets (filter some? [key-hit key-val])
        target-idxs (set (map #(.getIdx ^INode %) targets))]
    (ui/set-node-prop! r (ui/item-node item :key-label) :text (prop-label prop-id))
    ;; Binding does not apply the initial SigO value — write it explicitly
    ;; (conflict state included).
    (refresh-key-display! r key-val key-text row)
    (rt/register-binding! r (.getIdx key-val) b)
    (when editable?
      ;; Deepest hit is usually :key-value (child of :key-hit); wire both so
      ;; either the label pad or the text starts/finishes recording.
      (doseq [^INode t targets]
        (rt/register-event! r (.getIdx t) :left-click on-click!)
        (rt/register-event! r (.getIdx t) :key on-key!)
        (rt/register-event! r (.getIdx t) :lost-focus
          (fn [_ _ evt]
            ;; Moving focus between key-hit and key-value is not a cancel
            ;; (needed so mouse-button finish on the pad still works).
            (when-not (contains? target-idxs (long (:new-focus-idx evt -1)))
              (cancel!))))))))

(defn- wire-callback-item! [r item {:keys [prop-id action]}]
  (show-only! item :callback-row)
  (ui/set-node-prop! r (ui/item-node item :callback-label) :text (prop-label prop-id))
  (rt/register-event! r (.getIdx ^INode (ui/item-node item :callback-button)) :left-click
    (fn [_ _ _] (action))))

(defn- category-block [cat-label content-rows]
  (into [{:type :cathead :label cat-label}]
        (concat content-rows
                [{:type :spacer :row-h category-gap}])))

(defn- all-settings-rows
  ([] (all-settings-rows nil))
  ([on-reset]
   (let [singleplayer? (boolean (bridge/call-adapter :singleplayer?))
         generic (filter #(or (not (:sp-only? %)) singleplayer?) props)
         keys (visible-key-rows)
         misc (cond-> [{:type :callback :prop-id "edit_ui" :action ui-customize/open!}]
                on-reset
                (conj {:type :callback :prop-id "reset_defaults" :action on-reset}))]
     (vec
       (concat
         (category-block "generic" (map #(assoc % :type :checkbox) generic))
         (category-block "keys" (map #(assoc % :type :key-binding) keys))
         (category-block "misc" misc))))))

(defn- rows-content-h [rows]
  (reduce (fn [acc row] (+ acc (double (or (:row-h row) row-h)))) 0.0 rows))

(defn- populate-settings-list! [r rows]
  (ui/list-set! r :settings-list rows
    (fn [rt item row]
      (case (:type row)
        :cathead (wire-cathead-item! rt item (:label row))
        :checkbox (do (update-checkbox-item! rt item row ((:get row)))
                      (wire-checkbox-click! rt item row))
        :key-binding (wire-key-binding-item! rt item row)
        :callback (wire-callback-item! rt item row)
        :spacer (wire-spacer-item! item)
        nil))))

(defn- thumb-node ^INode [r]
  (or (rt/node-by-id r :scrollbar) (rt/node-by-id r :scrollbar-hit)))

(defn- sync-scrollbar-thumb! [r progress]
  (let [^INode hit (rt/node-by-id r :scrollbar-hit)
        ^INode thumb (rt/node-by-id r :scrollbar)
        y (double (+ thumb-min-y (* (double progress) thumb-travel)))]
    (when hit
      (.setY hit y)
      (.setFlag hit node/FLAG-LAYOUT-DIRTY))
    (when (and thumb (not hit))
      (.setY thumb y)
      (.setFlag thumb node/FLAG-LAYOUT-DIRTY))))

(defn- fit-scale ^double [^UiRt rt*]
  (let [sw (rt/screen-w rt*)
        sh (rt/screen-h rt*)]
    (if (and (pos? sw) (pos? sh))
      (min 1.0 (* fit-margin (min (/ sw main-w) (/ sh main-h))))
      0.55)))

(defn- ensure-fit-scale! ^double [^UiRt rt*]
  (let [fit (fit-scale rt*)
        ^INode main (rt/node-by-id rt* :main)]
    (when (and main (> (Math/abs (- (.getScale main) fit)) 0.001))
      (.setScale main fit)
      (.setFlag main node/FLAG-LAYOUT-DIRTY)
      (rt/mark-tree-dirty! rt*))
    (if main (.getScale main) fit)))

(defn create-runtime []
  (let [r (rt/create-runtime)
        _ (rt/build! r (ui-xml/load-spec (modid/namespaced-path "guis/new/settings.xml")))
        ;; max-scroll updates when the list is rebuilt (e.g. after reset).
        scroll-px (atom 0.0)
        max-scroll (atom 0.0)
        set-scroll!
        (fn [new-px]
          (let [cap @max-scroll
                px (max 0.0 (min cap (double new-px)))
                progress (if (pos? cap) (/ px cap) 0.0)]
            (reset! scroll-px px)
            (ui/set-prop! r :settings-list :scroll-offset px)
            (sync-scrollbar-thumb! r progress)))
        drag-start-y (atom thumb-min-y)
        scroll-handler
        (fn [_ _ evt]
          (set-scroll! (- @scroll-px (* (double (:delta evt 0.0)) 40.0))))]
    (letfn [(reload-list! []
              (let [rows (all-settings-rows
                           (fn []
                             (reset-all-to-defaults!)
                             (reload-list!)))]
                (reset! max-scroll (max 0.0 (- (rows-content-h rows) visible-h)))
                (populate-settings-list! r rows)
                (set-scroll! @scroll-px)))]
      (reload-list!)
      (set-scroll! 0.0))
    (rt/put-user-signal! r :settings-pre-render
      (fn [_gg ^UiRt rt* _mx _my _pt]
        (ensure-fit-scale! rt*)))
    ;; Drag dy is screen-space; thumb Y is design-space — divide by cumScale.
    ;; Hit-test returns the deepest node (the 9px image), so wire both the
    ;; visible bar and the wider hit pad; always move :scrollbar-hit's Y.
    (let [wire-thumb-drag!
          (fn [id]
            (events/on! r id :drag-start
              (fn [_ _ _]
                (reset! drag-start-y (.getY ^INode (rt/node-by-id r :scrollbar-hit)))))
            (events/on! r id :drag
              (fn [_ _ evt]
                (let [^INode hit (rt/node-by-id r :scrollbar-hit)
                      sc (max 0.001 (.getCumScale hit))
                      new-y (max thumb-min-y
                                 (min thumb-max-y
                                      (+ @drag-start-y (/ (double (:dy evt)) sc))))
                      progress (/ (- new-y thumb-min-y) thumb-travel)]
                  (set-scroll! (* progress @max-scroll)))))
            (events/on! r id :mouse-scroll scroll-handler))]
      (wire-thumb-drag! :scrollbar-hit)
      (wire-thumb-drag! :scrollbar))
    (events/on! r :area :mouse-scroll scroll-handler)
    (events/on! r :settings-list :mouse-scroll scroll-handler)
    r))

(defn open! []
  (let [r (create-runtime)]
    (bridge/open-reactive-screen! r "Settings"
      {:on-pre-render (rt/user-signal r :settings-pre-render)})))
