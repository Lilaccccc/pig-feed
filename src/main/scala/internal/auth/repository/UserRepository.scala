package internal.auth.repository

import internal.auth.entity.account.User
import internal.auth.enums.account.Status
import internal.infra.errors.*
import internal.infra.utils.clampCount
import sqala.jdbc.JdbcTransactionContext
import sqala.metadata.*
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}

@table("account")
final case class UserModel(
  @autoInc id: Long,
  account: String,
  password: String,
  nickname: String,
  avatarUrl: Option[String] = None,
  bio: Option[String] = None,
  status: Int,
  role: String,
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
)

object UserRepository extends ColoredLogger {
  extension (u: User) {
    // 把数据库模型转换回领域对象，业务逻辑继续操作领域类型。
    private def restoreUser: User = {
      u.copy(
        followingCount = u.followingCount.map(c => clampCount(c)),
        followerCount = u.followerCount.map(c => clampCount(c)),
        workCount = u.workCount.map(c => clampCount(c))
      )
    }

    private def modelUser: UserModel = UserModel(
      id = u.id,
      account = u.account,
      password = u.password,
      nickname = u.nickname,
      avatarUrl = u.avatarUrl,
      bio = u.bio,
      status = u.status,
      role = u.role,
      createdAt = None,
      updatedAt = None
    )
  }

  // 将领域用户转换为 GORM 模型并写入 account 表。
  def save(u: User): Either[ErrAccountAlreadyExists, User] = {
    val model = UserModel(
      id = 0,
      account = u.account.trim,
      password = u.password.trim,
      nickname = u.nickname.trim,
      avatarUrl = u.registerAvatarUrl,
      bio = u.registerBio,
      status = u.status,
      role = u.role.trim,
      createdAt = Some(LocalDateTime.now),
      updatedAt = Some(LocalDateTime.now)
    )
    Try(db.insertAndReturn(model).id) match
      case Failure(exception) =>
        error(exception.getMessage)
        Left(ErrAccountAlreadyExists())
      case Success(value) => Right(u.copy(id = value))
  }

  // 根据账号查找用户，登录流程会调用它。
  def findByAccount(account: String): Either[ErrUserNotFound, User] = {
    db.fetchTo[User](internal.auth.sql.findByAccountSql(account))
      .headOption
      .map(m => Right(m.restoreUser))
      .getOrElse(Left(ErrUserNotFound()))
  }

  // 根据用户 ID 查找用户，鉴权后的个人资料接口会调用它。
  def findById(id: Long): Either[ErrUserNotFound, User] = try {
    val opt = db.fetchTo[User](internal.auth.sql.findByIdSql(id)).headOption
    debug(s"opt::$opt")
    opt.map(m => Right(m.restoreUser)).getOrElse(Left(ErrUserNotFound()))
  } catch {
    case e: Exception => error(e.getMessage); Left(ErrUserNotFound())
  }

  // 只更新资料字段，账号、密码、角色等字段保持原值。
  def updateProfile(u: User): Either[ErrUserNotFound, User] = {
    debug(s"user::$u")
    val model = u.modelUser.copy(updatedAt = Some(LocalDateTime.now))
    debug(s"model::$model")
    if db.update(model, true) > 0 then Right(u)
    else Left(ErrUserNotFound())
  }

  def lockNormalUser(userId: Long)(using JdbcTransactionContext): List[UserModel] = {
    import sqala.static.dsl.*
    val sql = query(from(UserModel).filter(u => u.id == userId && u.status == Status.Normal.value).forUpdate)
    db.fetch(sql)
  }
}
