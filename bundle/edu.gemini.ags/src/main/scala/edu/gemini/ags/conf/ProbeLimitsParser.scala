package edu.gemini.ags.conf

import java.io.InputStream

import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{SkyBackground, ImageQuality}
import edu.gemini.spModel.guide.GuideSpeed

import java.util.logging.{Level, Logger}

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
object ProbeLimitsParser {
  val Log: Logger =
    Logger.getLogger(getClass.getName)

  val iqMap: Map[String, ImageQuality] =
    ImageQuality.values().map(iq => iq.getPercentage.toString -> iq).toMap +  ("ANY" -> ImageQuality.ANY)

  val sbMap: Map[String, SkyBackground] =
    SkyBackground.values().map(sb => sb.getPercentage.toString -> sb).toMap + ("ANY" -> SkyBackground.ANY)

  val bandMap: Map[String, MagnitudeBand] =
    MagnitudeBand.all.map(b => b.name -> b).toMap

  val idMap: Map[String, MagLimitsId] =
    AllLimitsIds.map(g => g.name -> g).toMap

  // The parser validates most aspects of the CalcMap, but doesn't catch missing
  // tables or duplicate faintness keys.
  def validate(cm: CalcMap): String \/ Unit = {
    def missingIds: ValidationNel[String, Unit] =
      AllLimitsIds.filterNot(cm.contains).map(_.name) match {
        case Nil => ().success
        case ids => ids.mkString("Missing table for: ", ", ", "").failureNel
      }

    def incompleteTables: ValidationNel[String, Unit] = {
      val errors = cm.map { case (MagLimitsId(name), ProbeLimitsCalc(_, _, fm)) =>
        name -> (AllFaintnessKeys &~ fm.keySet)
      }.filterNot(_._2.isEmpty).map { case (name, keys) =>
        s"$name is missing entries: ${keys.mkString(", ")}"
      }

      errors match {
        case Nil    => ().success
        case h :: t => NonEmptyList(h, t: _*).failure
      }
    }

    (missingIds |+| incompleteTables).disjunction.leftMap(_.toList.mkString("\n"))
  }
}

import ProbeLimitsParser._

final class ProbeLimitsParser extends JavaTokenParsers {
  val id: Parser[MagLimitsId]     = """[^,]+""".r ^? (idMap, s => s"Unrecognized guide limit table id '$s'.")
  val band: Parser[MagnitudeBand] = """[a-zA-Z]+""".r ^? (bandMap, s => s"Unrecognized magnitude band '$s'.")
  val mag: Parser[Double]         = decimalNumber ^^ { _.toDouble }

  // When exported by the Google spreadsheet, there are commas in the blank
  // rows and columns.
  val chaff: Parser[List[String]] = rep(",")

  val calcDef: Parser[(MagLimitsId, MagnitudeBand, Double)] =
    (id<~",")~(band<~",")~(mag<~chaff) ^^ {
      case idVal~bandVal~magAdj => (idVal, bandVal, magAdj)
    }

  val iq: Parser[ImageQuality] =
    ("ANY" | wholeNumber) ^? (iqMap, s => s"Unable to parse '$s' as an image quality value.")

  val sb: Parser[SkyBackground] =
    ("ANY" | wholeNumber) ^? (sbMap, s => s"Unable to parse '$s' as a sky background value.")


  val limits: Parser[List[(GuideSpeed, Double)]] =
    repsep(mag, ",") ^? {
      case l@List(_, _, _) => GuideSpeed.values().toList.zip(l)
    }

  val limitsLine: Parser[FaintnessMap] =
    (iq<~",")~(sb<~",")~limits ^^ {
      case iqVal~sbVal~gsList => gsList.map { case (guideSpeed, limit) =>
          FaintnessKey(iqVal, sbVal, guideSpeed) -> limit
      }.toMap
    }

  val limitsMap: Parser[FaintnessMap] =
    repN(16, limitsLine) ^? { case lst@(_ :: _) => lst.reduce(_ ++ _) }

  val calcEntry: Parser[(MagLimitsId, ProbeLimitsCalc)] =
    chaff~>calcDef~limitsMap ^^ { case (id0, band0, adj)~tab =>
        id0 -> ProbeLimitsCalc(band0, adj, tab)
    }

  val calcMap: Parser[CalcMap] =
    rep(calcEntry) ^^ { _.toMap }

  private def read(src: Source): String \/ CalcMap =
    parseAll(calcMap, src.getLines().mkString("\n")) match {
      case Success(m, _)        => m.right
      case NoSuccess(msg, next) => s"Problem parsing guide probe limits on line ${next.pos.line}: $msg".left
    }

  private def message(t: Throwable): String = {
    val msg = "Exception loading guide probe limits" + Option(t.getMessage).fold("")(m => s": $m")
    // gross side-effect but we want to log what happened
    Log.log(Level.SEVERE, msg, t)
    msg
  }

  def read(is: InputStream): String \/ CalcMap =
    for {
      cm <- \/.fromTryCatchNonFatal(read(Source.fromInputStream(is, "UTF8"))).fold(message(_).left, identity)
      _  <- validate(cm)
    } yield cm
}
