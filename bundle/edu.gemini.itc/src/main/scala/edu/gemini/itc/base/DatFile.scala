package edu.gemini.itc.base

import java.util.Scanner
import java.util.logging.{Level, Logger}
import java.util.LinkedHashMap
import java.util.regex.Pattern
import scala.collection._
import scalaz._

/**
 * Set of tools to ingest dat files stored as resource files.
 * These files describe properties of transmission elements, filters, gratings, instruments etc.
 * Parsing of file is implemented using a scanner. Using parser/combinators turned out to be very slow. Even
 * using the pattern matching parsing in the scanner is extremely inefficient for huge dat files where we
 * know all the numbers are doubles. Using scan.next().toDouble is much more efficient than scan.nextDouble().
 * The contract regarding missing files and parsing errors is that this results in unchecked exceptions which
 * bubble all the way up to the servlet. This isn't better or worse than what we had originally.
 */
object DatFile {
  lazy val Log = Logger.getLogger(getClass.getName)

  // ===== Data containers

  type Data = Array[Array[Double]]

  case class Filter(wavelength: Double, data: Data)

  case class Instrument(name: String,
                        start: Int,                 // supported wavelength range start [nm]
                        end: Int,                   // supported wavelength range end [nm]
                        sampling: Double,           // sampling rate in dat files [nm]
                        backgroundFile: String,
                        plateScale: Double,         // [arcsec/pixel]
                        readNoise: Double,          // [electrons/pixel]
                        darkCurrent: Double)        // [electrons/s/pixel]

  case class Grating(name: String, resolvingPower: Int, blaze: Int, dispersion: Double, dispersionArray: Array[Array[Double]], resolution: Double)


  // ===== Parse utils
// EXPERIMENTAL; May or may not be used in a later stage.
//  abstract class Parser extends JavaTokenParsers {
//    override val whiteSpace                   = """(\s|,|;|(#[^\n]*))+""".r
//    def double     : Parser[Double]           = floatingPointNumber ^^ (_.toDouble)
//    def pair       : Parser[(Double, Double)] = double ~ double ^^ { case ~(a,b) => (a,b) }
//  }
//
//  class PlainSpectrumParser extends Parser {
//    private def spectrum   : Parser[List[(Double, Double)]] = rep(pair)
//
//    def parseString(s: String) = parseAll(spectrum, s)
//    def parseFile(f: String): Array[Array[Double]] = {
//      val s = new InputStreamReader(getClass.getResourceAsStream(f))
//      val r = parseAll(spectrum, s)
//      s.close()
//      val data = new Array[Array[Double]](2)
//      data(0) = r.get.map(_._1).toArray
//      data(1) = r.get.map(_._2).toArray
//      data
//    }
//    def parseFile(r: InputStreamReader) = parseAll(spectrum, r)
//  }



  // ===== Scan utils

  // delimiters are whitespaces, commas or semicolons and comments (everything from "#" up to next \n).
  private val Delimiters = Pattern.compile("(\\s|,|;|(#[^\\n]*))+")

  def scan(s: String): Scanner = new Scanner(s).useDelimiter(Delimiters)

  def scanFile(f: String): Scanner = {
    Option(getClass.getResourceAsStream(f)).fold {
      val msg = s"Unsupported configuration, missing data file $f"
      Log.fine(msg)
      throw new IllegalArgumentException(msg)
    } {
      new Scanner(_).useDelimiter(Delimiters)
    }
  }

  // Parsed user spectra are held for a few requests: the same text is needed once for
  // range validation and once for the SED, for every CCD of a request.
  private val UserSpectra = new LinkedHashMap[String, Data](8, 0.75f, true) {
    override def removeEldestEntry(e: java.util.Map.Entry[String, Data]) = size > 4
  }

  /** Parses a user supplied spectrum. The result is shared and must not be modified. */
  def fromUserSpectrum(s: String): Data = UserSpectra.synchronized {
    val cached = UserSpectra.get(s)
    if (cached != null) cached
    else {
      val data = parseArray(s)
      UserSpectra.put(s, data)
      data
    }
  }

  // ===== Cached data file loaders

  val arrays = cache { s =>
    parseArray(s.useDelimiter("\\A").next())
  }

  val filters = cache { s =>
    Filter(s.nextDouble(), scanArray(s))
  }

  val gratings = cache { s =>
    val l = mutable.MutableList[Grating]()
    while (s.hasNext) {
      val name           = s.next()
      val blaze          = s.nextInt()
      val resolvingPower = s.nextInt()
      val resolution     = s.nextDouble()
      val tmp     = s.next()
      var dispersion: Double = 0
      try {
        dispersion = tmp.toDouble
        l +=(Grating(name, resolvingPower, blaze, dispersion, null, resolution))
      } catch {
        case e : NumberFormatException => {
          val dDis = arrays.apply(tmp)
          l +=(Grating(name, resolvingPower, blaze, -999999, dDis, resolution))
        }
        case e : Exception => {
          Log.log(Level.SEVERE, "The grating configuration is not correct. ", e)
          throw e
        }
      }

    }
    l.map(l => l.name -> l).toMap
  }

  val instruments = cache { s =>
    Instrument(s.next, s.nextInt, s.nextInt, s.nextDouble, s.next, s.nextDouble, s.nextDouble, s.nextDouble)
  }

  private def scanArray(s: Scanner): Array[Array[Double]] = {
    val l = mutable.MutableList[(Double, Double)]()
    while (s.hasNext) {
      val pair = (s.next().toDouble, s.next().toDouble)
      l.+=(pair)
    }
    val data = new Array[Array[Double]](2)
    data(0) = l.map(_._1).toArray
    data(1) = l.map(_._2).toArray
    data
  }

  private def isDelimiter(c: Char): Boolean =
    c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000B' || c == ',' || c == ';'

  /**
   * Parses pairs of doubles separated by whitespace, commas or semicolons, with "#" comments running to
   * the end of the line. Same tokens and same Double.parseDouble as the scanner version, without the regex.
   */
  private def parseArray(text: String): Data = {
    val n = text.length
    var xs = new Array[Double](1024)
    var ys = new Array[Double](1024)
    var count = 0
    var i = 0
    var pending = Double.NaN
    var havePending = false
    while (i < n) {
      val c = text.charAt(i)
      if (c == '#') {
        while (i < n && text.charAt(i) != '\n') i += 1
      } else if (isDelimiter(c)) {
        i += 1
      } else {
        val start = i
        while (i < n && { val d = text.charAt(i); d != '#' && !isDelimiter(d) }) i += 1
        val v = java.lang.Double.parseDouble(text.substring(start, i))
        if (havePending) {
          if (count == xs.length) {
            xs = java.util.Arrays.copyOf(xs, count * 2)
            ys = java.util.Arrays.copyOf(ys, count * 2)
          }
          xs(count) = pending
          ys(count) = v
          count += 1
          havePending = false
        } else {
          pending = v
          havePending = true
        }
      }
    }
    if (havePending) throw new IllegalArgumentException("odd number of values, expected wavelength and flux pairs")
    val data = new Array[Array[Double]](2)
    data(0) = java.util.Arrays.copyOf(xs, count)
    data(1) = java.util.Arrays.copyOf(ys, count)
    data
  }

  /** Loads a file and parses it using the given scanner unless it is already available in the cache. */
  // Note: this could be changed to use a weak hash map in case we run into memory issues
  private def cache[T](load: Scanner => T): String => T = Memo.immutableHashMapMemo[String, T] { f =>
    Log.fine(s"Caching file $f")
    load(scanFile(f))
  }

}

