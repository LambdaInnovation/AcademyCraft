(ns cn.li.ac.content.ability.teleporter.location-teleport-reactive-test
  "Reactive loctele GUI flow, driven through the REAL hit-test + click/key
  dispatch (same harness as gui_flow_test): clicking the \"Add...\" input
  enters edit mode, typing keeps focus + hides the placeholder, Enter
  confirms over the network, scroll rebuilds preserve the in-flight input
  session (upstream ElementList scrolls without rebuilding), and the
  rows-tick animates the upstream Blend values (menu grow-in, row wash)."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.managed-screens :as managed-screens]
            [cn.li.ac.content.ability.teleporter.location-teleport-reactive :as lr]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.layout :as layout]
            [cn.li.mcmod.ui.runtime :as rt])
  (:import [cn.li.mcmod.ui.node INode]))

(defn- reset-fixture [f]
  (managed-screens/reset-managed-screen-state-for-test!)
  (try (f)
       (finally
         (managed-screens/reset-managed-screen-state-for-test!))))

(use-fixtures :each reset-fixture)

(defn- test-runtime []
  (let [r (rt/create-runtime)]
    (rt/build! r (#'lr/root-spec))
    (rt/resize! r 640.0 480.0)
    (rt/put-user-signal! r :scroll-idx (atom 0))
    (rt/put-user-signal! r :rows-open-ms (atom (#'lr/now-ms)))
    (rt/put-user-signal! r :rows-base-ms (atom (#'lr/now-ms)))
    (#'lr/rebuild-list! r "player-a" [:ctx "ctx-a"] true)
    (#'lr/attach-rows-tick! r)
    r))

(defn- seed-locations! [names]
  (managed-screens/update-screen-state!
    :ac/location-teleport [:ctx "ctx-a"]
    {:locations [] :exp 0.0 :current-pos nil :limits {}}
    (fn [_]
      {:locations (vec (for [[i name] (map-indexed vector names)]
                         {:name name
                          :world-id "minecraft:overworld"
                          :x 0.0 :y 64.0 :z (double i)
                          :can-perform? true
                          :cp-cost 10.0}))
       :exp 0.0
       :current-pos {:x 1.0 :y 2.0 :z 3.0 :world-id "minecraft:overworld"}
       :limits {}})))

(defn- add-row-of [r]
  (let [row-nodes @(rt/user-signal r :row-nodes)]
    (some (fn [[_ row]] (when (:input row) row)) row-nodes)))

(defn- add-item-idx-of [r]
  (let [row-nodes @(rt/user-signal r :row-nodes)]
    (some (fn [[i row]] (when (:input row) i)) row-nodes)))

(defn- input-text [^INode input]
  (str (.getOSlot input 0)))

(defn- click-at! [r ^INode n]
  (layout/ensure-layout! r)
  (layout/ensure-tape! r)
  (events/dispatch-mouse-press! r (+ (.getAbsX n) 2.0) (+ (.getAbsY n) 2.0) 0))

(defn- advance-ticks!
  "Run the per-frame ticks once: push the partial-ticks signal (the computed-o
  marks the tick binding dirty) then flush the dirty-bindings queue.
  SigD.dSet only notifies on a VALUE CHANGE — alternate the pushed value so
  every call actually re-runs the ticks."
  [r]
  (let [^cn.li.mcmod.uipojo.signal.SigD s (rt/partial-ticks-sig r)]
    (.dSet s (- 1.0 (.dGet s))))
  (rt/flush! r))

(deftest rows-build-with-highlight-box-behind-and-row-node-map-test
  (let [r (test-runtime)
        row-nodes @(rt/user-signal r :row-nodes)]
    (is (= 1 (count row-nodes)) "default state: just the add row")
    (let [[item-idx row] (first row-nodes)]
      (is (some? (:input row)))
      (is (some? (:hl row)))
      (is (some? (:ok row)))
      (is (nil? (:name row)) "the add row has no name node")
      (is (= 0 (long (:n row))) "add row is the last row (n = total locations)")
      ;; The highlight box is the FIRST template child — painted and
      ;; hit-tested BEHIND the row content, so it never steals clicks from
      ;; the input/buttons (node idx follows creation order).
      (is (< (.getIdx ^INode (:hl row)) (.getIdx ^INode (:input row)))))))

(deftest clicking-the-add-input-enters-edit-mode-and-keeps-focus-test
  (let [r (test-runtime)
        row (add-row-of r)
        ^INode input (:input row)]
    (is (.isVisible ^INode (:ph row)) "placeholder visible before the click")
    (click-at! r input)
    (is (= (.getIdx input) (rt/focus-idx r)) "click focuses the input")
    (is (not (.isVisible ^INode (:ph row))) "placeholder hidden while editing")
    ;; typing lands in the input and keeps focus + placeholder state
    (events/dispatch-editable-key! r 65 \A)
    (events/dispatch-editable-key! r 66 \B)
    (is (= "AB" (input-text input)) "typed characters land in the input")
    (is (= (.getIdx input) (rt/focus-idx r)) "typing keeps focus")
    (is (not (.isVisible ^INode (:ph row))) "placeholder stays hidden with text")
    ;; clicking the input again while focused is a no-op (text preserved)
    (click-at! r input)
    (is (= "AB" (input-text input)) "re-clicking the focused input keeps the text")))

(deftest enter-confirms-the-name-over-the-network-test
  (let [r (test-runtime)
        sent (atom [])]
    (with-redefs [net-client/send-to-server (fn [owner msg payload _cb]
                                              (swap! sent conj [owner msg payload]))]
      (let [row (add-row-of r)
            ^INode input (:input row)]
        (click-at! r input)
        (events/dispatch-editable-key! r 72 \H)
        (events/dispatch-editable-key! r 73 \I)
        (events/dispatch-editable-key! r 257 nil) ;; Enter
        (is (= 1 (count @sent)))
        (let [[_ msg payload] (first @sent)]
          (is (= "ability:req/location-teleport/add" msg))
          (is (= {:name "HI"} payload)))
        (is (= -1 (rt/focus-idx r)) "confirm leaves edit mode (upstream removeFocus)")))))

(deftest scrolling-preserves-the-in-flight-input-session-test
  ;; Upstream ElementList scrolls WITHOUT rebuilding, so a focused TextBox
  ;; keeps its text; our scroll rebuild destroys the node — the rebuild must
  ;; restore focus + text onto the fresh input (data rebuilds, i.e. after
  ;; add/remove, intentionally do NOT restore — upstream leaves edit mode).
  (let [r (rt/create-runtime)]
    (rt/build! r (#'lr/root-spec))
    (rt/resize! r 640.0 480.0)
    (rt/put-user-signal! r :scroll-idx (atom 0))
    (rt/put-user-signal! r :rows-open-ms (atom (#'lr/now-ms)))
    (rt/put-user-signal! r :rows-base-ms (atom (#'lr/now-ms)))
    (seed-locations! (mapv #(str "loc" %) (range 8)))
    (#'lr/rebuild-list! r "player-a" [:ctx "ctx-a"] true)
    (#'lr/attach-rows-tick! r)
    (let [row (add-row-of r)
          ^INode input (:input row)]
      (is (some? input) "the add row is reachable with 8 saved locations
                          (it is the last slot of the scrollable content)")
      (click-at! r input)
      (events/dispatch-editable-key! r 65 \A)
      (events/dispatch-editable-key! r 66 \B)
      (events/dispatch-editable-key! r 67 \C)
      (is (= "ABC" (input-text input)))
      ;; scroll down — rebuild WITHOUT re-fading (attach-scroll! bumps the
      ;; scroll-idx then calls rebuild-list! with reset-fade? false)
      (swap! (rt/user-signal r :scroll-idx) inc)
      (#'lr/rebuild-list! r "player-a" [:ctx "ctx-a"] false)
      (let [row2 (add-row-of r)
            ^INode input2 (:input row2)]
        (is (= "ABC" (input-text input2)) "typed text survives the scroll rebuild")
        (is (= (.getIdx input2) (rt/focus-idx r)) "focus survives the scroll rebuild")
        (is (not (.isVisible ^INode (:ph row2))) "placeholder stays hidden")))))

(deftest rows-tick-animates-menu-grow-and-row-wash-test
  ;; Upstream: menu Blend(0, 0.4) on transform.height; every row washed
  ;; white at blend * (0.1 idle | 0.4 hovered) — wrapBack's colorRect +
  ;; the add-template Tint. The tick runs on a flush after the
  ;; partial-ticks signal pushes (per-frame pull).
  (let [r (test-runtime)
        ^INode menu (rt/node-by-id r :menu)
        row (add-row-of r)
        ^INode hl (:hl row)
        base @(rt/user-signal r :rows-base-ms)]
    (advance-ticks! r)
    (is (< (.getH menu) 10.0) "menu starts collapsed (grows from 0)")
    (is (< (.getDSlot hl 3) 0.01) "row wash starts transparent")
    ;; now-ms carries a ^long return hint, so the compiled call sites cast
    ;; the var root to IFn$L — the stub must be prim-hinted too.
    (with-redefs-fn {#'lr/now-ms (fn ^long [] (+ (long base) 500))}
      (fn [] (advance-ticks! r)))
    (is (= 530.0 (.getH menu)) "menu fully grown after 0.4s")
    (is (< (Math/abs (- 0.1 (.getDSlot hl 3))) 1.0e-9)
        "unhovered row washed at idle alpha 0.1")
    ;; hovering the row brightens the wash to upstream AlphaHighlight 0.4
    (rt/set-hovered-idx! r (add-item-idx-of r))
    (with-redefs-fn {#'lr/now-ms (fn ^long [] (+ (long base) 500))}
      (fn [] (advance-ticks! r)))
    (is (< (Math/abs (- 0.4 (.getDSlot hl 3))) 1.0e-9)
        "hovered row washed at 0.4")))
