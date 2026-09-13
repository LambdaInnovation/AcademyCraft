(ns cn.li.mcmod.ui.layout-test
  "布局数学单元测试：对齐/pivot/scale parity（与旧 traversal.clj 对照）、tape 扁平化、hit-test。"
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.ui.layout :as layout]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.dsl :as dsl]
            [cn.li.mcmod.ui.node :as node])
  (:import [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.runtime UiRt]))

;; ============================================================================
;; Layout parity — 与旧 traversal.clj 的 LambdaLib2 对齐语义对照
;; ============================================================================

;; 旧 traversal.clj 公式：
;;   own-scale = @(:scale widget)
;;   cum-scale = parent-scale * own-scale
;;   sw = w * own-scale  (scaled width)
;;   sh = h * own-scale  (scaled height)
;;   align-offset-x = / (- pw sw) 2.0  (center) / (- pw sw) (right) / 0 (left)
;;   align-offset-y = / (- ph sh) 2.0  (middle) / (- ph sh) (bottom) / 0 (top)
;;   pivot-shift-x = pivot-x * w
;;   pivot-shift-y = pivot-y * h
;;   child-x = align-offset-x + wx - pivot-shift-x
;;   child-y = align-offset-y + wy - pivot-shift-y
;;   abs-x = px + child-x * parent-scale
;;   abs-y = py + child-y * parent-scale
;;
;; 本系统（layout/compute-abs-pos!）同样实现。

(deftest layout-left-top-alignment
  "左对齐+顶对齐：align-offset=0，等同于 direct offset。"
  (let [n (node/create-node 0 nil :box
                             {:x 10.0 :y 20.0 :w 100.0 :h 50.0 :scale 1.0
                              :align-w :left :align-h :top}
                             0 0 {})]
    (layout/compute-abs-pos! n 0.0 0.0 1.0 200.0 100.0)
    ;; child-x = 0 + 10 - (0*100) = 10.0
    ;; child-y = 0 + 20 - (0*50) = 20.0
    ;; abs-x = 0 + 10 * 1 = 10.0
    ;; abs-y = 0 + 20 * 1 = 20.0
    (is (== 10.0 (.getAbsX n)))
    (is (== 20.0 (.getAbsY n)))
    (is (== 1.0 (.getCumScale n)))
    (is (not (.hasFlag n node/FLAG-LAYOUT-DIRTY)))))

(deftest layout-center-middle-alignment
  "居中：scaled size 在父尺寸内居中。"
  (let [n (node/create-node 0 nil :box
                             {:x 0.0 :y 0.0 :w 100.0 :h 50.0 :scale 1.0
                              :align-w :center :align-h :middle}
                             0 0 {})]
    (layout/compute-abs-pos! n 0.0 0.0 1.0 200.0 100.0)
    ;; sw=100*1.0=100, sh=50*1.0=50
    ;; align-offset-x = (200-100)/2 = 50.0
    ;; align-offset-y = (100-50)/2 = 25.0
    ;; child-x = 50 + 0 - 0 = 50.0
    ;; child-y = 25 + 0 - 0 = 25.0
    ;; abs-x = 0 + 50*1 = 50.0
    ;; abs-y = 0 + 25*1 = 25.0
    (is (== 50.0 (.getAbsX n)))
    (is (== 25.0 (.getAbsY n)))))

(deftest layout-right-bottom-alignment
  "右对齐+底对齐。"
  (let [n (node/create-node 0 nil :box
                             {:x 0.0 :y 0.0 :w 100.0 :h 50.0 :scale 1.0
                              :align-w :right :align-h :bottom}
                             0 0 {})]
    (layout/compute-abs-pos! n 0.0 0.0 1.0 200.0 100.0)
    ;; align-offset-x = 200-100 = 100.0
    ;; align-offset-y = 100-50 = 50.0
    ;; child-x = 100 + 0 - 0 = 100.0
    ;; child-y = 50 + 0 - 0 = 50.0
    (is (== 100.0 (.getAbsX n)))
    (is (== 50.0 (.getAbsY n)))))

(deftest layout-with-scale-and-pivot
  "带缩放和 pivot 的布局。"
  (let [n (node/create-node 0 nil :box
                             {:x 10.0 :y 10.0 :w 80.0 :h 40.0 :scale 2.0
                              :pivot-x 0.5 :pivot-y 0.5
                              :align-w :left :align-h :top}
                             0 0 {})]
    (layout/compute-abs-pos! n 100.0 50.0 3.0 400.0 300.0)
    ;; own-scale=2.0, cum-scale=3.0*2.0=6.0
    ;; sw=80*2=160.0, sh=40*2=80.0
    ;; align-offset-x=0 (left), align-offset-y=0 (top)
    ;; pivot-shift-x=0.5*80=40.0, pivot-shift-y=0.5*40=20.0
    ;; child-x=0+10-40=-30.0, child-y=0+10-20=-10.0
    ;; abs-x=100+(-30)*3=10.0
    ;; abs-y=50+(-10)*3=20.0
    (is (== 10.0 (.getAbsX n)))
    (is (== 20.0 (.getAbsY n)))
    (is (== 6.0 (.getCumScale n)))))

(deftest layout-nested-children
  "子布局递归通过 ensure-children-layout! 验证。"
  (let [parent (node/create-node 0 :parent :group
                                  {:x 10.0 :y 10.0 :w 200.0 :h 100.0 :scale 1.0
                                   :align-w :left :align-h :top}
                                  0 0 {})
        child (node/create-node 1 :child :box
                                 {:x 5.0 :y 5.0 :w 50.0 :h 20.0 :scale 1.0
                                  :align-w :left :align-h :top}
                                 0 0 {})]
    (node/add-child! parent child)
    ;; 先计算父
    (layout/compute-abs-pos! parent 0.0 0.0 1.0 300.0 200.0)
    (is (== 10.0 (.getAbsX ^INode parent)))
    ;; 递归子节点（末参 ancestor-recomputed? — 父已重算 → 级联子树）
    (let [^INode p parent]
      (#'cn.li.mcmod.ui.layout/ensure-children-layout!
       p (.getAbsX p) (.getAbsY p) (.getCumScale p) (.getW p) (.getH p) true))
    ;; child abs-x = parent-abs-x + (child-x)*parent-cum-scale
    ;; = 10 + (0+5-0)*1 = 15.0
    (is (== 15.0 (.getAbsX ^INode child)))
    (is (== 15.0 (.getAbsY ^INode child)))))

;; ============================================================================
;; ensure-layout! — 端到端（通过 runtime）
;; ============================================================================

(deftest ensure-layout-end-to-end
  (let [r (rt/create-runtime)]
    ;; 设置屏幕尺寸
    (rt/resize! r 400.0 300.0)
    ;; 构建树
    (rt/build! r (dsl/group {:id :root :w 400 :h 300}
                   (dsl/box {:id :panel :x 50 :y 50 :w 300 :h 200})))
    ;; 布局
    (layout/ensure-layout! r)
    (let [^INode root (rt/node-by-id r :root)
          ^INode panel (rt/node-by-id r :panel)]
      (is (== 0.0 (.getAbsX root)) "root 应从左上角开始")
      (is (== 0.0 (.getAbsY root)))
      ;; panel: parent=root(root.abs=0,0 cumScale=1.0), local=50,50
      ;; abs = 0 + (0+50-0)*1 = 50
      (is (== 50.0 (.getAbsX panel)))
      (is (== 50.0 (.getAbsY panel))))))

;; ============================================================================
;; tape 扁平化
;; ============================================================================

(deftest tape-flatten-basic
  (let [r (rt/create-runtime)]
    (rt/resize! r 400.0 300.0)
    (rt/build! r (dsl/group {:id :root :w 400 :h 300}
                   (dsl/box {:id :box1 :z 1})
                   (dsl/text {:id :txt1 :z 0})))
    (layout/ensure-layout! r)
    (let [^objects tape (layout/ensure-tape! r)]
      (is (some? tape))
      (is (pos? (alength tape)) "非空 tape")
      (is (instance? INode (aget tape 0)) "首个元素应为 root Node")
      (is (not (rt/tree-dirty? r)) "tape 构建后 tree-dirty 应清除"))))

(deftest tape-z-order-stable-sort
  "兄弟按 z 排序，等 z 保持原始顺序。"
  (let [r (rt/create-runtime)]
    (rt/resize! r 400.0 300.0)
    (rt/build! r (dsl/group {:id :root :w 400 :h 300}
                   (dsl/box {:id :back :z -1})
                   (dsl/box {:id :front :z 10})
                   (dsl/text {:id :mid :z 5})))
    (layout/ensure-layout! r)
    (let [^objects tape (layout/ensure-tape! r)]
      ;; tape 中节点顺序应为 root → back → mid → front（按 z 升序 + 稳定）
      (is (some? tape))
      ;; 遍历找到三个子节点的顺序
      (let [ids (loop [i 0 acc []]
                  (if (< i (alength tape))
                    (let [entry (aget tape i)]
                      (if (instance? INode entry)
                        (recur (unchecked-inc-int i) (conj acc (.getId ^INode entry)))
                        (recur (unchecked-inc-int i) acc)))
                    acc))]
        ;; back 应在 mid 前，mid 应在 front 前
        (let [back-pos (.indexOf ids :back)
              mid-pos  (.indexOf ids :mid)
              front-pos (.indexOf ids :front)]
          (is (< back-pos mid-pos) "back 应在 mid 前")
          (is (< mid-pos front-pos) "mid 应在 front 前"))))))

(deftest tape-invisible-subtree-skipped
  "不可见整棵子树应被 tape 剔除。"
  (let [r (rt/create-runtime)]
    (rt/resize! r 400.0 300.0)
    (rt/build! r (dsl/group {:id :root :w 400 :h 300}
                   (dsl/box {:id :visible-box})
                   (dsl/box {:id :invisible-box :visible? false}
                     (dsl/text {:id :hidden-child}))))
    (layout/ensure-layout! r)
    (let [^objects tape (layout/ensure-tape! r)]
      (let [ids (loop [i 0 acc []]
                  (if (< i (alength tape))
                    (let [entry (aget tape i)]
                      (if (instance? INode entry)
                        (recur (unchecked-inc-int i) (conj acc (.getId ^INode entry)))
                        (recur (unchecked-inc-int i) acc)))
                    acc))]
        (is (some #{:visible-box} ids) "可见节点应在 tape 中")
        (is (not-any? #{:invisible-box} ids) "不可见节点不应在 tape 中")
        (is (not-any? #{:hidden-child} ids) "不可见子树的子节点也不应在 tape 中")))))

;; ============================================================================
;; hit-test
;; ============================================================================

(deftest hit-test-basic
  (let [r (rt/create-runtime)]
    (rt/resize! r 400.0 300.0)
    (rt/build! r (dsl/group {:id :root :w 400 :h 300}
                   (dsl/box {:id :panel :x 50 :y 50 :w 100 :h 80})))
    (layout/ensure-layout! r)
    (layout/ensure-tape! r)
    (let [hit (layout/hit-test r 100.0 90.0)]
      (is (some? hit))
      (is (= :panel (.getId ^INode hit))))
    (let [hit-outside (layout/hit-test r 10.0 10.0)]
      ;; 当前 hit-test 是纯边界包含判定（无 kind 级 hit? 过滤，见 layout.clj
      ;; 文档字符串），root group 覆盖全屏，故落在 panel 外但 root 内的点命中 root。
      (is (= :root (.getId ^INode hit-outside)) "落在 root 内、panel 外的点命中 root"))
    (let [hit-edge (layout/hit-test r 149.0 129.0)]  ;; 150 是边界外
      (is (= :panel (.getId ^INode hit-edge)) "右上角边界内"))
    (let [hit-outside2 (layout/hit-test r 150.0 130.0)]
      (is (= :root (.getId ^INode hit-outside2)) "超出 panel 边界但仍在 root 内，命中 root"))))

(deftest hit-test-respects-clip-viewport
  "Scrolled content moved outside its clip=true viewport must NOT answer
   hit-tests — otherwise rolled-up text swallows clicks on UI declared
   earlier in the tape (the about app's tab buttons sit above its scroll
   area; after scrolling, clicks on them landed on the rolled-out text)."
  (let [r (rt/create-runtime)]
    (rt/resize! r 400.0 300.0)
    (rt/build! r (dsl/group {:id :root :w 400 :h 300}
                   (dsl/box {:id :tab :x 10 :y 10 :w 100 :h 30})
                   (dsl/group {:id :viewport :x 10 :y 50 :w 200 :h 100 :clip true}
                     (dsl/text {:id :row :x 0 :y 0 :w 200 :h 20 :text "row"}))))
    (layout/ensure-layout! r)
    (layout/ensure-tape! r)
    ;; row sits inside the viewport — click at its position hits the row.
    (let [hit (layout/hit-test r 20.0 60.0)]
      (is (= :row (.getId ^INode hit))))
    ;; Scroll the row UP out of the viewport (like about's content after
    ;; set-scroll!): the row's abs position now overlaps the tab area.
    (let [^INode row (rt/node-by-id r :row)]
      (.setY row -40.0)
      (.setFlag row node/FLAG-LAYOUT-DIRTY))
    (layout/ensure-layout! r)
    ;; Without clip awareness, the rolled-out row would win the hit at the
    ;; tab position (tape order: tab ... viewport -> row). It must not.
    (let [hit (layout/hit-test r 20.0 30.0)]
      (is (some? hit))
      (is (= :tab (.getId ^INode hit))
          "rolled-out row is clipped away; the tab below receives the click"))))

(deftest hit-test-deepest-overlap
  "重叠节点取最深层（painter 序最后）。"
  (let [r (rt/create-runtime)]
    (rt/resize! r 400.0 300.0)
    (rt/build! r (dsl/group {:id :root :w 400 :h 300}
                   (dsl/box {:id :back :x 0 :y 0 :w 200 :h 200 :z 0})
                   (dsl/box {:id :front :x 50 :y 50 :w 100 :h 100 :z 10})))
    (layout/ensure-layout! r)
    (layout/ensure-tape! r)
    ;; 点击重叠区域 → 应命中 front（z 更高，painter 序在后）
    (let [hit (layout/hit-test r 100.0 100.0)]
      (is (some? hit))
      (is (= :front (.getId ^INode hit))))
    ;; 点击仅 back 覆盖区域
    (let [hit-back (layout/hit-test r 20.0 20.0)]
      (is (some? hit-back))
      (is (= :back (.getId ^INode hit-back))))))

;; ============================================================================
;; tape clip/transform 哨兵
;; ============================================================================

(deftest tape-clip-sentinels
  "带 :clip? 的 group 应包裹 push-clip/pop-clip 哨兵。"
  (let [r (rt/create-runtime)]
    (rt/resize! r 400.0 300.0)
    (rt/build! r (dsl/group {:id :root :w 400 :h 300}
                   (dsl/group {:id :clip-group :clip? true :w 200 :h 200}
                     (dsl/text {:id :inner}))))
    (layout/ensure-layout! r)
    (let [^objects tape (layout/ensure-tape! r)]
      ;; 扫描找到 push-clip 和 pop-clip
      (let [tags (loop [i 0 acc []]
                   (if (< i (alength tape))
                     (let [entry (aget tape i)]
                       (recur (unchecked-inc-int i) (conj acc
                         (cond (instance? INode entry) (.getId ^INode entry)
                               (= layout/push-clip-sentinel entry) :push-clip
                               (= layout/pop-clip-sentinel entry) :pop-clip
                               (= layout/push-transform-sentinel entry) :push-xf
                               (= layout/pop-transform-sentinel entry) :pop-xf
                               :else :unknown))))
                     acc))]
        (is (some #{:push-clip} tags) "应有 push-clip 哨兵")
        (is (some #{:pop-clip} tags) "应有 pop-clip 哨兵")
        (is (some #{:inner} tags) "子节点也应在 tape 中")))))
