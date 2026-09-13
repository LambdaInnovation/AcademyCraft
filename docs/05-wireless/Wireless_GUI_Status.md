# Wireless GUI 状态与缺陷报告

> 状态标签：**现行**（运维排障参考）

本文档合并自：Matrix & Wireless 完整性检查报告（V1/V2）、Wireless Matrix GUI 完整性分析、Wireless Node GUI 修复报告。路径与命名空间以当前项目为准（`academy`、`assets/academy/guis/`、`academy:textures/guis/` 等）。

---

## 一、资源与路径现状

### 1.1 路径约定

- **布局/XML**：项目中使用 `assets/academy/guis/`、`academy:guis/rework/`（如 `page_wireless.xml`）；不再使用 `gui/layouts/` 等旧路径。
- **纹理**：代码中可能使用 `academy:textures/gui/` 或 `academy:textures/guis/`；实际资源在 `assets/academy/textures/guis/` 等，需与代码引用一致。

### 1.2 已知资源缺口（待补齐）

- **Node GUI**：若仍引用以下路径，需确保文件存在或改为占位/默认纹理：  
  `node_background.png`、`ui_inventory.png`、`ui_wireless_node.png`、`effect_node.png`、`wireless_node.png` 等。
- **Matrix GUI**：`wireless_matrix.png`、Matrix 用 XML 布局（若使用 `page_wireless_matrix`）需放在 `academy` 资源下。
- **XML 布局**：`page_wireless_matrix.xml` 等应在 **`ac/src/main/resources/assets/academy/guis/`**（或项目约定的 guis 子路径）下，与对应 `gui.clj` / XML 加载路径一致。

---

## 二、Node GUI 修复摘要

- **容量同步**：NodeContainer 已支持 `capacity`、`max-capacity`，直方图等组件可正确显示。
- **快速移动**：通过元数据/槽位配置修正 shift-click 行为。
- **性能**：网络轮询与充电相关逻辑已降频（如轮询 0.2 TPS、充电 2 TPS），避免每 tick 执行。
- **容器生命周期**：容器关闭时清理，避免泄漏。
- **架构**：协议与分发在 `mcmod`/`ac` 中初始化，Forge 层做 API 适配；能量相关使用 **`cn.li.ac.energy.operations`**。

---

## 三、Matrix GUI 缺陷与待办

- **同步与 Container**：MatrixContainer 需完整提供直方图所需字段（如 `max-capacity`），make-sync-packet / MatrixStatePacket 需包含这些字段，避免直方图崩溃。
- **生命周期与性能**：建议与 Node 一致：容器 on-close 清理、ticker 节流，避免内存泄漏与过高 TPS。
- **GUI 元数据**：槽位与 GUI 元数据需支持 `max-capacity` 等字段，与 Node 对齐。
- **资源**：Matrix 所需 XML 与纹理统一放在 `academy` 资源下，路径与代码引用一致。

---

## 四、检查清单（维护用）

- [ ] 所有 Wireless GUI 相关 XML 位于 `assets/academy/guis/`（或项目约定路径）。
- [ ] 纹理路径（`academy:textures/gui/` 或 `guis/`）与实际文件一致；缺失项已补齐或降级处理。
- [ ] Node/Matrix Container 与 sync packet 包含直方图与 UI 所需全部字段。
- [ ] 容器 on-close 与 ticker 节流已实现，无泄漏与性能问题。
- [ ] 文档内交叉引用已更新为 `05-wireless/Node_GUI.md`、`05-wireless/Matrix_GUI.md`、`05-wireless/Wireless_GUI_Status.md`。
