(ns cn.li.mcmod.runtime.ui-provider
  (:require [cn.li.mcmod.ui.runtime :as runtime]
            [cn.li.mcmod.ui.layout :as layout]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.signal :as signal]
            [cn.li.mcmod.ui.node :as node]))

(defn runtime-provider [_]
  {:user-signal #'runtime/user-signal :dispose! #'runtime/dispose! :resize! #'runtime/resize!
   :flush! #'runtime/flush! :put-user-signal! #'runtime/put-user-signal!
   :clock-ms-sig #'runtime/clock-ms-sig :partial-ticks-sig #'runtime/partial-ticks-sig
   :game-ticks-sig #'runtime/game-ticks-sig :get-tape-arr #'runtime/get-tape-arr
   :focus-idx #'runtime/focus-idx :hovered-idx #'runtime/hovered-idx
   :node-by-idx #'runtime/node-by-idx :set-hovered-idx! #'runtime/set-hovered-idx!
   :ensure-layout! #'layout/ensure-layout! :ensure-tape! #'layout/ensure-tape!
   :hit-test #'layout/hit-test
   :dispatch-editable-key! #'events/dispatch-editable-key! :dispatch-key! #'events/dispatch-key!
   :dispatch-char! #'events/dispatch-char! :dispatch-mouse-press! #'events/dispatch-mouse-press!
   :dispatch-mouse-release! #'events/dispatch-mouse-release!
   :dispatch-mouse-drag! #'events/dispatch-mouse-drag! :dispatch-scroll! #'events/dispatch-scroll!
   :sset-l! #'signal/sset-l! :sset-d! #'signal/sset-d!
   :push-clip-sentinel (constantly layout/push-clip-sentinel)
   :pop-clip-sentinel (constantly layout/pop-clip-sentinel)
   :push-transform-sentinel (constantly layout/push-transform-sentinel)
   :pop-transform-sentinel (constantly layout/pop-transform-sentinel)
   :flag-hovered (constantly node/FLAG-HOVERED)
   :flag-focused (constantly node/FLAG-FOCUSED)
   :flag-render-dirty (constantly node/FLAG-RENDER-DIRTY)})
