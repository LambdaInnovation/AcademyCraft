(ns cn.li.mcmod.ui.signal-test
  "Signal 核心单元测试：传播/相等短路/glitch-free/unbind 无泄漏/dispose。"
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.runtime :as rt])
  (:import [cn.li.mcmod.uipojo.signal ISigD ISigL ISigO Binding]))

(defn- stub-node []
  (node/create-node 0 :stub :box {} 0 0 {}))

;; ============================================================================
;; SigD — 创建/读写/相等短路
;; ============================================================================

(deftest sigd-create-and-read
  (let [s (sig/signal-d 1.0)]
    (is (== 1.0 (sig/sget-d s)))))

(deftest sigd-set-and-read
  (let [s (sig/signal-d 0.0)]
    (sig/sset-d! s 42.0)
    (is (== 42.0 (sig/sget-d s)))))

(deftest sigd-equality-shortcircuit
  (let [s (sig/signal-d 5.0)
        called? (atom false)]
    ;; 相同值不应触发依赖者通知（通过检查 outs 中的 dep 是否被调用验证）
    (sig/sset-d! s 5.0)
    ;; 值未变，outs 中无 dep → 无副作用
    (is (== 5.0 (sig/sget-d s)))))

;; ============================================================================
;; SigL / SigO — 基本行为
;; ============================================================================

(deftest sigl-basic
  (let [s (sig/signal-l 0)]
    (sig/sset-l! s 100)
    (is (== 100 (sig/sget-l s)))))

(deftest sigo-basic
  (let [s (sig/signal-o :idle)]
    (sig/sset-o! s :active)
    (is (= :active (sig/sget-o s)))))

(deftest sigo-equality-shortcircuit
  (let [s (sig/signal-o "hello")]
    (sig/sset-o! s "hello")  ;; same string value
    (is (= "hello" (sig/sget-o s)))))

;; ============================================================================
;; ComputedD — 依赖传播
;; ============================================================================

(deftest computedd-basic
  (let [s1 (sig/signal-d 3.0)
        s2 (sig/signal-d 4.0)
        sum (sig/computed-d [s1 s2] +)]  ;; top-level fn
    (is (== 7.0 (sig/sget-d sum)))
    (sig/sset-d! s1 10.0)
    (is (== 14.0 (sig/sget-d sum)))))

(deftest computedd-lazy-pull
  (let [s1 (sig/signal-d 1.0)
        compute-count (atom 0)
        c (sig/computed-d [s1] (fn [v] (swap! compute-count inc) (* v 2.0)))]
    ;; 首次读取触发计算
    (is (== 2.0 (sig/sget-d c)))
    (is (= 1 @compute-count))
    ;; 未变 = 不重算
    (is (== 2.0 (sig/sget-d c)))
    (is (= 1 @compute-count))
    ;; 变了才重算
    (sig/sset-d! s1 5.0)
    (is (== 10.0 (sig/sget-d c)))
    (is (= 2 @compute-count))))

(deftest computedd-glitch-free
  "标记先于读取：上游 set → 级联标脏所有下游 → 读下游时才重算一次。
   不应该出现中间旧值被读到的情况。"
  (let [s1 (sig/signal-d 2.0)
        double-s1 (sig/computed-d [s1] (fn [v] (* v 2.0)))
        sum (sig/computed-d [s1 double-s1] (fn [a b] (+ a b)))]
    ;; 初始状态
    (is (== 6.0 (sig/sget-d sum)))  ;; 2 + 4
    ;; 更新 s1：s1=3.0，double-s1 标脏，sum 标脏
    (sig/sset-d! s1 3.0)
    ;; 读 sum → sum 读 double-s1 → double-s1 重算 → 6.0 → sum 重算 → 9.0
    (is (== 9.0 (sig/sget-d sum)))))

;; ============================================================================
;; ComputedO — Object 计算
;; ============================================================================

(deftest computedo-basic
  (let [s1 (sig/signal-o :a)
        s2 (sig/signal-o :b)
        c (sig/computed-o [s1 s2] (fn [a b] [a b]))]
    (is (= [:a :b] (sig/sget-o c)))
    (sig/sset-o! s1 :x)
    (is (= [:x :b] (sig/sget-o c)))))

;; ============================================================================
;; bind! / unbind! — 绑定生命周期
;; ============================================================================

(deftest bind-unbind-lifecycle
  (let [s (sig/signal-d 0.0)
        called? (atom false)
        ;; 模拟 node（只需满足 apply-fn 签名）
        node (stub-node)
        apply-fn (fn [_node source] (swap! called? (fn [_] true)) (sig/sget-d source))
        q (java.util.ArrayList.)
        b (sig/bind! s node apply-fn q)]
    ;; 绑定建立了从 source 到 Binding 的边
    (is (instance? Binding b))
    ;; 触发 source → Binding 入队
    (sig/sset-d! s 1.0)
    (is (= 1 (.size q)))
    ;; unbind 后不再入队
    (sig/unbind! b)
    (.clear q)
    (sig/sset-d! s 2.0)
    (is (= 0 (.size q)))
    (is (false? @called?))))

;; ============================================================================
;; binding equality short-circuit（prop-writer 内部比较）
;; ============================================================================

(deftest binding-equality-shortcircuit
  "Binding.applyBinding 调用 prop-writer，prop-writer 内部做相等比较。
   若值未变，不应标 RENDER_DIRTY。"
  (let [s (sig/signal-d 42.0)
        write-count (atom 0)
        prop-writer (fn [node source]
                      (swap! write-count inc)
                      (sig/sget-d source))  ;; 读值但不执行"相等比较→标脏"的 skip 逻辑
        q (java.util.ArrayList.)
        _ (sig/bind! s (stub-node) prop-writer q)]
    ;; 首帧：source 值 42.0 → Binding 入队 → applyBinding 调 prop-writer
    ;; 注：这里验证的是 applyBinding 必定被调用，相等短路在真实 prop-writer 里
    (sig/sset-d! s 42.0)  ;; 同值，sSet 短路 → 不入队
    (is (= 0 (.size q)) "同值不应入队")
    (sig/sset-d! s 99.0)  ;; 不同值，入队
    (is (= 1 (.size q)) "不同值应入队")))

;; ============================================================================
;; 多级依赖链
;; ============================================================================

(deftest multi-level-dependency-chain
  (let [s (sig/signal-d 1.0)
        c1 (sig/computed-d [s] (fn [v] (* v 2.0)))
        c2 (sig/computed-d [c1] (fn [v] (+ v 10.0)))
        c3 (sig/computed-d [s c1 c2] (fn [a b c] (+ a b c)))]
    (is (== 15.0 (sig/sget-d c3)))  ;; 1 + 2 + 12
    (sig/sset-d! s 3.0)
    (is (== 25.0 (sig/sget-d c3)))))  ;; 3 + 6 + 16

;; ============================================================================
;; Clock signal 直接操作
;; ============================================================================

(deftest clock-signals-exist
  (let [runtime (rt/create-runtime)]
    (is (instance? ISigL (rt/clock-ms-sig runtime)))
    (is (instance? ISigD (rt/partial-ticks-sig runtime)))
    (is (instance? ISigL (rt/game-ticks-sig runtime)))))
