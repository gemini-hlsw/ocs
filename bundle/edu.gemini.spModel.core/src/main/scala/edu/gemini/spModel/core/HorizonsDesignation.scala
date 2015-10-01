package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** 
 * Unique Horizons designation, which should allow for reproduceable ephemeris queries <b>if</b> the
 * values passed to the constructors are extracted correctly from search results. See `horizons.md`
 * in the bundle source for more information.
 */
sealed abstract class HorizonsDesignation(val queryString: String) 
  extends Product with Serializable

object HorizonsDesignation {

  /** 
   * Designation for a comet, in the current apparition. Example: `C/1973 E1` for Kohoutek, yielding
   * the query string `DES=C/1973 E1;CAP`.
   */
  final case class Comet(des: String) extends HorizonsDesignation(s"DES=$des;CAP")

  /**
   * Designation for an asteroid under modern naming conventions. Example: `1971 UC1` for
   * 1896 Beer, yielding a query string `DES=1971 UC1`.
   */
  final case class Asteroid(des: String) extends HorizonsDesignation(s"DES=$des")

  /**
   * Designation for an asteroid under "old" naming conventions. These are small numbers. Example:
   * `4` for Vesta, yielding a query string `4;`
   */
  final case class AsteroidOldStyle(num: Int) extends HorizonsDesignation(s"$num;")

  /**
   * Designation for a major body (planet or satellite thereof). These have small numbers. Example:
   * `606` for Titan, yielding a query string `606`.
   */
  final case class MajorBody(num: Int) extends HorizonsDesignation(s"$num")

}
