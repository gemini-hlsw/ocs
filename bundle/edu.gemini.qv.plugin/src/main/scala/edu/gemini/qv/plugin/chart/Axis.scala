package edu.gemini.qv.plugin.chart

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.qpt.shared.sp.Band
import edu.gemini.qv.plugin.QvStore.NamedElement
import edu.gemini.qv.plugin.filter.core.Filter.{HasDummyTarget, Partner, RA}
import edu.gemini.qv.plugin.filter.core._
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth
import edu.gemini.spModel.gemini.gmos.{GmosNorthType, GmosSouthType}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.spModel.obs.SPObservation.Priority

trait Axis extends NamedElement {
  val groups: Seq[Filter]

  /** True if all categories on this axis represent RA bins.
   *  This must includes dummy target bins for targets with RA=0 and Dec=0.
   */
  def isTime: Boolean = {
    !groups.exists({
      case HasDummyTarget(_)  => false
      case RA(_,_)            => false
      case _                  => true
    })
  }

}

object Axis {

  /** Creates an axis for a given set of filters, if there are no filters the axis is a stub and non editable. */
  // TODO: Here we should "require(g.nonEmpty)" but older versions exported the Observations and Programs filter
  // TODO: stubs to file. On cleaning this up I have to make sure this case is handled properly.
  def apply(l: String, g: Seq[Filter]) = {
    new Axis {
      override val isEditable = g.nonEmpty
      val label = l
      val groups = g
    }
  }

  /** Creates a non editable axis stub without filters that serves as a placeholder for dynamic
    * filters which are calculated on-the-fly based on the data that is present.
    */
  def apply(l: String) = {
    new Axis {
      override val isEditable = false
      val label = l
      val groups = Seq()
    }
  }

  // create a set of default axes
  val Instruments     = forValues[SPComponentType]("Instruments", Filter.Instruments().sortedValues, Filter.Instruments.apply)
  val Bands           = forValues[Band]           ("Bands", Filter.Bands().sortedValues, Filter.Bands.apply)
  val Partners        = forValues[Partner]        ("Partners", Filter.Partners().sortedValues, Filter.Partners.apply)
  val Priorities      = forValues[Priority]       ("User Priorities", Filter.Priorities().sortedValues, Filter.Priorities.apply)
  val IQs             = forValues[ImageQuality]   ("Image Quality", Filter.IQs().sortedValues, Filter.IQs.apply)
  val CCs             = forValues[CloudCover]     ("Cloud Cover", Filter.CCs().sortedValues, Filter.CCs.apply)
  val WVs             = forValues[WaterVapor]     ("Water Vapor", Filter.WVs().sortedValues, Filter.WVs.apply)
  val SBs             = forValues[SkyBackground]  ("Sky Background", Filter.SBs().sortedValues, Filter.SBs.apply)
  val GmosNDispersers = forValues[DisperserNorth] ("GMOS-N Dispersers", Filter.GmosN.Dispersers().sortedValues, Filter.GmosN.Dispersers.apply)
  val GmosNIfuXDispersers = Axis("GMOS-N IFUs / Dispersers", Seq(
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.B1200_G5301)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.B600_G5303)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.B600_G5307)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.DEFAULT)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.MIRROR)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R150_G5306)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R400_G5305)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R400_G5310)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R600_G5304)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_1), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R831_G5302)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.B1200_G5301)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.B600_G5303)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.B600_G5307)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.DEFAULT)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.MIRROR)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R150_G5306)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R400_G5305)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R400_G5310)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R600_G5304)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.IFU_3), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R831_G5302))
  ))
  val GmosNMosXDispersers = Axis("GMOS-N MOS / Dispersers", Seq(
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.B1200_G5301)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.B600_G5303)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.B600_G5307)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.DEFAULT)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.MIRROR)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R150_G5306)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R400_G5305)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R400_G5310)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R600_G5304)),
    FilterAnd(Filter.GmosN.FocalPlanes(GmosNorthType.FPUnitNorth.CUSTOM_MASK), Filter.GmosN.Dispersers(GmosNorthType.DisperserNorth.R831_G5302))
  ))
  val GmosSDispersers = forValues[DisperserSouth] ("GMOS-S Dispersers", Filter.GmosS.Dispersers().sortedValues, Filter.GmosS.Dispersers.apply)
  val GmosSIfuXDispersers = Axis("GMOS-S IFUs / Dispersers", Seq(
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_1), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.B1200_G5321)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_1), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.B600_G5323)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_1), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.DEFAULT)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_1), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.MIRROR)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_1), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R150_G5326)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_1), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R400_G5325)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_1), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R600_G5324)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_1), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R831_G5322)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_3), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.B1200_G5321)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_3), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.B600_G5323)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_3), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.DEFAULT)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_3), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.MIRROR)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_3), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R150_G5326)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_3), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R400_G5325)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_3), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R600_G5324)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.IFU_3), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R831_G5322))
  ))
  val GmosSMosXDispersers = Axis("GMOS-S MOS / Dispersers", Seq(
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.CUSTOM_MASK), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.B1200_G5321)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.CUSTOM_MASK), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.B600_G5323)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.CUSTOM_MASK), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.DEFAULT)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.CUSTOM_MASK), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.MIRROR)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.CUSTOM_MASK), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R150_G5326)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.CUSTOM_MASK), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R400_G5325)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.CUSTOM_MASK), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R600_G5324)),
    FilterAnd(Filter.GmosS.FocalPlanes(GmosSouthType.FPUnitSouth.CUSTOM_MASK), Filter.GmosS.Dispersers(GmosSouthType.DisperserSouth.R831_G5322))
  ))
  val BigSheet = Axis("Big Sheet", Seq(
    FilterAnd(Filter.IQs(ImageQuality.PERCENT_20), Filter.CCs(CloudCover.PERCENT_50)),
    FilterAnd(Filter.IQs(ImageQuality.PERCENT_20), Filter.CCs(CloudCover.PERCENT_70)),
    FilterAnd(Filter.IQs(ImageQuality.PERCENT_70), Filter.CCs(CloudCover.PERCENT_50)),
    FilterAnd(Filter.IQs(ImageQuality.PERCENT_70), Filter.CCs(CloudCover.PERCENT_70)),
    FilterAnd(Filter.IQs(ImageQuality.PERCENT_85), Filter.CCs(CloudCover.PERCENT_50)),
    FilterAnd(Filter.IQs(ImageQuality.PERCENT_85), Filter.CCs(CloudCover.PERCENT_70))
  ))

  // These axes depend on the actual observations and programs that are available in the data we're looking at.
  // They are created "on-the-fly" whenever one of these stubs is selected using Program.forPrograms() and
  // Observation.forObservations() respectively. Note that the axes stubs also need to be used for looking up
  // axes when loading configurations from disk.
  // TODO: Make calculated axes part of default axes.
  val Observations = Axis("Observations")
  val Programs     = Axis("Programs")
  val Dynamics     = Seq(Observations, Programs)

  // ra default axes, note: ra values in obd are defined in degrees, not hours
  val RA025 = Axis("RA 0.25", HasDummyTarget(Some(true)) +: raRanges(0.25))
  val RA05  = Axis("RA 0.5", HasDummyTarget(Some(true)) +: raRanges(0.5))
  val RA1   = Axis("RA", HasDummyTarget(Some(true)) +: raRanges(1))
  val RA2   = Axis("RA 2", HasDummyTarget(Some(true)) +: raRanges(2))

  def forValues[A](label: String, v: Seq[A], f: Set[A] => Filter): Axis = {
    val s0 = v.map(Set(_))
    val s1 = s0.map(f)
    Axis(label, s1.toList)
  }

  def raRanges[A](step: Double): Seq[RA] =
    for {
      a <- RA.MinValue until (RA.MaxValue, step)
    } yield RA(a, a + step)

}


