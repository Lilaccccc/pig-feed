package internal.exposure.entity

final case class RecordViewEventResult(
  event: ViewEvent,
  exposure: Option[Exposure],
  published: Boolean
)
