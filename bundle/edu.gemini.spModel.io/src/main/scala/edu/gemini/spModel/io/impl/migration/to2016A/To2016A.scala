package edu.gemini.spModel.io.impl.migration.to2016A

import edu.gemini.spModel.io.impl.SpIOTags
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.io.impl.migration.PioSyntax._
import edu.gemini.spModel.pio.{Container, Document, Param, Version}
import edu.gemini.spModel.pio.xml.{PioXmlUtil}

import scala.collection.JavaConverters._

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
    addUnitsToELine, demotePluto
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

  // Turn Pluto into an asteroid by removing its system and object id, replacing them with system
  // and orbital elements from the properly resolved value below. Coordinates and valid-at date
  // are preserved.
  private def demotePluto(d: Document): Unit =
    for {
      ps  <- allTargets(d)
      sys <- Option(ps.getParam("system")).toList if sys.getValue == "Solar system object"
      obj <- Option(ps.getParam("object")).toList if obj.getValue == "PLUTO"
    } {
      ps.removeChild(obj)
      ps.removeChild(sys)
      plutoParams.foreach(ps.addParam)
    }

  // Params taken from a program with a properly resolved Pluto (now an asteroid). This includes
  // only the system and orbital elements. Get a FRESH set of params each time! (important)
  def plutoParams: List[Param] = {
    PioXmlUtil.read(
      <document>
        <container>
          <paramset name="pluto">
            <param name="system" value="MPC minor planet"/>
            <param name="epoch" value="2457217.5" units="JD"/>
            <param name="anode" value="110.2100007229519" units="degrees"/>
            <param name="aq" value="39.74409337717218" units="au"/>
            <param name="e" value="0.2543036816945946"/>
            <param name="inclination" value="17.36609399010031" units="degrees"/>
            <param name="lm" value="0.0" units="degrees"/>
            <param name="n" value="0.0" units="degrees/day"/>
            <param name="perihelion" value="114.2248220688449" units="degrees"/>
            <param name="epochOfPeri" value="2447885.60548777" units="JD"/>
          </paramset>
        </container>
      </document>.toString
    ).asInstanceOf[Document]
      .getContainers.get(0).asInstanceOf[Container]
      .getParamSet("pluto").getParams.asInstanceOf[java.util.List[Param]]
      .asScala.toList
  }


}
