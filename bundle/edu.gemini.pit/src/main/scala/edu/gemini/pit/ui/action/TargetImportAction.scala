package edu.gemini.pit.ui.action

import edu.gemini.pit.model.Model
import edu.gemini.pit.ui.binding._
import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.ui.editor.TargetImporter
import swing.{UIElement, Action}
import edu.gemini.pit.ui.util.SharedIcons
import edu.gemini.model.p1.immutable.Proposal

class TargetImportAction(shell:RichShell[Model], showTargets: => Unit) extends Action("Import Targets...") with Bound.Self[Model] {

  // Configure
  enabled = false
  icon = SharedIcons.ICON_ATTACH

  // Bound
  val targetLens = Model.proposal andThen Proposal.targets

  // Refresh
  override def refresh(mdl:Option[Model]) {
    enabled = mdl exists { m => m.proposal.proposalClass.key.isEmpty }
  }

  // Apply
  def apply() {
    for {
      m  <- model
      toAdd <- TargetImporter.open(UIElement.wrap(shell.peer))
    } {
      model = Some(targetLens.set(m, m.proposal.targets ++ toAdd))
      showTargets
    }
  }

}