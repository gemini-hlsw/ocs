package edu.gemini.catalog.ui

import javax.swing.UIManager

import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gmos.{GmosOiwfsGuideProbe, InstGmosSouth}
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

  def startup(args: Array[String]) {
    System.setProperty("apple.awt.antialiasing", "on")
    System.setProperty("apple.awt.textantialiasing", "on")
    Theme.install()

    UIManager.put("Button.defaultButtonFollowsFocus", true)

    val ra = Angle.fromHMS(20, 34, 11.369).getOrElse(Angle.zero)
    val dec = Declination.fromAngle(Angle.fromDMS(7, 24, 16.092).getOrElse(Angle.zero)).getOrElse(Declination.zero)
    val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
    val target = new SPTarget(ra.toDegrees, dec.toDegrees) <| {_.setName("Messier 80")}
    val env = TargetEnvironment.create(target)
    val inst = new InstGmosSouth <| {_.setPosAngle(0.0)}

    val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.PERCENT_80).cc(SPSiteQuality.CloudCover.PERCENT_80).iq(SPSiteQuality.ImageQuality.PERCENT_85)
    val ctx = ObsContext.create(env, inst, new JSome(Site.GN), conditions, null, null, JNone.instance(), JNone.instance())

    instance.showOn(ctx)
  }

}
