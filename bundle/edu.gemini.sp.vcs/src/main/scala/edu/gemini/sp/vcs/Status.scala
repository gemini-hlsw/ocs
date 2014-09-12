package edu.gemini.sp.vcs

/**
 * Science program node status.
 */
sealed trait Status {
  def abbr: String
}

object Status {
  case object Added     extends Status { val abbr = "A" }
  case object Deleted   extends Status { val abbr = "D" }
  case object Modified  extends Status { val abbr = "M" }
  case object Unchanged extends Status { val abbr = " " }

  val all = List(Added, Deleted, Modified, Unchanged )
}

//case class VerboseStatus(status: Status, upToDate: Boolean) {
//  override def toString = "%s%s".format(status.abbr, if (upToDate) " " else "*")
//}