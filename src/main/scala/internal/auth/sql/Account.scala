package internal.auth.sql

import utils.db.sql

private def findUserBase = {
  """SELECT
    |    a.id, 
    |    a.account, 
    |    a.password, 
    |    a.nickname, 
    |    a.avatar_url, 
    |    a.bio, 
    |    a.status, 
    |    a.role, 
    |    COALESCE(active_following.following_count, rs.following_count, 0) AS following_count, 
    |    COALESCE(active_followers.follower_count, rs.follower_count, 0) AS follower_count, 
    |    COALESCE(published_works.work_count, 0) AS work_count
    |FROM 
    |    account AS a
    |LEFT JOIN 
    |    user_relation_stat AS rs ON rs.user_id = a.id
    |LEFT JOIN (
    |    SELECT 
    |        user_id, 
    |        COUNT(*) AS following_count 
    |    FROM 
    |        user_follow 
    |    WHERE 
    |        status = 1 
    |    GROUP BY 
    |        user_id
    |) AS active_following ON active_following.user_id = a.id
    |LEFT JOIN (
    |    SELECT 
    |        target_user_id, 
    |        COUNT(*) AS follower_count 
    |    FROM 
    |        user_follow 
    |    WHERE 
    |        status = 1 
    |    GROUP BY 
    |        target_user_id
    |) AS active_followers ON active_followers.target_user_id = a.id
    |LEFT JOIN (
    |    SELECT 
    |        author_id, 
    |        COUNT(*) AS work_count 
    |    FROM 
    |        video 
    |    WHERE 
    |        status = 2 
    |    GROUP BY 
    |        author_id
    |) AS published_works ON published_works.author_id = a.id
    |""".stripMargin
}

def findByAccountSql(account: String) = s"$findUserBase WHERE a.account = '$account' LIMIT 1;".sql

def findByIdSql(id: Long) = s"$findUserBase WHERE a.id = $id LIMIT 1;".sql
