package edu.gemini.ags.conf


import java.io.InputStream

import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{SkyBackground, ImageQuality}
import edu.gemini.spModel.guide.GuideSpeed

import scala.io.Source
import scala.util.parsing.combinator.JavaTokenParsers
import scalaz._
import Scalaz._

/**
 * Identifier, Band, Saturation Adjustment
 * IQ, SB, FAST, MEDIUM, SLOW
 * ...
 *
 * Altair NGS, R, 13
 * 20, 20, 15.80, 17.30, 18.30
 * 20, 50, 15.70, 17.20, 18.20
 * 20, 80, 15.60, 17.10, 18.10
 * 20, ANY, 15.50, 17.00, 18.00
 * ...
 */
object ProbeLimitsParser extends JavaTokenParsers {
  val iqMap   = ImageQuality.values().map(iq => iq.getPercentage.toString -> iq).toMap +  ("ANY" -> ImageQuality.ANY)
  val sbMap   = SkyBackground.values().map(sb => sb.getPercentage.toString -> sb).toMap + ("ANY" -> SkyBackground.ANY)
  val bandMap = Band.values().map(b => b.name() -> b).toMap
  val idMap   = AllLimitsIds.map(g => g.name -> g).toMap

  def id: Parser[MagLimitsId] = """[^,]+""".r ^? (idMap, s => s"Unrecognized guide limit table id '$s'.")
  def band: Parser[Band]      = """[A-Z]+""".r ^? (bandMap, s => s"Unrecognized magnitude band '$s'.")
  def mag: Parser[Double]     = decimalNumber ^^ { _.toDouble }

  // When exported by the Google spreadsheet, there are commas in the blank
  // rows and columns.
  def chaff: Parser[List[String]] = rep(",")

  def calcDef: Parser[(MagLimitsId, Band, Double)] =
    (id<~",")~(band<~",")~(mag<~chaff) ^^ {
      case idVal~bandVal~magAdj => (idVal, bandVal, magAdj)
    }

  def iq: Parser[ImageQuality] =
    ("ANY" | wholeNumber) ^? (iqMap, s => s"Unable to parse '$s' as an image quality value.")

  def sb: Parser[SkyBackground] =
    ("ANY" | wholeNumber) ^? (sbMap, s => s"Unable to parse '$s' as a sky background value.")


  def limits: Parser[List[(GuideSpeed, Double)]] =
    repsep(mag, ",") ^? {
      case l@List(f, m, s) => GuideSpeed.values().toList.zip(l)
    }

  def limitsLine: Parser[FaintnessMap] =
    (iq<~",")~(sb<~",")~limits ^^ {
      case iqVal~sbVal~gsList => gsList.map { case (guideSpeed, limit) =>
          FaintnessKey(iqVal, sbVal, guideSpeed) -> limit
      }.toMap
    }

  def limitsMap: Parser[FaintnessMap] =
    repN(16, limitsLine) ^? { case lst@(_ :: _) => lst.reduce(_ ++ _) }

  def calcEntry: Parser[(MagLimitsId, ProbeLimitsCalc)] =
    chaff~>calcDef~limitsMap ^^ { case (id, band, adj)~tab =>
        id -> ProbeLimitsCalc(band, adj, tab)
    }

  def calcMap: Parser[CalcMap] =
    rep(calcEntry) ^^ { _.toMap }

  private def read(src: Source): String \/ CalcMap =
    parseAll(calcMap, src.getLines().mkString("\n")) match {
      case Success(m, _)        => m.right
      case NoSuccess(msg, next) => s"Problem parsing guide probe limits on line ${next.pos.line}: $msg".left
    }

  def read(is: InputStream): String \/ CalcMap =
    for {
      cm <- \/.fromTryCatch(read(Source.fromInputStream(is, "UTF8"))).fold(t => s"Exception loading guide probe limits: ${t.getMessage}".left, identity)
      _  <- validate(cm)
    } yield cm

}
