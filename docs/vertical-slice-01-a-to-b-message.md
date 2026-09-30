# Vertical Slice 01：A → B 发一条消息 → B 收到

> 这是 TeamFlow 第一个真正的聊天业务练习。
>
> **目标不是做出完整聊天系统，而是彻底搞懂 WebSocket 的双向实时通信。**

---

## 1. 目标

实现下面这条最小链路：

```text
Android A
   │
   │ WebSocket
   ▼
Spring Boot
   │
   │ 转发
   ▼
WebSocket
   │
   ▼
Android B
```

最终效果：

1. A 登录成功。
2. B 登录成功。
3. A、B 都建立 WebSocket。
4. A 输入 `Hello B`。
5. A 发送一帧 `SEND_MESSAGE`。
6. Spring Boot 收到消息。
7. Spring Boot 找到 B 的 WebSocket Session。
8. Spring Boot 把消息转发给 B。
9. B 的 UI 显示 `Hello B`。

---

## 2. 本轮明确不做

为了控制复杂度，这一轮**禁止主动引入**：

- Kafka
- Redis
- PostgreSQL 消息持久化
- Outbox
- Elasticsearch
- MinIO
- 微服务
- CQRS
- DDD Aggregate
- 消息历史记录
- 已读
- 撤回
- 群聊
- 文件消息
- 推送通知

### 当前消息暂时只存在于内存

服务器重启后消息消失，这是**刻意设计**。

本轮只学习：

> WebSocket 连接、Session 管理、消息接收、消息转发、Android 状态更新。

---

## 3. 前置条件

以下能力已经存在，不要重新实现：

- 用户注册
- 用户登录
- JWT Access Token
- `/api/v1/users/me`
- WebSocket JWT 握手认证
- Android DataStore Token

因此本轮默认：

```text
A = 已登录用户
B = 已登录用户
```

并且：

```text
A WebSocket Session = 已认证
B WebSocket Session = 已认证
```

---

## 4. 本轮最简单的消息协议

### A → Server

```json
{
  "type": "SEND_MESSAGE",
  "messageId": "客户端生成的唯一 ID",
  "timestamp": 0,
  "payload": {
    "toUserId": "B 的 userId",
    "content": "Hello B"
  }
}
```

### Server → B

```json
{
  "type": "MESSAGE_RECEIVED",
  "messageId": "原消息 ID",
  "timestamp": 0,
  "payload": {
    "fromUserId": "A 的 userId",
    "content": "Hello B"
  }
}
```

> 当前协议故意简单：先直接使用 `toUserId`，暂时不引入 Conversation。

---

## 5. 后端你需要自己思考的东西

不要直接复制代码，先回答下面的问题。

### 5.1 Server 如何知道“这个 WebSocket 属于谁”？

你已经完成了 JWT 握手认证。

因此连接建立后，服务器应该能够得到：

```text
WebSocketSession
        ↓
Principal
        ↓
userId
```

---

### 5.2 Server 怎么找到 B？

第一版最简单：

```text
Map<userId, WebSocketSession>
```

但请自己考虑：

- 用户已经有连接怎么办？
- 用户断开以后 Map 怎么清理？
- 同一个用户两个设备连接怎么办？
- 发送给一个不存在/离线用户怎么办？

> **本轮先选择最简单方案。**
>
> 多设备、多节点以后再解决。

---

### 5.3 谁负责消息转发？

建议最简单的职责：

```text
WebSocket Handler
       ↓
读取发送者
       ↓
解析 toUserId
       ↓
查找 B Session
       ↓
sendMessage(B)
```

不要在这一轮加入 Kafka。

---

## 6. 后端建议的最小文件职责

你可以自己决定最终包结构，但至少要能分清：

```text
WebSocket 配置
    ↓
连接建立 / 关闭
    ↓
消息接收
    ↓
消息解析
    ↓
在线用户 Session 管理
    ↓
消息转发
```

### 建议思考的组件

#### WebSocket Config

负责：

- 注册 `/ws`
- 配置 Handler
- 配置握手认证

#### ConnectionManager

负责：

- userId → WebSocket Session
- 注册连接
- 移除连接
- 查询连接

> TODO：本轮先用内存结构，不要上 Redis。

#### ChatWebSocketHandler

负责：

- 收到客户端消息
- 读取当前用户
- 解析消息
- 调用连接管理器找到接收者
- 转发消息

> TODO：Handler 不要塞一堆业务逻辑。
>
> 先做到“能工作”，后面再重构。

#### Message DTO

至少需要：

- type
- messageId
- timestamp
- payload

> TODO：客户端协议对象和领域实体先不要混在一起。

---

## 7. Android 需要自己完成的链路

```text
ChatScreen
    ↓
ChatViewModel
    ↓
ChatRepository
    ↓
WebSocket Client
```

### ChatScreen

负责：

- 输入框
- 发送按钮
- 消息列表
- 当前连接状态

不要：

- 直接操作 WebSocket
- 直接解析 JSON
- 直接管理 Session

---

### ChatViewModel

负责：

- UI State
- 发送消息
- 接收消息
- 维护消息列表
- 暴露 WebSocket 状态

例如最终你应该能得到类似：

```text
ChatUiState
├── connectionState
├── messages
├── input
└── error
```

---

### ChatRepository

负责：

- connect()
- disconnect()
- sendMessage()
- observeMessages()

> TODO：不要把 OkHttp / WebSocket 的底层细节泄漏到 Compose UI。

---

## 8. 本轮最重要的学习点

### ① WebSocket 是长连接

不是：

```text
请求 → 响应 → 结束
```

而是：

```text
Connect
   ↓
保持连接
   ↓
A → Server
   ↓
Server → B
   ↓
B → Server
   ↓
Server → A
```

---

### ② Server 必须保存连接状态

因为 Server 需要知道：

```text
userId = A → 哪个 Session
userId = B → 哪个 Session
```

---

### ③ WebSocket Session 不是用户

```text
User
  ↓
可以有
  ↓
WebSocket Session
```

一个用户未来可能有多个设备。

---

### ④ 消息发送成功 ≠ 消息持久化

这一轮：

```text
A 发送
 ↓
Server 转发
 ↓
B 收到
```

就算成功。

但是：

```text
Server 重启
 ↓
消息消失
```

也是正常的。

因为**持久化属于下一阶段**。

---

## 9. 第一版验收标准

### 后端

- [ ] A 能建立 WebSocket
- [ ] B 能建立 WebSocket
- [ ] Server 能识别 A/B 的 userId
- [ ] Server 能登记在线 Session
- [ ] A 发送 `SEND_MESSAGE`
- [ ] Server 能解析消息
- [ ] Server 能找到 B
- [ ] Server 能把消息转给 B
- [ ] B 能收到 `MESSAGE_RECEIVED`

### Android

- [ ] Chat 页面能显示连接状态
- [ ] 输入消息
- [ ] 点击发送
- [ ] A UI 能看到自己的发送结果（可以先本地显示）
- [ ] B UI 能实时出现消息

### 最终测试

同时运行两个客户端：

```text
Client A
userId = A

Client B
userId = B
```

A：

```text
Hello B
```

B：

```text
Hello B
```

看到这一个结果：

> **A 发 → B 收**

本轮就完成。

---

## 10. AI Coding 使用规则

本项目使用 AI 辅助开发时，本轮请遵循：

1. 不让 AI 一次性生成全部代码。
2. 先让 AI 解释设计，再自己写。
3. 每完成一个类，再让 AI Review。
4. 遇到报错先自己定位，再让 AI 辅助。
5. 明确告诉 AI：

```text
当前任务：
A → B WebSocket 消息转发。

不要引入：
Kafka / Redis / DB / DDD / Outbox。

只解决当前问题。
```

---

## 11. 做完之后再思考

完成最小 Demo 后，再问自己：

1. 如果 B 不在线怎么办？
2. 如果 B 断线了怎么办？
3. 如果一个用户有两个设备怎么办？
4. 如果 Server 有两台怎么办？
5. 如果 Server 重启怎么办？
6. 如果消息不能丢怎么办？

这些问题就是下一阶段引入：

```text
数据库
Redis
Kafka
Outbox
消息补偿
多节点 WebSocket
```

的真实原因。

> **先把 A → B 做懂，再解决这些问题。**

---

## 12. 本轮结束后的下一步

不要直接进入复杂架构。

下一条只增加一个能力：

```text
A → B
    ↓
消息保存 PostgreSQL
    ↓
B 重连
    ↓
还能看到历史消息
```

到这里，你才真正开始理解：

> **为什么聊天系统需要数据库。**
