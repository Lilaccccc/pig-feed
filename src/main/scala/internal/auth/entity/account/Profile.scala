package internal.auth.entity.account

import io.circe.Codec
import sttp.tapir.Schema

// 应用层对外暴露的用户资料视图，屏蔽密码等敏感字段。
final case class Profile(
  id: Long,
  account: String,
  nickname: String,
  avatarUrl: String,
  bio: String,
  status: Int,
  role: String,
  followingCount: Int,
  followerCount: Int,
  workCount: Int
) derives Codec, Schema

object Profile {
  extension (u: User) {
    def toProfile: Profile = {
      Profile(
        id = u.id,
        account = u.account,
        nickname = u.nickname,
        avatarUrl = u.avatarUrl.getOrElse(""),
        bio = u.bio.getOrElse(""),
        status = u.status,
        role = u.role,
        followingCount = u.followingCount.getOrElse(0),
        followerCount = u.followerCount.getOrElse(0),
        workCount = u.workCount.getOrElse(0)
      )
    }
  }
}
