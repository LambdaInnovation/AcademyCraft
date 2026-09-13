# 无线 GUI 消息 DSL 使用指南

> 状态标签：**现行**（规范文档）

本文档描述无线 GUI 消息的统一管理机制，说明**如何新增消息、如何修改消息、排查问题时的快速定位路径**。

---

## 一、架构概览

```
messages_dsl.clj          ← 纯工具层：message-id 生成、校验、查询 API
    │
    ├── node_messages.clj     ← node 域所有消息的单点声明
    │
    ├── matrix_messages.clj   ← matrix 域所有消息的单点声明
    │
    └── wireless_messages.clj ← 聚合 catalog，加载时跨域校验
             │
             ├── node_gui_xml.clj          (客户端发送端)
             ├── node_network_handler.clj  (服务端注册端)
             ├── matrix_gui_xml.clj        (客户端发送端)
             └── matrix_network_handler.clj (服务端注册端)
```

**消息 ID 字符串只在声明文件（`node_messages` / `matrix_messages`）里生成一次**；
客户端发送端和服务端注册端都通过 `(node-msgs/msg :action-key)` 或
`(matrix-msgs/msg :action-key)` 取值，永远不自行拼接字符串。

---

## 二、消息 ID 命名规则

消息 ID 由 DSL 按固定规则生成：

```
wireless_<domain>_<action-token>
```

- `<domain>` — 域名（`:node → node`，`:matrix → matrix`）
- `<action-token>` — action keyword 的 `name`，连字符替换为下划线

示例：

| domain  | action            | 生成结果                          |
|---------|-------------------|-----------------------------------|
| `:node` | `:get-status`     | `wireless_node_get_status`        |
| `:node` | `:list-networks`  | `wireless_node_list_networks`     |
| `:matrix` | `:gather-info`  | `wireless_matrix_gather_info`     |
| `:matrix` | `:change-ssid`  | `wireless_matrix_change_ssid`     |

> **不要手写消息字符串**，让 DSL 生成。

---

## 三、当前消息清单

### node 域（`node_messages.clj`）

| action key         | 生成消息 ID                        | 用途         |
|--------------------|------------------------------------|--------------|
| `:get-status`      | `wireless_node_get_status`         | 查询节点连接状态 |
| `:change-name`     | `wireless_node_change_name`        | 修改节点名称    |
| `:change-password` | `wireless_node_change_password`    | 修改节点密码    |
| `:list-networks`   | `wireless_node_list_networks`      | 查询周围无线网络 |
| `:connect`         | `wireless_node_connect`            | 连接到无线网络  |
| `:disconnect`      | `wireless_node_disconnect`         | 断开无线网络    |

### matrix 域（`matrix_messages.clj`）

| action key         | 生成消息 ID                          | 用途           |
|--------------------|--------------------------------------|----------------|
| `:gather-info`     | `wireless_matrix_gather_info`        | 查询网络信息    |
| `:init`            | `wireless_matrix_init`               | 初始化无线网络  |
| `:change-ssid`     | `wireless_matrix_change_ssid`        | 修改 SSID       |
| `:change-password` | `wireless_matrix_change_password`    | 修改网络密码    |

---

## 四、新增消息 Checklist

以下步骤以"为 node 新增一条 `:ping` 消息"为例，逐步演示。

### Step 1 — 在声明文件添加 action key

```clojure
;; node_messages.clj
(def node-actions
  [:get-status
   :change-name
   :change-password
   :list-networks
   :connect
   :disconnect
   :ping])          ;; ← 新增
```

保存后 `wireless_messages/catalog` 在下次加载时自动包含该条目，
并完成唯一性和命名规范校验。若有冲突则加载即报错，无法启动。

### Step 2 — 编写服务端 handler

```clojure
;; node_network_handler.clj
(defn handle-ping
  [payload player]
  {:pong true})
```

### Step 3 — 注册 handler

```clojure
;; node_network_handler.clj — register-handlers! 内
(net-server/register-handler (node-msgs/msg :ping) handle-ping)
```

### Step 4 — 客户端发送

```clojure
;; node_gui_xml.clj 或其他客户端代码
(net-client/send-to-server
  (node-msgs/msg :ping)
  payload
  callback)
```

### Step 5 — 更新测试

```clojure
;; messages_dsl_test.clj — expected-node-actions
(def expected-node-actions
  #{:get-status :change-name :change-password
    :list-networks :connect :disconnect
    :ping})     ;; ← 新增，让 test-node-messages-complete 通过
```

### Step 6 — 运行测试验证

在 REPL 中执行：
```clojure
(require 'cn.li.wireless.gui.messages-dsl-test :reload)
(cn.li.wireless.gui.messages-dsl-test/run-all-tests)
```

---

## 五、修改消息 Checklist

> **警告：无线消息 ID 是客户端-服务端协议的一部分，修改后需要同步部署客户端和服务端。**

1. 在声明文件修改 action key（如从 `:get-status` 改为 `:query-status`）。
2. 全局搜索既有 key 字面量 `(node-msgs/msg :get-status)` 并替换为新 key。
3. 更新测试里的 `expected-node-actions` / `expected-matrix-actions`。
4. 运行测试验证映射正确。

消息 ID 是联机协议的一部分；变更时要同步更新客户端与服务端，并在同一次变更中删除不再使用的 ID。

---

## 六、添加新的消息域

如未来有第三种无线设备（如 `:repeater`），需要：

### Step 1 — 新增声明文件

新建 `repeater_messages.clj`（参照 `node_messages.clj`）：

```clojure
(ns cn.li.wireless.gui.repeater-messages
  (:require [cn.li.wireless.gui.messages-dsl :as msg-dsl]))

(def repeater-actions [:get-status :set-mode])

(def repeater-domain-spec
  (msg-dsl/build-domain-spec :repeater repeater-actions))

(defn msg [action]
  (or (get-in repeater-domain-spec [:messages action])
      (throw (ex-info "Unknown repeater message action" {:action action}))))
```

### Step 2 — 纳入聚合 catalog

```clojure
;; wireless_messages.clj
(:require ...
          [cn.li.wireless.gui.repeater-messages :as repeater-msgs])

(def catalog
  (msg-dsl/build-catalog
    [node-msgs/node-domain-spec
     matrix-msgs/matrix-domain-spec
     repeater-msgs/repeater-domain-spec]))   ;; ← 新增
```

---

## 七、关键文件索引

| 文件 | 职责 |
|------|------|
| `wireless/gui/messages_dsl.clj` | DSL 工具函数（生成、校验、查询） |
| `wireless/gui/node_messages.clj` | node 消息单点声明 |
| `wireless/gui/matrix_messages.clj` | matrix 消息单点声明 |
| `wireless/gui/wireless_messages.clj` | 聚合 catalog，加载即校验 |
| `wireless/gui/node_gui_xml.clj` | node 客户端消息使用端 |
| `wireless/gui/node_network_handler.clj` | node 服务端消息注册端 |
| `wireless/gui/matrix_gui_xml.clj` | matrix 客户端消息使用端 |
| `wireless/gui/matrix_network_handler.clj` | matrix 服务端消息注册端 |
| `test/.../messages_dsl_test.clj` | DSL 契约测试（唯一性、命名、映射） |
