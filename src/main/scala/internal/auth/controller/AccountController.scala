package internal.auth.controller

import internal.auth.controller.dto.*
import internal.auth.controller.handler.AccountHandler as Handler
import internal.infra.route.RouteOps.*
import utils.result.R
import utils.route.Controller

class AccountController extends Controller {

  override val serverEndpointList = List(register, login, logout, me, updateMe, get)

  // 会话资源用于登录态：创建会话表示登录，删除当前会话表示登出。
  // /sessions
  private def login = this.endpoint.post
    .in("sessions" and jsonBody[LoginByPasswordRequest])
    .out(jsonBody[R[TokenResponse]])
    .logic(Handler.login)

  // 当前项目使用无状态 JWT，服务端无需清理会话数据。
  // /sessions/current
  private def logout = this.endpoint.delete
    .in("sessions" / "current")
    .out(jsonBody[R[String]])
    .security((context: Context, _) => "ok")

  // /users
  private def register = this.endpoint.post
    .in("users" and jsonBody[RegisterRequest])
    .out(jsonBody[R[UserProfileResponse]])
    .logic(Handler.register)

  // /users/me
  private def me = this.endpoint.get
    .in("users" / "me")
    .out(jsonBody[R[UserProfileResponse]])
    .security((context: Context, _) => Handler.me(context.userId))

  // /users/update/me
  private def updateMe = this.endpoint.patch
    .in("users" / "update" / "me" and jsonBody[UpdateProfileRequest])
    .out(jsonBody[R[UserProfileResponse]])
    .security((context: Context, request) => Handler.updateMe(context.userId, request))

  // /users/{userId}
  private def get = this.endpoint.get
    .in("users" / path[Long]("userId"))
    .out(jsonBody[R[PublicUserProfileResponse]])
    .logic(userId => Handler.get(userId))
}
