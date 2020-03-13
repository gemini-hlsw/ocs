package jsky.app.ot.tpe.feat

import java.awt.{AlphaComposite, Composite}

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.ghost.Ghost
import jsky.app.ot.tpe.{TpeContext, TpeDragSensitive, TpeImageFeature}
import jsky.app.ot.util.{BasicPropertyList, OtColor}

import scala.swing.Color

final class TpeGhostIFUFeature extends TpeImageFeature("GHOST", "Show the GHOST IFUs") with TpeDragSensitive {
  override def isEnabled(ctx: TpeContext): Boolean =
    super.isEnabled(ctx) && ctx.instrument.is(SPComponentType.INSTRUMENT_GHOST)

  // Property to control drawing of the IFU ranges.

}

object TpeGhostIFUFeature {
  private val PropShowRanges: String = "Show IFUs and ranges"
  private val properties: BasicPropertyList = new BasicPropertyList(Ghost.getClass.getName)
  properties.registerBooleanProperty(PropShowRanges, true)

  // The values used to render the probe ranges
  private val ProbeRangeColor: Color = OtColor.SALMON
  private val Blocked: Composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, )
}