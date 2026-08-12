-- 用户账户表
CREATE TABLE `account`
(
    `id`         bigint AUTO_INCREMENT COMMENT '用户ID，主键',
    `account`    varchar(64)  NOT NULL COMMENT '用户账号，登录名',
    `password`   varchar(255) NOT NULL COMMENT '用户密码，加密存储',
    `nickname`   varchar(128) NOT NULL COMMENT '用户昵称',
    `avatar_url` varchar(512) COMMENT '用户头像URL',
    `bio`        varchar(255) COMMENT '用户个人简介',
    `status`     bigint       NOT NULL DEFAULT 1 COMMENT '用户状态：1-正常，2-禁用，3-已注销',
    `role`       varchar(32)  NOT NULL COMMENT '用户角色：admin-管理员，user-普通用户，vip-VIP用户',
    `created_at` datetime(3) NULL COMMENT '创建时间',
    `updated_at` datetime(3) NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_account_account` (`account`) COMMENT '账号唯一索引'
) COMMENT '用户账户表';

-- 视频特征向量表（用于推荐系统）
CREATE TABLE `video_embedding`
(
    `video_id`       bigint COMMENT '视频ID，关联video表',
    `model`          varchar(64) NOT NULL COMMENT '向量模型名称，如：clip-vit-base-patch32',
    `dimension`      bigint      NOT NULL COMMENT '向量维度',
    `embedding_json` json        NOT NULL COMMENT '向量数据的JSON存储',
    `text_hash`      varchar(64) NOT NULL COMMENT '视频文本内容的哈希值，用于去重和版本管理',
    `created_at`     datetime(3) NULL COMMENT '创建时间',
    `updated_at`     datetime(3) NULL COMMENT '更新时间',
    PRIMARY KEY (`video_id`, `model`),
    UNIQUE INDEX `uk_video_model` (`video_id`,`model`) COMMENT '视频-模型唯一联合索引',
    INDEX            `idx_model_updated` (`model`,`updated_at`) COMMENT '模型更新时间索引，用于增量更新查询'
) COMMENT '视频特征向量表，存储视频的多模态向量特征';

-- 视频基本信息表
CREATE TABLE `video`
(
    `id`              bigint AUTO_INCREMENT COMMENT '视频ID，主键',
    `author_id`       bigint       NOT NULL COMMENT '作者ID，关联account表',
    `title`           varchar(128) NOT NULL COMMENT '视频标题',
    `description`     varchar(512) COMMENT '视频描述',
    `media_url`       varchar(512) NOT NULL COMMENT '视频文件存储URL',
    `cover_url`       varchar(512) NOT NULL COMMENT '视频封面图URL',
    `status`          tinyint      NOT NULL DEFAULT 2 COMMENT '视频状态：1-审核中，2-已发布，3-审核拒绝，4-已下架',
    `published_at`    datetime(3) NULL COMMENT '发布时间',
    `idempotency_key` varchar(128) COMMENT '幂等键，防止重复提交',
    `created_at`      datetime(3) NULL COMMENT '创建时间',
    `updated_at`      datetime(3) NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX             `idx_author_status` (`author_id`,`status`,`created_at`) COMMENT '作者-状态-时间索引，用于查询作者视频列表',
    UNIQUE INDEX `uk_author_idempotency` (`author_id`,`idempotency_key`) COMMENT '作者-幂等键唯一索引，防止重复创建',
    INDEX             `idx_status_published` (`status`,`published_at`) COMMENT '状态-发布时间索引，用于首页推荐和列表查询'
) COMMENT '视频基本信息表';

-- 视频统计数据表
CREATE TABLE `video_stat`
(
    `video_id`       bigint AUTO_INCREMENT COMMENT '视频ID，主键，关联video表',
    `like_count`     bigint NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count`  bigint NOT NULL DEFAULT 0 COMMENT '评论数',
    `favorite_count` bigint NOT NULL DEFAULT 0 COMMENT '收藏数',
    `created_at`     datetime(3) NULL COMMENT '创建时间',
    `updated_at`     datetime(3) NULL COMMENT '更新时间',
    PRIMARY KEY (`video_id`)
) COMMENT '视频统计数据表，存储视频的各类互动统计数据';

-- 用户收件箱表（Feed流）
CREATE TABLE `feed_inbox`
(
    `id`           bigint AUTO_INCREMENT COMMENT '收件箱记录ID，主键',
    `user_id`      bigint NOT NULL COMMENT '接收用户ID，关联account表',
    `video_id`     bigint NOT NULL COMMENT '视频ID，关联video表',
    `author_id`    bigint NOT NULL COMMENT '视频作者ID，关联account表',
    `published_at` datetime(3) NOT NULL COMMENT '视频发布时间',
    `created_at`   datetime(3) NULL COMMENT '创建时间（入箱时间）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_video` (`user_id`,`video_id`) COMMENT '用户-视频唯一索引，防止重复入箱',
    INDEX          `idx_user_published_video` (`user_id`,`published_at`,`video_id`) COMMENT '用户-发布时间-视频索引，用于拉取Feed流',
    INDEX          `idx_video` (`video_id`) COMMENT '视频ID索引，用于反向查询',
    INDEX          `idx_author` (`author_id`) COMMENT '作者ID索引，用于查询用户发布视频的推送情况'
) COMMENT '用户收件箱表，用于实现关注用户的视频Feed流推送';

-- 视频观看事件表
CREATE TABLE `video_view_events`
(
    `id`         bigint AUTO_INCREMENT COMMENT '事件ID，主键',
    `user_id`    bigint      NOT NULL COMMENT '用户ID，关联account表',
    `video_id`   bigint      NOT NULL COMMENT '视频ID，关联video表',
    `scene`      varchar(32) NOT NULL COMMENT '观看场景：home-首页推荐，search-搜索结果，follow-关注流，profile-个人主页',
    `request_id` varchar(64) COMMENT '请求ID，用于追踪用户行为链路',
    `event_type` varchar(32) NOT NULL COMMENT '事件类型：start-开始播放，heartbeat-心跳，end-结束播放',
    `watch_ms`   bigint      NOT NULL DEFAULT 0 COMMENT '观看时长（毫秒）',
    `completed`  boolean     NOT NULL DEFAULT false COMMENT '是否完整播放',
    `created_at` datetime(3) NULL COMMENT '创建时间（事件发生时间）',
    PRIMARY KEY (`id`),
    INDEX        `idx_user_created` (`user_id`,`created_at`) COMMENT '用户-时间索引，用于用户观看历史查询',
    INDEX        `idx_video_created` (`video_id`,`created_at`) COMMENT '视频-时间索引，用于视频热度统计',
    INDEX        `idx_user_scene_created` (`scene`,`created_at`) COMMENT '场景-时间索引，用于场景化数据分析',
    INDEX        `idx_request_event` (`request_id`,`event_type`) COMMENT '请求-事件类型索引，用于追踪完整播放链路'
) COMMENT '视频观看事件表，记录用户观看行为明细数据';

-- 视频曝光记录表
CREATE TABLE `exposures`
(
    `id`               bigint AUTO_INCREMENT COMMENT '曝光记录ID，主键',
    `user_id`          bigint      NOT NULL COMMENT '用户ID，关联account表',
    `video_id`         bigint      NOT NULL COMMENT '视频ID，关联video表',
    `first_exposed_at` datetime(3) NOT NULL COMMENT '首次曝光时间',
    `last_exposed_at`  datetime(3) NOT NULL COMMENT '最后曝光时间',
    `exposure_count`   bigint      NOT NULL DEFAULT 1 COMMENT '曝光总次数',
    `last_scene`       varchar(32) NOT NULL COMMENT '最近一次曝光的场景',
    `created_at`       datetime(3) NULL COMMENT '创建时间',
    `updated_at`       datetime(3) NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_video` (`user_id`,`video_id`) COMMENT '用户-视频唯一索引，用于去重统计',
    INDEX              `idx_user_last_exposed` (`user_id`,`last_exposed_at`) COMMENT '用户-最后曝光时间索引，用于用户曝光历史查询',
    INDEX              `idx_video_last_exposed` (`video_id`,`last_exposed_at`) COMMENT '视频-最后曝光时间索引，用于视频曝光度分析'
) COMMENT '视频曝光记录表，记录用户看到视频的曝光行为';

-- 互动行为表（点赞/收藏/分享等）
CREATE TABLE `interaction_action`
(
    `id`              bigint AUTO_INCREMENT COMMENT '互动记录ID，主键',
    `user_id`         bigint      NOT NULL COMMENT '用户ID，关联account表',
    `video_id`        bigint      NOT NULL COMMENT '视频ID，关联video表',
    `action_type`     varchar(16) NOT NULL COMMENT '互动类型：like-点赞，favorite-收藏，share-分享，dislike-点踩',
    `status`          tinyint     NOT NULL DEFAULT 1 COMMENT '状态：1-有效，0-已取消',
    `idempotency_key` varchar(128) COMMENT '幂等键，防止重复提交',
    `created_at`      datetime(3) NULL COMMENT '创建时间',
    `updated_at`      datetime(3) NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_video_type` (`user_id`,`video_id`,`action_type`) COMMENT '用户-视频-类型唯一索引，保证每种互动唯一',
    INDEX             `idx_user_type_status` (`user_id`,`action_type`,`status`) COMMENT '用户-类型-状态索引，用于查询用户互动列表',
    INDEX             `idx_video_type_status` (`video_id`,`action_type`,`status`) COMMENT '视频-类型-状态索引，用于统计视频互动数据'
) COMMENT '互动行为表，记录用户对视频的点赞、收藏、分享等互动行为';

-- 评论表
CREATE TABLE `interaction_comment`
(
    `id`              bigint AUTO_INCREMENT COMMENT '评论ID，主键',
    `video_id`        bigint        NOT NULL COMMENT '视频ID，关联video表',
    `user_id`         bigint        NOT NULL COMMENT '评论用户ID，关联account表',
    `content`         varchar(1000) NOT NULL COMMENT '评论内容',
    `status`          tinyint       NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-已删除，2-审核中，3-审核拒绝',
    `idempotency_key` varchar(128) COMMENT '幂等键，防止重复提交',
    `created_at`      datetime(3) NULL COMMENT '创建时间',
    `updated_at`      datetime(3) NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX             `idx_video_status_created` (`video_id`,`status`,`created_at`,`updated_at`) COMMENT '视频-状态-时间复合索引，用于查询视频评论列表',
    INDEX             `idx_user_created` (`user_id`,`created_at`) COMMENT '用户-时间索引，用于查询用户评论历史',
    UNIQUE INDEX `uk_user_idempotency` (`user_id`,`idempotency_key`) COMMENT '用户-幂等键唯一索引'
) COMMENT '评论表，记录用户对视频的评论内容';

-- 用户消息通知表
CREATE TABLE `user_message`
(
    `id`               bigint AUTO_INCREMENT COMMENT '消息ID，主键',
    `user_id`          bigint        NOT NULL COMMENT '接收消息的用户ID，关联account表',
    `type`             varchar(16)   NOT NULL COMMENT '消息类型：like-点赞，comment-评论，follow-关注，system-系统通知',
    `title`            varchar(128)  NOT NULL COMMENT '消息标题',
    `content`          varchar(1024) NOT NULL COMMENT '消息内容',
    `actor_id`         bigint        NOT NULL DEFAULT 0 COMMENT '触发者的用户ID，0表示系统',
    `actor_nickname`   varchar(128) COMMENT '触发者昵称（冗余存储）',
    `actor_avatar_url` varchar(512) COMMENT '触发者头像URL（冗余存储）',
    `event_id`         varchar(64) COMMENT '事件ID，用于关联具体业务事件',
    `idempotency_key`  varchar(128) COMMENT '幂等键，防止重复发送',
    `is_read`          boolean       NOT NULL DEFAULT false COMMENT '是否已读：true-已读，false-未读',
    `created_at`       datetime(3) NULL COMMENT '创建时间（消息发送时间）',
    `read_at`          datetime(3) NULL COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    INDEX              `idx_user_read_created` (`user_id`,`is_read`,`created_at`) COMMENT '用户-已读-时间索引，用于查询未读消息列表',
    UNIQUE INDEX `uk_user_event` (`user_id`,`event_id`) COMMENT '用户-事件唯一索引，防止重复消息',
    UNIQUE INDEX `uk_user_idempotency` (`user_id`,`idempotency_key`) COMMENT '用户-幂等键唯一索引',
    INDEX              `idx_user_created` (`created_at`) COMMENT '创建时间索引，用于批量清理历史消息'
) COMMENT '用户消息通知表，存储各类业务消息和系统通知';

-- 播放配置表
CREATE TABLE `playback_config`
(
    `id`            bigint AUTO_INCREMENT COMMENT '配置ID，主键',
    `platform`      varchar(16) NOT NULL COMMENT '平台类型：ios-苹果，android-安卓，web-网页端',
    `network_type`  varchar(16) NOT NULL COMMENT '网络类型：wifi-WiFi网络，4G-4G网络，5G-5G网络',
    `preload_count` bigint      NOT NULL COMMENT '预加载视频数量',
    `buffer_ms`     bigint      NOT NULL COMMENT '缓冲时长（毫秒）',
    `updated_at`    datetime(3) NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_platform_network` (`platform`,`network_type`) COMMENT '平台-网络唯一索引，确保每个平台网络组合只有一条配置'
) COMMENT '播放配置表，根据不同平台和网络类型配置不同的播放策略';

-- 播放QoS质量日志表
CREATE TABLE `playback_qos_log`
(
    `id`              bigint AUTO_INCREMENT COMMENT '日志ID，主键',
    `user_id`         bigint NOT NULL COMMENT '用户ID，关联account表',
    `video_id`        bigint NOT NULL COMMENT '视频ID，关联video表',
    `first_frame_ms`  bigint COMMENT '首帧加载耗时（毫秒）',
    `stutter_count`   bigint NOT NULL DEFAULT 0 COMMENT '卡顿次数',
    `watch_ms`        bigint NOT NULL DEFAULT 0 COMMENT '实际观看时长（毫秒）',
    `idempotency_key` varchar(128) COMMENT '幂等键，防止重复上报',
    `created_at`      datetime(3) NULL COMMENT '创建时间（上报时间）',
    PRIMARY KEY (`id`),
    INDEX             `idx_user_video_time` (`user_id`,`video_id`,`created_at`) COMMENT '用户-视频-时间索引，用于用户播放质量分析',
    UNIQUE INDEX `uk_user_idempotency` (`user_id`,`idempotency_key`) COMMENT '用户-幂等键唯一索引',
    INDEX             `idx_video_time` (`video_id`,`created_at`) COMMENT '视频-时间索引，用于视频播放质量统计'
) COMMENT '播放QoS质量日志表，记录视频播放的体验质量数据';

-- 用户关注关系表
CREATE TABLE `user_follow`
(
    `id`              bigint AUTO_INCREMENT COMMENT '关注记录ID，主键',
    `user_id`         bigint  NOT NULL COMMENT '关注者用户ID，关联account表',
    `target_user_id`  bigint  NOT NULL COMMENT '被关注用户ID，关联account表',
    `status`          tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-正在关注，0-已取消关注',
    `idempotency_key` varchar(128) COMMENT '幂等键，防止重复提交',
    `created_at`      datetime(3) NULL COMMENT '创建时间（首次关注时间）',
    `updated_at`      datetime(3) NULL COMMENT '更新时间（状态变更时间）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_target` (`user_id`,`target_user_id`) COMMENT '关注者-被关注者唯一索引，保证关注关系唯一',
    INDEX             `idx_user_status_updated` (`user_id`,`status`,`updated_at`,`target_user_id`) COMMENT '关注者-状态-时间索引，用于查询用户的关注列表',
    INDEX             `idx_target_status_updated` (`target_user_id`,`status`,`updated_at`) COMMENT '被关注者-状态-时间索引，用于查询用户的粉丝列表'
) COMMENT '用户关注关系表，记录用户之间的关注和粉丝关系';

-- 用户关系统计表
CREATE TABLE `user_relation_stat`
(
    `user_id`         bigint AUTO_INCREMENT COMMENT '用户ID，主键，关联account表',
    `following_count` bigint NOT NULL DEFAULT 0 COMMENT '关注数（该用户关注了多少人）',
    `follower_count`  bigint NOT NULL DEFAULT 0 COMMENT '粉丝数（多少人关注了该用户）',
    `created_at`      datetime(3) NULL COMMENT '创建时间',
    `updated_at`      datetime(3) NULL COMMENT '更新时间',
    PRIMARY KEY (`user_id`)
) COMMENT '用户关系统计表，存储用户关注数和粉丝数的统计信息';