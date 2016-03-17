package edu.gemini.spModel.io.impl.migration

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.io.impl.SpIOTags
import edu.gemini.spModel.pio.xml.PioXmlUtil
import edu.gemini.spModel.pio.{Container, ParamSet, Document, Version}

import java.io.StringWriter

/**
 * Base trait for all migrations.
 */
trait Migration {

  import PioSyntax._

  def version: Version

  def conversions: List[Document => Unit]

  /** Applies all conversion functions in order if the document is older than
    * the `version`.
    */
  def updateProgram(d: Document): Unit =
    d.containers.find(_.getKind == SpIOTags.PROGRAM).filter { c =>
      c.getVersion.compareTo(version) < 0
    }.foreach { _ =>
      conversions.foreach(_.apply(d))
    }

  val ParamSetBase               = "base"
  val ParamSetObservation        = "Observation"
  val ParamSetTarget             = "spTarget"
  val ParamSetTemplateParameters = "Template Parameters"

  // Extract all the target paramsets, be they part of an observation or
  // template parameters.
  protected def allTargets(d: Document): List[ParamSet] = {
    val names = Set(ParamSetBase, ParamSetTarget)

    val templateTargets = for {
      cs  <- d.findContainers(SPComponentType.TEMPLATE_PARAMETERS)
      tps <- cs.allParamSets if tps.getName == ParamSetTemplateParameters
      ps  <- tps.allParamSets if names(ps.getName)
    } yield ps

    val obsTargets = for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
    } yield ps

    templateTargets ++ obsTargets
  }


  /** (obs paramset, target paramset) paramset pairs **/
  protected def obsAndBases(d: Document): List[(ParamSet, ParamSet)] =
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if ps.getName == ParamSetBase
    } yield (obs.getParamSet(ParamSetObservation), ps)

  /** Writes the document to an XML String for debugging. */
  protected def formatDocument(d: Document): String = {
    val writer = new StringWriter()
    PioXmlUtil.write(d, writer)
    writer.toString
  }
}
