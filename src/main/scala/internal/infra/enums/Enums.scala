package internal.infra.enums

def MaxLimit            = 100
def MaxTitleLength              = 128
def MaxIdempotencyKeyLength     = 128
def ActionStatCounterShardCount = 16
private def DefaultQueryLimit   = 20

extension (limit: Int) {
  def normalizeLimit: Int = {
    if limit <= 0 then return DefaultQueryLimit
    if limit > MaxLimit then return MaxLimit
    limit
  }
}
