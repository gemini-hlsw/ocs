package edu.gemini.pit.ui.view.scheduling

import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.util._
import edu.gemini.pit.model.Model
import scala.swing._
import edu.gemini.pit.ui._
import binding.BoundControls.BoundText
import javax.swing._
import edu.gemini.pit.ui.binding._
import com.jgoodies.forms.factories.Borders._

class SchedulingView(val shellAdvisor: ShellAdvisor) extends BorderPanel with BoundView[Proposal] {
  panel =>

  // Bound
  val lens = Model.proposal

  override def children = List(scheduling)

  // Configure content, defined below
  add(new GridBagPanel with Rows {
      border = DLU4_BORDER
    val MESSAGE = "<html><body style='width:100%'>Describe any timing constraints<br/> for queue programs and provide<br/> required dates for time-critical<br/> or synchronous observing and<br/> impossible dates for<br/> classical nights.</body><html>"
    addRow(new Label(MESSAGE), new ScrollPane(scheduling), GridBagPanel.Fill.Both, 100)
  }, BorderPanel.Position.Center)

  // Scheduling
  object scheduling extends TextArea with BoundText[Proposal] {
    val boundView = panel
    val lens = Proposal.scheduling
    rows = 5
    peer.setWrapStyleWord(true)
    peer.setLineWrap(true)
    border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  }

}