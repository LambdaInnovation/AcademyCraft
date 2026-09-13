(ns cn.li.ac.terminal.client.apps.ui-customize-reactive
  "Reactive port of AcademyCraft's CustomizeUI, opened from Settings > Misc."
  (:require [cn.li.ac.config.common :as config-common]
            [cn.li.ac.config.gameplay :as gameplay]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.config.registry :as config-reg]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.xml :as ui-xml])
  (:import [cn.li.mcmod.ui.node INode]))

(def ^:private elements
  [{:id :cpbar :config-key :hud-cpbar-position :preview-id :preview-cpbar}
   {:id :keyhint :config-key :hud-keyhint-position :preview-id :preview-keyhint}
   {:id :media :config-key :hud-media-position :preview-id :preview-media}
   {:id :notification :config-key :hud-notification-position :preview-id :preview-notification}])

;; Upstream wrapEdit tints the edit field's own DrawTexture on confirm --
;; Colors.fromRGBA32(0x333333ff) when the value took, 0xbb3333ff when it did
;; not. Those are RGBA32; ours are the same colours as ARGB.
(def ^:private edit-bg-ok 0xFF333333)
(def ^:private edit-bg-error 0xFFBB3333)

;; Upstream marks the selected element by adding an Outline component to its
;; preview widget, which defaults to white at lineWidth 2. The mark boxes carry
;; outline-width="2.0"; only the colour is switched at runtime.
(def ^:private preview-outline 0xFFFFFFFF)
(def ^:private preview-outline-off 0x00000000)

(defn- set-position!
  "Upstream Node.setPosition only writes the in-memory Property; nothing
   touches the file until SettingsUI.onGuiClosed calls config.save(). Record
   the key so the close handler can flush exactly what changed."
  [dirty config-key position]
  (config-reg/set-config-value! config-common/gameplay-domain config-key position)
  (swap! dirty assoc config-key position))

(defn- flush-positions! [dirty]
  (when-let [fw-atom (fw/fw-atom)]
    (doseq [[config-key position] @dirty]
      (platform/call-adapter fw-atom :config-persist :persist!
                             config-common/gameplay-domain config-key position)))
  (reset! dirty {}))

(defn- local [suffix]
  (or (i18n/translate (str "gui." modid/MOD-ID ".uiedit." suffix)) suffix))

(defn- base-position [element-id sw sh]
  (case element-id
    :cpbar [(- sw 205.0) 12.0]
    :keyhint [(- sw 78.0) (+ (- (/ sh 2.0) 25.0) 30.0)]
    :media [(- sw 151.0) (- sh 42.0)]
    :notification [0.0 15.0]
    [0.0 0.0]))

(defn- set-preview-position! [r {:keys [id preview-id]} sw sh]
  (let [[base-x base-y] (base-position id sw sh)
        [dx dy] (gameplay/hud-position id)
        ^INode preview (rt/node-by-id r preview-id)]
    (.setX preview (+ base-x dx))
    (.setY preview (+ base-y dy))
    (.setFlag preview node/FLAG-LAYOUT-DIRTY)))

(defn- set-selected-preview! [r selected-id]
  (doseq [{:keys [id]} elements]
    (ui/set-prop! r (keyword (str "preview-" (name id) "-mark")) :outline
                  (if (= id selected-id) preview-outline preview-outline-off))))

(defn- position-editbox!
  "Park the edit box beside the row that was clicked, as upstream does:

     edit.pos(button.x + button.transform.width * button.scale + 5,
              button.y + button.transform.height * button.scale / 2
                       - edit.transform.height / 2)

   The row's absolute position already folds in #main's 0.5 scale, and
   #editbox hangs off the unscaled root, so its own x/y are that same space."
  [r ^INode row]
  (when row
    (let [^INode box (rt/node-by-id r :editbox)
          s (.getCumScale row)]
      (.setX box (+ (.getAbsX row) (* (.getW row) s) 5.0))
      (.setY box (- (+ (.getAbsY row) (/ (* (.getH row) s) 2.0))
                    (/ (.getH box) 2.0)))
      (.setFlag box node/FLAG-LAYOUT-DIRTY))))

(defn- valid-coordinate [value]
  (let [n (Double/parseDouble (str value))]
    (when (or (< n -512.0) (> n 512.0))
      (throw (NumberFormatException. "HUD coordinate is outside [-512, 512]")))
    n))

(defn- edit-position! [r selected dirty axis value sw sh]
  (when-let [{:keys [id config-key] :as element}
             (some #(when (= (:id %) @selected) %) elements)]
    (try
      (let [[x y] (gameplay/hud-position id)
            n (valid-coordinate value)
            position (if (= axis :x) [n y] [x n])]
        (set-position! dirty config-key position)
        (ui/set-prop! r (if (= axis :x) :edit_x_bg :edit_y_bg) :fill edit-bg-ok)
        (set-preview-position! r element sw sh))
      (catch NumberFormatException _
        (ui/set-prop! r (if (= axis :x) :edit_x_bg :edit_y_bg) :fill edit-bg-error)))))

(defn- select-element! [r selected rows {:keys [id]}]
  ;; Upstream changeEditFocus opens with `if (node == prevFocus) return;`.
  (when-not (= @selected id)
    (reset! selected id)
    (let [[x y] (gameplay/hud-position id)]
      ;; Same reason as create-runtime: :visible? has no prop-writer, so
      ;; set-prop! could only throw. This one fired on the first element click.
      (.setVisible ^INode (rt/node-by-id r :editbox) true)
      (position-editbox! r (get @rows id))
      (ui/set-prop! r :edit_x :text (str x))
      (ui/set-prop! r :edit_y :text (str y))
      (ui/set-prop! r :edit_x_bg :fill edit-bg-ok)
      (ui/set-prop! r :edit_y_bg :fill edit-bg-ok)
      (set-selected-preview! r id))))

(defn create-runtime []
  (let [r (rt/create-runtime)
        [sw sh] (or (bridge/get-window-size) [854 480])
        sw (double sw)
        sh (double sh)
        selected (atom nil)
        ;; The editbox anchors to the clicked list row, so keep each row node
        ;; by element id.
        rows (atom {})
        dirty (atom {})]
    (rt/build! r (ui-xml/load-spec (modid/namespaced-path "guis/new/ui_edit.xml")))
    (ui/set-prop! r :header :text (local "elements"))
    ;; :visible? is a build-time node prop, not a prop-writer — set-prop!
    ;; throws "no prop-writer" on it. Visibility is a node field at runtime.
    ;; (ui_edit.xml already declares visible="false"; this keeps a re-opened
    ;; runtime from inheriting a shown editbox.)
    (.setVisible ^INode (rt/node-by-id r :editbox) false)
    (ui/list-set! r :hud-list elements
      (fn [runtime item element]
        (swap! rows assoc (:id element) item)
        (ui/set-node-prop! runtime (ui/item-node item :label)
                           :text (local (str "elm." (name (:id element)))))
        (rt/register-event! runtime (.getIdx ^INode item) :left-click
          (fn [_ _ _] (select-element! r selected rows element)))))
    ;; Previews are shown, not clicked: upstream attaches no listener to them,
    ;; the element list is the only way to change focus.
    (doseq [element elements]
      (set-preview-position! r element sw sh))
    (events/on-confirm-input r :edit_x
      (fn [_ _ value] (edit-position! r selected dirty :x value sw sh)))
    (events/on-confirm-input r :edit_y
      (fn [_ _ value] (edit-position! r selected dirty :y value sw sh)))
    (rt/put-user-signal! r :uiedit-on-close #(flush-positions! dirty))
    r))

(defn open! []
  (let [r (create-runtime)]
    (bridge/open-reactive-screen! r "Customize UI"
      {:on-close (rt/user-signal r :uiedit-on-close)})))
