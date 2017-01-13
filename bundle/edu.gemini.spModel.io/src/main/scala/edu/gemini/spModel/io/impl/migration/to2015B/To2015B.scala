package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.spModel.io.PioSyntax
import edu.gemini.spModel.io.impl.migration.Migration

import java.util.UUID

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.io.impl.SpIOTags
import edu.gemini.spModel.obscomp.SPNote

import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio._
import edu.gemini.spModel.target.MagnitudePio

import scala.annotation.tailrec
import scalaz._, Scalaz._

/** Convert to new target model. This is side-effecty, sorry. */
object To2015B extends Migration {
  import PioSyntax._
  import BrightnessParser._

  val version = Version.`match`("2015B-1")

  // These constants are take from mainline code, where they are private to implementations and
  // ultimately will go away (but we will need them here for a while longer).

  val PARAM_SYSTEM     = "system"
  val PARAM_RA         = "c1"
  val PARAM_DEC        = "c2"
  val PARAM_DELTA_RA   = "pm1"
  val PARAM_DELTA_DEC  = "pm2"
  val PARAM_EPOCH      = "epoch"
  val PARAM_EPOCH_PERI = "epochOfPeri"
  val PARAM_NOTE_TEXT  = "NoteText"
  val PARAM_NAME       = "name"
  val PARAM_BRIGHTNESS = "brightness"
  val PARAM_OBJECT     = "object"
  val PARAM_TITLE      = "title"

  val CONTAINER_TEMPLATE_GROUP = "templateGroup"
  val CONTAINER_OBSERVATION    = "observation"

  val PARAMSET_NOTE                = "Note"
  val PARAMSET_MAGNITUDES          = "magnitudeList"
  val PARAMSET_TEMPLATE_GROUP      = "Template Group"

  val VALUE_NAME_UNTITLED = "(untitled)"

  val VALUE_SYSTEM_B1950    = "B1950"
  val VALUE_SYSTEM_J2000    = "J2000"
  val VALUE_SYSTEM_JNNNN    = "JNNNN"
  val VALUE_SYSTEM_BNNNN    = "BNNNN"
  val VALUE_SYSTEM_APPARENT = "Apparent"

  val UNITS_JD                        = "JD"
  val UNITS_SECONDS_PER_YEAR          = "seconds/year"
  val UNITS_ARCSECONDS_PER_YEAR       = "arcsecs/year"
  val UNITS_MILLI_ARCSECONDS_PER_YEAR = "milli-arcsecs/year"
  val UNITS_YEARS                     = "years"

  val SYS_ASA_MAJOR_PLANET = "AsA major planet"
  val SYS_ASA_MINOR_PLANET = "AsA minor planet"
  val SYS_ASA_COMET        = "AsA comet"
  val SYS_JPL_MAJOR_PLANET = "JPL major planet"
  val SYS_JPL_MINOR_BODY   = "JPL minor body"
  val SYS_MPC_MINOR_PLANET = "MPC minor planet"
  val SYS_MPC_COMET        = "MPC comet"

  val SYS_SOLAR_OBJECT     = "Solar system object"

  private val MagnitudeNoteTitle = "2015B Magnitude Updates"
  private val MagnitudeNoteText  = (name: String) =>
    s"""|The unstructured 'brightness' property for targets was deprecated in 2010B and removed
        |in 2015B. This note records the disposition of old brightness values associated with
        |the targets in $name.
        |
        |A "parsed" message means that the brightness value was converted into one or more
        |structured magnitude values with known pass bands. "failed" means that this process
        |failed, so you may wish to update the magnitude table manually. "ignored" means that
        |a structured magnitude table was already present and parsing was skipped.
        |
        |""".stripMargin

  private val PrecessionNoteTitle = "2015B Target Precession"
  private val PrecessionNoteText  = (name: String) =>
    s"""At the start of the 2015B semester all B1950 coordinates were precessed to J2000.  The
       |original B1950 coordinates and proper motions of the targets in $name are:
       |
     """.stripMargin

  private val PioFactory = new PioXmlFactory()

  // These will be applied in the given order
  val conversions: List[Document => Unit] = List(
    brightnessToMagnitude,
    uselessSystemsToJ2000,
    b1950ToJ2000,
    convertComet,
    convertMinorPlanet,
    convertSolar
  )

  // Convenience op for \/
  implicit class MoreDisjunctionOps[A, B](ab: A \/ B) {
    def unsafeExtract(implicit ev: A <:< Throwable): B =
      ab.fold(throw _, identity)
  }

  // Convert B1950 coordinates to J2000
  private def b1950ToJ2000(d: Document): Unit =
    for {
      ps    <- allTargets(d)
      pSys  <- Option(ps.getParam(PARAM_SYSTEM)).toList if pSys.getValue == VALUE_SYSTEM_B1950
    } {

      // Params
      val pRa    = ps.getParam(PARAM_RA)
      val pDec   = ps.getParam(PARAM_DEC)
      val pdRa   = ps.getParam(PARAM_DELTA_RA)
      val pdDec  = ps.getParam(PARAM_DELTA_DEC)
      val pEpoch = ps.getParam(PARAM_EPOCH)

      // Record the original B1950 RA/Dec for the note.
      val coordsString = s"(${pRa.getValue}, ${pDec.getValue}) \u0394(${pdRa.getValue}, ${pdDec.getValue})"

      // Get coords in degrees and PM in degrees/yr
      val ra   = parseHmsOrDegrees(pRa.getValue).unsafeExtract
      val dec  = parseDmsOrDegrees(pDec.getValue).unsafeExtract
      val dRa  = degreesPerYear(pdRa)
      val dDec = degreesPerYear(pdDec)

      // Convert to J2000
      val (ra0, dec0, dRa0, dDec0) = toJ2000(ra, dec, dRa, dDec)

      // Set new values
      pRa.setValue(ra0.toString)
      pDec.setValue(dec0.toString)
      pdRa.setValue((dRa0 * 1000 * 60 * 60).toString)
      pdRa.setUnits(UNITS_MILLI_ARCSECONDS_PER_YEAR)
      pdDec.setValue((dDec0 * 1000 * 60 * 60).toString)
      pdDec.setUnits(UNITS_MILLI_ARCSECONDS_PER_YEAR)
      pEpoch.setValue("2000")
      pEpoch.setUnits(UNITS_YEARS)
      pSys.setValue(VALUE_SYSTEM_J2000)

      appendNote(ps, coordsString, PrecessionNoteTitle, PrecessionNoteText)
    }

  // Remove JNNNN and APPARENT coordinate systems and replace unceremoniously with J2000.
  // These appear only in engineering and commissioning, each only once. BNNNN is unused.
  private def uselessSystemsToJ2000(d: Document): Unit = {
    val systems = Set(VALUE_SYSTEM_JNNNN, VALUE_SYSTEM_BNNNN, VALUE_SYSTEM_APPARENT)
    for {
      ps  <- allTargets(d)
      p   <- Option(ps.getParam(PARAM_SYSTEM)).toList if systems(p.getValue)
    } p.setValue(VALUE_SYSTEM_J2000)
  }

  // Turn old `brightness` property into a magnitude table if none exists, if possible, recording
  // our work in a per-observation note.
  private def brightnessToMagnitude(d: Document): Unit = {
    for {
      ps  <- allTargets(d)
      b   <- ps.value(PARAM_BRIGHTNESS).filter(_.nonEmpty).toList
    } (parseBrightness(b), Option(ps.getParamSet(PARAMSET_MAGNITUDES))) match {
      case (None, _)           => appendMagNote(ps, "failed: " + b)
      case (Some(ms), Some(_)) => appendMagNote(ps, "ignored: " + b)
      case (Some(ms), None)    => appendMagNote(ps, "parsed: " + b)
        ps.addParamSet(MagnitudePio.instance.toParamSet(PioFactory, ms.toList.asImList))
    }
  }

  // Append a message to the shared magnitude note for the given obs, relating to the given target
  private def appendMagNote(target: ParamSet, s: String): Unit =
    appendNote(target, s, MagnitudeNoteTitle, MagnitudeNoteText)

  def appendNote(target: ParamSet, s: String, title: String, text: String => String, append: Boolean = true): Unit = {
    // Find the parent that should hold the note.
    @tailrec
    def noteParent(node: PioNode): Option[Container] = {
      val names = Set(CONTAINER_OBSERVATION, CONTAINER_TEMPLATE_GROUP)
      node match {
        case null                             => None
        case c: Container if names(c.getKind) => Some(c)
        case _                                => noteParent(node.getParent)
      }
    }

    noteParent(target).foreach { parent =>
      val pset  = noteText(parent, title, text)
      if (append) {
        val param   = pset.getParam(PARAM_NOTE_TEXT)
        val curText = param.getValue
        val tName   = target.value(PARAM_NAME).getOrElse(VALUE_NAME_UNTITLED)
        param.setValue(s"$curText  $tName $s\n")
      }
    }
  }

  // Get or create the magnitude update note, returning its ParamSet
  private def noteText(parent: Container, title: String, text: String => String): ParamSet =
    findNoteText(parent, title).getOrElse(createNoteText(parent, title, text))

  // Find the [first] magnitude note and return its ParamSet, if any
  private def findNoteText(parent: Container, title: String): Option[ParamSet] =
    (for {
      note  <- parent.findContainers(SPComponentType.INFO_NOTE)
      ps    <- note.paramSets if ps.getName == PARAMSET_NOTE
      name  <- ps.value(ISPDataObject.TITLE_PROP).toList if name == title
    } yield ps).headOption

  // Create the magnitude node and return its ParamSet
  private def createNoteText(parent: Container, title: String, text: String => String): ParamSet = {
    // If we're adding the note to a template group, fish out the template
    // group name.  Otherwise if it is an observation the container name itself
    // is the observation name.
    val name =
      if (parent.getKind == CONTAINER_TEMPLATE_GROUP) {
        (for {
          ps <- Option(parent.getParamSet(PARAMSET_TEMPLATE_GROUP))
          p  <- Option(ps.getParam(PARAM_TITLE))
          t  <- Option(p.getValue)
        } yield t).getOrElse(parent.getName)
      } else
        parent.getName

    // Our note (easy way to get the ParamSet)
    val note = new SPNote()
    note.setTitle(title)
    note.setNote(text(name))

    // Container for ISPObsComponent
    val container = PioFactory.createContainer(
      SpIOTags.OBSCOMP,
      SPComponentType.INFO_NOTE.broadType.value,
      note.getVersion
    )
    container.setName(SPComponentType.INFO_NOTE.readableStr)
    container.setSubtype(SPComponentType.INFO_NOTE.narrowType)
    container.setKey(UUID.randomUUID.toString)

    // Hook it all up
    val ps = note.getParamSet(PioFactory)
    container.addParamSet(ps)
    parent.addContainer(container)
    ps

  }

  // Proper Motion conversion to degrees/yr
  def degreesPerYear(p: Param): Double = {
    val n = p.getValue.toDouble
    p.getUnits match {
      case UNITS_SECONDS_PER_YEAR          => n / (60 * 60)
      case UNITS_ARCSECONDS_PER_YEAR       => n / (60 * 60)
      case UNITS_MILLI_ARCSECONDS_PER_YEAR => n / (60 * 60 * 1000)
    }
  }

  def parseHmsOrDegrees(s: String): NumberFormatException \/ Double =
    Angle.parseHMS(s).map(_.toDegrees) orElse s.parseDouble.disjunction

  def parseDmsOrDegrees(s: String): NumberFormatException \/ Double =
    Angle.parseDMS(s).map(_.toDegrees) orElse s.parseDouble.disjunction

  // Convert B1950 to J2000. RA and Dec in degrees, dRA, dDec in degrees/yr.
  def toJ2000(ra: Double, dec: Double, dra: Double, ddec: Double): (Double, Double, Double, Double) = {
    import java.awt.geom.Point2D.{ Double => P2D }
    val (coords, pm) = (new P2D(ra, dec), new P2D(dra, ddec))
    if (dra == 0 && ddec == 0) {
      WSCon.fk425e(coords, 1950)
      (coords.x, coords.y, 0, 0)
    } else {
      WSCon.fk425m(coords, pm)
      (coords.x, coords.y, pm.x, pm.y)
    }
  }

  private lazy val convertNonSidEpoch = (ps: ParamSet) => {
    // we were storing epoch in JD but setting the units to "years"
    Option(ps.getParam(PARAM_EPOCH)).foreach { _.setUnits(UNITS_JD) }
    Option(ps.getParam(PARAM_EPOCH_PERI)).foreach { _.setUnits(UNITS_JD) }
  }

  private lazy val convertComet       = convertSystem(SYS_JPL_MINOR_BODY)(SYS_JPL_MINOR_BODY,SYS_ASA_COMET, SYS_MPC_COMET)(convertNonSidEpoch)
  private lazy val convertMinorPlanet = convertSystem(SYS_MPC_MINOR_PLANET)(SYS_MPC_MINOR_PLANET,SYS_ASA_MINOR_PLANET)(convertNonSidEpoch)

  private lazy val convertSolar =
    convertSystem(SYS_SOLAR_OBJECT)(SYS_ASA_MAJOR_PLANET, SYS_JPL_MAJOR_PLANET) { ps =>
      val n = ps.getParam("name")
      Pio.addParam(PioFactory, ps, PARAM_OBJECT, n.getValue.toUpperCase)
    }

  // Construct a conversion that replaces the target system and optionally makes other changes to
  // the target's paramset.
  private def convertSystem(to: String)(from: String*)(f: ParamSet => Unit): Document => Unit = { d =>
    val systems = Set(from: _*)
    for {
      ps  <- allTargets(d)
      p   <- Option(ps.getParam(PARAM_SYSTEM)).toList if systems(p.getValue)
    } { p.setValue(to); f(ps) }
  }

}


