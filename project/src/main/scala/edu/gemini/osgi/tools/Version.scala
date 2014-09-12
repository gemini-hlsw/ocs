package edu.gemini.osgi.tools

/** A version per R4 spec section 3.1.4. The qualifier is unused. */
case class Version(major: Int, minor: Int, micro: Int) extends Ordered[Version] {

  implicit class ZeroOrElse(n: Int) {
    def orElse(m: Int) = 
      if (n != 0) n else m
  }

  def compare(other: Version) = 
    (major - other.major) orElse (minor - other.minor) orElse (micro - other.micro)

  override def toString = 
    if (this == Version.MaxValue) "Infinity" else "%d.%d.%d".format(major, minor, micro)

}

object Version {

  def parse(s: String): Version = 
    Option(s).map {
      case "Infinity" => MaxValue
      case _ =>
        val parts = (s.split("\\.").take(3).map(_.toInt) ++ Array(0, 0, 0)).take(3)
        Version(parts(0), parts(1), parts(2))
    } getOrElse MinValue

  val MinValue = Version(0, 0, 0)
  val MaxValue = Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)

}
