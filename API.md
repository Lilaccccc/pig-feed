# API 文档

> 本文档由项目中所有实现 `Controller` 特质的类自动整理而成，共覆盖 **9 个控制器**、**33 个 API 端点**。

---

## 通用约定

### 基础信息

| 项目 | 说明 |
|------|------|
| 协议 | HTTP |
| 数据格式 | JSON（`application/json`） |
| 字符编码 | UTF-8 |
| 日期格式 | `yyyy-MM-dd'T'HH:mm:ss`（ISO-8601 LocalDateTime） |

### 认证机制

| 标记 | 说明 |
|------|------|
| 🔒 需要认证 | 请求需携带 `Authorization: Bearer <JWT>` 头，由 JWT 解析出 `userId` 和 `role` 注入上下文 |
| 🌐 公开接口 | 无需认证（使用 `.logic` 注册的端点） |

### 统一响应格式

所有接口返回 `R[T]` 包装结构：

```json
{
  "code": 1000,
  "msg": "请求成功",
  "data": { ... }
}
```

#### 状态码

| code | 含义 |
|------|------|
| 1000 | 请求成功 |
| 1001 | 请求失败 |
| 1002 | 参数错误 |
| 1003 | 未授权 |
| 1004 | 资源不存在 |
| 1005 | 禁止访问 |
| 1006 | 未查询到数据 |
| 10018 | 用户 ID 无效 |
| 30004 | 视频 ID 无效 |
| 30011 | 幂等键过长 |
| 30012 | limit 参数无效 |
| 50002 | 游标无效 |
| 50003 | 不支持的 Scene |
| 50004 | 需要 viewerId |
| 50005 | 加载 Feed 失败 |
| 90000 | 加载推荐失败 |
| 90001 | 加载曝光决策失败 |
| 90002 | 保存推荐曝光失败 |
| 90003 | scene 为空 |
| 90004 | watchMs 为负数 |
| 90005 | scene 过长 |
| 90006 | eventType 无效 |
| 90007 | requestId 过长 |
| 90008 | 维度不匹配 |

### 幂等键

部分写操作需要请求头 `Idempotency-Key`，用于防止重复提交。

### 分页约定

| 模式 | 参数 | 说明 |
|------|------|------|
| 游标分页 | `cursor`、`limit` | `cursor` 为空表示首页，响应含 `nextCursor` 和 `hasMore` |
| 偏移分页 | `limit`、`offset` | 传统分页方式 |

---

## 1. 账户模块（AccountController）

> 文件：`internal/auth/controller/AccountController.scala`

### 1.1 用户注册

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/users` |
| 认证 | 🌐 公开 |
| 描述 | 注册新用户 |

**请求体** `RegisterRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| account | String | 账号 |
| password | String | 密码 |
| nickname | String | 昵称 |

**响应** `R[UserProfileResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户 ID |
| account | String | 账号 |
| nickname | String | 昵称 |
| avatarUrl | String | 头像 URL |
| bio | String | 个人简介 |
| status | Int | 用户状态 |
| role | String | 角色 |
| followingCount | Int | 关注数 |
| followerCount | Int | 粉丝数 |
| workCount | Int | 作品数 |

### 1.2 用户登录

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/sessions` |
| 认证 | 🌐 公开 |
| 描述 | 账号密码登录，创建会话（JWT） |

**请求体** `LoginByPasswordRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| account | String | 账号 |
| password | String | 密码 |

**响应** `R[TokenResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | String | JWT 访问令牌 |
| tokenType | String | 令牌类型 |
| expiresInSeconds | Long | 过期时间（秒） |

### 1.3 用户登出

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/sessions/current` |
| 认证 | 🌐 公开（无状态 JWT，服务端无需清理） |
| 描述 | 登出当前会话 |

**响应** `R[String]`

### 1.4 获取当前用户信息

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/users/me` |
| 认证 | 🔒 需要认证 |
| 描述 | 获取登录用户的完整资料 |

**响应** `R[UserProfileResponse]`（同 1.1）

### 1.5 更新当前用户信息

| 项目 | 说明 |
|------|------|
| 方法 | `PATCH` |
| 路径 | `/users/update/me` |
| 认证 | 🔒 需要认证 |
| 描述 | 更新登录用户的资料 |

**请求体** `UpdateProfileRequest`

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| nickname | Option[String] | None | 昵称 |
| avatarUrl | Option[String] | None | 头像 URL |
| bio | Option[String] | None | 个人简介 |

**响应** `R[UserProfileResponse]`（同 1.1）

### 1.6 获取指定用户公开信息

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/users/{userId}` |
| 认证 | 🌐 公开 |
| 描述 | 获取指定用户的公开资料 |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |

**响应** `R[PublicUserProfileResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户 ID |
| nickname | String | 昵称 |
| avatarUrl | String | 头像 URL |
| bio | String | 个人简介 |
| followingCount | Int | 关注数 |
| followerCount | Int | 粉丝数 |
| workCount | Int | 作品数 |

---

## 2. 视频模块（UsersVideoController）

> 文件：`internal/video/controller/UsersVideoController.scala`

### 2.1 创建视频

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/users/create/videos` |
| 认证 | 🔒 需要认证 |
| 描述 | 创建新视频（需幂等键） |

**请求头**

| 头部 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Idempotency-Key | String | 是 | 幂等键，防止重复创建 |

**请求体** `CreateVideoRequest`

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| title | String | - | 标题 |
| description | Option[String] | None | 描述 |
| mediaUrl | String | - | 媒体 URL |
| coverUrl | String | - | 封面 URL |

**响应** `R[VideoResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 视频 ID |
| authorId | Long | 作者 ID |
| title | String | 标题 |
| description | String | 描述 |
| mediaUrl | String | 媒体 URL |
| coverUrl | String | 封面 URL |
| status | Int | 视频状态 |
| likeCount | Long | 点赞数 |
| commentCount | Long | 评论数 |
| favoriteCount | Long | 收藏数 |
| publishedAt | LocalDateTime | 发布时间 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

### 2.2 获取视频

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/users/videos/get/{videoId}` |
| 认证 | 🌐 公开 |
| 描述 | 获取指定视频详情 |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |

**响应** `R[VideoResponse]`（同 2.1）

### 2.3 删除视频

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/users/videos/delete/{videoId}` |
| 认证 | 🔒 需要认证 |
| 描述 | 删除指定视频（仅作者可操作） |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |

**响应** `R[Long]`（返回被删除的视频 ID）

### 2.4 获取作者视频列表

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/users/{userId}/videos` |
| 认证 | 🌐 公开 |
| 描述 | 获取指定用户发布的视频列表 |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |

**查询参数**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| limit | Int | 20 | 每页数量 |
| offset | Int | 0 | 偏移量 |

**响应** `R[VideoListResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List[VideoResponse] | 视频列表 |
| limit | Int | 每页数量 |
| offset | Int | 偏移量 |

### 2.5 获取我的视频列表

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/users/me/videos/page` |
| 认证 | 🔒 需要认证 |
| 描述 | 获取当前登录用户发布的视频列表 |

**查询参数**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| limit | Int | 20 | 每页数量 |
| offset | Int | 0 | 偏移量 |

**响应** `R[VideoListResponse]`（同 2.4）

---

## 3. 上传模块（UploadController）

> 文件：`internal/upload/controller/UploadController.scala`

### 3.1 上传文件

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/uploads` |
| 认证 | 🔒 需要认证 |
| 描述 | 上传文件（multipart/form-data），请求体大小受 `MaxUploadBytes` 限制 |

**请求体** `multipart/form-data` - `UploadForm`

| 字段 | 类型 | 说明 |
|------|------|------|
| file | Part[File] | 文件内容 |

**响应** `R[UploadResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| url | String | 文件访问 URL |
| kind | String | 文件类型 |
| filename | String | 文件名 |
| size | Long | 文件大小（字节） |

---

## 4. 关系模块（FollowController）

> 文件：`internal/relation/controller/FollowController.scala`

### 4.1 关注用户

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/users/me/follow/{targetUserId}` |
| 认证 | 🔒 需要认证 |
| 描述 | 关注指定用户（需幂等键） |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| targetUserId | Long | 目标用户 ID |

**请求头**

| 头部 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Idempotency-Key | String | 是 | 幂等键 |

**响应** `R[FollowResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 当前用户 ID |
| targetUserId | Long | 目标用户 ID |
| status | Int | 关注状态 |
| following | Boolean | 是否已关注 |
| followingCount | Int | 当前用户关注数 |
| followerCount | Int | 目标用户粉丝数 |

### 4.2 取消关注

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/users/me/unfollow/{targetUserId}` |
| 认证 | 🔒 需要认证 |
| 描述 | 取消关注指定用户（需幂等键） |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| targetUserId | Long | 目标用户 ID |

**请求头**

| 头部 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Idempotency-Key | String | 是 | 幂等键 |

**响应** `R[FollowResponse]`（同 4.1）

### 4.3 获取关注列表

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/users/me/following/list` |
| 认证 | 🔒 需要认证 |
| 描述 | 获取当前用户的关注列表（游标分页） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| cursor | String | 游标（空表示首页） |
| limit | Int | 每页数量 |

**响应** `R[RelationListResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List[RelationUserResponse] | 用户列表 |
| nextCursor | String | 下一页游标 |
| hasMore | Boolean | 是否还有更多 |

**RelationUserResponse**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| nickname | String | 昵称 |
| avatarUrl | String | 头像 URL |
| bio | String | 个人简介 |
| followedAt | LocalDateTime | 关注时间 |

### 4.4 获取粉丝列表

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/users/me/followers/list` |
| 认证 | 🔒 需要认证 |
| 描述 | 获取当前用户的粉丝列表（游标分页） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| cursor | String | 游标（空表示首页） |
| limit | Int | 每页数量 |

**响应** `R[RelationListResponse]`（同 4.3）

---

## 5. 消息模块（MessageController）

> 文件：`internal/message/controller/MessageController.scala`

### 5.1 获取消息列表

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/messages/list` |
| 认证 | 🔒 需要认证 |
| 描述 | 获取当前用户的消息列表（游标分页） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| limit | Int | 每页数量 |
| cursor | String | 游标（空表示首页） |

**响应** `R[MessageListResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List[MessageResponse] | 消息列表 |
| nextCursor | String | 下一页游标 |
| hasMore | Boolean | 是否还有更多 |

**MessageResponse**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 消息 ID |
| userId | Long | 接收用户 ID |
| messageType | String | 消息类型 |
| title | String | 标题 |
| eventId | String | 事件 ID |
| actorId | Long | 触发者 ID |
| actorNickname | String | 触发者昵称 |
| actorAvatarUrl | String | 触发者头像 URL |
| isRead | Boolean | 是否已读 |
| createdAt | Option[LocalDateTime] | 创建时间 |
| readAt | Option[LocalDateTime] | 已读时间 |

### 5.2 标记消息已读

| 项目 | 说明 |
|------|------|
| 方法 | `PATCH` |
| 路径 | `/messages/markread` |
| 认证 | 🔒 需要认证 |
| 描述 | 批量标记消息为已读 |

**请求体** `MarkReadRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| messageIds | List[Long] | 消息 ID 列表 |

**响应** `R[MarkReadResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| updatedCount | Long | 已更新的消息数量 |

### 5.3 获取未读消息数

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/messages-stats/countunread` |
| 认证 | 🔒 需要认证 |
| 描述 | 获取当前用户的未读消息数量 |

**响应** `R[UnreadStatResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| unreadCount | Long | 未读消息数 |

---

## 6. 互动模块（InteractionController）

> 文件：`internal/interaction/controller/InteractionController.scala`
>
> 注意：本模块使用 **查询参数**（query）传递 `videoId`、`commentId`，而非路径参数。

### 6.1 点赞视频

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/videos/like?videoId={videoId}` |
| 认证 | 🔒 需要认证 |
| 描述 | 点赞指定视频（需幂等键） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |

**请求头**

| 头部 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Idempotency-Key | String | 是 | 幂等键 |

**响应** `R[ActionResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |
| actionType | String | 动作类型（like） |
| active | Boolean | 是否激活 |
| likeCount | Long | 点赞数 |
| favoriteCount | Long | 收藏数 |

### 6.2 取消点赞

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/videos/unlike?videoId={videoId}` |
| 认证 | 🔒 需要认证 |
| 描述 | 取消点赞指定视频（需幂等键） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |

**请求头**

| 头部 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Idempotency-Key | String | 是 | 幂等键 |

**响应** `R[ActionResponse]`（同 6.1）

### 6.3 收藏视频

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/videos/favorite?videoId={videoId}` |
| 认证 | 🔒 需要认证 |
| 描述 | 收藏指定视频（需幂等键） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |

**请求头**

| 头部 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Idempotency-Key | String | 是 | 幂等键 |

**响应** `R[ActionResponse]`（同 6.1，actionType 为 favorite）

### 6.4 取消收藏

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/videos/unfavorite?videoId={videoId}` |
| 认证 | 🔒 需要认证 |
| 描述 | 取消收藏指定视频（需幂等键） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |

**请求头**

| 头部 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Idempotency-Key | String | 是 | 幂等键 |

**响应** `R[ActionResponse]`（同 6.1）

### 6.5 创建评论

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/videos/createcomments?videoId={videoId}` |
| 认证 | 🔒 需要认证 |
| 描述 | 对指定视频发表评论（需幂等键） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |

**请求头**

| 头部 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Idempotency-Key | String | 是 | 幂等键 |

**请求体** `CreateCommentRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| context | String | 评论内容 |

**响应** `R[CommentResponse]`

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| id | Long | - | 评论 ID |
| videoId | Long | - | 视频 ID |
| userId | Long | - | 评论者 ID |
| userNickname | String | - | 评论者昵称 |
| userAvatarUrl | String | - | 评论者头像 URL |
| content | String | - | 评论内容 |
| createdAt | LocalDateTime | - | 创建时间 |
| commentCount | Option[Long] | None | 评论总数 |

### 6.6 获取评论列表

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/videos/listcomments?videoId={videoId}` |
| 认证 | 🌐 公开 |
| 描述 | 获取指定视频的评论列表（游标分页） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |
| limit | Int | 每页数量 |
| cursor | String | 游标（空表示首页） |

**响应** `R[CommentListResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List[CommentResponse] | 评论列表 |
| nextCursor | String | 下一页游标 |
| hasMore | Boolean | 是否还有更多 |

### 6.7 删除评论

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/comments/delete?commentId={commentId}` |
| 认证 | 🔒 需要认证 |
| 描述 | 删除指定评论（评论者或管理员可操作） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| commentId | Long | 评论 ID |

**响应** `R[DeleteCommentResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| commentId | Long | 评论 ID |
| status | Int | 评论状态 |
| commentCount | Long | 剩余评论数 |

---

## 7. Feed 模块（FeedController）

> 文件：`internal/feed/controller/FeedController.scala`

### 7.1 获取 Feed 列表

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/feeds/items` |
| 认证 | 🔒 需要认证 |
| 描述 | 获取 Feed 列表（游标分页，支持多场景） |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| limit | Int | 每页数量（1-100，默认 10） |
| scene | String | 场景（timeline/recommend/following/hot） |
| cursor | String | 游标（空表示首页） |

**响应** `R[FeedItemsResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| scene | String | 场景 |
| items | List[FeedItemResponse] | Feed 列表 |
| nextCursor | String | 下一页游标 |
| hasMore | Boolean | 是否还有更多 |

**FeedItemResponse**

| 字段 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |
| authorId | Long | 作者 ID |
| authorNickname | String | 作者昵称 |
| authorAvatarUrl | String | 作者头像 URL |
| title | String | 标题 |
| description | String | 描述 |
| mediaUrl | String | 媒体 URL |
| coverUrl | String | 封面 URL |
| likeCount | Long | 点赞数 |
| commentCount | Long | 评论数 |
| favoriteCount | Long | 收藏数 |
| liked | Boolean | 当前用户是否已点赞 |
| favorited | Boolean | 当前用户是否已收藏 |
| publishedAt | LocalDateTime | 发布时间 |

### 7.2 查询 Feed

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/feeds/queries` |
| 认证 | 🔒 需要认证 |
| 描述 | 通过请求体查询 Feed（支持 clientContext） |

**请求体** `FeedQueryRequest`

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| scene | String | - | 场景 |
| cursor | String | - | 游标 |
| limit | Int | - | 每页数量 |
| clientContext | Map[String, String] | Map.empty | 客户端上下文 |

**响应** `R[FeedItemsResponse]`（同 7.1）

### 7.3 刷新 Feed

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/feeds/refresh` |
| 认证 | 🌐 公开 |
| 描述 | 刷新 Feed 缓存 |

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| limit | Int | 每页数量 |

**响应** `R[FeedItemsResponse]`（同 7.1）

---

## 8. 曝光模块（ExposureController）

> 文件：`internal/exposure/controller/ExposureController.scala`

### 8.1 创建观看事件

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/exposure/video/view/events` |
| 认证 | 🔒 需要认证 |
| 描述 | 上报视频观看事件，可能同步写入曝光记录 |

**请求体** `CreateViewEventRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |
| scene | String | 场景（最长 32 字符） |
| requestId | String | 请求 ID（最长 64 字符） |
| eventType | String | 事件类型（exposed/play/complete/skip） |
| watchMs | Int | 观看时长（毫秒，≥0） |
| completed | Boolean | 是否看完 |

**响应** `R[CreateViewEventResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| event | ViewEventResponse | 观看事件信息 |
| exposure | Option[ExposureResponse] | 曝光记录（可能为空） |
| published | Boolean | 是否已发布事件 |

**ViewEventResponse**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 事件 ID |
| userId | Long | 用户 ID |
| videoId | Long | 视频 ID |
| scene | String | 场景 |
| requestId | String | 请求 ID |
| eventType | String | 事件类型 |
| watchMs | Int | 观看时长 |
| completed | Boolean | 是否看完 |
| createdAt | LocalDateTime | 创建时间 |

**ExposureResponse**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| videoId | Long | 视频 ID |
| firstExposedAt | LocalDateTime | 首次曝光时间 |
| lastExposedAt | LocalDateTime | 最后曝光时间 |
| exposureCount | Int | 曝光次数 |
| lastScene | String | 最后曝光场景 |

---

## 9. 内部接口模块（InternalController）

> 文件：`internal/exposure/controller/InternalController.scala`
>
> 这些接口为内部服务调用，不对外暴露。

### 9.1 推荐候选

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/internal/recommendation/candidates` |
| 认证 | 🌐 公开 |
| 描述 | 一次完成召回、排序、打散，返回推荐候选列表 |

**请求体** `CandidateRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| scene | String | 场景 |
| requestId | String | 请求 ID |
| cursor | String | 游标 |
| limit | Int | 每页数量 |

**响应** `R[CandidateResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| scene | String | 场景 |
| requestId | String | 请求 ID |
| candidates | List[CandidateItemResponse] | 候选列表 |
| nextCursor | String | 下一页游标 |
| hasMore | Boolean | 是否还有更多 |

**CandidateItemResponse**

| 字段 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |
| authorId | Long | 作者 ID |
| rankScore | Double | 排序分数 |
| similarity | Double | 相似度 |
| hotScore | Int | 热度分数 |
| freshnessScore | Double | 新鲜度分数 |
| reason | String | 推荐原因 |
| publishedAt | LocalDateTime | 发布时间 |

### 9.2 曝光决策

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/internal/exposure/decisions` |
| 认证 | 🌐 公开 |
| 描述 | 判断候选视频是否近期曝光过 |

**请求体** `ExposureDecisionsRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| scene | String | 场景 |
| requestId | String | 请求 ID |
| videoIds | List[Long] | 视频 ID 列表 |

**响应** `R[ExposureDecisionsResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| scene | String | 场景 |
| requestId | String | 请求 ID |
| decisions | List[ExposureDecisionItemResponse] | 决策列表 |

**ExposureDecisionItemResponse**

| 字段 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频 ID |
| allowed | Boolean | 是否允许曝光 |
| reason | String | 决策原因（fresh/recently_exposed/unknown） |
| lastExposedAt | Option[LocalDateTime] | 最后曝光时间 |

### 9.3 保存曝光记录

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/internal/exposures` |
| 认证 | 🌐 公开 |
| 描述 | 写入曝光记录 |

**请求体** `ExposuresRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| scene | String | 场景 |
| requestId | String | 请求 ID |
| videoIds | List[Long] | 视频 ID 列表 |

**响应** `R[ExposuresResponse]`

| 字段 | 类型 | 说明 |
|------|------|------|
| exposures | List[ExposureItemResponse] | 曝光记录列表 |

**ExposureItemResponse**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| videoId | Long | 视频 ID |
| firstExposedAt | LocalDateTime | 首次曝光时间 |
| lastExposedAt | LocalDateTime | 最后曝光时间 |
| exposureCount | Int | 曝光次数 |
| lastScene | String | 最后曝光场景 |

---

## 附录：端点汇总表

| # | 方法 | 路径 | 认证 | 模块 | 描述 |
|---|------|------|------|------|------|
| 1 | POST | `/users` | 🌐 | 账户 | 用户注册 |
| 2 | POST | `/sessions` | 🌐 | 账户 | 用户登录 |
| 3 | DELETE | `/sessions/current` | 🌐 | 账户 | 用户登出 |
| 4 | GET | `/users/me` | 🔒 | 账户 | 获取当前用户信息 |
| 5 | PATCH | `/users/update/me` | 🔒 | 账户 | 更新当前用户信息 |
| 6 | GET | `/users/{userId}` | 🌐 | 账户 | 获取指定用户公开信息 |
| 7 | POST | `/users/create/videos` | 🔒 | 视频 | 创建视频 |
| 8 | GET | `/users/videos/get/{videoId}` | 🌐 | 视频 | 获取视频 |
| 9 | DELETE | `/users/videos/delete/{videoId}` | 🔒 | 视频 | 删除视频 |
| 10 | GET | `/users/{userId}/videos` | 🌐 | 视频 | 获取作者视频列表 |
| 11 | GET | `/users/me/videos/page` | 🔒 | 视频 | 获取我的视频列表 |
| 12 | POST | `/uploads` | 🔒 | 上传 | 上传文件 |
| 13 | PUT | `/users/me/follow/{targetUserId}` | 🔒 | 关系 | 关注用户 |
| 14 | DELETE | `/users/me/unfollow/{targetUserId}` | 🔒 | 关系 | 取消关注 |
| 15 | GET | `/users/me/following/list` | 🔒 | 关系 | 获取关注列表 |
| 16 | GET | `/users/me/followers/list` | 🔒 | 关系 | 获取粉丝列表 |
| 17 | GET | `/messages/list` | 🔒 | 消息 | 获取消息列表 |
| 18 | PATCH | `/messages/markread` | 🔒 | 消息 | 标记消息已读 |
| 19 | GET | `/messages-stats/countunread` | 🔒 | 消息 | 获取未读消息数 |
| 20 | PUT | `/videos/like` | 🔒 | 互动 | 点赞视频 |
| 21 | DELETE | `/videos/unlike` | 🔒 | 互动 | 取消点赞 |
| 22 | PUT | `/videos/favorite` | 🔒 | 互动 | 收藏视频 |
| 23 | DELETE | `/videos/unfavorite` | 🔒 | 互动 | 取消收藏 |
| 24 | POST | `/videos/createcomments` | 🔒 | 互动 | 创建评论 |
| 25 | GET | `/videos/listcomments` | 🌐 | 互动 | 获取评论列表 |
| 26 | DELETE | `/comments/delete` | 🔒 | 互动 | 删除评论 |
| 27 | GET | `/feeds/items` | 🔒 | Feed | 获取 Feed 列表 |
| 28 | POST | `/feeds/queries` | 🔒 | Feed | 查询 Feed |
| 29 | GET | `/feeds/refresh` | 🌐 | Feed | 刷新 Feed |
| 30 | POST | `/exposure/video/view/events` | 🔒 | 曝光 | 创建观看事件 |
| 31 | POST | `/internal/recommendation/candidates` | 🌐 | 内部 | 推荐候选 |
| 32 | POST | `/internal/exposure/decisions` | 🌐 | 内部 | 曝光决策 |
| 33 | POST | `/internal/exposures` | 🌐 | 内部 | 保存曝光记录 |
