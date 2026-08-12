package internal.auth.service

import internal.auth.entity.account.Profile.toProfile
import internal.auth.entity.account.{LoginResult, Profile, User}
import internal.auth.entity.jwt.JwtManager
import internal.auth.enums.account.Role
import internal.auth.repository.UserRepository
import internal.infra.errors.*
import utils.base.ColoredLogger

object AccountService extends ColoredLogger {
  // 创建新用户：领域层负责校验和加密密码，仓储层负责持久化。
  def register(account: String, password: String, nickname: String): Either[Exception, Profile] = {
    User
      .news(account, password, nickname)
      .flatMap(u => UserRepository.save(u))
      .map(u => u.toProfile)
      .fold(
        err => Left(err.asInstanceOf[Exception]),
        profile => Right(profile)
      )
  }

  // 完成账号密码登录，认证通过后签发访问 token。
  def login(account: String, password: String): Either[Exception, LoginResult] = {
    if account.isBlank then return Left(ErrEmptyAccount())

    UserRepository
      .findByAccount(account)
      .flatMap(u => u.authenticate(password))
      // token 内写入用户 ID 和角色，后续鉴权中间件会解析并放入请求上下文。
      .flatMap(u => JwtManager.signAccessToken(u.id, Role.fromName(u.role)))
      .fold(
        err => Left(err.asInstanceOf[Exception]),
        token => Right(LoginResult.bearer(token))
      )
  }

  // 根据登录态中的用户 ID 读取当前用户资料。
  def getProfile(userId: Long): Either[Exception, Profile] = {
    if userId <= 0 then return Left(ErrInvalidUserID())
    UserRepository
      .findById(userId)
      .fold(
        err => Left(err.asInstanceOf[Exception]),
        u => Right(u.toProfile)
      )
  }

  // 根据用户 ID 读取公开资料，用于访问他人主页。
  def getPublicProfile(userId: Long): Either[Exception, Profile] = {
    if userId <= 0 then return Left(ErrInvalidUserID())
    UserRepository
      .findById(userId)
      .fold(
        err => Left(err.asInstanceOf[Exception]),
        u => Right(u.toProfile)
      )
  }

  // 支持部分更新，nil 表示该字段没有出现在请求体中。
  def updateProfile(userId: Long, nickname: Option[String] = None, avatarUrl: Option[String] = None, bio: Option[String] = None): Either[Exception, Profile] = {
    if userId <= 0 then return Left(ErrInvalidUserID())
    UserRepository
      .findById(userId)
      .flatMap(u => u.updateProfile(nickname, avatarUrl, bio))
      .flatMap(u => UserRepository.updateProfile(u))
      .fold(
        err => Left(err.asInstanceOf[Exception]),
        u => Right(u.toProfile)
      )
  }
}
