package edu.gemini.itc.shared

import java.io.InputStreamReader
import java.util.Scanner
import java.util.regex.Pattern

import scala.collection.parallel.mutable
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Set of tools to ingest dat files stored as resource files.
 * These files describe properties of transmission elements, filters, instruments etc.
 * TODO: This needs some additional TLC, in particular the caching.
 */
object DatFile {

  // ===== Scan utils

  // delimiters are whitespaces, commas or semicolons and comments (everything from "#" up to next \n).
  private val Delimiters = Pattern.compile("(\\s|,|;|(#[^\\n]*))+")

  def scan(f: String): Scanner = {
    Option(getClass.getResourceAsStream(f)).fold {
      throw new IllegalArgumentException(s"Missing data file $f")
    } {
      new Scanner(_).useDelimiter(Delimiters)
    }
  }

  def scanString(s: String): Scanner = new Scanner(s).useDelimiter(Delimiters)

  // ===== Parse utils
  abstract class Parser extends JavaTokenParsers {
    override val whiteSpace                   = """[,\s]+""".r
    def comment    : Parser[Any]              = """#[^\n]*""".r
    def double     : Parser[Double]           = floatingPointNumber ^^ (_.toDouble)
    def pair       : Parser[(Double, Double)] = double ~! double ^^ { case ~(a,b) => (a,b) }
    def twoDoubles : Parser[(Double, Double)] = rep(comment) ~> pair <~ rep(comment)
  }

  class PlainSpectrumParser extends Parser {
    private def spectrum   : Parser[List[(Double, Double)]] = rep(twoDoubles)

    def parseString(s: String) = parseAll(spectrum, s)
    def parseFile(f: String) = {
      val s = new InputStreamReader(getClass.getResourceAsStream(f))
      val r = parseAll(spectrum, s)
      s.close()
      r
    }
    def parseFile(r: InputStreamReader) = parseAll(spectrum, r)
  }

  class WLSpectrumParser extends Parser {
    private def spectrum   : Parser[(Double, List[(Double, Double)])] = rep(comment) ~> double ~! rep(twoDoubles) ^^ { case ~(a,b) => (a,b)}

    def parseString(s: String) = parseAll(spectrum, s)
    def parseFile(f: String) = {
      val s = new InputStreamReader(getClass.getResourceAsStream(f))
      val r = parseAll(spectrum, s)
      s.close()
      r
    }
    def parseFile(r: InputStreamReader) = parseAll(spectrum, r)
  }

  private val wlcache = mutable.ParHashMap[String, (java.lang.Double, Array[Array[Double]])]()
  def loadSpectrumWithWavelength(file: String): (java.lang.Double, Array[Array[Double]]) = {
    // TODO: caching error handling
    if (wlcache.contains(file)) wlcache(file)
    else {
      System.out.println("Loading into cache: " + file)
      val r = new WLSpectrumParser().parseFile(file)
      if (!r.successful) System.out.println("*********ERROR FOR " + file)
      val data = new Array[Array[Double]](2)
      data(0) = r.get._2.map(_._1).toArray
      data(1) = r.get._2.map(_._2).toArray
      val value = (new java.lang.Double(r.get._1), data)
      wlcache.put(file, value)
      value
    }
  }

  private val cache = mutable.ParHashMap[String, Array[Array[Double]]]()
  def loadSpectrum(file: String): Array[Array[Double]] = {
    // TODO: caching error handling
    if (cache.contains(file)) cache(file)
    else {
      System.out.println("Loading into cache: " + file)
      val r = new PlainSpectrumParser().parseFile(file)
      if (!r.successful) System.out.println("*********ERROR FOR " + file)
      val data = new Array[Array[Double]](2)
      data(0) = r.get.map(_._1).toArray
      data(1) = r.get.map(_._2).toArray
      cache.put(file, data)
      data
    }
  }

  def parseSpectrum(spectrum: String): Array[Array[Double]] = {
      System.out.println("Parsing user spectrum")
      val r = new PlainSpectrumParser().parseString(spectrum)
      if (!r.successful) System.out.println("*********ERROR FOR user spectrum")
      val data = new Array[Array[Double]](2)
      data(0) = r.get.map(_._1).toArray
      data(1) = r.get.map(_._2).toArray
      data
  }

}

