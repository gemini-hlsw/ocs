package edu.gemini.catalog.ui

import javax.swing.UIManager

import edu.gemini.catalog.api._
import edu.gemini.spModel.core._
import edu.gemini.spModel.obs.context.ObsContext

import scala.swing.SwingApplication

import scalaz._
import Scalaz._

/**
 * Test Launcher for the Catalog navigator
 */
object CatalogQueryDemo extends SwingApplication {
  import QueryResultsFrame.instance
  import edu.gemini.shared.util.immutable.{None => JNone, Some => JSome}
  import edu.gemini.spModel.gemini.niri.{InstNIRI, NiriOiwfsGuideProbe}
  import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
  import edu.gemini.spModel.guide.GuideProbe
  import edu.gemini.spModel.target.SPTarget
  import edu.gemini.spModel.target.env.TargetEnvironment
  import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
  import jsky.util.gui.Theme

  val query = CatalogQuery(Coordinates(RightAscension.fromAngle(Angle.fromDegrees(3.1261166666666895)),Declination.fromAngle(Angle.fromDegrees(337.93268333333333)).getOrElse(Declination.zero)),RadiusConstraint.between(Angle.zero,Angle.fromDegrees(0.16459874517619255)),List(MagnitudeConstraints(RBandsList,FaintnessConstraint(16.0),Some(SaturationConstraint(3.1999999999999993)))),UCAC4)

  def startup(args: Array[String]) {
    System.setProperty("apple.awt.antialiasing", "on")
    System.setProperty("apple.awt.textantialiasing", "on")
    Theme.install()

    UIManager.put("Button.defaultButtonFollowsFocus", true)

    val ra = Angle.fromHMS(16, 17, 2.410).getOrElse(Angle.zero)
    val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(22, 58, 33.888).getOrElse(Angle.zero)).getOrElse(Declination.zero)
    val guiders = Set[GuideProbe](NiriOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
    val target = new SPTarget(ra.toDegrees, dec.toDegrees) <| {_.setName("Messier 80")}
    val env = TargetEnvironment.create(target)
    val inst = new InstNIRI <| {_.setPosAngle(0.0)}

    val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.PERCENT_80).cc(SPSiteQuality.CloudCover.PERCENT_80).iq(SPSiteQuality.ImageQuality.PERCENT_85)
    val ctx = ObsContext.create(env, inst, new JSome(Site.GN), conditions, null, null, JNone.instance())

    instance.showOn(ctx)
  }

}
