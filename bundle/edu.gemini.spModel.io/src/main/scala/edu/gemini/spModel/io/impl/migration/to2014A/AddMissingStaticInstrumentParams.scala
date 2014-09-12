package edu.gemini.spModel.io.impl.migration.to2014A

import edu.gemini.spModel.pio.{Pio, Container, Version}
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.io.impl.migration.Util._
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.gemini.gnirs.{SeqConfigGNIRS, InstGNIRS}

/**
 * Correct the static instrument component to contain all iterated items in the
 * first instrument iterator (so that the first step synchronization works).
 */
object AddMissingStaticInstrumentParams {
  val Version2014A = Version.`match`("2014A-1")
  val Factory = new PioXmlFactory

  def update(obs: Container, instType: SPComponentType, iterType: SPComponentType, paramNames: Set[String]): Unit = {
    val oc = new ObsContainer(obs)

    // Get the instrument data object and iterator container, if there are matching types
    val ctx = if (obs.getVersion.compareTo(Version2014A) >= 0) None
              else for {
                instContainer <- oc.instrument(instType)
                instParamSet  <- instContainer.dataObject
                iterContainer <- oc.findIterator(iterType)
              } yield (instParamSet, iterContainer)

    ctx.foreach { case (instParamSet, iterContainer) =>
      val m = iterContainer.params(paramNames)
      m.foreach { case (name, value) => Pio.addParam(Factory, instParamSet, name, value) }
    }
  }

  val GnirsParams = Set(
    InstGNIRS.ACQUISITION_MIRROR_PROP,
    InstGNIRS.CAMERA_PROP,
    InstGNIRS.DECKER_PROP,
    InstGNIRS.FILTER_PROP
  ).map(_.getName)

  def update(obs: Container): Unit = {
    update(obs, InstGNIRS.SP_TYPE, SeqConfigGNIRS.SP_TYPE, GnirsParams)
  }
}
