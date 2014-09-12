package edu.gemini.phase2.skeleton.factory

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover    => SPCloudCover}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{ImageQuality  => SPImageQuality}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{SkyBackground => SPSkyBackground}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{WaterVapor    => SPWaterVapor}

import edu.gemini.model.p1.{mutable => M}
import M.CloudCover._
import M.ImageQuality._
import M.SkyBackground._
import M.WaterVapor._

import edu.gemini.model.p1.immutable.Condition

object SpSiteQualityFactory {
  private val ccMap = Map(
    cc50  -> SPCloudCover.PERCENT_50,
    cc70  -> SPCloudCover.PERCENT_70,
    cc80  -> SPCloudCover.PERCENT_80,
    cc100 -> SPCloudCover.ANY
  )

  private val iqMap = Map(
    iq20  -> SPImageQuality.PERCENT_20,
    iq70  -> SPImageQuality.PERCENT_70,
    iq85  -> SPImageQuality.PERCENT_85,
    iq100 -> SPImageQuality.ANY
  )

  private val sbMap = Map(
    sb20  -> SPSkyBackground.PERCENT_20,
    sb50  -> SPSkyBackground.PERCENT_50,
    sb80  -> SPSkyBackground.PERCENT_80,
    sb100 -> SPSkyBackground.ANY
  )

  private val wvMap = Map(
    wv20  -> SPWaterVapor.PERCENT_20,
    wv50  -> SPWaterVapor.PERCENT_50,
    wv80  -> SPWaterVapor.PERCENT_80,
    wv100 -> SPWaterVapor.ANY
  )

  def create(c: Condition): Either[String, SPSiteQuality] =
    for {
      cc <- toSpCondition(c.cc, ccMap).right
      iq <- toSpCondition(c.iq, iqMap).right
      sb <- toSpCondition(c.sb, sbMap).right
      wv <- toSpCondition(c.wv, wvMap).right
    } yield {
      val sp = new SPSiteQuality()
      sp.setCloudCover(cc)
      sp.setImageQuality(iq)
      sp.setSkyBackground(sb)
      sp.setWaterVapor(wv)
      c.maxAirmass foreach { d =>
        sp.setElevationConstraintType(SPSiteQuality.ElevationConstraintType.AIRMASS)
        sp.setElevationConstraintMax(d)
        sp.setElevationConstraintMin(1.0) // UX-1544
      }
      sp
    }

  private def toSpCondition[A <: Enum[A], B <: Enum[B]](a: A, m: Map[A, B]): Either[String, B] =
    m.get(a).toRight("No matching SP value for %s".format(a.name()))


  /*
  def conditions(proposal: Proposal): Either[String, List[SPSiteQuality]] = {
    val i: Either[String, List[SPSiteQuality]] = Right(Nil)
    (i/:proposal.conditions) {
      (e, c) => e.right flatMap { lst =>
        create(c).right map { sq => sq :: lst }
      }
    }
  }
  */
}
