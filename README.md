# TeamFlow-Agent

一个用于沉淀 Android + Spring Boot + WebSocket 实战能力的学习型聊天项目。

## 当前开发目标

先不要追求复杂架构。

当前只做最小链路：

`A → WebSocket → Spring Boot → WebSocket → B`

即：

> A 发一条消息，B 实时收到。

## 当前刻意不使用

- Kafka
- Redis
- Elasticsearch
- MinIO
- Outbox
- CQRS
- 微服务
- DDD 复杂分层

这些技术以后逐步加入。

## 当前前置条件

Identity 第一条 Vertical Slice 已经完成：

- 注册
- 登录
- JWT
- /me
- WebSocket 鉴权
- Android DataStore Token

因此当前聊天 Slice 默认 A、B 都已经登录，并且都能成功建立 WebSocket 连接。

## 学习原则

1. 先自己设计，再写代码。
2. AI 只辅助分析、解释和 Code Review，不直接代写完整功能。
3. 每次只增加一个核心能力。
4. 代码跑通后，必须能够自己解释请求是怎么流转的。

当前任务见：

- [docs/vertical-slice-01-a-to-b-message.md](docs/vertical-slice-01-a-to-b-message.md)
