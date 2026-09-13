(ns cn.li.mcmod.ui.spec
  "UI spec 校验 — 仅在 build!/XML 加载期调用，渲染路径零校验。

   用 cn.li.mcmod.schema.core 构建编译期 validator。
   使用 lazy-validator（atom 缓存，非 delay/memoize——铁律9 合规）。"
  (:require [cn.li.mcmod.schema.core :as schema]))

;; ============================================================================
;; Spec schema 定义
;; ============================================================================

(def ^:private node-spec-schema
  "Node spec 的 schema 向量。编译为 validator fn 后仅校验顶层结构；
   子节点递归在 build! 时逐个检查 kind 表存在性。"
  [:map
   [:kind keyword?]
   [:id [:or nil? keyword? string?]]
   [:props [:or nil? map?]]
   [:children [:or nil? [:vector (constantly true)]]]])

(def ^:private compiled-validator
  (schema/lazy-validator node-spec-schema))

;; ============================================================================
;; 公共 API
;; ============================================================================

(defn valid?
  "校验 spec 数据合法性（加载期调用，非渲染路径）。"
  [spec]
  (schema/valid? (compiled-validator) spec))

(defn validate!
  "校验 spec 数据，不合法则抛出 ex-info 带 explain 详情。
   返回 spec（链式调用）。"
  [spec]
  (when-not (schema/valid? (compiled-validator) spec)
    (throw (ex-info (str "Invalid UI spec: " (schema/explain node-spec-schema spec))
                    {:spec spec :explain (schema/explain node-spec-schema spec)})))
  spec)
