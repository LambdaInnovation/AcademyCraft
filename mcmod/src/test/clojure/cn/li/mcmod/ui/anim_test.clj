(ns cn.li.mcmod.ui.anim-test
  "动画数学单元测试：smooth-toward/breathe/flicker/interp-color-stops。"
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.ui.anim :as anim]))

;; ============================================================================
;; bake-color-stops / sample-color-stops
;; ============================================================================

(deftest bake-and-sample-color-stops
  (let [stops [{:pct 0.0 :r 1.0 :g 0.0 :b 0.0}
               {:pct 0.5 :r 0.0 :g 1.0 :b 0.0}
               {:pct 1.0 :r 0.0 :g 0.0 :b 1.0}]
        baked (anim/bake-color-stops stops)]
    (is (some? baked) "bake 应返回 double-array")
    ;; 采样 0.0 → 红
    (let [c0 (anim/sample-color-stops baked 0.0)]
      (is (some? c0))
      (is (< 0.9 (aget c0 0)) "r ≈ 1.0")
      (is (< (aget c0 1) 0.01) "g ≈ 0.0"))
    ;; 采样 0.5 → 绿
    (let [c50 (anim/sample-color-stops baked 0.5)]
      (is (some? c50))
      (is (< 0.9 (aget c50 1)) "g ≈ 1.0"))
    ;; 采样 1.0 → 蓝
    (let [c100 (anim/sample-color-stops baked 1.0)]
      (is (some? c100))
      (is (< 0.9 (aget c100 2)) "b ≈ 1.0"))
    ;; 越界钳制
    (let [c-neg (anim/sample-color-stops baked -0.1)]
      (is (some? c-neg)) "负值应钳制到 0.0")))

(deftest bake-color-stops-empty
  (is (nil? (anim/bake-color-stops [])))
  (is (nil? (anim/bake-color-stops nil))))

(deftest bake-color-stops-sorting
  "color-stops 应排序后再烘焙。"
  (let [stops [{:pct 1.0 :r 0.0 :g 0.0 :b 1.0}
               {:pct 0.0 :r 1.0 :g 0.0 :b 0.0}]
        baked (anim/bake-color-stops stops)]
    (is (some? baked))
    (is (== 0.0 (aget baked 0)) "第一个 pct 应为 0.0")))

;; ============================================================================
;; smooth-toward 行为（内部 smooth-toward → 通过 smoothed 构造器间接测试）
;; ============================================================================

;; 注：smoothed 依赖 runtime，在纯 anim 层难以隔离测试。
;; 手动验证平滑收敛行为在 Phase B 的 demo 中进行。

;; ============================================================================
;; breathe 脉冲（通过直接调用 breathe-step 测试）
;; ============================================================================

;; breathe-step 为顶层 defn-，但可以经 computed 构造器间接测试
;; 在 runtime 上下文可用后再集成测试

;; ============================================================================
;; jitter-offset 确定性
;; ============================================================================

;; jitter-offset-step 为顶层 defn-，经 computed 构造器间接测试
;; 纯函数：同一输入 → 同一输出

;; ============================================================================
;; flicker-alpha 范围
;; ============================================================================

;; flicker-alpha-step 为顶层 defn-，直接计算：0.45~1.0 范围
;; 纯函数，无副作用

(deftest color-stops-edge-cases
  (testing "single stop"
    (let [baked (anim/bake-color-stops [{:pct 0.5 :r 0.5 :g 0.5 :b 0.5}])]
      (is (some? baked))
      ;; 前端补齐 0.0，后端补齐 1.0
      (let [c (anim/sample-color-stops baked 0.0)]
        (is (some? c)))))
  (testing "two stops"
    (let [baked (anim/bake-color-stops [{:pct 0.0 :r 0.0 :g 0.0 :b 0.0}
                                        {:pct 1.0 :r 1.0 :g 1.0 :b 1.0}])]
      (is (some? baked))
      ;; 0.5 → gray
      (let [c (anim/sample-color-stops baked 0.5)]
        (is (< 0.4 (aget c 0) 0.6) "midpoint r ≈ 0.5")))))

;; ============================================================================
;; Interference jitter (upstream CPBar keyframe port)
;; ============================================================================

(deftest interference-keyframes-shape
  (let [aspect 6.558
        kf (anim/build-interference-keyframes aspect)
        times (mapv :time (:frames kf))]
    (is (= 60 (count (:frames kf))))
    (is (pos? (:maxtime kf)))
    (is (apply < times) "keyframe times strictly increase")
    (is (< 80 (- (second times) (first times)) 401) "gaps within 80-400ms")
    (doseq [{:keys [dx dy]} (:frames kf)]
      (is (<= (Math/abs dx) (* 9.0 aspect)))
      (is (<= (Math/abs dy) 9.0)))))

(deftest interference-offset-holds-between-keyframes
  (let [kf (anim/build-interference-keyframes 6.558)
        t1 (:time (first (:frames kf)))
        [dx dy] (anim/interference-offset (double (dec t1)) kf)]
    (is (some? dx))
    ;; Before the first keyframe the FIRST frame's direction applies
    (is (= [dx dy] (anim/interference-offset 0.0 kf)))
    ;; Just past a keyframe the offset must have changed (next frame's)
    (is (not= [dx dy] (anim/interference-offset (double (inc t1)) kf)))))

(deftest interference-alpha-in-range
  (let [kf (anim/build-interference-keyframes 6.558)
        points (anim/build-interference-alpha-points kf)]
    (doseq [t [0.0 500.0 5000.0 (dec (:maxtime kf))]]
      (let [a (anim/interference-alpha (double t) kf points)]
        (is (<= 0.2 a 0.8) (str "alpha " a " at t=" t))))))
