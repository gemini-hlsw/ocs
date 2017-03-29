package edu.gemini.spModel.core


import scalaz._, Scalaz._

/**
 * Unique Horizons designation, which should allow for reproducible ephemeris queries <b>if</b> the
 * values passed to the constructors are extracted correctly from search results. See `horizons.md`
 * in the bundle source for more information.
 */
sealed abstract class HorizonsDesignation(val queryString: String)
  extends Product with Serializable {
    def des: String // designation, human readable

    import edu.gemini.spModel.core.HorizonsDesignation._

    /** Exports an HorizonsDesignation to a String in a format that can be read
      * by the `HorizonsDesignation.read` method.
      */
    def show: String =
      this match {
        case Comet(des)            => s"Comet_$des"
        case AsteroidNewStyle(des) => s"AsteroidNew_$des"
        case AsteroidOldStyle(num) => s"AsteroidOld_$num"
        case MajorBody(num)        => s"MajorBody_$num"
      }
  }

object HorizonsDesignation {

  /**
   * Designation for a comet, in the current apparition. Example: `C/1973 E1` for Kohoutek, yielding
   * the query string `NAME=C/1973 E1;CAP`.
   */
  final case class Comet(des: String) extends HorizonsDesignation(s"NAME=$des;CAP")
  object Comet {
    val des: Comet @> String = Lens.lensu((a, b) => a.copy(des = b), _.des)
  }

  sealed abstract class Asteroid(s: String) extends HorizonsDesignation(s)

  /**
   * Designation for an asteroid under modern naming conventions. Example: `1971 UC1` for
   * 1896 Beer, yielding a query string `ASTNAM=1971 UC1`.
   */
  final case class AsteroidNewStyle(des: String) extends Asteroid(s"ASTNAM=$des")
  object AsteroidNewStyle {
    val des: AsteroidNewStyle @> String = Lens.lensu((a, b) => a.copy(des = b), _.des)
  }

  /**
   * Designation for an asteroid under "old" naming conventions. These are small numbers. Example:
   * `4` for Vesta, yielding a query string `4;`
   */
  final case class AsteroidOldStyle(num: Int) extends Asteroid(s"$num;") {
    def des = num.toString
  }
  object AsteroidOldStyle {
    val num: AsteroidOldStyle @> Int = Lens.lensu((a, b) => a.copy(num = b), _.num)
  }

  /**
   * Designation for a major body (planet or satellite thereof). These have small numbers. Example:
   * `606` for Titan, yielding a query string `606`.
   */
  final case class MajorBody(num: Int) extends HorizonsDesignation(s"$num") {
    def des = num.toString
  }
  object MajorBody {
    val num: MajorBody @> Int = Lens.lensu((a, b) => a.copy(num = b), _.num)
  }

  private val CometRegex       = """Comet_(.+)""".r
  private val AsteroidRegex    = """AsteroidNew_(.+)""".r
  private val AsteroidNumRegex = """AsteroidOld_(-?\d+)""".r
  private val MajorRegex       = """MajorBody_(-?\d+)""".r

  /** Extracts an HorizonsDesignation from a String written by the `show`
    * method, if possible.
    */
  def read(s: String): Option[HorizonsDesignation] = s match {
    case CometRegex(des)     => some(Comet(des))
    case AsteroidRegex(des)  => some(AsteroidNewStyle(des))
    case AsteroidNumRegex(i) => some(AsteroidOldStyle(i.toInt))
    case MajorRegex(i)       => some(MajorBody(i.toInt))
    case _                   => none
  }
}
