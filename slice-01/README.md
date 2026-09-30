# TeamFlow Vertical Slice 01

这是当前最小可运行聊天实验：

Android A
→ WebSocket
→ Spring Boot
→ WebSocket
→ Android B

目标只有一个：A 发一条消息，B 收到。

## 当前刻意不使用

Kafka、Redis、PostgreSQL 消息持久化、Outbox、Elasticsearch、MinIO、STOMP、微服务、DDD。

服务器只在内存中保存在线连接，消息也只做实时转发。

## 身份说明

因为 GitHub 仓库目前没有放入你本地已经完成的 JWT 认证实现，所以这个 standalone demo 临时使用：

ws://10.0.2.2:8080/ws?userId=alice

这是开发测试方式，不是生产认证方式。

你之后把它合并进已经通过验收的 TeamFlow 主工程时，把 DevUserIdHandshakeInterceptor 替换成你现有的 JWT WebSocket 握手认证即可。ChatWebSocketHandler 已经优先读取 WebSocket Principal，所以改动点很小。

## 后端

打开 slice-01/backend。

如果你的本地环境有 Maven：

mvn spring-boot:run

如果 IntelliJ IDEA 已配置 Maven，也可以直接运行 TeamFlowSlice01Application。

健康检查：

http://localhost:8080/api/v1/health

## Android

用 Android Studio 打开 slice-01/android。

需要两台 Android Emulator。

客户端 A：

My userId = alice
Target userId = bob

客户端 B：

My userId = bob
Target userId = alice

两边都点击 Connect。

然后在 A 输入消息并点击 Send。

B 应该实时显示：

[alice] Hello Bob

## 阅读代码顺序

1. WebSocketChatRepository.connect()
2. DevUserIdHandshakeInterceptor
3. ChatWebSocketHandler.afterConnectionEstablished()
4. ConnectionManager
5. ChatWebSocketHandler.handleTextMessage()
6. WebSocketChatRepository.onMessage()
7. ChatViewModel
8. MainActivity

这条顺序就是一次完整的 WebSocket 消息链路。
