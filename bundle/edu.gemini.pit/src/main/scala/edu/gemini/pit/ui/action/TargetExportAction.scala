package edu.gemini.pit.ui.action

import edu.gemini.pit.model.Model
import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.ui.binding._
import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.editor.TargetExporter
import swing.{UIElement, Action}

class TargetExportAction(shell: RichShell[Model]) extends Action("Export Targets...") with Bound[Model, List[Target]] {

  enabled = false

  def lens = Model.proposal andThen Proposal.targets

  override def refresh(m: Option[List[Target]]) {
    // enable when we actually have targets and at least one has data
    enabled = m exists { _.exists(TargetExporter.isExportable) }
  }

  def apply() {
    model foreach { targets => TargetExporter.open(UIElement.wrap(shell.peer), targets) }
  }
}
