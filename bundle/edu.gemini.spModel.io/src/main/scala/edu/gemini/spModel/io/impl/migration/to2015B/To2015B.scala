package edu.gemini.spModel.io.impl.migration.to2015B

import java.util.UUID

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.io.impl.SpIOTags
import edu.gemini.spModel.obscomp.SPNote

import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio.{ Document, Container, ParamSet }
import edu.gemini.spModel.target.MagnitudePio

/** Convert to new target model. This is side-effecty, sorry. */
object To2015B {
  import PioSyntax._
  import BrightnessParser._

  // Entry point here
  def updateProgram(d: Document): Unit =
    conversions.foreach(_.apply(d))

  private val MagnitudeNoteTitle = "2015B Magnitude Updates"
  private val PioFactory = new PioXmlFactory()

  // These will be applied in the given order
  private val conversions: List[Document => Unit] = List(
    brightnessToMagnitude,
    uselessSystemsToJ2000
  )

  // Remove JNNNN and APPARENT coordinate systems and replace unceremoniously with J2000.
  // These appear only in engineering and commissioning, each only once. So not a big deal.
  private def uselessSystemsToJ2000(d: Document): Unit = {
    val systems = Set("JNNNN", "BNNNN", "Apparent")
    val names = Set("base", "spTarget")
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
      p   <- Option(ps.getParam("system")).toList if systems(p.getValue)
    } p.setValue("J2000")
  }

  // Turn old `brightness` property into a magnitude table if none exists, if possible, recording
  // our work in a per-observation note.
  private def brightnessToMagnitude(d: Document): Unit = {
    val names = Set("base", "spTarget")
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
      b   <- ps.value("brightness").filter(_.nonEmpty).toList
    } (parseBrightness(b), Option(ps.getParamSet("magnitudeList"))) match {
      case (None, _)           => appendNote(obs, ps, "failed: " + b)
      case (Some(ms), Some(_)) => appendNote(obs, ps, "ignored: " + b)
      case (Some(ms), None)    => appendNote(obs, ps, "parsed: " + b)
        ps.addParamSet(MagnitudePio.instance.toParamSet(PioFactory, ms.list.asImList))
    }
  }

  // Append a message to the shared magnitude note for the given obs, relating to the given target
  private def appendNote(obs: Container, target: ParamSet, s: String): Unit = {
    val pset  = noteText(obs)
    val param = pset.getParam("NoteText")
    val text  = param.getValue
    val tName = target.value("name").getOrElse("(untitled)")
    param.setValue(s"$text  $tName $s\n")
  }

  // Get or create the magnitude update note, returning its ParamSet
  private def noteText(obs: Container): ParamSet =
    findNoteText(obs).getOrElse(createNoteText(obs))

  // Find the [first] magnitude note and return its ParamSet, if any
  private def findNoteText(obs: Container): Option[ParamSet] =
    (for {
      note  <- obs.findContainers(SPComponentType.INFO_NOTE)
      ps    <- note.paramSets if ps.getName == "Note"
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
}


