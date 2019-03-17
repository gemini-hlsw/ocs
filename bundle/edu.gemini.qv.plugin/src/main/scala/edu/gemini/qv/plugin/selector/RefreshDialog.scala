package edu.gemini.qv.plugin.selector

import javax.swing.BorderFactory
import javax.swing.border.EtchedBorder

import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.qv.plugin.ui.QvGui.Instructions
import edu.gemini.qv.plugin.util.SolutionProvider
import edu.gemini.qv.plugin.{QvContext, QvToolMenu}
import edu.gemini.spModel.core.{ProgramType, Semester}
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obsclass.ObsClass

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.GridBagPanel.Anchor
import scala.swing.GridBagPanel.Fill._
import scala.swing._

/**
 */
class RefreshDialog(owner: Window, ctx: QvContext) extends Dialog(owner) {

  title = "QV Data Selection and Reload"
  preferredSize = new Dimension(900, 400)
  modal = true


  pack()
  centerOnScreen()

  // ===== ACTIONS

  val clearCacheAction = Action("Clear Cache") {
    Future {
      SolutionProvider(ctx).clear()
      Runtime.getRuntime.gc()
    }.onComplete({
      case _ => Swing.onEDT(memoryBar.update())
    })
  }

  val cancelAction = Action("Cancel") {
    dispose()
  }

  val refreshAction = new QvToolMenu.RefreshAction("Load", ctx, prepare)

  private def prepare(): Unit = {
    ctx.dataSource.selectedSemesters = semestersPanel.selection
    ctx.dataSource.selectedTypes = progTypesPanel.selection
    ctx.dataSource.selectedClasses = obsClassesPanel.selection
    ctx.dataSource.selectedStatuses = obsStatusPanel.selection
    ctx.dataSource.includeCompletedPrograms = includeCompletedOption.selected
    ctx.dataSource.includeInactivePrograms = includeInactiveOption.selected
    Swing.onEDT(close())
  }


  // ==== UI


  object panel extends GridBagPanel {

    border = BorderFactory.createEmptyBorder(0, 8, 0, 8)

    layout(new Label("", QvGui.QvIcon, Alignment.Center)) = new Constraints {
      gridx = 0; gridy = 0
      anchor = Anchor.North
    }
    layout(instructions) = new Constraints {
      gridx = 1; gridy = 0
      gridwidth = 3
      weightx = 0.75
      fill = Horizontal
    }
    layout(semestersPanel) = new Constraints {
      gridx = 0; gridy = 1
      weightx = 0.25
      fill = Both
    }
    layout(progTypesPanel) = new Constraints {
      gridx = 1; gridy = 1
      weightx = 0.25
      fill = Both
    }
    layout(obsStatusPanel) = new Constraints {
      gridx = 2; gridy = 1
      weightx = 0.25
      fill = Both
    }
    layout(obsClassesPanel) = new Constraints {
      gridx = 3; gridy = 1
      weightx = 0.25
      fill = Both
    }
    layout(Swing.VStrut(5)) = new Constraints {
      gridx = 0; gridy = 2
      weightx = 1
      fill = Horizontal
    }
    layout(optionsPanel) = new Constraints {
      gridx = 0; gridy = 3
      gridwidth = 4
      weightx = 1
      fill = Horizontal
    }
    layout(Swing.VStrut(5)) = new Constraints {
      gridx = 0; gridy = 4
      weightx = 1
      fill = Horizontal
    }
    layout(buttonPanel) = new Constraints {
      gridx = 0; gridy = 5
      gridwidth = 4
      weightx = 1
      fill = Horizontal
    }
  }

  contents = panel


  object instructions extends Instructions {
    text =
      """Select which types of observations from which semesters should be loaded.
        |
        |Keep in mind that more observations also mean more memory consumption and CPU usage. Currently there is no
        |support for "invalid" observations without instruments, conditions and/or targets; this includes all daytime calibrations.
      """.stripMargin
  }

  object optionsPanel extends BoxPanel(Orientation.Horizontal) {
    contents += includeCompletedOption
    contents += Swing.HStrut(10)
    contents += includeInactiveOption
  }

  object buttonPanel extends BoxPanel(Orientation.Horizontal) {
    tooltip = "The total number of observations that are currently loaded in QV."
    contents += new Label(s"${ctx.dataSource.observations.size} Observations loaded")
    contents += Swing.HStrut(20)
//    contents += clearCacheBtn
//    contents += Swing.HStrut(5)
    contents += new Label("Memory usage: ")
    contents += memoryBar
    contents += Swing.HStrut(20)
    contents += refreshBtn
    contents += Swing.HStrut(5)
    contents += cancelBtn
  }

  object includeCompletedOption extends CheckBox("Include Completed Programs") {
    tooltip = "Check if you want to incldue observations from programs that are marked as completed."
    selected = ctx.dataSource.includeCompletedPrograms
  }

  object includeInactiveOption extends CheckBox("Include Inactive Programs") {
    tooltip = "Check if you want to incldue observations from programs that are marked as inactive."
    selected = ctx.dataSource.includeInactivePrograms
  }

  object clearCacheBtn extends Button(clearCacheAction) {
    focusable = false
  }

  object cancelBtn extends Button(cancelAction) {
    focusable = false
  }

  object refreshBtn extends Button(refreshAction) {
    focusable = false
  }

  object memoryBar extends ProgressBar {
    tooltip = "The memory currently used in relation to the maximum amount of memory the application has available."
    labelPainted = true
    update()
    def update() {
      min = 0
      max = (Runtime.getRuntime.maxMemory() / 1024 / 1024).toInt
      value = ((Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / 1024 / 1024).toInt
      label = f"$value%.0f / $max%.0f MB"
    }
  }

  object semestersPanel extends SelectPanel[Semester] (
    "Semesters",
    ctx.dataSource.availableSemesters.toSeq.sorted,
    ctx.dataSource.selectedSemesters,
    _.toString
  )

  object progTypesPanel extends SelectPanel[ProgramType] (
    "Program Types",
    ctx.dataSource.availableTypes.toSeq.sortBy(_.abbreviation),
    ctx.dataSource.selectedTypes,
    t => s"${t.name} (${t.abbreviation})"
  )

  object obsStatusPanel extends SelectPanel[ObservationStatus](
    "Observation Statuses",
    ctx.dataSource.availableStatuses.toSeq.sortBy(_.ordinal),
    ctx.dataSource.selectedStatuses,
    _.displayValue()
  )

  object obsClassesPanel extends SelectPanel[ObsClass](
    "Observation Classes",
    ctx.dataSource.availableClasses.toSeq.sortBy(_.ordinal),
    ctx.dataSource.selectedClasses,
    _.displayValue())

  class SelectPanel[A](label: String, values: Seq[A], selectedValues: Set[A], toLabel: A => String) extends BoxPanel(Orientation.Vertical) {
    border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), label)
    values.map(v => {
      contents += new SelectBox[A](v, toLabel(v)) {
        selected = selectedValues.contains(v)
      }
    })
    contents += Swing.VGlue

    def selection: Set[A] = contents.map({
      case c: SelectBox[A] if c.selected => Some(c.value)
      case _ => scala.None
    }).flatten.toSet

  }

  class SelectBox[A](val value: A, label: String) extends CheckBox(label)

}
