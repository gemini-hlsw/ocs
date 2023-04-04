package edu.gemini.spModel.template


import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.sp.SPComponentType.{SCHEDULING_CONDITIONS, TELESCOPE_TARGETENV}
import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.shared.util.TimeValue
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover.{PERCENT_50 => CC50, PERCENT_80 => CC80}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

trait TestHelp {
  // The program factory interprets a null key as a request to create a new key
  val WithSomeNewKey: SPNodeKey = null

  val ScienceTargetName = "Biff"
  val EditTargetName    = "Henderson"
  val ScienceTargetCC   = CC50
  val EditTargetCC      = CC80
  val EditPA            = 42.0

  def newTarget(name: String): SPTarget =
    new SPTarget() <| (_.setName(name))

  def newTargetObsComp(name: String): TargetObsComp =
    new TargetObsComp <| (_.setTargetEnvironment(TargetEnvironment.create(newTarget(name))))

  def newSiteQuality(cc: CloudCover): SPSiteQuality =
    new SPSiteQuality() <| (_.setCloudCover(cc))

  def newParameters(targetName: String, cc: CloudCover): TemplateParameters =
    TemplateParameters.newInstance(newTarget(targetName), newSiteQuality(cc), new TimeValue(1, TimeValue.Units.hours))

  def siteQuality(o: ISPObservation): Option[SPSiteQuality] =
    dataObject[SPSiteQuality](o, SCHEDULING_CONDITIONS)

  def target(o: ISPObservation): Option[TargetObsComp] =
    dataObject[TargetObsComp](o, TELESCOPE_TARGETENV)

  def dataObject[D: Manifest](o: ISPObservation, ct: SPComponentType): Option[D] =
    obsComp[D](o, ct).map(_._2)

  def obsComp[D: Manifest](o: ISPObservation, ct: SPComponentType): Option[(ISPObsComponent, D)] =
    o.getObsComponents.asScala.find { _.getType == ct }.map(oc => (oc, oc.getDataObject)).collect {
      case (oc, d: D) => (oc, d)
    }

}
