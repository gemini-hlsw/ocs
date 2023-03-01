package edu.gemini.pit.ui.view.obs

import edu.gemini.model.p1.immutable._
import edu.gemini.pit.model._
import edu.gemini.pit.ui.editor._
import edu.gemini.pit.ui.util.SharedIcons._
import edu.gemini.pit.ui.util._
import java.awt.datatransfer._
import javax.swing.TransferHandler._

import scala.swing._
import scalaz.Scalaz._
import scalaz.{ Band => _, _ }
import javax.swing.{Action => _, _}

import edu.gemini.gsa.client.api.GsaParams
import edu.gemini.gsa.client.impl.GsaUrl
import edu.gemini.pit.ui.CommonActions._
import java.awt.Color

import edu.gemini.pit.ui.{HackClipboard, ShellAdvisor}
import edu.gemini.pit.ui.binding._
import edu.gemini.pit.ui.robot.{AgsRobot, GsaRobot}
import edu.gemini.pit.ui.DataFlavors._
import edu.gemini.pit.ui.util.gface.SimpleListViewer
import edu.gemini.shared.gui.Browser

// The observation list/tree, which has two visible instances (Band 1/2 and Band 3). This is by far the most complex
// view, in particular because of drag/drop and copy/paste. The main structure we use here is an ObsListModel which is
// probably where to start when trying to figure this out.
class ObsListView(shellAdvisor:ShellAdvisor, band:Band) extends BorderPanel with BoundView[Proposal] {panel =>
  implicit val boolMonoid = Monoid.instance[Boolean](_ || _,  false)

  // HACK: there will be two instances of this view, each referencing the other. Because we can't construct this stuff
  // recursively we must set it after construction. No harm if it's not set; it just means we can't sync up the reorder
  // bars, which is a good usability hack because it makes copy/paste legal in general.
  var other: Option[ObsListView] = None

  // An indication of whether this tab is enabled or not, calculated in refresh()
  var tabEnabled = false

  // Bound
  val lens = Model.proposal
  override val children = List(toolbar, viewer, Fixes, viewer.transferHandler.PasteAction)

  // A lens we can use elsewhere to get an ObsListModel
  lazy val olmLens: Lens[Proposal, ObsListModel] = Lens.lensu(
    (a, b) => Proposal.observations.set(a, b.all),
    a => ObsListModel(a.observations, band, bar.reorder.gs, a.semester))

  // Our retarget actions (defined at the very bottom)
  override lazy val commonActions = Map(
    Cut -> viewer.transferHandler.CutAction,
    Copy -> viewer.transferHandler.CopyAction,
    Paste -> viewer.transferHandler.PasteAction)

  // When we get a new model, adjust the set of visible controls
  override def refresh(m:Option[Proposal]): Unit = {
    m.foreach {p =>

      tabEnabled = band == Band.BAND_1_2 || (p.proposalClass match {
        case q: QueueProposalClass         => q.band3request.isDefined
        case f: FastTurnaroundProgramClass => f.band3request.isDefined
        case s: SpecialProposalClass       => s.band3request.isDefined && s.sub.specialType == SpecialProposalType.GUARANTEED_TIME
        case _                    => false
      }) || p.observations.exists(o => o.band == Band.BAND_3 && o.nonEmpty)

      if (tabEnabled) {

        // Re-adding is a no-op, and replacing is automatic
        add(bar, BorderPanel.Position.North)
        add(viewer, BorderPanel.Position.Center)
        add(toolbar, BorderPanel.Position.South)

      } else {

        add(emptyLabel, BorderPanel.Position.North)
        add(disabledLabel, BorderPanel.Position.Center)
        add(emptyLabel, BorderPanel.Position.South)

      }

      // Force repaint
      peer.repaint()

    }
  }

  object emptyLabel extends Label

  object disabledLabel extends TextArea {
    enabled = false
    lineWrap = true
    wordWrap = true
    background = panel.background
    border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
    peer.setDisabledTextColor(Color.DARK_GRAY)
    text =
      "Band 3 time is available only for proposal class \"Queue Observing at Gemini\" and only if the \"Consider for " +
        "Band 3\" option is selected. You may edit these options on the \"Time Requests\" tab in the " +
        "view to the left."
  }

  // When a new obs is being created, this is the template we use. It is based on whatever is currently
  // selected in the viewer, and the current band preference.
  def templateObs = viewer.selection match {
    case None                 => Observation.empty.copy(band = band)
    case Some(ObsElem(o))     => o
    case Some(oe:ObsGroup[_]) => oe.toObs(band)
  }

  // Bar at the top, with the reorder doodad
  object bar extends BorderPanel {

    // A nonstandard border
    border = BorderFactory.createEmptyBorder(2, 4, 2, 2)

    // Our content, defined below
    add(label, BorderPanel.Position.West)
    add(reorder, BorderPanel.Position.Center)

    // A label
    object label extends Label("Group by:") {
      foreground = Color.DARK_GRAY
    }

    lazy val Hint = "Drag and drop the headers to reorder them."

    // A reorder bar, which shows the default ordering at first.
    object reorder extends ReorderBar(ObsListGrouping.default.productIterator.toSeq:_*)(Hint) {
      var gs = ObsListGrouping.default
      reactions += {
        case ReorderBar.Reorder(gr) => gr.asInstanceOf[Seq[ObsListGrouping[_]]] match {
          case Seq(a, b, c) =>
            gs = (a, b, c)
            other.foreach(_.bar.reorder.copyStateFrom(this))
            rebind()
        }
      }
    }

  }

  object toolbar extends BorderPanel with Bound.Self[Proposal] {

    override def children = List(tools, label)

    add(label, BorderPanel.Position.North)
    add(tools, BorderPanel.Position.Center)

    def sumOfBand3(p: Proposal, b1: Double): String = {
      val b3 = p.observations.filter(_.band == Band.BAND_3).flatMap(_.totalTime).map(_.hours).sum
      "Sum observation times: %3.2f hr | Sum Band 3 times: %3.2f hr".format(b1, b3)
    }

    object label extends Label with Bound[Proposal, List[Observation]] {
      val lens = Proposal.observations
      horizontalAlignment = Alignment.Left
      opaque = true
      foreground = Color.DARK_GRAY
      background = tools.background
      border = BorderFactory.createCompoundBorder(
        tools.border,
        BorderFactory.createEmptyBorder(2, 4, 2, 4))
      override def refresh(m:Option[List[Observation]]): Unit = {
        text = ~panel.model.map {p =>
          val b1 = p.observations.filter(_.band == Band.BAND_1_2).flatMap(_.totalTime).map(_.hours).sum
          p.proposalClass match {
            case q:QueueProposalClass if q.band3request.isDefined =>
              sumOfBand3(p, b1)
            case s:SpecialProposalClass if s.band3request.isDefined && s.sub.specialType == SpecialProposalType.GUARANTEED_TIME =>
              sumOfBand3(p, b1)
            case _ =>
              "Sum observation times: %3.2f hr".format(b1)
          }
        }
      }
    }

  }

  // Our toolbar
  object tools extends StdToolbar with Bound[Proposal, ObsListModel] {

    val lens = olmLens

    override def refresh(m:Option[ObsListModel]): Unit = {
      addBlueprint.enabled = canEdit
      addCondition.enabled = canEdit
      addTarget.enabled = canEdit
      addObservation.enabled = canEdit
    }

    add(addCondition)
    add(addBlueprint)
    add(addTarget)
    add(addObservation)
    addSeparator()
    add(delete)
    addFlexibleSpace()
    add(checkGsa)

    // Generic button for adding an obs component, using a lens and a thunk (which will be an editor invocation)
    private def addButton[A](icon:Icon, icon_dis:Icon, name:String, lens:Lens[Observation, Option[A]], f: Proposal => Option[A]) =
      ToolButton(icon, icon_dis, "Add %s".format(name)) {
        for (p <- panel.model; a <- f(p); m <- model)
          model = Some(m + lens.set(templateObs, Some(a)))
      }

    // Add buttons for obs components using the helper above
    lazy val addBlueprint = addButton(ICON_DEVICE, ICON_DEVICE_DIS, "Resource", Observation.blueprint, BlueprintEditor.open(_, None, canEdit, panel))
    lazy val addCondition = addButton(ICON_CONDS, ICON_CONDS_DIS, "Conditions", Observation.condition, p => ConditionEditor.open(None, canEdit, panel))

    // Add for target must be special-cased
    lazy val addTarget = ToolButton(ICON_SIDEREAL, ICON_SIDEREAL_DIS, "Add Target") {
      for {
        p            <- panel.model
        t            <- TargetEditor.open(p.semester, None, canEdit, panel)
      } {
        val p0 = Proposal.targets.mod(ts => t :: ts, p)
        val p1 = Proposal.observations.mod(os => templateObs.copy(target = Some(t)) :: os, p0)
        panel.model = Some(p1)
      }
    }

    // Add button for an observation, which doesn't quite work with the abstraction above
    lazy val addObservation = ToolButton(ICON_CLOCK, ICON_CLOCK_DIS, "Add Observation Time") {
      for {
        m <- model
        o <- ObservationEditor.open(Some(templateObs), canEdit, panel)
      } model = Some(m + o)
    }

    // A Delete button
    object delete extends ToolButton(REMOVE, REMOVE_DISABLED, "Delete") {

      enabled = false

      viewer.onSelectionChanged {s =>
        enabled = canEdit && ~(for (m <- model) yield {
            ~s.map {
              case ObsElem(o)    => o.calculatedTimes.isDefined
              case g:ObsGroup[_] => m.childrenOf(g).nonEmpty
            }
          })
      }

      def apply(): Unit = {
        for (m <- model; e <- viewer.selection)
          model = Some(m.cut(e))
      }
    }

    object checkGsa extends ToolButton(ICON_GSA, ICON_GSA_DIS, "Display the results of the archive search in browser") {
      enabled = false

      viewer.onSelectionChanged {s =>
        enabled = ~s.map {
          case ObsElem(o) => viewer.ready(o)
          case _          => false
        }
      }

      def apply(): Unit = {
        viewer.selection.flatMap {
          case ObsElem(o) => GsaParams.get(o).map(p => GsaUrl(p))
          case _          => None
        }.foreach {url =>
          try {
            Browser.open(url)
          } catch {
            case _:Exception => // Sorry
          }
        }
      }
    }

  }

  object viewer extends SimpleListViewer[Proposal, ObsListModel, ObsListElem] {list =>

    // Our lens
    val lens = olmLens

    // D&D support disablement if not editable
    override def refresh(m:Option[ObsListModel]): Unit = {
      super.refresh(m)
      this.viewer.getTable.setDragEnabled(canEdit)
    }

    // Repaint everything when the catalog or guide star state changes
    AgsRobot.addListener {_:Any => refresh()}
    GsaRobot.addListener {_:Any => refresh()}

    // A generic edit action for an obs group (used in the double-click handler)
    def edit[A](g:ObsGroup[A], f: Proposal => Option[A], lens:Lens[Observation, Option[A]]): Unit = {
      for (p <- panel.model; a <- f(p)) {
        val included = ~model.map(_.childrenOf(g))
        model = model.map(_.map {
          case o if included.contains(o) => lens.set(o, Some(a)).copy(meta = None) // TODO: verify guiding
          case o                         => o
        })
      }
    }

    // Method for handling the editing of the target group, since this is quite unique.
    def editTargetGroup(tg: TargetGroup): Unit = {

      // generic edit doesn't work for targets anymore; we have more work to do now.
      for {
        m            <- panel.model
        olm          <- model
        newT         <- TargetEditor.open(Semester.current, tg.t, canEdit, panel)
      } {

        val included = ~model.map(_.childrenOf(tg))

        val os = olm.map {
          case o if included.contains(o) => Observation.target.set(o, Some(newT)).copy(meta = None)
          case o                         => o
        }.all

        val ts = Proposal.targets.get(m)
        val p0 = Proposal.observations.set(m, os)
        val p1 = tg.t match {
          case None    => Proposal.targets.set(p0, newT :: ts)
          case Some(t) => ts.indexOf(t) match {
            case -1 => Proposal.targets.set(p0, newT :: ts)
            case n  => Proposal.targets.set(p0, ts.updated(n, newT))
          }
        }

        panel.model = Some(p1)
      }
    }


    // Our double-click handler for editing
    onDoubleClick {

      // Edit handlers for groups
      case cg:ConditionGroup => edit(cg, p => ConditionEditor.open(cg.c, canEdit, panel), Observation.condition)
      case bg:BlueprintGroup => edit(bg, BlueprintEditor.open(_, bg.b, canEdit, panel), Observation.blueprint)
      case tg:TargetGroup    => editTargetGroup(tg)

      // Edit an obs
      case ObsElem(o) =>
        for {
          m <- model
          o0 <- ObservationEditor.open(Some(o), canEdit, panel)
        } model = Some(m.replace(o, o0))

    }

    // N.B. compiler crashes here if viewer is a top-level object
    object columns extends Enumeration {
      val Item, Time, Guiding, Vis, GOA = Value
    }

    import columns._

    // One-liners
    def size(m:ObsListModel) = m.elems.length
    def elementAt(m:ObsListModel, i:Int) = m.elems(i)
    def empty(kind:String) = "«empty %s; double-click to edit»".format(kind)

    override def indent(e:ObsListElem) = {
      case Item => ~model.map(_.depth(e))
    }

    override def foreground(e:ObsListElem) = {
      case Item => e match {
        case ObsElem(o) if o.calculatedTimes.isEmpty => Color.LIGHT_GRAY
        case g:ObsGroup[_] if g.isEmpty              => Color.LIGHT_GRAY
        case _                                       => Color.BLACK
      }
    }

    def columnWidth = {
      case Item    => (200, Int.MaxValue)
      case Time    => (70, 70)
      case Guiding => (60, 60)
      case Vis     => (34, 34)
      case GOA     => (34, 34)
    }

    def ready(o:Observation):Boolean = model.exists {m => ready(m.sem, o)}

    def ready(s:Semester, o:Observation):Boolean =
      ~(for {
        _ <- o.condition
        b <- o.blueprint if !b.site.isExchange
        t <- o.target
        _ <- t.coords(s.midPoint)
        _ <- o.calculatedTimes
      } yield true)

    override def alignment(e:ObsListElem) = {
      case GOA => SwingConstants.CENTER
      case Vis => SwingConstants.CENTER
      case _   => SwingConstants.LEFT
    }

    import ObsPresentation.{Blank, guiding, visibility, gsa}

    def semPresentation[A](e:ObsListElem, f:(Semester, Observation) => ObsPresentation, g:ObsPresentation => A):A = g(e match {
      case ObsElem(o) if ready(o) => model.map(m => f(m.sem, o)).getOrElse(Blank)
      case _                      => Blank
    })

    def presentation[A](e:ObsListElem, f:Observation => ObsPresentation, g:ObsPresentation => A):A =
      semPresentation(e, (_, o) => f(o), g)

    override def tooltip(e:ObsListElem) = {
      case Guiding => presentation(e, guiding, _.tooltip)
      case Vis     => semPresentation(e, visibility, _.tooltip)
      case GOA     => presentation(e, gsa, _.tooltip)
    }

    val obsDisIcon = new CompositeIcon(SharedIcons.ICON_CLOCK_DIS, SharedIcons.OVL_ERROR)

    def icon(e:ObsListElem) = {
      case Item    => e match {
        case g:ObsGroup[_] => g.icon
        case ObsElem(o)    => if (o.calculatedTimes.isEmpty) obsDisIcon else ICON_CLOCK
      }
      case Guiding => presentation(e, guiding, _.icon)
      case Vis     => semPresentation(e, visibility, _.icon)
      case GOA     => presentation(e, gsa, _.icon)
    }

    def text(e:ObsListElem) = {
      case Item    => e match {
        case ObsElem(o) if o.calculatedTimes.isDefined => "Observation"
        case ObsElem(o)                                => empty("observation time")
        case e:ObsGroup[_] if e.isEmpty                => e.grouping match {
          case ObsListGrouping.Target    => empty("target")
          case ObsListGrouping.Blueprint => empty("resource configuration")
          case ObsListGrouping.Condition => empty("observing conditions")
        }
        case e:ObsGroup[_]                  => e.text.get
        case _                              => null
      }
      case Time    => e match {
        case ObsElem(o) if o.totalTime.isDefined => "%3.2f %s".format(o.totalTime.get.value, o.totalTime.get.units)
        case _                                   => null
      }
      case Guiding => presentation(e, guiding, _.text)
    }

    ///
    /// DND Support
    ///

    val myViewer = this.viewer

    import myViewer._

    {
      val t = myViewer.getTable
      t.setDragEnabled(true)
      t.setDropMode(DropMode.ON)
      t.setTransferHandler(transferHandler)
    }

    // Our transfer handler manages the business of DND, which is easier than using raw AWT but has some limitations
    // that might end up being unacceptable.
    object transferHandler extends TransferHandler {

      // Get the target element where the paste/drop will happen.
      private def targetGroup(ts:TransferSupport):Option[ObsGroup[_]] = {
        val target = if (ts.isDrop) Option(getElementAt(ts.getDropLocation.getDropPoint)) else list.selection
        target match {
          case Some(g:ObsGroup[_]) => Some(g)
          case _                   => None
        }
      }

      private def sourceObs(ts:TransferSupport):Option[(ObsListModel, List[ObsListElem])] = sourceObs(ts.getTransferable)

      // Get the source observations from the clipboard.
      private def sourceObs(tr:Transferable):Option[(ObsListModel, List[ObsListElem])] = try {

        // Could be an (ObsListModel, ObsListElem) or a Target
        val o0:Option[(ObsListModel, List[ObsListElem])] = for {
          m <- model // just to get us into Option
          if tr.isDataFlavorSupported(ObsListElemFlavor)
          t = tr.getTransferData(ObsListElemFlavor)
          t0 = t.asInstanceOf[(ObsListModel, ObsListElem)]
        } yield (t0._1, List(t0._2))

        val o1:Option[(ObsListModel, List[ObsListElem])] = for {
          m <- model
          if tr.isDataFlavorSupported(TargetFlavor)
          t = tr.getTransferData(TargetFlavor).asInstanceOf[Target]
          o = Observation.empty.copy(target = Some(t), band = m.band)
        } yield (m + o, List(TargetGroup(None, None, Some(t))))

        val o2:Option[(ObsListModel, List[ObsListElem])] = for {
          m <- model
          if tr.isDataFlavorSupported(TargetListFlavor)
          ts = tr.getTransferData(TargetListFlavor).asInstanceOf[List[Target]]
          os = ts.map(t => Observation.empty.copy(target = Some(t), band = m.band))
          gs = ts.map(t => TargetGroup(None, None, Some(t)))
        } yield (m ++ os, gs)

        o0 orElse o1 orElse o2

      } catch {
        case _:UnsupportedFlavorException => None
      }


      // The selected item may or may not be draggable, but if it is we can either move or copy
      override def getSourceActions(c: JComponent): Int = selection match {
        case _ if !canEdit => NONE
        case Some(e) if ~model.map(_.isDraggable(e)) => COPY_OR_MOVE
        case Some(_)                                 => COPY
        case _                                       => NONE
      }

      // Our selection is a transferable automatically, which is nice
      override def createTransferable(c:JComponent) = {
//        println("createTransferable called")
        TransferableObsListElem(for {
          m <- model
          s <- selection
        } yield (m, s))
      }

      // We can accept the drag if the model says it makes sense
      override def canImport(ts:TransferSupport):Boolean = {
//        println("canImport called")
        canEdit && ~(for {
          target <- targetGroup(ts)
          source <- sourceObs(ts)
          m <- model
        } yield {
          m.isDroppable(source, target)
        })
      }

      // When we're importing, if it's a drag/drop then the target has to be valid. If we're pasting and the target is
      // valid (the target is the current selection when pasting) then we insert stuff carefully, otherwise we just
      // append it to the end. This lets us paste top-level nodes and handles the case where the grouping has changed
      // between copy and paste. The user can drag stuff around to clean it up as needed.
      override def importData(ts:TransferSupport):Boolean = {
//        println("importData called")

        val isPaste = (!ts.isDrop) || ts.getDropAction == COPY
        val t = targetGroup(ts)

        ~(for {
          m <- model
          s <- sourceObs(ts)
        } yield {
          if (isPaste) {
            model = Some(m.paste(s, t))
            true
          } else {
            ~(for  {
              t <- t
              if s._2.length == 1 // should always be true for move
              m0 <- m.move((s._1, s._2.head), t)
            } yield {
              model = Some(m0)
              true
            })
          }
        })

      }

      // A cut action in our case is copy followed by a direct model manipulation; we can't delegate to the transfer
      // handler because we don't want to model drag/drop as cut/paste; it mucks up our undo state.
      object CutAction extends Action("Cut - Ignored") {
        enabled = false
        onSelectionChanged(sel => enabled = canEdit && sel.isDefined)
        def apply(): Unit = {
          for {
            m <- model
            e <- selection
          } {
            CopyAction()
            model = Some(m.cut(e))
          }
        }
      }

      // A copy action
      object CopyAction extends Action("Copy - Ignored") {
        enabled = false
        onSelectionChanged(sel => enabled = sel.isDefined)
        def apply(): Unit = {
          transferHandler.exportToClipboard(myViewer.getTable, HackClipboard, COPY)
        }
      }

      // A paste action
      object PasteAction extends Action("Paste - Ignored") with FlavorListener with Bound.Self[Proposal] {

        // Config
        enabled = false
        HackClipboard.addFlavorListener(this)

        // Create a transfer support from the current clipboard. The default TransferSupport is not a drop (good).
        private def ts = new TransferSupport(myViewer.getTable, HackClipboard.getContents(null))

        // React to clipboard events
        def flavorsChanged(e:FlavorEvent): Unit = {
          updateEnabledState()
        }

        override def refresh(m:Option[Proposal]): Unit = {
          updateEnabledState()
        }

        def updateEnabledState(): Unit = {
          enabled = canEdit && tabEnabled &&
            (HackClipboard.getAvailableDataFlavors.contains(TargetFlavor) ||
              HackClipboard.getAvailableDataFlavors.contains(TargetListFlavor) ||
              HackClipboard.getAvailableDataFlavors.contains(ObsListElemFlavor))
        }

        def apply(): Unit = {
            importData(ts)
        }
      }
    }
  }

  object Fixes extends Bound[Proposal, ObsListModel] {

    val lens = olmLens

    // General fix-it method.
    def fixer(findAndProcess: ObsListElem => Boolean): Unit = {
      model.foreach(_.elems.find(findAndProcess))
    }

    // Public method for fix-its
    def fixEmpty[A](grouping:ObsListGrouping[A]) = fixer {
      case g: ObsGroup[_] if (g.grouping == grouping) && g.grouping.get(g).isEmpty =>
        viewer.selection = Some(g)
        true
      case _ => false
    }

    // Add missing conditions.
    def addConditions() = fixer {
      case g@ConditionGroup(None, _, _) =>
        viewer.edit(g, p => ConditionEditor.open(None, canEdit, panel), Observation.condition)
        viewer.selection = Some(g)
        true
      case _ => false
    }

    // Edit the conditions
    def fixConditions(c: Condition) = fixer {
      case g@ConditionGroup(Some(gc), _, _) if c == gc =>
        viewer.edit(g, p => ConditionEditor.open(Some(c), canEdit, panel), Observation.condition)
        viewer.selection = Some(g)
        true
      case _ => false
    }

    // Add a missing blueprint.
    def addBlueprint() = fixer {
      case g@BlueprintGroup(_, None, _) =>
        viewer.edit(g, p => BlueprintEditor.open(p, None, canEdit, panel), Observation.blueprint)
        viewer.selection = Some(g)
        true
      case _ => false
    }

    // Edit the blueprint
    def fixBlueprint(b: BlueprintBase) = fixer {
      case g@BlueprintGroup(_, Some(gb), _) if b == gb =>
        viewer.edit(g, p => BlueprintEditor.open(p, b.some, canEdit, panel), Observation.blueprint)
        viewer.selection = Some(g)
        true
      case _ => false
    }

    // Add a missing target.
    def addTarget() = fixer {
      case g@TargetGroup(_, _, None) =>
        viewer.editTargetGroup(g)
        viewer.selection = Some(g)
        true
      case _ => false
    }

    // Edit the target
    def fixTarget(t: Target) = fixer {
      case g@TargetGroup(_, _, Some(gt)) if t == gt =>
        viewer.edit(g, p => TargetEditor.open(p.semester, t.some, canEdit, panel), Observation.target)
        viewer.selection = Some(g)
        true
      case _ => false
    }

    def indicateObservation(o:Observation) = fixer {
      case e: ObsElem if e.o == o =>
        viewer.selection = Some(e)
        true
      case _ => false
    }
  }
}
