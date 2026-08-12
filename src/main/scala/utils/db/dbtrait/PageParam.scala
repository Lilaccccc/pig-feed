package utils.db.dbtrait

trait PageParam {
  def pageNo: Option[Int]
  def pageSize: Option[Int]
  def sorted: Option[Boolean]
  def needCount: Option[Boolean]

  def getPageNo: Int            = pageNo.getOrElse(1)
  def getPageSize: Int          = pageSize.getOrElse(10)
  def getSorted: Boolean        = sorted.getOrElse(true)
  def getNeedCount: Boolean     = needCount.getOrElse(true)
}

object PageParam {
  def apply(
    no: Int = 1,
    size: Int = 10,
    isSorted: Boolean = true,
    isNeedCount: Boolean = true
  ): PageParam = new PageParam {
    override val pageNo: Option[Int]            = Some(no)
    override val pageSize: Option[Int]          = Some(size)
    override val sorted: Option[Boolean]        = Some(isSorted)
    override val needCount: Option[Boolean]     = Some(isNeedCount)
  }
}
