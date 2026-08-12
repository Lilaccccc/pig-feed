package internal.auth.controller.handler

import internal.auth.controller.dto.*
import internal.auth.service.AccountService
import internal.infra.errors.*
import io.scalaland.chimney.dsl.*
import utils.base.ColoredLogger
import utils.result.throws

object AccountHandler extends ColoredLogger {
  // 处理用户注册请求，成功后返回新用户资料。
  def register(request: RegisterRequest): UserProfileResponse = {
    // 具体注册规则在应用层和领域层执行，HTTP 层只传递请求字段。
    AccountService
      .register(request.account, request.password, request.nickname)
      .fold(
        err => err.throws(using this),
        profile => profile.transformInto[UserProfileResponse]
      )
  }

  // 处理账号密码登录，成功后返回 Bearer token。
  def login(request: LoginByPasswordRequest): TokenResponse = {
    // 具体注册规则在应用层和领域层执行，HTTP 层只传递请求字段。
    AccountService
      .login(request.account, request.password)
      .fold(
        // 登录失败统一映射为 401，避免暴露账号是否存在。
        err => err.throws(ErrInvalidCredentials())(using this),
        token => token.transformInto[TokenResponse]
      )
  }

  // 读取当前登录用户资料，用户 ID 来自 JWT 中间件写入的上下文。
  def me(userId: Long): UserProfileResponse = {
    AccountService
      .getProfile(userId)
      .fold(
        err => err.throws(using this),
        result => result.transformInto[UserProfileResponse]
      )
  }

  // 更新当前登录用户资料，请求体支持部分字段更新。
  def updateMe(userId: Long, request: UpdateProfileRequest): UserProfileResponse = {
    debug(s"request::$request")
    AccountService
      .updateProfile(userId, request.nickname, request.avatarUrl, request.bio)
      .fold(
        err => err.throws(using this),
        result => result.transformInto[UserProfileResponse]
      )
  }

  // 读取公开用户资料，用于访问他人主页。
  def get(userId: Long): PublicUserProfileResponse = {
    AccountService
      .getPublicProfile(userId)
      .fold(
        err => err.throws(using this),
        result => result.transformInto[PublicUserProfileResponse]
      )
  }
}
