package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** 
 * Unique Horizons designation, which must be formatted differently depending on the object type.
 * The `queryString` should yield a unique result.
 */
sealed abstract class HorizonsDesignation(val designation: String, val queryString: String) 
  extends Product with Serializable

object HorizonsDesignation {

  final case class Comet(des: String)     extends HorizonsDesignation(des, s"DES=$des;CAP")
  final case class Asteroid(des: String)  extends HorizonsDesignation(des, s"DES=$des")
  final case class MajorBody(des: String) extends HorizonsDesignation(des, des)

  val designation: HorizonsDesignation @> String =
    Lens.lensu({
      case (Comet(_), s)     => Comet(s)
      case (Asteroid(_), s)  => Asteroid(s)
      case (MajorBody(_), s) => MajorBody(s)
    }, _.designation)

}
