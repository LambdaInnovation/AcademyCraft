(ns cn.li.ac.content.ability.teleporter.location-teleport-reactive
  "Complete reactive replacement for location-teleport-screen.clj.
   All network/state logic (query/add/remove/perform, owner-key resolution,
   screen-state cache) is reused verbatim. Only CGUI widget construction is
   rewritten native.

   Upstream's blend animations are implemented in attach-rows-tick!: the
   menu panel grows in (Blend(0, 0.4)), every row fades in with the 0.06s
   per-row stagger (Blend(n*0.06, 0.2)), and the hovered row is washed
   white at 0.4 alpha (0.1 idle) — upstream wrapBack's colorRect plus the
   add-template Tint. The wash is a transparent :box painted BEHIND the row
   content (first template child), so it never wins hit-testing; the tick
   derives the hovered row from rt/hovered-idx via the parent walk."
  (:require [cn.li.ac.ability.messages :as catalog]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.managed-screens :as managed-screens]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.client.ui.registry :as widget-registry]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.xml :as ui-xml]
            [clojure.string :as str])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.signal ISigO]))

;; ============================================================================
;; Screen state — reused verbatim from location-teleport-screen.clj
;; ============================================================================

(def screen-id :ac/location-teleport)
(def ^:private default-state {:locations [] :exp 0.0 :current-pos nil :limits {}})

(defn- owner-key-from-player [player]
  (read-model/owner-key
    (read-model/canonical-client-owner
      {:client-session-id (runtime-hooks/require-player-state-session-id "teleporter.ui")
       :player-uuid (uuid/player-uuid player)}
      :location-teleport)
    :location-teleport))

(defn- screen-st [owner-key]
  (managed-screens/screen-state screen-id owner-key default-state))

(defn- update-screen! [owner-key f & args]
  (apply managed-screens/update-screen-state! screen-id owner-key default-state f args))

(defn apply-server-payload! [owner {:keys [locations exp current-pos limits]}]
  (let [ok (read-model/owner-key owner :location-teleport)]
    (update-screen! ok (fn [_] {:locations (vec (or locations []))
                                 :exp (double (or exp 0.0))
                                 :current-pos current-pos
                                 :limits (or limits {})}))
    nil))

(defn- net-owner [player-uuid]
  ;; The RPC owner contract requires :client-session-id; default-client-owner
  ;; derives it from the live connection (a bare :player-uuid map throws
  ;; ":client-owner contract violation" in send-to-server).
  (assoc (runtime-hooks/default-client-owner) :player-uuid player-uuid))

;; ============================================================================
;; Layout constants
;; ============================================================================

;; Layout lives in guis/new/loctele_new.xml (upstream loctele_new.xml: root
;; centered at scale 0.24, menu 442x530, rows 442x80 spacing 2, info panel
;; right-aligned). These constants drive the row placement math and scroll
;; window only.
(def ^:private list-y 18.0)
(def ^:private entry-h 80.0)
(def ^:private entry-spacing 2.0)
(def ^:private max-visible 6)

;; Upstream DefColors — the row name text toggles between them at runtime.
;; RGB only: the per-frame rows-tick animates the alpha byte (upstream
;; FontOption color alpha ramps with the row blend).
(def ^:private text-rgb-normal 0xC1CFD5)
(def ^:private text-rgb-disabled 0xA2A2A2)
(def ^:private text-rgb-input 0xA4D4E9)

;; Upstream Gui blend constants (LocationTeleport.scala): ElemTimeStep
;; 0.06s; wrapBack Blend(n*0.06, 0.2) with a white wash at 0.1 idle / 0.4
;; hovered; wrapButton Blend(n*0.06 + offset, 0.1) at 0.7 idle / 1.0
;; hovered; the elem name text Blend(n*0.06 + 0.1, 0.1); the add input
;; Blend(n*0.06, 0.2) at 0.4 idle / 0.8 focused; the menu Blend(0, 0.4).
(def ^:private elem-time-step 0.06)
(def ^:private row-blend-length 0.2)
(def ^:private btn-blend-length 0.1)
(def ^:private txt-blend-offset 0.1)
(def ^:private txt-blend-length 0.1)
(def ^:private menu-blend-length 0.4)
(def ^:private menu-max-height 530.0)
(def ^:private hl-alpha-idle 0.1)
(def ^:private hl-alpha-hover 0.4)
(def ^:private btn-alpha-idle 0.7)

(defn- blend-alpha
  "Upstream Gui.Blend.alpha — clamp((elapsed - offset) / length, 0, 1)."
  ^double [^double elapsed-s ^double offset-s ^double length-s]
  (max 0.0 (min 1.0 (/ (- elapsed-s offset-s) length-s))))

(defn now-ms
  "Wall clock for the blend animations — extracted so tests can with-redefs
  it to advance the animation deterministically."
  ^long []
  (System/currentTimeMillis))

(defn- write-box-tint!
  "Box :tint dslot (3) — a white overlay at alpha 0..1 (the renderer uses it
  verbatim when <= 1.0). Skip no-op writes so the node does not re-enter the
  render tape every frame."
  [^INode n ^double alpha]
  (when (and n (not= alpha (.getDSlot n 3)))
    (.setDSlot n 3 (double alpha))
    (.setFlag n node/FLAG-RENDER-DIRTY)))

(defn- write-text-color!
  "Text :color oslot (1, packed ARGB) — animate only the alpha byte over a
  fixed RGB (the renderer splices the alpha byte back into the normalized
  color, so a direct setOSlot is safe)."
  [^INode n ^long rgb ^double alpha01]
  (when n
    (let [argb (unchecked-int (bit-or (bit-shift-left (long (* 255.0 alpha01)) 24) rgb))]
      (when (not= argb (.getOSlot n 1))
        (.setOSlot n 1 argb)
        (.setFlag n node/FLAG-RENDER-DIRTY)))))

(defn- write-image-alpha!
  "Image :alpha dslot (0, 0..1)."
  [^INode n ^double alpha]
  (when (and n (not= alpha (.getDSlot n 0)))
    (.setDSlot n 0 (double alpha))
    (.setFlag n node/FLAG-RENDER-DIRTY)))

;; ============================================================================
;; set-tick! — force a per-frame side-effecting computed-o to actually run
;; (see developer panel-reactive.clj for the fuller writeup).
;; ============================================================================

(defn- pull-o! [_node source] (.sGet ^ISigO source) nil)

(defn- set-tick! [^UiRt rt key computed-sig]
  (when-let [old (rt/user-signal rt key)] (sig/unbind! old))
  (if computed-sig
    (let [^INode anchor (rt/node-by-id rt :root)
          b (sig/bind! computed-sig anchor pull-o! (rt/get-dirty-bindings-q rt))]
      (rt/register-binding! rt (.getIdx anchor) b)
      (rt/put-user-signal! rt key b))
    (rt/put-user-signal! rt key nil)))

;; ============================================================================
;; Network — mirrors old send-query!/send-action!, adapted to rebuild the
;; native list instead of a CGUI subtree.
;; ============================================================================

(declare rebuild-list!)

(defn- send-query! [^UiRt rt player-uuid owner-key]
  ;; A screen closed (disposed) after a teleport still has pending network
  ;; callbacks — they must not touch the disposed runtime (its userSignals
  ;; are cleared) or fire further requests.
  (when-not (rt/disposed? rt)
    (net-client/send-to-server (net-owner player-uuid)
      catalog/MSG-REQ-SAVED-POS-QUERY {}
      (fn [resp]
      ;; The server wraps the query result in {:action ... :snapshot ...} —
      ;; read the snapshot, not the top level.
      (let [snapshot (or (:snapshot resp) resp)]
        (when (and snapshot (:success? snapshot))
          (update-screen! owner-key
            (fn [_] {:locations (vec (or (:locations snapshot) []))
                     :exp (double (or (:exp snapshot) 0.0))
                     :current-pos (:current-pos snapshot)
                     :limits (or (:limits snapshot) {})}))
          ;; A fresh list re-fades the rows (upstream updateList) — but only
          ;; for real data changes: the ADD/REMOVE responses go through this
          ;; query, and it must not steal the input's focus while the user
          ;; is typing (there is no periodic refresh for that reason).
          (rebuild-list! rt player-uuid owner-key true)))))))

(defn- send-action! [^UiRt rt player-uuid msg-id payload owner-key]
  (net-client/send-to-server (net-owner player-uuid) msg-id payload
    (fn [resp]
      (send-query! rt player-uuid owner-key))))

;; ============================================================================
;; Row / add-row builders
;; ============================================================================

(defn- hide-row-sections! [^UiRt rt ^INode item]
  (doseq [id [:elem-row :add-row]]
    (when-let [^INode n (ui/item-node item id)]
      (.setVisible n false)
      ;; setVisible alone does not dirty the render tape — without this the
      ;; hidden section stays hittable and swallows the row's clicks.
      (.setFlag n node/FLAG-LAYOUT-DIRTY))))

(defn- show-row-section! [^UiRt rt ^INode item section-id]
  (hide-row-sections! rt item)
  (when-let [^INode n (ui/item-node item section-id)]
    (.setVisible n true)
    (.setFlag n node/FLAG-LAYOUT-DIRTY))
  (rt/mark-tree-dirty! rt))

;; ============================================================================
;; List rebuild — the visible scroll window + optional add-row
;; ============================================================================

(defn- hovered-item-idx
  "Walk up from the hit-test result to the first node that is a list item
  root (a :hit-map key), or -1. Hit-test returns the DEEPEST node (name
  text, icon, highlight box) — the walk is needed to know which row is
  hovered."
  [^UiRt rt hit-map]
  (let [idx (rt/hovered-idx rt)]
    (loop [n (when (>= idx 0) (rt/node-by-idx rt idx))]
      (cond
        (nil? n) -1
        (contains? hit-map (.getIdx ^INode n)) (.getIdx ^INode n)
        :else (recur (.getParentNode ^INode n))))))

(defn- hovered-location [^UiRt rt hit-map]
  ;; the row entry, so hovering anywhere on a row shows its info panel.
  (let [item-idx (hovered-item-idx rt hit-map)]
    (when (>= item-idx 0)
      (get hit-map item-idx))))

(defn- rebuild-list!
  "Rebuild the visible scroll window + the add row. `reset-fade?` re-bases
  the rows' fade-in animation — upstream updateList creates fresh Blend
  objects per row, so add/remove re-fades the rows; scrolling does not."
  [^UiRt rt player-uuid owner-key reset-fade?]
  (let [{:keys [locations limits current-pos]} (screen-st owner-key)
        total (count locations)
        scroll-a (rt/user-signal rt :scroll-idx)
        ;; The add row is the LAST slot of the scrollable content (upstream
        ;; compList = locations + add_template): once max-visible locations
        ;; exist, the window is max-visible-1 elems + the add row, so the
        ;; add row stays reachable no matter how many locations are saved
        ;; (it used to be appended after a full window and was permanently
        ;; clipped below the fold with 6+ locations).
        scroll-max (max 0 (- (inc total) max-visible))
        start (max 0 (min @scroll-a scroll-max))
        _ (reset! scroll-a start)
        elems-window (if (>= total max-visible) (dec max-visible) total)
        visible-locs (vec (drop start (take (+ start elems-window) locations)))
        items (into (mapv (fn [idx loc] {:type :elem :loc loc :idx idx})
                          (range) visible-locs)
                    [{:type :add}])
        hit-map (atom {})
        ;; Per-row node/alpha-state map for the per-frame rows-tick — the
        ;; row's stagger index n (upstream Blend offset n*0.06), the name
        ;; RGB (normal/disabled), and the highlight/name/button/input nodes.
        row-nodes (atom {})
        ;; Scroll rebuilds (reset-fade? false) must not drop the user's
        ;; in-flight add-input session: upstream's ElementList scrolls
        ;; without rebuilding, so its TextBox keeps focus + text. Capture
        ;; both and restore them onto the fresh input node after the
        ;; rebuild. Data rebuilds (add/remove) intentionally do NOT restore
        ;; — upstream leaves edit mode after confirm.
        focused-add-text (when-not reset-fade?
                           (when-let [old (rt/user-signal rt :row-nodes)]
                             (let [old-add (some (fn [[_ r]] (when (:input r) r)) @old)]
                               (when (and old-add
                                          (= (rt/focus-idx rt)
                                             (.getIdx ^INode (:input old-add))))
                                 (str (.getOSlot ^INode (:input old-add) 0))))))]
    (when reset-fade?
      (rt/put-user-signal! rt :rows-base-ms (atom (now-ms))))
    (ui/list-set! rt :list-ctr items
      (fn [rt ^INode item row]
        (case (:type row)
          :elem
          (let [loc (:loc row)
                idx (:idx row)
                can? (boolean (:can-perform? loc))
                n (+ start idx)
                ^INode name-n (ui/item-node item :name)
                ^INode tp-n (ui/item-node item :btn-tp)
                ^INode del-n (ui/item-node item :btn-del)]
            (show-row-section! rt item :elem-row)
            (ui/set-node-prop! rt name-n :text (str (or (:name loc) "?")))
            (when tp-n
              (.setVisible tp-n can?)
              (.setFlag tp-n node/FLAG-LAYOUT-DIRTY)
              (rt/mark-tree-dirty! rt))
            ;; Hit-map value carries the row's design y so the info panel can
            ;; follow the hovered row (upstream setMessage moves info to ypos).
            (swap! hit-map assoc (.getIdx item)
                   {:loc loc :y (double (+ list-y (* idx (+ entry-h entry-spacing))))})
            (swap! row-nodes assoc (.getIdx item)
                   {:n n
                    :name-rgb (if can? text-rgb-normal text-rgb-disabled)
                    :hl (ui/item-node item :hl)
                    :name name-n
                    :tp tp-n
                    :del del-n})
            ;; Pre-zero the fade-in state — the first painted frame must not
            ;; flash full-bright before the rows-tick takes over (same eager
            ;; pattern as tutorial_reactive's first-open animation).
            (write-box-tint! (ui/item-node item :hl) 0.0)
            (write-text-color! name-n (if can? text-rgb-normal text-rgb-disabled) 0.0)
            (write-image-alpha! tp-n 0.0)
            (write-image-alpha! del-n 0.0)
            (when can?
              (rt/register-event! rt (.getIdx tp-n) :left-click
                (fn [_ _ _]
                  ;; UI events carry no session binding — pass the screen's
                  ;; session explicitly (owner-key's first element).
                  (client-sounds/queue-sound-effect!
                    (first owner-key)
                    {:type :sound :sound-id (modid/namespaced-path "tp.tp") :volume 0.5 :pitch 1.0})
                  (send-action! rt player-uuid catalog/MSG-REQ-SAVED-POS-PERFORM {:name (:name loc)} owner-key)
                  (bridge/close-screen!))))
            (rt/register-event! rt (.getIdx del-n) :left-click
              (fn [_ _ _]
                (send-action! rt player-uuid catalog/MSG-REQ-SAVED-POS-REMOVE {:name (:name loc)} owner-key))))
          :add
          (let [^INode input-n (ui/item-node item :input)
                ^INode ok-n (ui/item-node item :ok)
                input-idx (.getIdx ^INode input-n)]
            (show-row-section! rt item :add-row)
            ;; Upstream newAdd: hovering the add row shows the CURRENT
            ;; position (dimension + feet coords, no CP).
            (swap! hit-map assoc (.getIdx item)
                   {:loc (or current-pos {}) :add? true
                    :y (double (+ list-y (* (count visible-locs)
                                            (+ entry-h entry-spacing))))})
            (swap! row-nodes assoc (.getIdx item)
                   {:n total
                    :hl (ui/item-node item :hl)
                    :ph (ui/item-node item :ph)
                    :input input-n
                    :ok ok-n})
            ;; The "Add..." placeholder hides while the input is focused or
            ;; has text; it returns when the input is cleared and focus
            ;; leaves (upstream: the TextBox content IS "Add..." at 0.4
            ;; alpha, cleared on focus).
            (let [set-ph! (fn [visible?]
                            (when-let [^INode ph-n (ui/item-node item :ph)]
                              (.setVisible ph-n visible?)
                              (.setFlag ph-n node/FLAG-LAYOUT-DIRTY)
                              (rt/mark-tree-dirty! rt)))
                  has-text? (fn [] (pos? (count (str (.getOSlot input-n 0)))))
                  focused? (fn [] (= (rt/focus-idx rt) input-idx))
                  enter-edit! (fn []
                                ;; Upstream newAdd LeftClickEvent: enter edit
                                ;; mode — clear stale text, focus, hide the
                                ;; placeholder. A focused input keeps its
                                ;; text (clicking it while typing is a no-op).
                                (when-not (focused?)
                                  (ui/set-node-prop! rt input-n :text ""))
                                (events/gain-focus! rt input-idx)
                                (set-ph! false))
                  confirm-add! (fn []
                                 ;; Upstream confirmInput: submit the name
                                 ;; (max 16 chars), then leave edit mode.
                                 (let [name (str/trim (str (.getOSlot input-n 0)))
                                       name-len (int (or (:max-location-name-length limits) 16))]
                                   (when (and (not (str/blank? name)) (<= (count name) name-len))
                                     (send-action! rt player-uuid catalog/MSG-REQ-SAVED-POS-ADD {:name name} owner-key)
                                     (events/gain-focus! rt -1)
                                     (ui/set-node-prop! rt input-n :text ""))))]
              ;; Enter edit mode on click. The input node covers the
              ;; "Add..." placeholder area, which is where the user clicks;
              ;; the ok button keeps its own handler (the dispatch walk
              ;; stops at the first node with handlers). A whole-row handler
              ;; is intentionally NOT registered: dispatch-mouse-press!
              ;; re-focuses the HIT node after a handled click, so clicking
              ;; the row background would focus the group, not the input.
              (rt/register-event! rt input-idx :left-click
                (fn [_ _ _] (enter-edit!)))
              (rt/register-event! rt input-idx :change-content
                (fn [_ _ _] (set-ph! (and (not (has-text?)) (not (focused?))))))
              (rt/register-event! rt input-idx :lost-focus
                (fn [_ _ _] (set-ph! (not (has-text?)))))
              ;; Upstream ConfirmInputEvent — Enter submits the name.
              (rt/register-event! rt input-idx :confirm-input
                (fn [_ _ _] (confirm-add!)))
              (rt/register-event! rt (.getIdx ^INode ok-n) :left-click
                (fn [_ _ _] (confirm-add!))))
            ;; Pre-zero the fade-in state (see :elem branch).
            (write-box-tint! (ui/item-node item :hl) 0.0)
            (write-text-color! (ui/item-node item :ph) text-rgb-input 0.0)
            (write-text-color! input-n text-rgb-input 0.0)
            (write-image-alpha! ok-n 0.0))
          nil)))
    (rt/put-user-signal! rt :hit-map hit-map)
    (rt/put-user-signal! rt :row-nodes row-nodes)
    (when (some? focused-add-text)
      ;; Restore the scrolled-away add-input session onto the fresh nodes
      ;; (see focused-add-text above).
      (let [[item-idx add-row] (some (fn [[i r]] (when (:input r) [i r])) @row-nodes)]
        (when add-row
          (let [^INode new-input (:input add-row)]
            (ui/set-node-prop! rt new-input :text focused-add-text)
            (events/gain-focus! rt (.getIdx new-input))
            (when-let [^INode ph (ui/item-node (rt/node-by-idx rt item-idx) :ph)]
              (.setVisible ph false)
              (.setFlag ph node/FLAG-LAYOUT-DIRTY)
              (rt/mark-tree-dirty! rt))))))
    nil))

;; ============================================================================
;; Info panel — hover detail or EXP/cross-dim status, refreshed each frame
;; ============================================================================

(defn- attach-info-panel-tick! [^UiRt rt _owner-key]
  ;; Upstream MessageTab: the info panel shows the hovered row's message
  ;; (dimension, coords, CP cost) and moves to the row's y; hidden otherwise.
  (set-tick! rt :info-tick
    (sig/computed-o [(rt/clock-ms-sig rt)]
      (fn [_]
        (let [hit-map @(or (rt/user-signal rt :hit-map) (atom {}))
              entry (hovered-location rt hit-map)
              ^INode info (rt/node-by-id rt :info)]
          (if entry
            (let [;; The add row shows the CURRENT position — the local
                  ;; player's live feet coords (upstream player.posX/Y/Z), no
                  ;; server round-trip; the world id falls back to the
                  ;; snapshot's current-pos.
                  loc (if (:add? entry)
                        (merge (:loc entry)
                               (or (bridge/call-adapter :local-player-pos) {}))
                        (:loc entry))
                  coords (format "(%.0f, %.0f, %.0f)"
                                 (double (or (:x loc) 0.0))
                                 (double (or (:y loc) 0.0))
                                 (double (or (:z loc) 0.0)))
                  ;; Upstream: the add row shows the CURRENT position
                  ;; (dimension + coords, no CP); a saved row shows its
                  ;; stored location + CP cost.
                  lines (if (:add? entry)
                          [(str (or (:world-id loc) "?") " (#" (int (or (:dim-id loc) 0)) ")")
                           coords]
                          [(str (or (:world-id loc) "?") " (#" (int (or (:dim-id loc) 0)) ")")
                           coords
                           (str (int (or (:cp-cost loc) 0)) " CP")])]
              (when info
                (when-not (.isVisible info)
                  (.setVisible info true)
                  (.setFlag info node/FLAG-LAYOUT-DIRTY)
                  (rt/mark-tree-dirty! rt))
                (.setY info (double (:y entry)))
                (.setFlag info node/FLAG-LAYOUT-DIRTY))
              (doseq [[i line] (map-indexed vector lines)]
                (ui/set-prop! rt (keyword (str "info-line-" i)) :text line))
              ;; Clear the unused trailing message lines.
              (doseq [i (range (count lines) 4)]
                (ui/set-prop! rt (keyword (str "info-line-" i)) :text "")))
            (when info
              (when (.isVisible info)
                (.setVisible info false)
                (.setFlag info node/FLAG-LAYOUT-DIRTY)
                (rt/mark-tree-dirty! rt))))
          nil)))))

;; ============================================================================
;; Rows tick — upstream Blend animations (menu grow, per-row fade, hover
;; highlight, button/text alpha ramps)
;; ============================================================================

(defn- attach-rows-tick! [^UiRt rt]
  ;; All of upstream's blend animations, wall-clock driven off
  ;; :rows-open-ms (screen open) / :rows-base-ms (last list rebuild —
  ;; upstream updateList creates fresh Blend objects per row, so add/remove
  ;; re-fades the rows). Depends on partial-ticks-sig: clock-ms-sig freezes
  ;; while a GUI screen is open (tutorial_reactive note), which would stall
  ;; the animation.
  (set-tick! rt :rows-tick
    (sig/computed-o [(rt/clock-ms-sig rt) (rt/partial-ticks-sig rt)]
      (fn [_ _]
        (let [now-ms (now-ms)
              elapsed-s (/ (- now-ms (long @(or (rt/user-signal rt :rows-base-ms) (atom 0)))) 1000.0)
              menu-s (/ (- now-ms (long @(or (rt/user-signal rt :rows-open-ms) (atom 0)))) 1000.0)
              hit-map @(or (rt/user-signal rt :hit-map) (atom {}))
              row-nodes @(or (rt/user-signal rt :row-nodes) (atom {}))
              hover-idx (hovered-item-idx rt hit-map)
              focus-idx (rt/focus-idx rt)]
          ;; Menu grow-in — upstream Blend(0, 0.4) on menu.transform.height.
          (when-let [^INode menu (rt/node-by-id rt :menu)]
            (let [h (* menu-max-height (blend-alpha menu-s 0.0 menu-blend-length))]
              (when (not= h (.getH menu))
                (.setH menu (double h))
                (.setFlag menu node/FLAG-LAYOUT-DIRTY))))
          (doseq [[item-idx row] row-nodes]
            (let [n (double (long (:n row)))
                  row-blend (blend-alpha elapsed-s (* n elem-time-step) row-blend-length)
                  hovered? (= (long item-idx) hover-idx)]
              ;; Row white wash — upstream wrapBack colorRect: blend * (0.1
              ;; idle | 0.4 hovered). The hl box sits BEHIND the row content
              ;; so it never wins hit-testing; hover comes from the
              ;; hovered-idx parent walk, not FLAG-HOVERED.
              (write-box-tint! (:hl row) (* row-blend (if hovered? hl-alpha-hover hl-alpha-idle)))
              ;; Name text alpha ramp — Blend(n*0.06 + 0.1, 0.1).
              (write-text-color! (:name row) (long (or (:name-rgb row) text-rgb-normal))
                                  (blend-alpha elapsed-s (+ (* n elem-time-step) txt-blend-offset)
                                               txt-blend-length))
              (when-let [^INode tp (:tp row)]
                ;; wrapButton btn_teleport: Blend(n*0.06 + 0.03, 0.1),
                ;; 0.7 idle / 1.0 hovered.
                (write-image-alpha! tp (* (blend-alpha elapsed-s (+ (* n elem-time-step) 0.03)
                                                        btn-blend-length)
                                          (if (= hover-idx (.getIdx tp)) 1.0 btn-alpha-idle))))
              (when-let [^INode del (:del row)]
                ;; wrapButton btn_remove: Blend(n*0.06 + 0.05, 0.1).
                (write-image-alpha! del (* (blend-alpha elapsed-s (+ (* n elem-time-step) 0.05)
                                                        btn-blend-length)
                                           (if (= hover-idx (.getIdx del)) 1.0 btn-alpha-idle))))
              (when-let [^INode ok (:ok row)]
                ;; wrapButton btn_confirm: Blend(n*0.06, 0.1).
                (write-image-alpha! ok (* (blend-alpha elapsed-s (* n elem-time-step) btn-blend-length)
                                          (if (= hover-idx (.getIdx ok)) 1.0 btn-alpha-idle))))
              (when-let [^INode input (:input row)]
                ;; Add input + "Add..." placeholder: Blend(n*0.06, 0.2) at
                ;; 0.4 idle / 0.8 while focused (upstream inputText alpha).
                (let [in-a (* row-blend (if (= focus-idx (.getIdx input)) 0.8 0.4))]
                  (write-text-color! (:ph row) text-rgb-input in-a)
                  (write-text-color! input text-rgb-input in-a)))))
          nil)))))

;; ============================================================================
;; Scroll handling
;; ============================================================================

(defn- attach-scroll! [^UiRt rt player-uuid owner-key]
  (events/on! rt :list-bg :mouse-scroll
    (fn [_ _ evt]
      (let [{:keys [locations]} (screen-st owner-key)
            scroll-max (max 0 (- (inc (count locations)) max-visible))
            delta (double (or (:delta evt) 0.0))]
        (when (pos? scroll-max)
          (swap! (rt/user-signal rt :scroll-idx) #(max 0 (min scroll-max (+ % (if (neg? delta) 1 -1)))))
          (rebuild-list! rt player-uuid owner-key false))))))

;; ============================================================================
;; Root spec + entry point
;; ============================================================================

(defn- root-spec []
  ;; The whole layout (geometry + templates) lives in
  ;; guis/new/loctele_new.xml — upstream loctele_new.xml port.
  (ui-xml/load-spec (modid/namespaced-path "guis/new/loctele_new.xml")))

(defn create-runtime [player]
  (let [r (rt/create-runtime)
        owner-key (owner-key-from-player player)
        player-uuid (uuid/player-uuid player)]
    (rt/build! r (root-spec))
    (rt/put-user-signal! r :scroll-idx (atom 0))
    (rt/put-user-signal! r :rows-open-ms (atom (now-ms)))
    (rt/put-user-signal! r :rows-base-ms (atom (now-ms)))
    (attach-scroll! r player-uuid owner-key)
    (attach-info-panel-tick! r owner-key)
    (attach-rows-tick! r)
    (rebuild-list! r player-uuid owner-key true)
    ;; No periodic server refresh — upstream only queries at open and after
    ;; add/remove, and a query response rebuilds the list, which would
    ;; destroy the focused input node and drop the user's typing (the
    ;; input reverted to "Add..." about a second after clicking it).
    (send-query! r player-uuid owner-key)
    r))

(defn open-screen! [player _payload]
  (let [r (create-runtime player)]
    {:type :reactive-screen :runtime r :title "Location Teleport"}))

(defn close-screen! [owner]
  (managed-screens/clear-screen-state! screen-id (read-model/owner-key owner :location-teleport)))

(defn open! [player]
  (bridge/open-reactive-screen! (create-runtime player) "Location Teleport"))

(defn init!
  "Register location-teleport widget factory. Idempotent."
  []
  (install/framework-once! ::init
    (fn []
      (widget-registry/register-widget-factory! :ac/saved-position
        ;; The :location-teleport/ui-open channel payload carries only the
        ;; query data — resolve the client player here like the skill-tree
        ;; widget factory does (a payload :player would be nil).
        (fn [payload]
          (open-screen! (bridge/get-client-player) (or payload {}))))
      (log/debug "Location Teleport reactive screen registered")))
  nil)
