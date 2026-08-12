package internal.auth.entity.account

import internal.auth.enums.account.{Role, Status}
import internal.infra.errors.*
import utils.base.ColoredLogger
import utils.result.ErrorResult
import org.mindrot.jbcrypt.BCrypt

import scala.util.Try

// 账号聚合根，保存登录凭证、展示资料和权限角色。
final case class User(
  id: Long,
  account: String,
  password: String,
  nickname: String,
  avatarUrl: Option[String] = None,
  bio: Option[String] = None,
  status: Int,
  role: String,
  // FollowingCount 和 FollowerCount 来自关系模块统计表，用于个人页展示。
  followingCount: Option[Int] = None,
  followerCount: Option[Int] = None,
  workCount: Option[Int] = None
) {
  def registerAvatarUrl = Some(avatarUrl.getOrElse("").trim)
  def registerBio = Some(bio.getOrElse("").trim)
}

object User extends ColoredLogger {
  // 创建新用户，负责输入清洗、必填校验和密码哈希。
  def news(account: String, password: String, nickname: String): Either[ErrorResult, User] = {
    if account.isBlank then return Left(ErrEmptyAccount())
    if password.isBlank then return Left(ErrEmptyPassword())
    if nickname.isBlank then return Left(ErrEmptyNickname())

    val result = (hash: String) =>
      User(
        id = 0,
        account = account.trim,
        password = hash,
        nickname = nickname.trim,
        status = Status.Normal.value,
        role = Role.User.name
      )

    // 密码只保存 bcrypt 哈希，数据库中不会保存明文密码。
    Try(BCrypt.hashpw(password.trim, BCrypt.gensalt()))
      .map(hash => Right(result(hash)))
      .getOrElse(Left(ErrHashPasswordFailed()))
  }

  extension (u: User) {
    // 校验用户输入密码是否匹配已保存的 bcrypt 哈希。
    def authenticate(password: String): Either[ErrorResult, User] = {
      if password.isBlank then return Left(ErrEmptyPassword())
      if !BCrypt.checkpw(password.trim, u.password) then return Left(ErrInvalidCredentials())
      Right(u)
    }

    def updateProfile(nickname: Option[String], avatarUrl: Option[String], bio: Option[String]): Either[ErrorResult, User] = try {
      if nickname.exists(_.isBlank) && avatarUrl.exists(_.isBlank) && bio.exists(_.isBlank) then return Left(ErrEmptyProfileUpdate())
      Right(u.copy(nickname = nickname.map(_.trim).getOrElse(u.nickname), avatarUrl = avatarUrl.map(_.trim), bio = bio.map(_.trim)))
    } catch {
      case e: Exception => error(e.getMessage); Left(ErrEmptyProfileUpdate())
    }
  }
}
