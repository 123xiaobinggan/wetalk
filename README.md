wetalk
仿微信IM客户端

frontend：
electron + vue3 + vite + pinia 状态管理 + vue-router 路由管理 + ws 长连接 

backend:
java + springboot3 + mybatis + mysql + redis + netty + webRTC P2P 音视频

server:
2 cores + 2GB memory + 40GB SSD + 1Mbps bandwidth + nginx 反向代理

使用netty: 支持高并发
p2p通信: 流量不经服务器端转发, 节省带宽, 提高性能
redis: 缓存用户信息和群成员id, 降低数据库查询压力

客户端功能:
1. 登录注册
2. 支持私聊、群聊
3. 会话管理功能
4. 支持图片视频文件传输
5. 支持私聊语音通话
6. 朋友圈功能

ws通信事件制定:
type:
  chat

  call

  friend

  group

  system

  conn

event:
  chat:{
    send,
    receive,
    recall,
    ack,
    failed
  }

  call:{
    audio_call,
    audio_reject,
    audio_accept,
    audio_end,
    audio_busy
  }

  friend: {
    request_send,
    request_accept,
    friend_update
  }

  group: {
    create,
    join,
    disband,
    quit,
    set_role,
    group_update,
    members_update
  }

  conn: {
    ping,
    pong
  }

  system: {
    error,
    notice,
    online,
    offline
  }

数据库表结构:
user:
CREATE TABLE `user` (
    `user_id` int NOT NULL AUTO_INCREMENT COMMENT 'Primar  Key',
    `accountId` varchar(12) NOT NULL COMMENT 'accountId',
    `username` varchar(20) NOT NULL COMMENT 'username',
    `avatar` varchar(255) DEFAULT '',
    `sex` tinyint DEFAULT '1' COMMENT '0:female; 1:male',
    `password` varchar(32) NOT NULL COMMENT 'Password',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT 'Status',
    `area_name` varchar(50) DEFAULT NULL COMMENT 'Area Name',
    `area_code` varchar(50) DEFAULT NULL COMMENT 'Area Code',
    `personal_signature` varchar(50) DEFAULT NULL COMMENT 'Personal Signature',
    `last_login_time` datetime DEFAULT NULL COMMENT 'Last Login Time',
    `last_off_time` datetime DEFAULT NULL COMMENT 'Last Off Time',
    `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created Time',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `accountId` (`accountId`)
) ENGINE = InnoDB AUTO_INCREMENT = 7 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'user'

conversation:
CREATE TABLE `conversation` (
    `user_id` bigint NOT NULL COMMENT '用户 id',
    `conv_id` bigint NOT NULL AUTO_INCREMENT,
    `session_id` bigint DEFAULT NULL COMMENT '共同会话的id',
    `conv_type` tinyint(1) NOT NULL COMMENT '会话类型: 0-单聊, 1-群聊',
    `peer_id` bigint DEFAULT NULL COMMENT '会话对象 id, conv_type=1时有效',
    `group_id` bigint DEFAULT NULL COMMENT '群 id, conv_type=2时有效',
    `group_remark` varchar(64) DEFAULT NULL COMMENT '群聊备注，仅conv_type==true时有效',
    `pinned` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶: 0-否, 1-是',
    `muted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否免打扰: 0-否, 1-是',
    `unread_cnt` int NOT NULL DEFAULT '0' COMMENT '未读消息数',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `last_msg_id` bigint DEFAULT NULL COMMENT '最后一条消息 id',
    `last_msg_time` datetime DEFAULT NULL COMMENT '最后消息时间',
    `last_cleared_time` datetime DEFAULT NULL,
    `last_msg_brief` varchar(255) DEFAULT NULL COMMENT '最后一条消息的摘要',
    `last_msg_sender_id` bigint DEFAULT NULL COMMENT '最后一条信息的发送者，conv_type=1时有效',
    `deleted` tinyint DEFAULT '0' COMMENT '0:正常 1: 已删除',
    PRIMARY KEY (`conv_id`),
    UNIQUE KEY `user_id` (
        `user_id`,
        `conv_type`,
        `peer_id`
    ),
    UNIQUE KEY `user_id_2` (`user_id`, `session_id`),
    KEY `user_id_3` (
        `user_id`,
        `pinned`,
        `last_msg_time`,
        `created_time`
    )
) ENGINE = InnoDB AUTO_INCREMENT = 95 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci

friendship:
CREATE TABLE `friendship` (
    `user_id` int NOT NULL,
    `friend_user_id` int NOT NULL,
    `remark` varchar(20) DEFAULT NULL COMMENT '备注',
    `blocked` tinyint DEFAULT '0' COMMENT '拉黑',
    `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '成为好友的时间',
    `hide_my_moments` tinyint DEFAULT '0' COMMENT '0：允许对方看朋友圈 1：不允许',
    `hide_friend_moments` tinyint DEFAULT '0' COMMENT '0: 不隐藏对方朋友圈 1：屏蔽对方朋友圈',
    PRIMARY KEY (`user_id`, `friend_user_id`),
    KEY `user_id` (`user_id`),
    KEY `user_id_2` (
        `user_id`,
        `friend_user_id`,
        `blocked`
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'friendships'

friend_request:
CREATE TABLE `friend_request` (
    `friend_request_id` bigint NOT NULL AUTO_INCREMENT,
    `requester_user_id` int NOT NULL,
    `requestee_user_id` int NOT NULL,
    `request_msg` varchar(255) DEFAULT NULL COMMENT '打招呼消息',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '0:已读,1:未读 2: 已同意',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `hide_my_moments` tinyint DEFAULT '0' COMMENT '0:不隐藏我的朋友圈 1: 隐藏',
    `hide_friend_moments` tinyint DEFAULT '0' COMMENT '0:不隐藏对方朋友圈 1: 隐藏',
    PRIMARY KEY (`friend_request_id`),
    KEY `idx_user_id_status` (
        `requester_user_id`,
        `requestee_user_id`,
        `status`
    )
) ENGINE = InnoDB AUTO_INCREMENT = 33 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'friend_requests'

group:
CREATE TABLE `group` (
    `group_id` bigint NOT NULL AUTO_INCREMENT,
    `session_id` bigint DEFAULT NULL,
    `group_name` varchar(64) NOT NULL,
    `group_avatar` varchar(255) DEFAULT NULL,
    `owner_user_id` bigint NOT NULL,
    `member_cnt` int NOT NULL,
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `announcement` text COMMENT '群公告',
    `status` tinyint DEFAULT '0' COMMENT '0:正常 1:解散 2: 全员禁言',
    PRIMARY KEY (`group_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 17 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'group'

group_member:
CREATE TABLE `group_member` (
    `group_id` bigint NOT NULL,
    `member_user_id` bigint NOT NULL,
    `role` tinyint DEFAULT '0' COMMENT '0 普通成员,1 管理员,2 群主',
    `my_nickname` varchar(64) DEFAULT NULL,
    `joined_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `join_seq` bigint NOT NULL DEFAULT '0',
    `silence` tinyint DEFAULT '0' COMMENT '禁言',
    PRIMARY KEY (`group_id`, `member_user_id`),
    KEY `idx_member` (`member_user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群成员表'

message:
CREATE TABLE `message` (
    `msg_id` bigint NOT NULL AUTO_INCREMENT,
    `conv_type` tinyint(1) NOT NULL COMMENT '0: DM, 1: Group',
    `sender_id` bigint DEFAULT NULL,
    `peer_id` bigint DEFAULT NULL,
    `session_id` bigint NOT NULL,
    `msg_type` tinyint(1) NOT NULL COMMENT '1: Text, 2: Audio, 3: Image, 4: Video, 5: File 6:系统消息',
    `content` json NOT NULL,
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `recall_flag` tinyint DEFAULT '0' COMMENT '撤回状态：1-撤回',
    `quote_msg_id` bigint DEFAULT NULL COMMENT '引用消息',
    PRIMARY KEY (`msg_id`),
    KEY `session_id` (`session_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 325 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci

deleted_message:
CREATE TABLE `deleted_message` (
    `msg_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `deleted_time` datetime DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `msg_id` (`msg_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
