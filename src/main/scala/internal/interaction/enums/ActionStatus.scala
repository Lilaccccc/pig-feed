package internal.interaction.enums

enum ActionStatus(val value: Int) {
  case Active extends ActionStatus(1)
  case Canceled extends ActionStatus(2)
  
  case Unknown extends ActionStatus(0)
}

object ActionStatus {
  def value(int: Int): ActionStatus = ActionStatus.values.find(_.value == int).getOrElse(Unknown)
  
  def value(bool: Boolean): ActionStatus = if bool then Active else Canceled
}