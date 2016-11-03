package edu.gemini.phase2.template.factory.impl.gnirs

import edu.gemini.spModel.gemini.gnirs.blueprint.SpGnirsBlueprintImaging
import edu.gemini.spModel.gemini.gnirs.GNIRSParams._

case class GnirsImaging(blueprint:SpGnirsBlueprintImaging) extends GnirsBase[SpGnirsBlueprintImaging] {

  import blueprint.{pixelScale, filter => oldFilter}

  // Local imports
  import Filter.PAH
  import WellDepth.DEEP

  // **** IF INSTRUMENT MODE == IMAGING ****
  // INCLUDE  {16}, {17}, {18} - {21} (in this order) in a Target Group
  //         Add the note "Centering and dithering" to the top of the
  //         imaging group.
  //         SET PIXEL SCALE FROM PI
  //         FOR {16}, {20} (acqs for standards) IF PIXEL SCALE = 0.05 \
  //             SET EXPOSURE TIME=15.0 in the first GNIRS iterator: Image of keyhole

  //
  //         FOR ALL OBS in Group :
  //             SET FILTER FROM PI IN ALL GNIRS ITERATORS
  //                 The GNIRS filters are changing in the 12B OT (see
  //                 REL-444 in JIRA). Here is the new mapping:
  //                 X (1.10um) => order 6 (X)
  //                 J (1.25um) => J-MK: 1.25um
  //                 H (1.65um) => order 4 (H-MK)
  //                 K (2.20um) => K-MK: 2.20um
  //                 H2 (2.122um) => H2: 2.12um
  //                 PAH (3.295) => PAH: 3.3um
  //             IF FILTER == PAH SET Well Depth = Deep
  //             REL-2646 removes the following line:
  //             X - SET Central Wavelength according to FILTER in all GNIRS iterators

  val filter = oldFilter match {
    case Filter.ORDER_5 => Filter.J // J (1.25um) => J-MK: 1.25um
    case Filter.ORDER_3 => Filter.K // K (2.20um) => K-MK: 2.20um
    case _              => oldFilter
  }

  include(16, 17, 18, 19, 20, 21) in TargetGroup
  addNote("Centering and dithering") in TargetGroup

  forGroup(TargetGroup)(
    setPixelScale(pixelScale))

  forObs(16, 20)(
    ifTrue(pixelScale == PixelScale.PS_005)(
      setExposureTime(15.0),
      mutateSeq.atIndex(0)(mapStepsByKey(PARAM_EXPOSURE_TIME) {
        case _ => 15.0
      })
    ))

  forGroup(TargetGroup)(
    setFilter(filter),
    mutateSeq(
      iterate(PARAM_FILTER, List(filter))),
    ifTrue(filter == PAH)(
      setWellDepth(DEEP)))
}
