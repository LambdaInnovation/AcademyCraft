(ns cn.li.mcmod.ui.dsl
  "声明式 DSL — 代码构建 UI spec（P.I.C.A.S.O.-O：通用 Map/Vector 结构）。

   每个 kind 对应一个同名函数：接收 props map 和可选的 children。
   spec 数据与 XML loader 产物同构 → 统一走 ui.runtime/build!。

   用法示例：
   (group {:id :root :w 200 :h 100}
     (box {:id :panel :fill 0xFF353535 :x 10 :y 10 :w 180 :h 80}
       (text {:id :label :text \"Hello\" :x 5 :y 5})
       (image {:id :icon :src \"academy:textures/gui/icon.png\" :x 0 :y 0 :w 16 :h 16})))")

(defn- spec-map
  "构建 spec map 的内部辅助。"
  [kind props children]
  (cond-> {:kind kind :props props}
    (seq children) (assoc :children (vec children))))

;; ============================================================================
;; 容器
;; ============================================================================

(defn group
  "容器节点。:clip? true → 裁剪子内容；:transform? true → PoseStack push/pop。"
  [props & children]
  (spec-map :group props children))

;; ============================================================================
;; 视觉节点
;; ============================================================================

(defn box
  "填充/描边/着色矩形。props: :fill :outline :tint :hover-tint 等。"
  [props & children]
  (spec-map :box props children))

(defn image
  "贴图节点。props: :src :alpha :u :v :tex-w :tex-h。"
  [props]
  (spec-map :image props nil))

(defn text
  "文本节点。props: :text :font :font-size :color :x-offset :y-offset :align :editable?。"
  [props]
  (spec-map :text props nil))

(defn progress
  "进度条节点。props: :progress :color-stops :bg-src :fg-src :icon-src :hint-percent :corner :scroll-offset。"
  [props]
  (spec-map :progress props nil))

(defn shader-quad
  "Shader 四边形。props: :shader-props。"
  [props]
  (spec-map :shader-quad props nil))

(defn shader-ring
  "Shader 环形进度。props: :progress :inner :outer :shader-props。"
  [props]
  (spec-map :shader-ring props nil))

(defn shader-progress
  "Shader 进度（水平填充）。props: :progress :shader-props。"
  [props]
  (spec-map :shader-progress props nil))

(defn gradient
  "渐变色块。props: :stops :alpha :angle。"
  [props]
  (spec-map :gradient props nil))

(defn line
  "旋转线段。props: :x1 :y1 :x2 :y2 :thickness :color :alpha。"
  [props]
  (spec-map :line props nil))

(defn crosshair
  "反射准星。props: :phase :intensity。"
  [props]
  (spec-map :crosshair props nil))

(defn glow-line
  "8 段模糊光晕（ACRenderingHelper.drawGlow/lineSegmentGlow 移植）。
   dslots 由调用方每帧直接 .setDSlot：:x0 :x1 :y :line-w :glow-sz :tint :no-center。"
  [props]
  (spec-map :glow-line props nil))

;; ============================================================================
;; 动态列表
;; ============================================================================

(defn list-node
  "模板实例化列表。props: :spacing :template（node-spec map）。
   子内容由 list-set! 运行时按数据 key 生成。"
  [props]
  (spec-map :list props nil))
