package edu.gemini.spModel.io.impl.migration.to2020A

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.io.PioSyntax._
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.obs.ObsParamSetCodecs._
import edu.gemini.spModel.pio.codec._
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio.{Document, Version}
import edu.gemini.spModel.util.DefaultSchedulingBlock

import scalaz._
import Scalaz._


object To2020A extends Migration {

  val schedulingBlockKey = "schedulingBlock"

  override val version = Version.`match`("2020A-1")

  override val conversions: List[Document => Unit] =
    List(
      addSchedulingBlocks
    )

  val fact = new PioXmlFactory

  private def addSchedulingBlocks(d: Document): Unit =
    d.findContainers(SPComponentType.OBSERVATION_BASIC).foreach { o =>
      for {
        ps  <- Option(o.getParamSet(ParamSetObservation))          // data object
        pid <- observationId(o).map(_.getProgramID).right.toOption // pid
      } {
        if (ps.paramSet(schedulingBlockKey).isEmpty) {
          ps.addParamSet(DefaultSchedulingBlock.forPid(pid).encode(schedulingBlockKey))
        }
      }
    }

}
