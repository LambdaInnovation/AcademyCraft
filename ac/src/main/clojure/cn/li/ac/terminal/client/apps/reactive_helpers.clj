(ns cn.li.ac.terminal.client.apps.reactive-helpers
  "Shared reactive helpers for terminal apps.
   Replaces find-widget + set-texture! + set-text-color! + set-visible! patterns
   with signal-driven declarative bindings."
  (:require [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.xml :as ui-xml]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.ac.config.modid :as modid]))

;; ============================================================================
;; Load XML + build runtime
;; ============================================================================

(defn load-app
  "Load terminal app XML layout, build runtime. Returns runtime."
  [xml-resource]
  (let [r (rt/create-runtime)
        spec (ui-xml/load-spec (modid/namespaced-path xml-resource))]
    (rt/build! r spec)
    r))

;; ============================================================================
;; Checkbox row (replaces settings checkbox pattern)
;; ============================================================================

(defn checkbox-signal
  "Create a boolean signal bound to a toggle node.
   Usage: (let [s (checkbox-signal r :btn-attack-player)]
            (sig/sset-o! s true))"
  [r id]
  (let [s (sig/signal-o false)]
    (rt/put-user-signal! r id s)
    s))

;; ============================================================================
;; Tab state signal
;; ============================================================================

(defn tab-signal
  "Create tab state signal. ssets change visible bindings.
   Usage: (let [tab (tab-signal r :credits)]
            (sig/sset-o! tab :donate))"
  [r initial-tab]
  (let [s (sig/signal-o initial-tab)]
    (rt/put-user-signal! r :active-tab s)
    s))

;; ============================================================================
;; Scroll offset signal
;; ============================================================================

(defn scroll-signal
  "Create scroll offset signal for scrollable text areas.
   Updated via mouse-scroll handler."
  [r]
  (let [s (sig/signal-d 0.0)]
    (rt/put-user-signal! r :scroll-offset s)
    s))

;; ============================================================================
;; Open app
;; ============================================================================

(defn open-app!
  "Open terminal app screen via bridge."
  [r title]
  (bridge/open-reactive-screen! r (or title "Terminal App")))
