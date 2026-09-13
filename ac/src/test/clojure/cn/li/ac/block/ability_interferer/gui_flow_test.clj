(ns cn.li.ac.block.ability-interferer.gui-flow-test
  "Ability interferer whitelist GUI flow: + opens a VISIBLE focused input,
  Enter confirms (server send + optimistic atom update), blur closes it, the
  same-click focus handoff back to btn_add must NOT close the fresh input,
  + reopens after blur, saved rows are selectable and - removes the selection
  (all driven through the REAL click dispatch + hit-test)."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.block.ability-interferer.gui-reactive :as gui]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.layout :as layout]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.xml :as ui-xml]))

(def ^:private attach-binds! #'gui/attach-binds!)
(def ^:private open-add-input! #'gui/open-add-input!)

(defn- test-runtime []
  (let [r (rt/create-runtime)]
    (rt/build! r (ui-xml/load-spec (modid/namespaced-path "guis/rework/new/page_interfere.xml")))
    (rt/resize! r 640.0 480.0)
    r))

(defn- fake-container []
  {:tile-entity {}
   :minecraft-container nil
   :input-open (atom false)
   :whitelist (atom [])
   :enabled (atom false)
   :range (atom 10.0)
   :energy (atom 0.0)
   :max-energy (atom 10000.0)})

(defn- click-at! [r node-id]
  (layout/ensure-layout! r)
  (let [n (rt/node-by-id r node-id)
        mx (+ (.getAbsX n) 2.0)
        my (+ (.getAbsY n) 2.0)]
    (events/dispatch-mouse-press! r mx my 0)))

(deftest real-click-plus-blur-reopen-cycle-test
  (let [r (test-runtime)
        c (fake-container)
        sent (atom nil)]
    (with-redefs [net-client/send-to-server (fn [& _] (reset! sent true))]
      (attach-binds! r c nil nil nil)
      (click-at! r :btn_add)
      (is (some? (rt/node-by-id r :wl-input)) "first + click opens the input")
      ;; blur by clicking elsewhere
      (click-at! r :panel_config)
      (is (nil? (rt/node-by-id r :wl-input)) "blur closes the input")
      ;; second + click must reopen
      (click-at! r :btn_add)
      (is (some? (rt/node-by-id r :wl-input)) "second + click reopens the input"))))

(deftest row-click-selects-and-remove-deletes-test
  (let [r (test-runtime)
        c (fake-container)
        sent (atom nil)]
    (with-redefs [net-client/send-to-server (fn [owner msg payload cb]
                                              (reset! sent [owner msg payload cb]))]
      (attach-binds! r c nil nil nil)
      (reset! (:whitelist c) ["Alice"])
      ;; render the row (the atom watch fires on the next flush; trigger directly)
      (rt/flush! r)
      (let [row-id (keyword "wl-bg-0")]
        (is (some? (rt/node-by-id r row-id)) "saved entry row is rendered")
        (click-at! r row-id)
        ;; the row click selects — the - button now has a target
        (click-at! r :btn_remove)
        (is (= [] @(:whitelist c)) "- removes the selected entry")
        (is (some? @sent) "remove-from-whitelist sent to server")))))

(deftest confirm-then-second-plus-click-reopens-input-test
  (let [r (test-runtime)
        c (fake-container)
        sent (atom nil)]
    (with-redefs [net-client/send-to-server (fn [& _] (reset! sent true))]
      (attach-binds! r c nil nil nil)
      (click-at! r :btn_add)
      (is (some? (rt/node-by-id r :wl-input)) "first + opens input")
      ;; type + Enter (confirm path, not blur)
      (let [input (rt/node-by-id r :wl-input)]
        (events/gain-focus! r (.getIdx input)))
      (events/dispatch-editable-key! r 65 \A)
      (events/dispatch-editable-key! r 257 nil)
      (is (nil? (rt/node-by-id r :wl-input)) "confirm closes input")
      (is (= ["A"] @(:whitelist c)) "entry saved")
      ;; second + click must reopen
      (click-at! r :btn_add)
      (is (some? (rt/node-by-id r :wl-input)) "second + click reopens input after confirm"))))

(deftest row-text-area-click-selects-test
  "The saved row's NAME TEXT is painted over the row image and wins the
  hit-test; clicks landing on the text must still select the row (the click
  bubble only walks the parent chain, so the sibling row image never sees
  them — the handler must be registered on the text too)."
  (let [r (test-runtime)
        c (fake-container)
        sent (atom nil)]
    (with-redefs [net-client/send-to-server (fn [& _] (reset! sent true))]
      (attach-binds! r c nil nil nil)
      (reset! (:whitelist c) ["Alice"])
      (rt/flush! r)
      (let [text-id (keyword "wl-name-0")
            n (rt/node-by-id r text-id)]
        (is (some? n) "name text node rendered")
        ;; click dead-center of the TEXT (where users actually click)
        (let [mx (+ (.getAbsX n) (/ (.getW n) 2.0))
              my (+ (.getAbsY n) (/ (.getH n) 2.0))]
          (events/dispatch-mouse-press! r mx my 0))
        (click-at! r :btn_remove)
        (is (= [] @(:whitelist c))
            "text-area click selects the row so - can remove it")))))

(deftest add-input-opens-visible-focused-and-confirms-test
  (let [r (test-runtime)
        c (fake-container)
        sent (atom nil)]
    (with-redefs [net-client/send-to-server (fn [owner msg payload cb]
                                              (reset! sent [owner msg payload cb]))]
      (attach-binds! r c nil nil nil)
      (open-add-input! r nil c (atom 0) (atom nil) (.getIdx (rt/node-by-id r :btn_add)))
      (is (some? (rt/node-by-id r :wl-input)) "editable input node created")
      (is (some? (rt/node-by-id r :wl-input-bg)) "visible background box created")
      (is (= (.getIdx (rt/node-by-id r :wl-input)) (rt/focus-idx r))
          "input gains focus immediately (upstream gainFocus)")
      ;; type 'A' then Enter
      (events/dispatch-editable-key! r 65 \A)
      (events/dispatch-editable-key! r 257 nil)
      (is (= ["A"] @(:whitelist c)) "confirm adds to whitelist atom")
      (is (some? @sent) "add-to-whitelist sent to server")
      (is (nil? (rt/node-by-id r :wl-input)) "confirm closes the input"))))

(deftest focus-handoff-back-to-btn-add-keeps-input-test
  (let [r (test-runtime)
        c (fake-container)
        screen {:runtime r
                :signals {:energy (sig/signal-d 0.0) :max-energy (sig/signal-d 1.0)
                          :status (sig/signal-o "x") :gen-speed (sig/signal-o "x")
                          :progress (sig/signal-d 0.0)}
                :container c}]
    (with-redefs [net-client/send-to-server (fn [& _] nil)]
      (attach-binds! r c nil nil nil)
      (open-add-input! r nil c (atom 0) (atom nil) (.getIdx (rt/node-by-id r :btn_add)))
      (let [input-idx (.getIdx (rt/node-by-id r :wl-input))
            btn-add-idx (.getIdx (rt/node-by-id r :btn_add))]
        ;; dispatch-mouse-press! tail: gain-focus!(clicked node) — same click
        (events/gain-focus! r btn-add-idx)
        (is (some? (rt/node-by-id r :wl-input))
            "focus returning to btn_add in the same click must not close the input")
        ;; the per-frame update! re-asserts the input's focus (a re-gain inside
        ;; lost-focus is overwritten by the outer gain-focus!'s set-focus-idx!)
        (gui/update! screen)
        (is (= input-idx (rt/focus-idx r))
            "per-frame update re-asserts the input focus")
        ;; typing + Enter now work without a manual click
        (events/dispatch-editable-key! r 65 \A)
        (events/dispatch-editable-key! r 257 nil)
        (is (= ["A"] @(:whitelist c)) "typing/Enter work straight after +")
        ;; a REAL blur elsewhere closes it (upstream LostFocusEvent -> dispose)
        (events/gain-focus! r 0)
        (is (nil? (rt/node-by-id r :wl-input)) "blur closes the input")
        ;; and + can open a fresh input afterwards (issue: second click dead)
        (open-add-input! r nil c (atom 0) (atom nil) (.getIdx (rt/node-by-id r :btn_add)))
        (is (some? (rt/node-by-id r :wl-input)) "second open works after blur")))))
