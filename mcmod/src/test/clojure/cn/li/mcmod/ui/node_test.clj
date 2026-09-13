(ns cn.li.mcmod.ui.node-test
  "Node 模型与 kind 表单元测试。"
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.slot-write :as slot-write]
            [cn.li.mcmod.ui.signal :as sig])
  (:import [cn.li.mcmod.ui.node INode]
           [java.util ArrayList]))

;; ============================================================================
;; create-node 基本字段
;; ============================================================================

(deftest create-node-basic
  (let [n (node/create-node 0 :my-id :box
                             {:x 10.0 :y 20.0 :w 100.0 :h 50.0 :scale 1.5 :z 3.0
                              :pivot-x 0.5 :pivot-y 0.5
                              :align-w :center :align-h :middle
                              :visible? true}
                             1 1 {})]
    (is (instance? INode n))
    (is (= 0 (.getIdx n)))
    (is (= :my-id (.getId n)))
    (is (= :box (.getKind n)))
    (is (== 10.0 (.getX n)))
    (is (== 20.0 (.getY n)))
    (is (== 100.0 (.getW n)))
    (is (== 50.0 (.getH n)))
    (is (== 1.5 (.getScale n)))
    (is (== 3.0 (.getZ n)))
    (is (== 0.5 (.getPivotX n)))
    (is (== 0.5 (.getPivotY n)))
    (is (= 1 (.getAlignW n)))   ;; center = 1
    (is (= 1 (.getAlignH n)))   ;; middle = 1
    (is (.isVisible n))
    ;; 新建节点 layout-dirty
    (is (.hasFlag n node/FLAG-LAYOUT-DIRTY))
    (is (.hasFlag n node/FLAG-RENDER-DIRTY))))

(deftest create-node-defaults
  (let [n (node/create-node 0 nil :group {} 0 0 {})]
    (is (== 0.0 (.getX n)))
    (is (== 0.0 (.getY n)))
    (is (== 0.0 (.getW n)))
    (is (== 0.0 (.getH n)))
    (is (== 1.0 (.getScale n)))
    (is (== 0.0 (.getZ n)))
    (is (== 0 (.getAlignW n)))   ;; left = 0
    (is (== 0 (.getAlignH n)))))  ;; top = 0

;; ============================================================================
;; Flags
;; ============================================================================

(deftest flag-operations
  (let [n (node/create-node 0 nil :box {} 0 0 {})]
    ;; 创建时自带 LAYOUT_DIRTY | RENDER_DIRTY
    (is (.hasFlag n node/FLAG-LAYOUT-DIRTY))
    (is (.hasFlag n node/FLAG-RENDER-DIRTY))
    ;; 清除 LAYOUT_DIRTY
    (.clearFlag n node/FLAG-LAYOUT-DIRTY)
    (is (not (.hasFlag n node/FLAG-LAYOUT-DIRTY)))
    (is (.hasFlag n node/FLAG-RENDER-DIRTY))
    ;; 设置 CLIP
    (.setFlag n node/FLAG-CLIP)
    (is (.hasFlag n node/FLAG-CLIP))
    ;; 不互相影响
    (is (not (.hasFlag n node/FLAG-LAYOUT-DIRTY)))))

;; ============================================================================
;; Kind 表存在性
;; ============================================================================

(deftest all-kinds-defined
  (doseq [k [:group :box :image :text :progress :crosshair
             :shader-quad :shader-ring :shader-progress
             :gradient :line :list]]
    (testing (str "kind " k " exists")
      (is (contains? node/kinds k) (str "Missing kind: " k))
      (let [kdef (get node/kinds k)]
        (is (map? kdef))
        (is (map? (:dslots kdef)))
        (is (map? (:oslots kdef)))
        (is (int? (:oslots-backend-base kdef)))
        (is (map? (:prop-writers kdef)))))))

;; ============================================================================
;; Kind prop-writer 存在性验证
;; ============================================================================

(deftest prop-writers-for-key-kinds
  (let [progress-def (get node/kinds :progress)]
    (is (contains? (:prop-writers progress-def) :progress))
    (is (contains? (:prop-writers progress-def) :hint))
    (is (map? (:progress (:prop-writers progress-def))))
    (is (fn? (slot-write/resolve-sig-writer progress-def :progress)))))

;; ============================================================================
;; add-child! / remove-child! / child-count
;; ============================================================================

(deftest child-operations
  (let [parent (node/create-node 0 :parent :group {} 0 0 {})
        child1 (node/create-node 1 :c1 :box {} 0 0 {})
        child2 (node/create-node 2 :c2 :text {} 0 0 {})]
    (is (zero? (node/child-count parent)))
    (node/add-child! parent child1)
    (is (= 1 (node/child-count parent)))
    (is (identical? parent (.getParentNode ^INode child1)))
    (node/add-child! parent child2)
    (is (= 2 (node/child-count parent)))
    (node/remove-child! parent child1)
    (is (= 1 (node/child-count parent)))
    ;; child2 还在
    (let [^objects cs (.getChildrenArr ^INode parent)]
      (is (identical? child2 (aget cs 0)))
      (is (nil? (aget cs 1))))))

;; ============================================================================
;; Dynamic slots
;; ============================================================================

(deftest dynamic-slots-read-write
  (let [n (node/create-node 0 :test :box {} 4 4 {})]
    (.setDSlot n 0 3.14)
    (.setDSlot n 1 2.71)
    (.setOSlot n 0 "hello")
    (.setOSlot n 1 [1 2 3])
    (is (== 3.14 (.getDSlot n 0)))
    (is (== 2.71 (.getDSlot n 1)))
    (is (= "hello" (.getOSlot n 0)))
    (is (= [1 2 3] (.getOSlot n 1)))))

;; ============================================================================
;; prop-writer 相等短路（关键行为）
;; ============================================================================

(deftest prop-writer-equality-shortcircuit
  (let [n (node/create-node 0 nil :box {:fill 0xFF000000} 4 4 {})
        s (sig/signal-d 0xFF000000)
        writer (slot-write/resolve-sig-writer (get node/kinds :box) :fill)]
    (is (fn? writer))
    ;; 首写 — 置 RENDER_DIRTY
    (writer n s)
    (is (.hasFlag n node/FLAG-RENDER-DIRTY) "首次写入应标脏")
    ;; 清除
    (.clearFlag n node/FLAG-RENDER-DIRTY)
    ;; 同值再写 — writer 内部相等比较，不标脏
    (writer n s)
    (is (not (.hasFlag n node/FLAG-RENDER-DIRTY)) "同值不应标脏")
    ;; 不同值
    (sig/sset-d! s 0xFFFFFFFF)
    (.clearFlag n node/FLAG-RENDER-DIRTY)
    (writer n s)
    (is (.hasFlag n node/FLAG-RENDER-DIRTY) "不同值应标脏")))

;; ============================================================================
;; Visible 切换
;; ============================================================================

(deftest visibility-toggle
  (let [n (node/create-node 0 nil :box {:visible? true} 0 0 {})]
    (is (.isVisible n))
    (.setVisible n false)
    (is (not (.isVisible n)))))
