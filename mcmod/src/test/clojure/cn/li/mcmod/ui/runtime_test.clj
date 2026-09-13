(ns cn.li.mcmod.ui.runtime-test
  "UiRt 运行时核心测试：build!/flush!/resize!/dispose!。"
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.slot-write :as slot-write]
            [cn.li.mcmod.ui.dsl :as dsl]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.layout :as layout])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

;; ============================================================================
;; create-runtime / dispose!
;; ============================================================================

(deftest create-and-dispose
  (let [r (rt/create-runtime)]
    (is (instance? UiRt r))
    (is (false? (rt/disposed? r)))
    (rt/dispose! r)
    (is (rt/disposed? r))))

;; ============================================================================
;; build! — 基本树构建
;; ============================================================================

(deftest build-simple-tree
  (let [r (rt/create-runtime)
        spec (dsl/group {:id :root :w 200 :h 100}
               (dsl/box {:id :panel :x 10 :y 10 :w 180 :h 80}
                 (dsl/text {:id :label :text "Hello" :x 5 :y 5})))]
    (rt/build! r spec)
    (let [root (rt/node-by-id r :root)]
      (is (instance? INode root))
      (is (= :root (.getId ^INode root)))
      (is (= :group (.getKind ^INode root))))
    (let [panel (rt/node-by-id r :panel)]
      (is (instance? INode panel))
      (is (= :box (.getKind ^INode panel))))
    (let [label (rt/node-by-id r :label)]
      (is (instance? INode label))
      (is (= :text (.getKind ^INode label))))))

(deftest build-tree-dirty
  (let [r (rt/create-runtime)]
    (is (rt/tree-dirty? r))
    (rt/build! r (dsl/group {}))
    (is (rt/tree-dirty? r) "build! 后应标记 tree-dirty")))

;; ============================================================================
;; node-by-id / node-by-idx
;; ============================================================================

(deftest node-lookup
  (let [r (rt/create-runtime)]
    (rt/build! r (dsl/group {:id :root}
                   (dsl/text {:id :t1})))
    (is (some? (rt/node-by-id r :root)))
    (is (some? (rt/node-by-id r :t1)))
    (is (nil? (rt/node-by-id r :nonexistent)))
    ;; idx: root=0, t1=1
    (is (some? (rt/node-by-idx r 0)))
    (is (some? (rt/node-by-idx r 1)))
    (is (nil? (rt/node-by-idx r 99)))))

;; ============================================================================
;; flush! — 脏绑定冲刷
;; ============================================================================

(deftest flush-dirty-bindings
  (let [r (rt/create-runtime)
        s (sig/signal-d 0.0)
        n (node/create-node 0 :flush-test :box {} 1 1 {})
        apply-count (atom 0)
        writer (fn [nd src]
                 (swap! apply-count inc)
                 (sig/sget-d src))
        b (sig/bind! s n writer (rt/get-dirty-bindings-q r))]
    ;; sset 不同值 → Binding 入队
    (sig/sset-d! s 1.0)
    (is (= 1 (.size (rt/get-dirty-bindings-q r))))
    ;; flush → 调 applyBinding
    (rt/flush! r)
    (is (= 1 @apply-count))
    (is (= 0 (.size (rt/get-dirty-bindings-q r))))))

;; ============================================================================
;; resize!
;; ============================================================================

(deftest resize-shortcircuit
  (let [r (rt/create-runtime)]
    (rt/resize! r 800.0 600.0)
    (is (rt/tree-dirty? r))
    (rt/clear-tree-dirty! r)
    ;; 同尺寸 → 不标记
    (rt/resize! r 800.0 600.0)
    (is (not (rt/tree-dirty? r)))
    ;; 不同尺寸 → 标记
    (rt/resize! r 1024.0 768.0)
    (is (rt/tree-dirty? r))))

;; ============================================================================
;; user-signals
;; ============================================================================

(deftest user-signal-store
  (let [r (rt/create-runtime)
        s (sig/signal-d 0.75)]
    (rt/put-user-signal! r :cp-percent s)
    (let [got (rt/user-signal r :cp-percent)]
      (is (== 0.75 (sig/sget-d got))))))

;; ============================================================================
;; 事件注册
;; ============================================================================

(deftest event-registration
  (let [r (rt/create-runtime)
        called? (atom false)]
    (rt/register-event! r 0 :left-click (fn [rt-node evt] (reset! called? true)))
    (let [handlers (rt/get-event-handlers r 0 :left-click)]
      (is (= 1 (count handlers))))
    (is (nil? (rt/get-event-handlers r 0 :right-click)))
    (rt/remove-node-events! r 0)
    (is (nil? (rt/get-event-handlers r 0 :left-click)))))

;; ============================================================================
;; list-set! — binding cleanup on item removal
;; ============================================================================

(deftest list-set-clears-bindings-on-rebuild
  (let [r (rt/create-runtime)
        spec (dsl/group {:id :root}
               (dsl/list-node {:id :entries :w 100 :h 40
                               :template (dsl/box {:id :row :w 100 :h 16})}))
        _ (rt/build! r spec)
        s (sig/signal-d 0.5)
        writer (slot-write/resolve-sig-writer (get node/kinds :box) :hover-tint)]
    (ui/list-set! r :entries [1 2]
      (fn [_rt item-root _item]
        (let [b (sig/bind! s item-root writer (rt/get-dirty-bindings-q r))]
          (rt/register-binding! r (.getIdx item-root) b))))
    (is (pos? (rt/binding-count r)))
    (ui/list-set! r :entries [] (fn [_ _ _] nil))
    (is (zero? (rt/binding-count r)))
    (sig/sset-d! s 1.0)
    (rt/flush! r)
    (is (zero? (.size (rt/get-dirty-bindings-q r))))))

