package edu.gemini.spModel.io.impl.migration.to2016B

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.io.impl.migration.PioSyntax._
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio.{Pio, ParamSet, Document, Version}
import edu.gemini.spModel.target.env.GuideGroup

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

object To2016B extends Migration {
  val version = Version.`match`("2016B-1")

  val conversions: List[Document => Unit] = List(
    updateGuideEnvironment
  )

  val fact = new PioXmlFactory

  private def updateGuideEnvironment(d: Document): Unit = {

    def disabledGroup: ParamSet =
      fact.createParamSet(GuideGroup.ParamSetName) <|
        (ps => Pio.addParam(fact, ps, "tag", GuideGroup.AutoDisabledTag.toString))

    def addEmptyGuideEnv(targetEnv: ParamSet): Unit = {
      val guideEnv = fact.createParamSet("guideEnv")

      Pio.addIntParam(fact, guideEnv, "primary", 0)
      guideEnv.addParamSet(disabledGroup)

      targetEnv.addParamSet(guideEnv)
    }

    def addDisabledAutomaticGroup(guideEnv: ParamSet): Unit = {
      val manualGroups = guideEnv.getParamSets("guideGroup").asScala.toList

      // Add the manual tag to all the existing groups.
      manualGroups.foreach { grp =>
        Pio.addParam(fact, grp, "tag", GuideGroup.ManualTag.toString)
      }

      // Increment primary group to account for the new auto group
      val primary = Pio.getIntValue(guideEnv, "primary", 0) + (manualGroups.nonEmpty ? 1 | 0)
      val param   = Option(guideEnv.getParam("primary")) | {
        fact.createParam("primary") <| guideEnv.addParam
      }
      param.setValue(primary.toString)

      // Remove all guide gropus.
      while (guideEnv.removeChild("guideGroup") != null) {}

      // Add them back in the correct order.
      (disabledGroup :: manualGroups).foreach(guideEnv.addParamSet)
    }

    val tes = for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets.filter(_.getName == "targetEnv")
    } yield ps

    tes.foreach { paramSet =>
      Option(paramSet.getParamSet("guideEnv")).fold(addEmptyGuideEnv(paramSet))(addDisabledAutomaticGroup)
    }
  }
}
