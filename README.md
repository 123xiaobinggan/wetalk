# WeTalk - 仿微信即时通讯客户端

基于 Electron + Vue3 的跨平台即时通讯应用，支持私聊、群聊、音视频通话、朋友圈等核心功能。

## ✨ 特性

- 💬 **私聊 & 群聊**：支持文字、图片、视频、文件传输
- 📞 **语音通话**：基于 WebRTC 的 P2P 音视频通话
- 🔄 **消息撤回**：支持消息撤回功能
- 📱 **朋友圈**：发布动态、点赞、评论
- 🔔 **实时推送**：WebSocket 长连接，消息实时送达
- 🚀 **高性能**：Netty 高并发 + Redis 缓存 + P2P 流量直连

## 🛠 技术栈

### 前端 (Frontend)

| 技术       | 用途       |
| ---------- | ---------- |
| Electron   | 桌面端框架 |
| Vue 3      | UI 框架    |
| Vite       | 构建工具   |
| Pinia      | 状态管理   |
| Vue Router | 路由管理   |
| WebSocket  | 实时通信   |

### 后端 (Backend)

| 技术                 | 用途                 |
| -------------------- | -------------------- |
| Java + Spring Boot 3 | 主框架               |
| MyBatis              | ORM                  |
| MySQL                | 数据库               |
| Redis                | 缓存                 |
| Netty                | WebSocket 长连接服务 |
| WebRTC               | P2P 音视频通话       |

### 服务器 (Server)

| 配置     | 规格      |
| -------- | --------- |
| CPU      | 2 核心    |
| 内存     | 2 GB      |
| 存储     | 40 GB SSD |
| 带宽     | 1 Mbps    |
| 反向代理 | Nginx     |

## 🏗 架构设计

- **Netty 长连接**：支持高并发 WebSocket 连接
- **P2P 通信**：音视频流量不经服务器转发，节省带宽
- **Redis 缓存**：缓存用户信息和群成员 ID，降低数据库查询压力

## 📦 功能模块

### 客户端功能

- [x] 用户登录 / 注册
- [x] 私聊消息
- [x] 群聊消息
- [x] 会话管理（置顶、免打扰、删除）
- [x] 图片 / 视频 / 文件传输
- [x] 语音通话（P2P）
- [x] 朋友圈（发布、点赞、评论）

## 📡 WebSocket 通信协议

### 消息类型 (Type)

| Type     | 说明       |
| -------- | ---------- |
| `chat`   | 聊天消息   |
| `call`   | 音视频通话 |
| `friend` | 好友相关   |
| `group`  | 群组相关   |
| `system` | 系统通知   |
| `conn`   | 连接维护   |

### 事件定义 (Event)

#### chat - 聊天

| Event     | 说明     |
| --------- | -------- |
| `send`    | 发送消息 |
| `receive` | 接收消息 |
| `recall`  | 撤回消息 |
| `ack`     | 消息确认 |
| `failed`  | 发送失败 |

#### call - 通话

| Event          | 说明         |
| -------------- | ------------ |
| `audio_call`   | 发起语音通话 |
| `audio_reject` | 拒绝通话     |
| `audio_accept` | 接听通话     |
| `audio_end`    | 结束通话     |
| `audio_busy`   | 忙线         |

#### friend - 好友

| Event            | 说明         |
| ---------------- | ------------ |
| `request_send`   | 发送好友请求 |
| `request_accept` | 接受好友请求 |
| `friend_update`  | 好友信息更新 |

#### group - 群组

| Event            | 说明       |
| ---------------- | ---------- |
| `create`         | 创建群聊   |
| `join`           | 加入群聊   |
| `disband`        | 解散群聊   |
| `quit`           | 退出群聊   |
| `set_role`       | 设置角色   |
| `group_update`   | 群信息更新 |
| `members_update` | 成员更新   |

#### conn - 连接

| Event  | 说明     |
| ------ | -------- |
| `ping` | 心跳请求 |
| `pong` | 心跳响应 |

#### system - 系统

| Event     | 说明     |
| --------- | -------- |
| `error`   | 错误通知 |
| `notice`  | 系统通知 |
| `online`  | 上线通知 |
| `offline` | 下线通知 |

## 🗄 数据库表结构

### 用户相关

| 表名             | 说明         |
| ---------------- | ------------ |
| `user`           | 用户基本信息 |
| `friendship`     | 好友关系     |
| `friend_request` | 好友请求     |

### 会话相关

| 表名              | 说明           |
| ----------------- | -------------- |
| `conversation`    | 会话列表       |
| `message`         | 消息记录       |
| `deleted_message` | 已删除消息记录 |

### 群组相关

| 表名           | 说明     |
| -------------- | -------- |
| `group`        | 群组信息 |
| `group_member` | 群成员   |

<details>
<summary>详细建表 SQL（点击展开）</summary>

#### user

```sql
CREATE TABLE `user` (
    `user_id` int NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `accountId` varchar(12) NOT NULL COMMENT '账号ID',
    `username` varchar(20) NOT NULL COMMENT '用户名',
    `avatar` varchar(255) DEFAULT '',
    `sex` tinyint DEFAULT '1' COMMENT '0:女; 1:男',
    `password` varchar(32) NOT NULL COMMENT '密码',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态',
    `area_name` varchar(50) DEFAULT NULL COMMENT '地区名称',
    `area_code` varchar(50) DEFAULT NULL COMMENT '地区代码',
    `personal_signature` varchar(50) DEFAULT NULL COMMENT '个性签名',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_off_time` datetime DEFAULT NULL COMMENT '最后离线时间',
    `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `accountId` (`accountId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE `conversation` (
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `conv_id` bigint NOT NULL AUTO_INCREMENT,
    `session_id` bigint DEFAULT NULL COMMENT '共同会话ID',
    `conv_type` tinyint(1) NOT NULL COMMENT '会话类型: 0-单聊, 1-群聊',
    `peer_id` bigint DEFAULT NULL COMMENT '会话对象ID',
    `group_id` bigint DEFAULT NULL COMMENT '群ID',
    `group_remark` varchar(64) DEFAULT NULL COMMENT '群聊备注',
    `pinned` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶',
    `muted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否免打扰',
    `unread_cnt` int NOT NULL DEFAULT '0' COMMENT '未读消息数',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `last_msg_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
    `last_msg_time` datetime DEFAULT NULL COMMENT '最后消息时间',
    `last_cleared_time` datetime DEFAULT NULL,
    `last_msg_brief` varchar(255) DEFAULT NULL COMMENT '最后一条消息摘要',
    `last_msg_sender_id` bigint DEFAULT NULL COMMENT '最后消息发送者',
    `deleted` tinyint DEFAULT '0' COMMENT '0:正常 1:已删除',
    PRIMARY KEY (`conv_id`),
    UNIQUE KEY `user_id` (`user_id`, `conv_type`, `peer_id`),
    UNIQUE KEY `user_id_2` (`user_id`, `session_id`),
    KEY `user_id_3` (`user_id`, `pinned`, `last_msg_time`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `friendship` (
    `user_id` int NOT NULL,
    `friend_user_id` int NOT NULL,
    `remark` varchar(20) DEFAULT NULL COMMENT '备注',
    `blocked` tinyint DEFAULT '0' COMMENT '拉黑',
    `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '成为好友时间',
    `hide_my_moments` tinyint DEFAULT '0' COMMENT '0:允许看朋友圈 1:不允许',
    `hide_friend_moments` tinyint DEFAULT '0' COMMENT '0:不屏蔽 1:屏蔽对方朋友圈',
    PRIMARY KEY (`user_id`, `friend_user_id`),
    KEY `user_id` (`user_id`),
    KEY `user_id_2` (`user_id`, `friend_user_id`, `blocked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友关系表';

CREATE TABLE `friend_request` (
    `friend_request_id` bigint NOT NULL AUTO_INCREMENT,
    `requester_user_id` int NOT NULL,
    `requestee_user_id` int NOT NULL,
    `request_msg` varchar(255) DEFAULT NULL COMMENT '打招呼消息',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '0:未读 1:已读 2:已同意',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `hide_my_moments` tinyint DEFAULT '0',
    `hide_friend_moments` tinyint DEFAULT '0',
    PRIMARY KEY (`friend_request_id`),
    KEY `idx_user_id_status` (`requester_user_id`, `requestee_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友请求表';

CREATE TABLE `group` (
    `group_id` bigint NOT NULL AUTO_INCREMENT,
    `session_id` bigint DEFAULT NULL,
    `group_name` varchar(64) NOT NULL,
    `group_avatar` varchar(255) DEFAULT NULL,
    `owner_user_id` bigint NOT NULL,
    `member_cnt` int NOT NULL,
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `announcement` text COMMENT '群公告',
    `status` tinyint DEFAULT '0' COMMENT '0:正常 1:解散 2:全员禁言',
    PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群组表';


CREATE TABLE `group_member` (
    `group_id` bigint NOT NULL,
    `member_user_id` bigint NOT NULL,
    `role` tinyint DEFAULT '0' COMMENT '0:普通成员 1:管理员 2:群主',
    `my_nickname` varchar(64) DEFAULT NULL,
    `joined_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `join_seq` bigint NOT NULL DEFAULT '0',
    `silence` tinyint DEFAULT '0' COMMENT '禁言',
    PRIMARY KEY (`group_id`, `member_user_id`),
    KEY `idx_member` (`member_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群成员表';

CREATE TABLE `message` (
    `msg_id` bigint NOT NULL AUTO_INCREMENT,
    `conv_type` tinyint(1) NOT NULL COMMENT '0:私聊 1:群聊',
    `sender_id` bigint DEFAULT NULL,
    `peer_id` bigint DEFAULT NULL,
    `session_id` bigint NOT NULL,
    `msg_type` tinyint(1) NOT NULL COMMENT '1:文字 2:语音 3:图片 4:视频 5:文件 6:系统消息',
    `content` json NOT NULL,
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `recall_flag` tinyint DEFAULT '0' COMMENT '撤回状态: 1-已撤回',
    `quote_msg_id` bigint DEFAULT NULL COMMENT '引用消息ID',
    PRIMARY KEY (`msg_id`),
    KEY `session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `deleted_message` (
    `msg_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `deleted_time` datetime DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `msg_id` (`msg_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

</details>

环境要求
Node.js 18+

Java 17+

MySQL 8.0+

Redis 6.0+

# 安装依赖
npm install

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 打包应用
npm run dist

# 编译
./gradlew clean bootJar

# 运行
java -jar build/libs/wetalk-0.0.1-SNAPSHOT.jar
