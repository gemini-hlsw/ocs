package jsky.app.ot.gemini.editor.targetComponent

import java.awt.Component

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.OT
import jsky.app.ot.ags.BagsManager
import jsky.app.ot.gemini.editor.targetComponent.GuidingFeedback.BagsStatusRow


class GuidingFeedbackEditor extends TelescopePosEditor {
  private val tab: GuidingFeedback.Table = new GuidingFeedback.Table
  def getComponent: Component = tab.peer

  override def edit(ctxOpt: GOption[ObsContext], target: SPTarget, node: ISPNode): Unit = {
    val mt = OT.getMagnitudeTable

    // Construct the rows for the table. Optionally a BAGS row, and then a list of AGS analysis rows.
    val rows = {
      // A lot of nulls can happen here, unfortunately, so proceed with caution.
      val bagsRow = for {
        n <- Option(node)
        k <- n.getNodeKey
      } yield BagsManager.instance.bagsStatus(k).map(BagsStatusRow)

      val feedbackRows = ctxOpt.asScalaOpt.map(GuidingFeedback.targetAnalysis(_, mt, target)).getOrElse(Nil)
      bagsRow.fold(feedbackRows)(_ :: feedbackRows)
    }
    if (rows.isEmpty)
      tab.clear()
    else
      tab.showRows(rows)
  }
}
