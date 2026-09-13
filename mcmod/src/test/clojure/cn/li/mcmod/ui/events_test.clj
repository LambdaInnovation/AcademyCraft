(ns cn.li.mcmod.ui.events-test
  "Regression tests for UI event dispatch.
   dispatch-char! must deliver the typed character as a real string: char[]
   does not override toString, so the old (Character/toChars ...) .toString
   produced the identity string (\"[C@<hash>\") and every codepoint arrived
   as (first ...) = \\[ in :change-content consumers (developer console input
   showed [[[[[[ regardless of the key pressed).

   dispatch-mouse-press! must focus editable text nodes on click even when
   the click has no handler — 5d94832ce made unhandled clicks clear focus
   (so visual-only nodes no longer swallow vanilla inventory-slot clicks),
   which also killed typing in handler-less editable fields (wireless-node
   info-area name/password, wireless-tab password boxes).

   Editable text scrolls horizontally when wider than the field (upstream
   TextBox.checkCaretRegion): the x-offset dslot goes negative so the tail
   stays visible instead of overflowing the box."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.layout :as layout]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.client.platform-bridge :as bridge])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

;; Stub the (private) font-width bridge: 8px per char, like a monospace
;; fallback — the loader installs the real one at runtime.
(alter-var-root (ns-resolve 'cn.li.mcmod.client.platform-bridge 'font-text-width-fn)
  (constantly (fn [_ text _] (* 8.0 (count (str text))))))

(defn- char-runtime
  "Minimal runtime with a single focusable group node."
  ^UiRt []
  (let [r (rt/create-runtime)]
    (rt/build! r {:kind :group
                  :props {:id :root :x 0.0 :y 0.0 :w 100.0 :h 100.0}})
    (rt/resize! r 200.0 200.0)
    r))

(defn- field-runtime
  "Runtime with one 40px editable text field, one 40px masked editable field
   and one visual-only box."
  ^UiRt []
  (let [r (rt/create-runtime)]
    (rt/build! r {:kind :group
                  :props {:id :root :x 0.0 :y 0.0 :w 300.0 :h 100.0}
                  :children [{:kind :text
                              :props {:id :field :x 10.0 :y 10.0 :w 40.0 :h 10.0
                                      :text "" :font-size 8.0 :editable? true}}
                             {:kind :text
                              :props {:id :pwd :x 10.0 :y 30.0 :w 40.0 :h 10.0
                                      :text "" :font-size 8.0 :editable? true :masked? true}}
                             {:kind :box
                              :props {:id :deco :x 10.0 :y 50.0 :w 80.0 :h 10.0
                                      :fill 0xFFFFFFFF}}]})
    (rt/resize! r 300.0 100.0)
    r))

(defn- text-value [^INode n]
  (str (.getOSlot n 0)))

(defn- capture-char
  "Register a :change-content handler on `id` returning the :char it received."
  [^UiRt rt id]
  (let [captured (atom nil)]
    (events/on! rt id :change-content
      (fn [_ _ evt] (reset! captured (:char evt))))
    (events/gain-focus! rt (.getIdx (rt/node-by-id rt id)))
    captured))

(deftest dispatch-char-delivers-real-char-string-test
  (let [rt (char-runtime)
        captured (capture-char rt :root)]
    (events/dispatch-char! rt (int \a))
    (is (= "a" @captured))))

(deftest dispatch-char-handles-surrogate-pair-test
  (let [rt (char-runtime)
        captured (capture-char rt :root)
        ;; U+1F600 — outside the BMP, Character/toChars returns 2 chars.
        codepoint 0x1F600]
    (events/dispatch-char! rt codepoint)
    (is (= (str (char 0xD83D) (char 0xDE00)) @captured))))

(deftest dispatch-key-returns-true-when-focus-node-has-key-handler-test
  "dispatch-key! must report whether a :key handler consumed the key (returned
   truthy) — hosts rely on it to let a focused popup consume ESC (closing only
   the popup) instead of the host closing the whole screen."
  (let [rt (char-runtime)
        keys (atom [])
        cover (.getIdx (rt/node-by-id rt :root))]
    (events/on! rt :root :key
      (fn [_ _ evt] (swap! keys conj (:key-code evt))))
    (events/gain-focus! rt cover)
    (is (not (nil? (events/dispatch-key! rt 256 0 0 0)))
        "consumed key returns a truthy value")
    (is (= [256] @keys))))

(deftest dispatch-key-returns-nil-without-focused-key-handler-test
  (let [rt (char-runtime)]
    ;; No focus — nothing to dispatch to.
    (is (nil? (events/dispatch-key! rt 256 0 0 0)))
    ;; Focus on a node with no :key handler — nothing invoked.
    (events/gain-focus! rt (.getIdx (rt/node-by-id rt :root)))
    (is (nil? (events/dispatch-key! rt 256 0 0 0)))))

(deftest dispatch-key-returns-nil-when-handler-declines-test
  "A :key handler that returns nil does not consume the key — the developer
   console's command input declines ESC, so the host still closes the UI."
  (let [rt (char-runtime)
        calls (atom 0)]
    (events/on! rt :root :key
      (fn [_ _ _] (swap! calls inc) nil))
    (events/gain-focus! rt (.getIdx (rt/node-by-id rt :root)))
    (is (nil? (events/dispatch-key! rt 256 0 0 0)))
    (is (= 1 @calls) "handler still ran; only its return value decides consumption")))

(deftest dispatch-char-unfocused-is-noop-test
  (let [rt (char-runtime)
        captured (atom :unset)]
    (events/on! rt :root :change-content
      (fn [_ _ evt] (reset! captured (:char evt))))
    ;; No gain-focus! — dispatch must not reach the handler.
    (events/dispatch-char! rt (int \a))
    (is (= :unset @captured))))

(deftest click-on-editable-field-focuses-and-types-test
  (let [rt (field-runtime)]
    (layout/ensure-layout! rt)
    (layout/ensure-tape! rt)
    ;; Click on the editable field (no handler) — must gain focus.
    (events/dispatch-mouse-press! rt 15.0 15.0 0)
    (is (= (.getIdx (rt/node-by-id rt :field)) (rt/focus-idx rt)))
    ;; Typing then appends to the field.
    (events/dispatch-editable-key! rt 0 (char \a))
    (is (= "a" (text-value (rt/node-by-id rt :field))))))

(deftest click-on-visual-only-node-clears-focus-test
  (let [rt (field-runtime)]
    (layout/ensure-layout! rt)
    (layout/ensure-tape! rt)
    (events/dispatch-mouse-press! rt 15.0 15.0 0)
    (is (= (.getIdx (rt/node-by-id rt :field)) (rt/focus-idx rt)))
    ;; Click on a handler-less visual box — focus must clear (vanilla
    ;; inventory-slot click-through), NOT stick to the box.
    (events/dispatch-mouse-press! rt 15.0 55.0 0)
    (is (= -1 (rt/focus-idx rt)))))

(deftest reclick-editable-restores-typing-test
  (let [rt (field-runtime)]
    (layout/ensure-layout! rt)
    (layout/ensure-tape! rt)
    (events/dispatch-mouse-press! rt 15.0 15.0 0)
    (events/dispatch-editable-key! rt 0 (char \a))
    ;; Focus lost (click elsewhere), then re-click the field — typing resumes.
    (events/dispatch-mouse-press! rt 15.0 55.0 0)
    (is (= -1 (rt/focus-idx rt)))
    (events/dispatch-mouse-press! rt 15.0 15.0 0)
    (events/dispatch-editable-key! rt 0 (char \b))
    (is (= "ab" (text-value (rt/node-by-id rt :field))))))

(defn- thumb-runtime
  "Runtime with a drag-only thumb (no :left-click handler — the settings/about
   scrollbar shape): an unhandled press must arm the drag chain anyway."
  ^UiRt []
  (let [r (rt/create-runtime)]
    (rt/build! r {:kind :group
                  :props {:id :root :x 0.0 :y 0.0 :w 200.0 :h 200.0}
                  :children [{:kind :image
                              :props {:id :thumb :x 180.0 :y 20.0 :w 10.0 :h 30.0
                                      :src "academy:textures/guis/bar.png"}}]})
    (rt/resize! r 200.0 200.0)
    (let [drags (atom [])
          starts (atom 0)]
      (events/on! r :thumb :drag
        (fn [_ _ evt] (swap! drags conj (:dy evt))))
      (events/on! r :thumb :drag-start
        (fn [_ _ _] (swap! starts inc)))
      {:rt r :drags drags :starts starts})))

(deftest unhandled-press-arms-drag-only-node-test
  (let [{:keys [rt drags starts]} (thumb-runtime)
        _ (layout/ensure-layout! rt)
        _ (layout/ensure-tape! rt)]
    ;; Press on the thumb: no :left-click handler anywhere — the old code
    ;; cleared the drag chain here and the scrollbar could never be dragged.
    (is (true? (events/dispatch-mouse-press! rt 185.0 35.0 0))
        "drag-only node claims the press")
    ;; Small moves stay under the 4px arming threshold.
    (events/dispatch-mouse-drag! rt 186.0 36.0 0)
    (is (zero? @starts))
    ;; Past the threshold the drag arms (:drag-start fires on the arming
    ;; move; :dy is measured from the press point, not incrementally).
    (events/dispatch-mouse-drag! rt 185.0 50.0 0)
    (is (= 1 @starts))
    (events/dispatch-mouse-drag! rt 185.0 60.0 0)
    (is (= [25.0] @drags))
    (events/dispatch-mouse-drag! rt 185.0 70.0 0)
    (is (= [25.0 35.0] @drags))
    (events/dispatch-mouse-release! rt 185.0 70.0 0)))

(deftest press-on-visual-only-node-still-passes-through-test
  (let [{:keys [rt drags starts]} (thumb-runtime)
        _ (layout/ensure-layout! rt)
        _ (layout/ensure-tape! rt)]
    ;; Press beside the thumb — no drag handlers in the chain, so the press
    ;; stays unclaimed (vanilla inventory-slot click-through preserved).
    (is (false? (events/dispatch-mouse-press! rt 30.0 30.0 0)))
    (events/dispatch-mouse-drag! rt 35.0 60.0 0)
    (is (zero? @starts))
    (is (empty? @drags))))

(defn- x-offset [rt id]
  (.getDSlot ^INode (rt/node-by-id rt id) 1))

(deftest editable-text-scrolls-when-overflowing-test
  (let [rt (field-runtime)]
    (events/gain-focus! rt (.getIdx (rt/node-by-id rt :field)))
    ;; 5 chars × 8px = 40px = field width — no scroll.
    (doseq [c "abcde"] (events/dispatch-editable-key! rt 0 c))
    (is (== 0.0 (x-offset rt :field)))
    ;; 6th char overflows to 48px — scroll left so the tail stays visible.
    (events/dispatch-editable-key! rt 0 \f)
    (is (== -8.0 (x-offset rt :field)))
    ;; Backspace back to fitting width — offset resets.
    (events/dispatch-editable-key! rt 259 (char 0))
    (is (== 0.0 (x-offset rt :field)))))

(deftest masked-field-scrolls-on-echo-width-test
  (let [rt (field-runtime)]
    (events/gain-focus! rt (.getIdx (rt/node-by-id rt :pwd)))
    ;; 8 typed chars display as 8 stars = 64px > 40px — scroll -24.
    (doseq [c "12345678"] (events/dispatch-editable-key! rt 0 c))
    (is (== -24.0 (x-offset rt :pwd)))
    ;; Programmatic clear + refocus resets the stale scroll.
    (.setOSlot ^INode (rt/node-by-id rt :pwd) 0 "")
    (.setFlag ^INode (rt/node-by-id rt :pwd) 2)
    (events/gain-focus! rt -1)
    (events/gain-focus! rt (.getIdx (rt/node-by-id rt :pwd)))
    (is (== 0.0 (x-offset rt :pwd)))))
