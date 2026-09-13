(ns cn.li.platform.neutral.ui
  (:require [clojure.string :as str])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]))

(def ^:private function-operations
  [:user-signal :dispose! :resize! :flush! :put-user-signal! :clock-ms-sig
   :partial-ticks-sig :game-ticks-sig :get-tape-arr :focus-idx :hovered-idx
   :node-by-idx :set-hovered-idx! :ensure-layout! :ensure-tape! :hit-test
   :dispatch-editable-key! :dispatch-key! :dispatch-char! :dispatch-mouse-press!
   :dispatch-mouse-release! :dispatch-mouse-drag! :dispatch-scroll! :sset-l! :sset-d!])
(def ^:private value-operations
  [:push-clip-sentinel :pop-clip-sentinel :push-transform-sentinel :pop-transform-sentinel
   :flag-hovered :flag-focused :flag-render-dirty])

(defn- unavailable [operation] (throw (IllegalStateException. (str "UI provider unavailable: " operation))))
(def user-signal (fn [& _] (unavailable :user-signal)))
(def dispose! (fn [& _] (unavailable :dispose!)))
(def resize! (fn [& _] (unavailable :resize!)))
(def flush! (fn [& _] (unavailable :flush!)))
(def put-user-signal! (fn [& _] (unavailable :put-user-signal!)))
(def clock-ms-sig (fn [& _] (unavailable :clock-ms-sig)))
(def partial-ticks-sig (fn [& _] (unavailable :partial-ticks-sig)))
(def game-ticks-sig (fn [& _] (unavailable :game-ticks-sig)))
(def get-tape-arr (fn [& _] (unavailable :get-tape-arr)))
(def focus-idx (fn [& _] (unavailable :focus-idx)))
(def hovered-idx (fn [& _] (unavailable :hovered-idx)))
(def node-by-idx (fn [& _] (unavailable :node-by-idx)))
(def set-hovered-idx! (fn [& _] (unavailable :set-hovered-idx!)))
(def ensure-layout! (fn [& _] (unavailable :ensure-layout!)))
(def ensure-tape! (fn [& _] (unavailable :ensure-tape!)))
(def hit-test (fn [& _] (unavailable :hit-test)))
(def dispatch-editable-key! (fn [& _] (unavailable :dispatch-editable-key!)))
(def dispatch-key! (fn [& _] (unavailable :dispatch-key!)))
(def dispatch-char! (fn [& _] (unavailable :dispatch-char!)))
(def dispatch-mouse-press! (fn [& _] (unavailable :dispatch-mouse-press!)))
(def dispatch-mouse-release! (fn [& _] (unavailable :dispatch-mouse-release!)))
(def dispatch-mouse-drag! (fn [& _] (unavailable :dispatch-mouse-drag!)))
(def dispatch-scroll! (fn [& _] (unavailable :dispatch-scroll!)))
(def sset-l! (fn [& _] (unavailable :sset-l!)))
(def sset-d! (fn [& _] (unavailable :sset-d!)))
(def push-clip-sentinel nil)
(def pop-clip-sentinel nil)
(def push-transform-sentinel nil)
(def pop-transform-sentinel nil)
(def flag-hovered nil)
(def flag-focused nil)
(def flag-render-dirty nil)
(doseq [operation function-operations]
  (intern *ns* (symbol (name operation)) (fn [& _] (unavailable operation))))
(doseq [operation value-operations]
  (intern *ns* (symbol (name operation)) nil))

(defn- facade-var [operation fallback]
  (let [facade-ns (the-ns 'cn.li.platform.neutral.ui)
        operation-symbol (symbol (name operation))]
    (or (ns-resolve facade-ns operation-symbol)
        (intern facade-ns operation-symbol fallback))))

(def FLAG-HOVERED nil)
(def FLAG-FOCUSED nil)
(def FLAG-RENDER-DIRTY nil)

;; UiRt is a neutral Java API.  Keeping this tiny predicate on the AOT side
;; avoids making renderers statically require the Clojure UI runtime namespace.
(defn disposed? [^UiRt rt]
  (.isDisposed rt))

(defn install! [operations]
  (let [expected (into #{} (concat function-operations value-operations))]
    (when (or (not= expected (set (keys operations))) (some (complement ifn?) (vals operations)))
      (throw (ex-info "UI provider contract mismatch" {:actual (sort (keys operations))})))
    (doseq [operation function-operations]
      (alter-var-root (facade-var operation (fn [& _] (unavailable operation)))
                      (constantly (get operations operation))))
    (doseq [operation value-operations]
      (alter-var-root (facade-var operation nil)
                      (constantly ((get operations operation))))))
  (let [facade-ns (the-ns 'cn.li.platform.neutral.ui)]
    (alter-var-root #'FLAG-HOVERED (constantly (var-get (ns-resolve facade-ns 'flag-hovered))))
    (alter-var-root #'FLAG-FOCUSED (constantly (var-get (ns-resolve facade-ns 'flag-focused))))
    (alter-var-root #'FLAG-RENDER-DIRTY (constantly (var-get (ns-resolve facade-ns 'flag-render-dirty)))))
  nil)
