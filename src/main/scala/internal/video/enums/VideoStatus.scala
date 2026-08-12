package internal.video.enums

enum VideoStatus(val value: Int) {
  case Draft     extends VideoStatus(1)
  case Published extends VideoStatus(2)
  case Offline   extends VideoStatus(3)
  case Deleted   extends VideoStatus(4)
  
  case Unknown   extends VideoStatus(0)
}
