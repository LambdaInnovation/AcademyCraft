(ns cn.li.mcmod.ui.events
  "Event dispatch & focus/drag management (pure logic)."
  (:require [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.layout :as layout]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.slot-write :as slot-write]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

(defn- invoke-handlers!
  "Run each handler, logging (not silently swallowing) any exception a
   handler throws — without this, a broken click/scroll/key handler just
   looks like the input had no effect at all, with no trace of why.
   Returns the handlers' return values — :key dispatch treats a truthy
   return as \"consumed\", so a handler can decline a key (ESC) it does not
   handle instead of swallowing it from the host."
  [event-key handlers rt node evt]
  (let [results (transient [])]
    (doseq [f handlers]
      (try
        (conj! results (f rt node evt))
        (catch Throwable e
          (log/stacktrace (str "UI event handler threw (" event-key ")") e))))
    (persistent! results)))

(defn on! [^UiRt rt id event-key f]
  (if-let [^INode node (rt/node-by-id rt id)]
    (rt/register-event! rt (.getIdx node) event-key f)
    (throw (ex-info (str "Node not found: " id) {:id id :event-key event-key})))
  nil)

(defn on-confirm-input
  "Register :confirm-input handler. Event map includes :value (current text)."
  [^UiRt rt id f]
  (on! rt id :confirm-input
       (fn [rt node evt] (f rt node (:value evt)))))

(defn on-change-content
  "Register :change-content handler. Event map includes :value (current text)."
  [^UiRt rt id f]
  (on! rt id :change-content
       (fn [rt node evt] (f rt node (:value evt)))))

(defn- text-editable? [^INode n]
  (boolean (get (.getStaticProps n) :editable?)))

(defn- text-value [^INode n]
  (str (or (.getOSlot n 0) "")))

(defn- set-text-value! [^INode n value]
  (slot-write/apply-prop! n :text :text (str value)))

(defn- emit-text-event! [^UiRt rt ^INode n event-key]
  (when-let [handlers (rt/get-event-handlers rt (.getIdx n) event-key)]
    (let [evt {:value (text-value n) :node-idx (.getIdx n)}]
      (invoke-handlers! event-key handlers rt n evt))))

(defn- display-text [^INode n]
  (let [s (text-value n)]
    (if (boolean (get (.getStaticProps n) :masked?))
      (apply str (repeat (count s) "*"))
      s)))

(defn- scroll-editable-into-view!
  "Match upstream TextBox.checkCaretRegion: text wider than the field scrolls
   left so the typing end (tail) stays visible — it never overflows the box.
   The renderer already offsets text by the node's x-offset dslot (idx 1), so
   writing a negative offset scrolls without touching the text nodes. Measured
   on the displayed text (masked fields measure the \"*\" echo, like upstream's
   processedContent)."
  [^INode n]
  (let [w (double (.getW n))
        fs (double (or (.getDSlot n 0) 14.0))
        tw (double (or (bridge/font-text-width-optional nil (display-text n) fs) 0.0))
        off (if (and (pos? w) (> tw w)) (- (- tw w)) 0.0)]
    (when-not (== off (double (.getDSlot n 1)))
      (.setDSlot n 1 off)
      (.setFlag n node/FLAG-RENDER-DIRTY))))

(defn dispatch-editable-key!
  "Handle backspace / enter / char for focused editable :text nodes.
   Returns true when the key was consumed."
  [^UiRt rt key-code typed-char]
  (let [focus-idx (rt/focus-idx rt)]
    (when (>= focus-idx 0)
      (when-let [^INode n (rt/node-by-idx rt focus-idx)]
        (when (text-editable? n)
          (let [enter-keys #{257 335 28}
                backspace-keys #{259 14}
                has-char? (and typed-char (not= typed-char (char 0)))]
            (cond
              (contains? backspace-keys key-code)
              (let [curr (text-value n)]
                (when (pos? (count curr))
                  (set-text-value! n (subs curr 0 (dec (count curr))))
                  (scroll-editable-into-view! n)
                  (emit-text-event! rt n :change-content))
                true)

              (contains? enter-keys key-code)
              (do (emit-text-event! rt n :confirm-input) true)

              has-char?
              (do (set-text-value! n (str (text-value n) typed-char))
                  (scroll-editable-into-view! n)
                  (emit-text-event! rt n :change-content)
                  true)

              :else false)))))))

(defn dispatch-click! [^UiRt rt mx my button event-key]
  ;; Bubble up the parent chain from the deepest hit (same walk as
  ;; dispatch-scroll!): handlers live on container groups (slot rows, carousel
  ;; pages) while hit-test lands on their leaf children (icons, labels,
  ;; background images) — without bubbling those handlers are unreachable.
  ;; The walk stops at the FIRST node with handlers, so a child with its own
  ;; handler still shadows its ancestors.
  (let [^INode hit (layout/hit-test rt (double mx) (double my))]
    (loop [node hit]
      (when node
        (if-let [handlers (rt/get-event-handlers rt (.getIdx ^INode node) event-key)]
          (let [evt {:x mx :y my :button button :node-idx (.getIdx ^INode node)}]
            (invoke-handlers! event-key handlers rt node evt))
          (recur (.getParentNode ^INode node)))))))

(defn dispatch-scroll! [^UiRt rt mx my scroll-delta]
  (let [^INode hit (layout/hit-test rt (double mx) (double my))]
    (loop [node hit]
      (when node
        (if-let [handlers (rt/get-event-handlers rt (.getIdx ^INode node) :mouse-scroll)]
          (let [evt {:x mx :y my :delta scroll-delta :node-idx (.getIdx ^INode node)}]
            (invoke-handlers! :mouse-scroll handlers rt node evt))
          (recur (.getParentNode ^INode node)))))))

(defn dispatch-key!
  "Dispatch a key press to the focus node's :key handlers (if any).
   Returns a truthy value when a handler consumed the key (returned truthy),
   nil otherwise — hosts use this to decide whether the key was handled by
   the UI (e.g. ESC closing a skill detail popup) before falling back to
   closing the whole screen. Handlers that do not handle the key (the
   developer console's command input, which ESC is not) must return nil so
   the host still closes the screen."
  [^UiRt rt key-code scan-code modifiers action]
  (let [focus-idx (rt/focus-idx rt)]
    (when (>= focus-idx 0)
      (when-let [handlers (rt/get-event-handlers rt focus-idx :key)]
        (when-let [^INode node (rt/node-by-idx rt focus-idx)]
          (let [evt {:key-code key-code :scan-code scan-code :modifiers modifiers
                     :action action :node-idx focus-idx}
                results (invoke-handlers! :key handlers rt node evt)]
            (some identity results)))))))

(defn dispatch-char! [^UiRt rt code-point]
  (let [focus-idx (rt/focus-idx rt)]
    (when (>= focus-idx 0)
      (when-let [handlers (rt/get-event-handlers rt focus-idx :change-content)]
        (when-let [^INode node (rt/node-by-idx rt focus-idx)]
          ;; char[] does not override toString — calling it yields the
          ;; identity string ("[C@<hash>"), so every codepoint arrived as
          ;; (first ...) = \[ in consumers. Build a real String instead.
          (let [^"[C" chars (Character/toChars (int code-point))
                evt {:char (String. chars)
                     :code-point (int code-point) :node-idx focus-idx}]
            (invoke-handlers! :change-content handlers rt node evt)))))))

(defn gain-focus! [^UiRt rt node-idx]
  (let [old-idx (rt/focus-idx rt)]
    (when (and (>= old-idx 0) (not= old-idx node-idx))
      (when-let [^INode old-node (rt/node-by-idx rt old-idx)]
        (.clearFlag old-node node/FLAG-FOCUSED)
        (.setFlag old-node node/FLAG-RENDER-DIRTY))
      (when-let [handlers (rt/get-event-handlers rt old-idx :lost-focus)]
        (when-let [^INode old-node (rt/node-by-idx rt old-idx)]
          (let [evt {:node-idx old-idx :new-focus-idx node-idx}]
            (invoke-handlers! :lost-focus handlers rt old-node evt)))))
    (rt/set-focus-idx! rt node-idx)
    (when (>= node-idx 0)
      (when-let [^INode node (rt/node-by-idx rt node-idx)]
        (.setFlag node node/FLAG-FOCUSED)
        (.setFlag node node/FLAG-RENDER-DIRTY)
        ;; Recompute the scroll offset on focus: a programmatic text set (connect
        ;; clears the password, teleport clears the name) leaves the previous
        ;; scroll in the x-offset dslot; the field must show its tail fresh.
        (when (text-editable? node)
          (scroll-editable-into-view! node)))
      (when-let [handlers (rt/get-event-handlers rt node-idx :gain-focus)]
        (when-let [^INode node (rt/node-by-idx rt node-idx)]
          (let [evt {:node-idx node-idx}]
            (invoke-handlers! :gain-focus handlers rt node evt)))))))

(defn remove-focus! [^UiRt rt]
  (let [old-idx (rt/focus-idx rt)]
    (when (>= old-idx 0) (gain-focus! rt -1))))

(defn- drag-armed-node-idx
  "Walk from the hit node up the parent chain (same walk as dispatch-click!)
  for the first node wiring :drag/:drag-start handlers. A scrollbar thumb
  registers only drag handlers — no :left-click — so an unhandled press on it
  must still arm the drag chain (settings/about/tutorial scrollbars were
  un-draggable without the tutorial's no-op :left-click workaround)."
  [^UiRt rt ^INode hit]
  (loop [node hit]
    (if node
      (if (or (rt/get-event-handlers rt (.getIdx ^INode node) :drag)
              (rt/get-event-handlers rt (.getIdx ^INode node) :drag-start))
        (.getIdx ^INode node)
        (recur (.getParentNode ^INode node)))
      -1)))

(defn dispatch-mouse-press! [^UiRt rt mx my button]
  (let [^INode hit (layout/hit-test rt (double mx) (double my))
        hit-idx (if hit (.getIdx hit) -1)
        handled? (boolean (dispatch-click! rt mx my button :left-click))]
    (if handled?
      (do
        (rt/set-drag-node-idx! rt hit-idx)
        (rt/set-drag-start-mx! rt (double mx))
        (rt/set-drag-start-my! rt (double my))
        (rt/set-drag-start-ms! rt (System/currentTimeMillis))
        (rt/set-dragging?! rt false)
        (gain-focus! rt hit-idx)
        true)
      (if (and hit (text-editable? hit))
        ;; An editable text field still needs focus to receive typing even
        ;; when its click has no handler (regression from 5d94832ce, which
        ;; made unhandled clicks clear focus — field clicks have no handler,
        ;; so the info-area name/password fields died). Editable wins over
        ;; drag arming: a field nested under a drag-capable ancestor must
        ;; focus, not arm the ancestor's drag.
        (do (gain-focus! rt hit-idx) false)
        (let [drag-idx (drag-armed-node-idx rt hit)]
          (if (>= drag-idx 0)
            ;; Drag-only nodes (scrollbar thumbs): arm the drag chain so the
            ;; :drag/:drag-start handlers fire on mouse-move. The press is
            ;; claimed — the vanilla container must not also process it.
            (do
              (rt/set-drag-node-idx! rt drag-idx)
              (rt/set-drag-start-mx! rt (double mx))
              (rt/set-drag-start-my! rt (double my))
              (rt/set-drag-start-ms! rt (System/currentTimeMillis))
              (rt/set-dragging?! rt false)
              true)
            (do
              ;; Visual-only nodes (for example background images) must not
              ;; claim the click. Clear UI state so the vanilla container can
              ;; process the same mouse press for an inventory slot.
              (rt/set-drag-node-idx! rt -1)
              (rt/set-dragging?! rt false)
              (gain-focus! rt -1)
              false)))))))

(defn dispatch-mouse-release! [^UiRt rt mx my button]
  (let [was-dragging? (rt/dragging? rt)]
    (when was-dragging?
      (when-let [handlers (rt/get-event-handlers rt (rt/drag-node-idx rt) :drag-stop)]
        (when-let [^INode node (rt/node-by-idx rt (rt/drag-node-idx rt))]
          (let [evt {:x mx :y my :button button :node-idx (rt/drag-node-idx rt)
                     :start-mx (rt/drag-start-mx rt) :start-my (rt/drag-start-my rt)}]
            (invoke-handlers! :drag-stop handlers rt node evt)))))
    (rt/set-dragging?! rt false)
    (rt/set-drag-node-idx! rt -1)))

(defn dispatch-mouse-drag! [^UiRt rt mx my button]
  (let [node-idx (rt/drag-node-idx rt)]
    (if (>= node-idx 0)
      (do
        (if (rt/dragging? rt)
          (when-let [handlers (rt/get-event-handlers rt node-idx :drag)]
            (when-let [^INode node (rt/node-by-idx rt node-idx)]
              (let [evt {:x mx :y my :button button :node-idx node-idx
                         :dx (- (double mx) (rt/drag-start-mx rt))
                         :dy (- (double my) (rt/drag-start-my rt))
                         :start-mx (rt/drag-start-mx rt)
                         :start-my (rt/drag-start-my rt)}]
                (invoke-handlers! :drag handlers rt node evt))))
          (let [dx (Math/abs (- (double mx) (rt/drag-start-mx rt)))
                dy (Math/abs (- (double my) (rt/drag-start-my rt)))]
            (when (or (> dx 4.0) (> dy 4.0))
              (rt/set-dragging?! rt true)
              (when-let [handlers (rt/get-event-handlers rt node-idx :drag-start)]
                (when-let [^INode node (rt/node-by-idx rt node-idx)]
                  (let [evt {:x mx :y my :button button :node-idx node-idx
                             :start-mx (rt/drag-start-mx rt)
                             :start-my (rt/drag-start-my rt)}]
                    (invoke-handlers! :drag-start handlers rt node evt)))))))
        true)
      false)))

(defn unbind-subtree! [^UiRt rt ^INode node]
  (rt/unbind-subtree! rt node))
