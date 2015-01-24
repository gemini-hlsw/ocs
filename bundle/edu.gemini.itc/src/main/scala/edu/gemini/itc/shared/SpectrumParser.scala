package edu.gemini.itc.shared

import java.io.InputStreamReader

import scala.collection.parallel.mutable
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Tools to parse array spectrums from files.
 * TODO: This needs some additional TLC, in particular the caching.
 */
object SpectrumParser {

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
  def loadFromWLFile(file: String): (java.lang.Double, Array[Array[Double]]) = {
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
  def loadFromFile(file: String): Array[Array[Double]] = {
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

}

