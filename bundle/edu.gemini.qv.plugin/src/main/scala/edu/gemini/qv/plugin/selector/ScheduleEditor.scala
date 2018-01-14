package edu.gemini.qv.plugin.selector

import java.time.Instant

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.qv.plugin.{QvContext, QvTool}
import edu.gemini.qv.plugin.ui.QvGui.Instructions
import edu.gemini.qv.plugin.ui.{CalendarDialog, QvGui}
import edu.gemini.qv.plugin.util.ScheduleCache._
import edu.gemini.qv.plugin.util.{ScheduleCache, SolutionProvider}
import edu.gemini.services.client.TelescopeSchedule.Constraint
import edu.gemini.services.client._
import edu.gemini.shared.gui.monthview.DateSelectionMode
import edu.gemini.spModel.core.ProgramId
import edu.gemini.spModel.gemini.inst.InstRegistry
import edu.gemini.util.skycalc.calc.Interval
import javax.swing.BorderFactory

import edu.gemini.shared.util.{DateTimeFormatters, DateTimeUtils}

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.GridBagPanel.Anchor._
import scala.swing.GridBagPanel.Fill._
import scala.swing.ListView.Renderer
import scala.swing._
import scala.swing.event.SelectionChanged
import scala.util.Failure
import scala.util.Success

/**
 * Editor UI that allows to add, delete and edit schedule constraints in the calendar.
 */
class ScheduleEditor(ctx: QvContext, scheduleCache: ScheduleCache) extends Dialog {

  var schedule = SolutionProvider(ctx).telescopeSchedule

  preferredSize = new Dimension(400, 300)
  modal = true

  object instructions extends Instructions {
    text =
      s"""Add or delete new external constraints.
        |All times are local times, ${ctx.site.timezone().getDisplayName}.
      """.stripMargin
  }
  object constraints extends ComboBox[ScheduleCache.ScheduleConstraint](ScheduleCache.Constraints.toSeq.sortBy(_.label)) {
    renderer = Renderer(_.label)
  }
  object instruments extends ComboBox[SPComponentType](InstRegistry.instance.types().toSeq.sortBy(_.readableStr)) {
    visible = false
    renderer = Renderer(_.readableStr)
  }

  val programIds =
    ctx.source.observations.
      map(_.getProg.getProgramId).                            // get SPProgramIDs from observations
      toSeq.sortWith((id1, id2) => id1.compareTo(id2) < 0).   // use the SPProgramID for sorting
      map(id => ProgramId.parse(id.stringValue))              // then translate to ProgramId (this should always be a StandardProgramId)
  
  object programs extends ComboBox[ProgramId](programIds)

  object dates extends GridBagPanel
  object scroll extends ScrollPane(dates)
  object addBtn extends Button(
    new Action("New Constraint") {
      icon = QvGui.AddIcon
      def apply() = newConstraint() })
    {
      focusable = false
    }
  object okBtn extends Button(Action("OK") { close(); scheduleCache.cacheSchedule(schedule, ctx.range) }) {focusable = false}


  val panel = new GridBagPanel {
    border = BorderFactory.createEmptyBorder(5,5,5,5)

    layout(new Label("", QvGui.CalendarIcon, Alignment.Center) {
      border = BorderFactory.createEmptyBorder(10,5,5,5)
    }) = new Constraints {
      gridx = 0
      gridy = 0
      anchor = North
    }
    layout(instructions) = new Constraints {
      gridx = 1
      gridy = 0
      weightx = 0.75
      fill = Horizontal
    }
    layout(constraints) = new Constraints {
      gridx = 0
      gridy = 1
      gridwidth = 2
      weightx = 1
      fill = Horizontal
    }
    layout(instruments) = new Constraints {
      gridx = 0
      gridy = 2
      gridwidth = 2
      weightx = 1
      fill = Horizontal
    }
    layout(programs) = new Constraints {
      gridx = 0
      gridy = 3
      gridwidth = 2
      weightx = 1
      fill = Horizontal
    }
    layout(Swing.VStrut(10)) = new Constraints {
      gridx = 0
      gridy = 4
    }
    layout(scroll) = new Constraints {
      gridx = 0
      gridy = 5
      gridwidth = 2
      weightx = 1
      weighty = 1
      fill = Both
    }
    layout(Swing.VStrut(10)) = new Constraints {
      gridx = 0
      gridy = 6
    }
    layout(addBtn) = new Constraints {
      gridx = 0
      gridy = 7
      gridwidth = 2
      fill = Horizontal
    }
    layout(Swing.VStrut(10)) = new Constraints {
      gridx = 0
      gridy = 8
    }
    layout(okBtn) = new Constraints {
      gridx = 0
      gridy = 9
      gridwidth = 2
      anchor = LineEnd
    }
  }

  contents = panel

  update()

  listenTo(constraints.selection, instruments.selection, programs.selection)
  reactions += {
    case SelectionChanged(_) => update()
  }

  def reloadSchedule(): Unit = {

    Swing.onEDT {
      instruments.enabled = false
      programs.enabled = false
      addBtn.enabled = false
      panel.enabled = false
    }

    TelescopeScheduleClient.getSchedule(QvTool.authClient, ctx.peer, ctx.range).
      andThen {
        case _ => Swing.onEDT {
          instruments.enabled = true
          programs.enabled = true
          addBtn.enabled = true
          panel.enabled = true
          panel.revalidate()
        }
      } onComplete {
        case Success(s) => Swing.onEDT {
          schedule = s
          update()
        }
        case Failure(t) => QvGui.showError("could not update", "could not update", t)
      }

  }

  def update(): Unit = {
    instruments.visible = constraints.selection.item == InstrumentConstraint
    programs.visible    = constraints.selection.item == ProgramConstraint
    addBtn.visible      = constraints.selection.item != LaserConstraint
    scroll.contents     = constraints.selection.item match {
      case InstrumentConstraint     =>
        val constraints = schedule.instrumentSchedule(instruments.selection.item).map(_.constraints).getOrElse(Seq())
        constraintsPanel(constraints)
      case ProgramConstraint        =>
        val constraints = schedule.programSchedule(programs.selection.item).map(_.constraints).getOrElse(Seq())
        constraintsPanel(constraints)
      case LaserConstraint => constraintsPanel(schedule.laserSchedule.constraints)
      case ShutdownConstraint       => constraintsPanel(schedule.shutdownSchedule.constraints)
      case EngineeringConstraint    => constraintsPanel(schedule.engineeringSchedule.constraints)
      case WeatherConstraint        => constraintsPanel(schedule.weatherSchedule.constraints)
    }
  }

  def constraintsPanel(constraints: Seq[Constraint]) = new GridBagPanel {
    constraints.zipWithIndex.foreach({case (i, y) =>
      val formatter = DateTimeFormatters(ctx.site.timezone).YYYY_MMM_DD_HHMMSS
      val s = formatter.format(Instant.ofEpochMilli(i.start))
      val e = formatter.format(Instant.ofEpochMilli(i.end))
      val l = new TextArea(s"$s - $e")
      val del = new Button( new Action("") {
        icon = QvGui.DelIcon
        def apply() = delConstraint(i)
      })
      layout(l) = new Constraints { gridx=0; gridy=y; weightx=1.0; fill=Horizontal }
      layout(del) = new Constraints { gridx=1; gridy=y }
      del.visible = !i.isInstanceOf[TelescopeSchedule.LaserConstraint]
    })
    layout(Swing.HGlue) = new Constraints { gridx=0; gridy=constraints.size; gridwidth=3; weighty=1.0; fill=Both }
  }

  def addConstraint(constraint: Constraint) = {
    TelescopeScheduleClient.addConstraint(QvTool.authClient, ctx.peer, constraint).
      onComplete({
        case Success(_) => reloadSchedule()
        case Failure(t) => QvGui.showError("Error Adding Constraint", "Could not add constraint.", t); reloadSchedule()
      })
  }

  def delConstraint(constraint: Constraint) = {
    TelescopeScheduleClient.deleteConstraint(QvTool.authClient, ctx.peer, constraint).
      onComplete({
        case Success(_) => reloadSchedule()
        case Failure(t) => QvGui.showError("Error Deleting Constraint", "Could not delete constraint.", t); reloadSchedule()
      })
  }

  def createConstraint(i: Interval) = constraints.selection.item match {
      case InstrumentConstraint   => TelescopeSchedule.InstrumentConstraint(instruments.selection.item, i)
      case ProgramConstraint      => TelescopeSchedule.ProgramConstraint(programs.selection.item, i)
      case LaserConstraint        => TelescopeSchedule.LaserConstraint(i)
      case WeatherConstraint      => TelescopeSchedule.WeatherConstraint(i)
      case ShutdownConstraint     => TelescopeSchedule.ShutdownConstraint(i)
      case EngineeringConstraint  => TelescopeSchedule.EngineeringConstraint(i)
    }

  def newConstraint() {

    val cd = new CalendarDialog("Add External Constraint", mode = DateSelectionMode.Interval, numMonthsToShow = 6, rows = 2)
    cd.pack()
    cd.setLocationRelativeTo(ScheduleEditor.this)
    cd.visible = true
    for {
      start <- cd.startDate
      end <- cd.endDate
    } yield {
      val s = start + DateTimeUtils.StartOfDayHour.hours.toMillis
      val e = end   + DateTimeUtils.StartOfDayHour.hours.toMillis + 2.days.toMillis
      addConstraint(createConstraint(Interval(s, e)))
    }
  }

}
