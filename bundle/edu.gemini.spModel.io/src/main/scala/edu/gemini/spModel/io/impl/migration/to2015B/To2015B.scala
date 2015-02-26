package edu.gemini.spModel.io.impl.migration.to2015B

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

import scalaz._, Scalaz._

/** Convert to new target model. This is side-effecty, sorry. */
object To2015B {
  import PioSyntax._
  import BrightnessParser._

  val Version_2015B = Version.`match`("2015B-1")

  def isPre2015B(c: Container): Boolean =
    c.getVersion.compareTo(Version_2015B) < 0

  // Entry point here
  def updateProgram(d: Document): Unit =
    d.containers.find(_.getKind == SpIOTags.PROGRAM).filter(isPre2015B).foreach { _ =>
      conversions.foreach(_.apply(d))
    }

  // These constants are take from mainline code, where they are private to implementations and
  // ultimately will go away (but we will need them here for a while longer).

  val PARAM_SYSTEM     = "system"
  val PARAM_RA         = "c1"
  val PARAM_DEC        = "c2"
  val PARAM_DELTA_RA   = "pm1"
  val PARAM_DELTA_DEC  = "pm2"
  val PARAM_NOTE_TEXT  = "NoteText"
  val PARAM_NAME       = "name"
  val PARAM_BRIGHTNESS = "brightness"
  val PARAM_OBJECT     = "object"

  val PARAMSET_NOTE       = "Note"
  val PARAMSET_BASE       = "base"
  val PARAMSET_TARGET     = "spTarget"
  val PARAMSET_MAGNITUDES = "magnitudeList"

  val VALUE_NAME_UNTITLED = "(untitled)"

  val VALUE_SYSTEM_B1950    = "B1950"
  val VALUE_SYSTEM_J2000    = "J2000"
  val VALUE_SYSTEM_JNNNN    = "JNNNN"
  val VALUE_SYSTEM_BNNNN    = "BNNNN"
  val VALUE_SYSTEM_APPARENT = "Apparent"

  val UNITS_SECONDS_PER_YEAR          = "seconds/year"
  val UNITS_ARCSECONDS_PER_YEAR       = "arcsecs/year"
  val UNITS_MILLI_ARCSECONDS_PER_YEAR = "milli-arcsecs/year"

  val SYS_ASA_MAJOR_PLANET = "AsA major planet"
  val SYS_ASA_MINOR_PLANET = "AsA minor planet"
  val SYS_ASA_COMET        = "AsA comet"
  val SYS_JPL_MAJOR_PLANET = "JPL major planet"
  val SYS_JPL_MINOR_BODY   = "JPL minor body"
  val SYS_MPC_MINOR_PLANET = "MPC minor planet"
  val SYS_MPC_COMET        = "MPC comet"

  val SYS_SOLAR_OBJECT     = "Solar system object"

  private val MagnitudeNoteTitle = "2015B Magnitude Updates"
  private val PioFactory = new PioXmlFactory()

  // These will be applied in the given order
  private val conversions: List[Document => Unit] = List(
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
  private def b1950ToJ2000(d: Document): Unit = {
    val names = Set(PARAMSET_BASE, PARAMSET_TARGET)
    for {
      obs   <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env   <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps    <- env.allParamSets if names(ps.getName)
      pSys  <- Option(ps.getParam(PARAM_SYSTEM)).toList if pSys.getValue == VALUE_SYSTEM_B1950

    } {

      // Params
      val pRa   = ps.getParam(PARAM_RA)
      val pDec  = ps.getParam(PARAM_DEC)
      val pdRa  = ps.getParam(PARAM_DELTA_RA)
      val pdDec = ps.getParam(PARAM_DELTA_DEC)

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
      pdRa.setValue((dRa0 * 60 * 60).toString)
      pdRa.setUnits(UNITS_SECONDS_PER_YEAR)
      pdDec.setValue((dDec0 * 60 * 60).toString)
      pdDec.setUnits(UNITS_SECONDS_PER_YEAR)
      pSys.setValue(VALUE_SYSTEM_J2000)

    }

  }

  // Remove JNNNN and APPARENT coordinate systems and replace unceremoniously with J2000.
  // These appear only in engineering and commissioning, each only once. BNNNN is unused.
  private def uselessSystemsToJ2000(d: Document): Unit = {
    val systems = Set(VALUE_SYSTEM_JNNNN, VALUE_SYSTEM_BNNNN, VALUE_SYSTEM_APPARENT)
    val names = Set(PARAMSET_BASE, PARAMSET_TARGET)
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
      p   <- Option(ps.getParam(PARAM_SYSTEM)).toList if systems(p.getValue)
    } p.setValue(VALUE_SYSTEM_J2000)
  }

  // Turn old `brightness` property into a magnitude table if none exists, if possible, recording
  // our work in a per-observation note.
  private def brightnessToMagnitude(d: Document): Unit = {
    val names = Set(PARAMSET_BASE, PARAMSET_TARGET)
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
      b   <- ps.value(PARAM_BRIGHTNESS).filter(_.nonEmpty).toList
    } (parseBrightness(b), Option(ps.getParamSet(PARAMSET_MAGNITUDES))) match {
      case (None, _)           => appendNote(obs, ps, "failed: " + b)
      case (Some(ms), Some(_)) => appendNote(obs, ps, "ignored: " + b)
      case (Some(ms), None)    => appendNote(obs, ps, "parsed: " + b)
        ps.addParamSet(MagnitudePio.instance.toParamSet(PioFactory, ms.list.asImList))
    }
  }

  // Append a message to the shared magnitude note for the given obs, relating to the given target
  private def appendNote(obs: Container, target: ParamSet, s: String): Unit = {
    val pset  = noteText(obs)
    val param = pset.getParam(PARAM_NOTE_TEXT)
    val text  = param.getValue
    val tName = target.value(PARAM_NAME).getOrElse(VALUE_NAME_UNTITLED)
    param.setValue(s"$text  $tName $s\n")
  }

  // Get or create the magnitude update note, returning its ParamSet
  private def noteText(obs: Container): ParamSet =
    findNoteText(obs).getOrElse(createNoteText(obs))

  // Find the [first] magnitude note and return its ParamSet, if any
  private def findNoteText(obs: Container): Option[ParamSet] =
    (for {
      note  <- obs.findContainers(SPComponentType.INFO_NOTE)
      ps    <- note.paramSets if ps.getName == PARAMSET_NOTE
      name  <- ps.value(ISPDataObject.TITLE_PROP).toList if name == MagnitudeNoteTitle
    } yield ps).headOption

  // Create the magnitude node and return its ParamSet
  private def createNoteText(obs: Container): ParamSet = {

    // Our note (easy way to get the ParamSet)
    val note = new SPNote()
    note.setTitle(MagnitudeNoteTitle)
    note.setNote(
      s"""|The unstructured 'brightness' property for targets was deprecated in 2010B and removed
          |in 2015B. This note records the disposition of old brightness values associated with
          |the targets in ${obs.getName}.
          |
          |A "parsed" message means that the brightness value was converted into one or more
          |structured magnitude values with known pass bands. "failed" means that this process
          |failed, so you may wish to update the magnitude table manually. "ignored" means that
          |a structured magnitude table was already present and parsing was skipped.
          |
          |""".stripMargin)

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
    obs.addContainer(container)
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

  private lazy val convertComet       = convertSystem(SYS_JPL_MINOR_BODY)(SYS_ASA_COMET, SYS_MPC_COMET)()
  private lazy val convertMinorPlanet = convertSystem(SYS_MPC_MINOR_PLANET)(SYS_ASA_MINOR_PLANET)()

  private lazy val convertSolar =
    convertSystem(SYS_SOLAR_OBJECT)(SYS_ASA_MAJOR_PLANET, SYS_JPL_MAJOR_PLANET) { ps =>
      val n = ps.getParam("name")
      Pio.addParam(PioFactory, ps, PARAM_OBJECT, n.getValue.toUpperCase)
    }

  // Construct a conversion that replaces the target system and optionally makes other changes to
  // the target's paramset.
  private def convertSystem(to: String)(from: String*)(f: ParamSet => Unit = _ => ()): Document => Unit = { d =>
    val systems = Set(from: _*)
    val names = Set(PARAMSET_BASE, PARAMSET_TARGET)
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
      p   <- Option(ps.getParam(PARAM_SYSTEM)).toList if systems(p.getValue)
    } { p.setValue(to); f(ps) }
  }

}


