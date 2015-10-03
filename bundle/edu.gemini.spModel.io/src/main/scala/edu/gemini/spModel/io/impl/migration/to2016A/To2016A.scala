package edu.gemini.spModel.io.impl.migration.to2016A

import edu.gemini.spModel.io.impl.SpIOTags
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.io.impl.migration.PioSyntax._
import edu.gemini.spModel.pio.{Container, Document, ParamSet, Version}
import squants.motion.KilometersPerSecond
import squants.radio.{WattsPerSquareMeter, WattsPerSquareMeterPerMicron}

/**
 * Migrate pre-2016A programs.
 */
object To2016A extends Migration {

  val Version_2016A = Version.`match`("2016A-1")

  val PARAMSET_EMISSION_LINE = "EmissionLine"

  def isPre2016A(c: Container): Boolean =
    c.getVersion.compareTo(Version_2016A) < 0

  // Entry point here
  def updateProgram(d: Document): Unit =
    d.containers.find(_.getKind == SpIOTags.PROGRAM).filter(isPre2016A).foreach { _ =>
      conversions.foreach(_.apply(d))
    }

  // These will be applied in the given order
  private val conversions: List[Document => Unit] = List(
    addUnitsToELine
  )

  // Starting 2016A we store squants quantities with their units, older programs need to have units added
  private def addUnitsToELine(d: Document): Unit = {
    for {
      ps  <- allTargets(d)
    } Option(ps.getParamSet(PARAMSET_EMISSION_LINE)).foreach(p => {
      p.getParam("width").setUnits(KilometersPerSecond.symbol)
      p.getParam("flux").setUnits(WattsPerSquareMeter.symbol)
      p.getParam("continuum").setUnits(WattsPerSquareMeterPerMicron.symbol)
    })
  }

}
