package edu.gemini.spModel.core

import java.text.DecimalFormat

import scalaz._, Scalaz._

/** An angle, convertible to various representations. */
sealed trait Angle extends java.io.Serializable {

  /**
   * This `Angle` in decimal degrees [0, 360)
   * @group Conversions
   */
  def toDegrees: Double

  /**
   * This `Angle` in signed degrees [-180 , 180]
   * @group Conversions
   */
  def toSignedDegrees: Double =
    Angle.signedDegrees(toDegrees)

  /**
   * This `Angle` in radians [0, 2π)
   * @group Conversions
   */
  def toRadians: Double =
    toDegrees.toRadians

  /**
   * This `Angle` in arcmins
   * @group Conversions
   */
  def toArcmins: Double =
    toDegrees * 60

  /**
   * This `Angle` in arcsecs
   * @group Conversions
   */
  def toArcsecs: Double =
    toDegrees * 3600

  /**
   * This `Angle` as an `HourAngle`.
   * @group Conversions
   */
  def toHourAngle: Angle.HourAngle = {
    val d = ((toDegrees % 360) + 360) % 360
    val h = d / 15
    val m = (h - h.intValue) * 60
    val s = (m - m.intValue) * 60
    new Angle.HourAngle {
      val hours = h.intValue
      val minutes = m.intValue
      val seconds = s
    }
  }

  /**
   * Alias for `toHourAngle`
   * @group Conversions
   */
  def toHMS: Angle.HMS =
    toHourAngle

  /**
   * This `Angle` in `Sexigesimal`.
   * @group Conversions
   */
  def toSexigesimal: Angle.Sexigesimal = {
    val m = (toDegrees - toDegrees.intValue) * 60
    val s = (m - m.intValue) * 60
    new Angle.Sexigesimal {
      val degrees = toDegrees.intValue
      val minutes = m.intValue
      val seconds = s
    }
  }

  /**
   * Alias for `toSexigesimal`
   * @group Conversions
   */
  def toDMS: Angle.DMS =
    toSexigesimal

  /**
   * Modular addition.
   * @group Operations
   */
  def +(a: Angle): Angle =
    Angle.fromDegrees(toDegrees + a.toDegrees)

  /**
   * Modular subtraction.
   * @group Operations
   */
  def -(a: Angle): Angle =
    Angle.fromDegrees(toDegrees - a.toDegrees)

  /**
   * Scalar multiplication.
   * @group Operations
   */
  def *(factor: Double): Angle =
    Angle.fromDegrees(toDegrees * factor)

  /**
   * Scalar division
   * @group Operations
   */
  def /(factor: Double): Option[Angle] =
    (factor != 0) option Angle.fromDegrees(toDegrees / factor)

  /**
   * Flip by 180°.
   * @group Operations
   */
  def flip: Angle =
    this + Angle.fromDegrees(180)

  /** @group Overrides */
  final override def toString =
    s"Angle($toDegrees°)"

  /** @group Overrides */
  final override def equals(a: Any) =
    a match {
      case a: Angle => a.toDegrees == this.toDegrees
      case _        => false
    }

  /** @group Overrides */
  final override def hashCode =
    toDegrees.hashCode

  /**
   * @see [[Angle.formatDegrees]]
   * @group Formatters
   */
  def formatDegrees: String =
    Angle.formatDegrees(this)

  /**
   * @see [[Angle.formatSexigesimal]]
   * @group Formatters
   */
  def formatSexigesimal: String =
    Angle.formatSexigesimal(this)

  /**
   * @see [[Angle.formatDMS]]
   * @group Formatters
   */
  def formatDMS: String =
    Angle.formatDMS(this)

 /**
   * @see [[Angle.formatHourAngle]]
   * @group Formatters
   */
  def formatHourAngle: String =
    Angle.formatHourAngle(this)

  /**
   * @see [[Angle.formatHMS]]
   * @group Formatters
   */
  def formatHMS: String =
    Angle.formatHMS(this)

}

object Angle {

  /**
   * Construct an `Angle` from the given value in degrees, which will be normalized to [0, 360).
   * @group Constructors
   */
  def fromDegrees(d: Double): Angle =
    new Angle {
      override val toDegrees = ((d % 360) + 360) % 360
    }

  /**
   * Construct an `Angle` from the given value in arcseconds
   * @group Constructors
   */
  def fromArcsecs(s: Double): Angle = fromDegrees(s / 3600)

  /**
   * Construct an `Angle` from the given value in arcminutes
   * @group Constructors
   */
  def fromArcmin(m: Double): Angle = fromDegrees(m / 60)

  /**
   * Construct an `Angle` from the given value in radians, which will be normalized to [0, 2π).
   * @group Constructors
   */
  def fromRadians(r: Double): Angle =
    fromDegrees(r.toDegrees)

  /**
   * Construct an `Angle` from the given value in hours, which will be normalized to [0, 24).
   * @group Constructors
   */
  def fromHours(h: Double): Angle =
    fromDegrees(h * 15.0)

  /**
   * Construct an `Angle` from the given hour angle components if possible; `minutes` and `seconds`
   * must be in [0, 60).
   * @group Constructors
   */
  def fromHourAngle(hours: Int, minutes: Int, seconds: Double): Option[Angle] =
    if (minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) None
    else if (hours < 0) fromHourAngle(hours + 24, minutes, seconds)
    else Some(fromDegrees((hours % 24) * 15.0 + minutes / 4.0 + seconds / (4.0 * 60)))

  /**
   * Alias for `fromHourAngle`.
   * @group Constructors
   */
  def fromHMS(hours: Int, minutes: Int, seconds: Double): Option[Angle] =
    fromHourAngle(hours, minutes, seconds)

  /**
   * Construct angle `Angle` from the given sexigesimal components if possible; `minutes` and
   * `seconds` must be in [0, 60).
   * @group Constructors
   */
  def fromSexigesimal(degrees: Int, minutes: Int, seconds: Double): Option[Angle] =
    if (degrees < 0 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) None
    else Some(fromDegrees(degrees + minutes / 60.0 + seconds / (60.0 * 60.0)))

  /**
   * Alias for `fromSexigesimal`.
   * @group Constructors
   */
  def fromDMS(degrees: Int, minutes: Int, seconds: Double): Option[Angle] =
    fromSexigesimal(degrees, minutes, seconds)

  /**
   * The `Angle` of zero degrees/radians.
   * @group Constructors
   */
  lazy val zero = fromDegrees(0.0)

  /**
   * Additive monoid for `Angle`. Note that this is not a strictly lawful monoid as no floating
   * point operations are associative at extreme precision, however for our purposes this will
   * never matter.
   * @group Typeclass Instances
   */
  implicit val AngleMonoid: Monoid[Angle] =
    new Monoid[Angle] {
      val zero = Angle.zero
      def append(a: Angle, b: => Angle): Angle = a + b
    }

  /** @group Typeclass Instances */
  implicit val AngleOrder: Order[Angle] =
    Order.orderBy(_.toDegrees)

  /** @group Typeclass Instances */
  implicit val AngleOrdering: scala.Ordering[Angle] =
    scala.Ordering.by(_.toDegrees)

  /**
   * Parse an angle from the given string in decimal degrees.
   * @group Parsers
   */
  def parseDegrees(s: String): NumberFormatException \/ Angle =
    try fromDegrees(s.toDouble).right
    catch {
      case nfe: NumberFormatException => nfe.left
    }

  /**
   * Parse an angle from the given string in radians.
   * @group Parsers
   */
  def parseRadians(s: String): NumberFormatException \/ Angle =
    try fromRadians(s.toDouble).right
    catch {
      case nfe: NumberFormatException => nfe.left
    }

  /**
   * Parse an angle from the given string in sexigesimal format. Accepted format is `d:m:s` where
   * each segment can be one or two digits, `d` can have a leading sign (`+` or `-`), and `s` can
   * have a decimal point followed by additional digits.
   * @group Parsers
   */
  def parseSexigesimal(s: String): NumberFormatException \/ Angle =
    s.trim.some.flatMap {
      case SexigesimalPat(a, b, c, d) =>
        for {
          s <- parseSign(a)
          a <- fromSexigesimal(b.toInt, c.toInt, d.toDouble)
        } yield if (s == -1) zero - a else a
      case _ => None
    } \/> new NumberFormatException(s)

  /**
   * Alias for `parseSexigesimal`.
   * @group Parsers
   */
  def parseDMS(s: String): NumberFormatException \/ Angle =
    parseSexigesimal(s)

  /**
   * Parse an angle from the given string in hour angle format. Accepted format is `h:m:s` where
   * each segment can be one or two digits, `d` can have a leading sign (`+` or `-`), and `s` can
   * have a decimal point followed by additional digits.
   * @group Parsers
   */
  def parseHourAngle(s: String): NumberFormatException \/ Angle =
    s.trim.some.flatMap {
      case SexigesimalPat(a, b, c, d) =>
        for {
          s <- parseSign(a)
          a <- fromHourAngle(b.toInt, c.toInt, d.toDouble)
        } yield if (s == -1) zero - a else a
      case _ => None
    } \/> new NumberFormatException(s)

  /**
   * Alias for `parseHourAngle`.
   * @group Parsers
   */
  def parseHMS(s: String): NumberFormatException \/ Angle =
    parseHourAngle(s)

  // Regex for Sexigesimal
  private val SexigesimalPat = """([-+])?(\d\d?\d?):(\d\d?):(\d\d?\.?\d*)""".r

  // Parse a sign
  private def parseSign(s: String): Option[Int] =
    Option(s).map(_.trim).orElse(Some("")) collect {
      case "+" | "" =>  1
      case "-"      => -1
    }

  /**
   * An hour angle; HMS or Hours, Minutes, Seconds.
   * @group Type Members
   */
  sealed trait HourAngle {
    def hours: Int
    def minutes: Int
    def seconds: Double
    override def toString = s"HMS(${format3(hours, minutes, seconds, 24, ":", 3)})"
  }

  /**
   * An angle in sexigesimal (base-60) representation; DMS or Degrees, Minutes, Seconds.
   * @group Type Members
   */
  sealed trait Sexigesimal {
    def degrees: Int
    def minutes: Int
    def seconds: Double
    override def toString = s"DMS(${format3(degrees, minutes, seconds, 360, ":", 2)})"
  }

  /** @group Type Members */
  type HMS = Angle.HourAngle

  /** @group Type Members */
  type DMS = Angle.Sexigesimal

  /**
   * Format the given `Angle` in decimal degrees with three fractional digits,
   * followed by the degree sign.
   * @group Formatters
   */
  def formatDegrees(a: Angle): String =
    f"${a.toDegrees}%4.03f°"

  /**
   * Format the given `Angle` in sexigesimal `d:mm:ss` with three fractional digits for
   * seconds.
   * @group Formatters
   */
  def formatSexigesimal(a: Angle, sep: String = ":", fractionalDigits: Int = 2): String = {
    val dms = a.toSexigesimal
    format3(dms.degrees, dms.minutes, dms.seconds, 360, sep, fractionalDigits)
  }

  /**
   * Alias for [[Angle.formatSexigesimal]].
   * @group Formatters
   */
  def formatDMS(a: Angle, sep: String = ":", fractionalDigits: Int = 2): String =
    formatSexigesimal(a, sep, fractionalDigits)

  /**
   * Format the given `Angle` in hour angle format `h:mm:ss` with three fractional digits for
   * seconds.
   * @group Formatters
   */
  def formatHourAngle(a: Angle, sep: String = ":", fractionalDigits: Int = 3): String = {
    val hms = a.toHourAngle
    format3(hms.hours, hms.minutes, hms.seconds, 24, sep, fractionalDigits)
  }

  /**
   * Alias for [[Angle.formatHourAngle]].
   * @group Formatters
   */
  def formatHMS(a: Angle, sep: String = ":", fractionalDigits: Int = 3): String =
    formatHourAngle(a, sep, fractionalDigits)

  // Abstract over HMS/DMS
  private[core] def format3(a: Int, b: Int, c: Double, max: Int, sep: String, fractionalDigits: Int): String = {
    val df =
      if (fractionalDigits > 0) new DecimalFormat(s"00.${"0" * fractionalDigits}")
      else new DecimalFormat("00")

    val s0 = df.format(c)
    val (s, carryC) = s0.startsWith("60") ? ((df.format(0), 1)) | ((s0, 0))

    val m0 = b + carryC
    val (m, carryB) = (m0 == 60) ? (("00", 1)) | ((f"$m0%02d", 0))

    val x = a + carryB

    if (x == max) s"00${sep}00$sep${df.format(0)}"
    else f"$x%02d$sep$m$sep$s"
  }

  def signedDegrees(d: Double): Double =
    ((d % 360) + 360) % 360 match {
      case nd if nd < 180.0 => nd
      case nd               => nd - 360.0
    }
}
