package edu.gemini.pit.ui.view.target

import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.util._
import edu.gemini.pit.model.Model
import edu.gemini.pit.catalog._
import edu.gemini.spModel.core.Coordinates
import scala.swing._
import scalaz._
import Scalaz._
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.TransferHandler._
import edu.gemini.pit.ui._
import CommonActions._
import editor.{TargetExporter, TargetImporter, TargetEditor}
import javax.swing.{JOptionPane, JComponent, TransferHandler, SwingConstants}
import edu.gemini.pit.ui.binding._
import DataFlavors._
import edu.gemini.pit.ui.util.gface.{MultiSelectSimpleListViewer, SortableHeaders}

class TargetView(val shellAdvisor:ShellAdvisor) extends BorderPanel with BoundView[Model] {panel =>

  // Bound
  val lens = Lens.lensId[Model]
  override def children = List(listView, toolbar)

  // Our retarget actions (defined at the very bottom).
  // For now the only thing you can do is copy targets (which can be pasted into the Obs view)
  override lazy val commonActions = Map(
    Copy -> listView.transferHandler.CopyAction)

  // Some lenses
  val targetLens = Model.proposal andThen Proposal.targets
  val semesterLens = Model.proposal andThen Proposal.semester

  // Some model slices
  def semester = model.map(semesterLens.get).getOrElse(sys.error("No model"))

  // Configure content, defined below
  add(listView, BorderPanel.Position.Center)
  add(toolbar, BorderPanel.Position.South)

  // We need to do this here (rather than in the delete buttons itself) to avoid a stack overlow on init
  listView.onSelectionChanged {s => toolbar.delete.enabled = canEdit && s.nonEmpty }

  // Public edit method (called from quick-fixes)
  def edit(t:Target) {
    for {
      m            <- model
      t0           <- TargetEditor.open(semesterLens.get(m), Some(t), canEdit, panel)
    } {
      val ts = targetLens.get(m)
      val i = ts.indexOf(t)
      if (i >= 0) {
        // this had better be true
        val ts0 = ts.updated(i, t0)
        model = Some(targetLens.set(m, ts0))
      }
    }
  }

  // Our list
  object listView extends MultiSelectSimpleListViewer[Model, Model, Target] with SortableHeaders[Model,Target] {

    // Bound
    val lens = Lens.lensId[Model]

    // Model slices
    def targets:List[Target] = model.map(targetLens.get).getOrElse(Nil)

    // One-liners
    def elementAt(m:Model, i:Int) = targets(i)
    def size(m:Model) = targets.size

    object columns extends Enumeration {
      val Name, RA, Dec, PM, /* Epoch, */ Magnitudes, Query = Value
    }
    import columns._

    // Sort
    def compare(t1:Target, t2:Target, c:Column) = ~model.map { m =>

      // Abstract a few things out (used below)
      // TODO: continue abstraction; duplicated logic in places below
      lazy val date = semesterLens.get(m).midPoint
      def coord(t:Target, f:Coordinates => BigDecimal): Double = t.coords(date).map(c => f(c).toDouble) | 0.0
      def ra(t:Target) = coord(t, c => BigDecimal(c.ra.toAngle.toDegrees))
      def dec(t:Target) = coord(t, c => BigDecimal(c.dec.toDegrees))
      def txt(t:Target) = ~text(t).lift(c)

      // Probably a better way to do this
      def cmp[A](f: Target => A)(implicit o:scala.Ordering[A]):Int = o.compare(f(t1), f(t2))

      c match {
        case RA  => cmp(ra)
        case Dec => cmp(dec)
        case _   => cmp(txt)
      }

    }


    def columnWidth = {
      case Name => (175, Integer.MAX_VALUE)
      case RA   => (90, 90)
      case Dec  => (90, 90)
      case PM   => (25, 25)
      case Magnitudes => (100, Integer.MAX_VALUE)
      case Query => (100, Integer.MAX_VALUE)
    }

    def text(e:Target) =
      if (!e.isEmpty) {
        case Name => e.name
        case RA   => e.coords(semester.midPoint).map(_.ra).map(raFormat.toString).orNull
        case Dec  => e.coords(semester.midPoint).map(_.dec).map(decFormat.toString).orNull
        case PM   => e match {
          case SiderealTarget(_, _, _, _, Some(_), _) => "*"
          case _                                      => null
        }
        //        case Epoch      => e.epoch.toString
        case Magnitudes => e match {
          case t:SiderealTarget    => t.magnitudes.map(_.band.name).sorted.mkString(" ")
          case t:NonSiderealTarget => t.magnitude(semester.midPoint).map(d => "*").orNull
          case t:TooTarget         => null
        }
        case Query => e match {
          case t:NonSiderealTarget => t.horizonsQuery.orNull
          case _                   => null
        }
      } else {
        case Name => e.name
      }

    def icon(i:Target) = {
      case Name => ok(i)
    }

    override def alignment(i:Target) = {
      case RA  => SwingConstants.RIGHT
      case Dec => SwingConstants.RIGHT
      case PM => SwingConstants.CENTER
    }

    DegreePreference.BOX.addPropertyChangeListener(new PropertyChangeListener {
      def propertyChange(pce:PropertyChangeEvent) {
        refresh()
      }
    })

    private def raFormat = DegreePreference.BOX.get match {
      case DegreePreference.DEGREES => DegreeFormatter.ra("")
      case DegreePreference.HMSDMS  => new HMSFormatter("")
    }

    private def decFormat = DegreePreference.BOX.get match {
      case DegreePreference.DEGREES => DegreeFormatter.dec("")
      case DegreePreference.HMSDMS  => new DecFormatter("")
    }

    def ok(t:Target) = t match {
      case _:SiderealTarget    => SharedIcons.ICON_SIDEREAL
      case _:NonSiderealTarget => SharedIcons.ICON_NONSIDEREAL
      case _:TooTarget         => SharedIcons.ICON_TOO
    }

    def warn(t:Target) = new CompositeIcon(ok(t), SharedIcons.OVL_WARN)
    def error(t:Target) = new CompositeIcon(ok(t), SharedIcons.OVL_ERROR)

    // On double-click just edit the first item in the selection.
    onDoubleClick(_.headOption.foreach(edit))

    // Set up copy/paste
    viewer.getTable.setTransferHandler(transferHandler)

    object transferHandler extends TransferHandler {

      override def getSourceActions(c:JComponent) = {
        if (selection.isEmpty) NONE else COPY
      }

      override def createTransferable(c:JComponent) = {
        TransferableTargetList(selection)
      }

      // A cut action
      object CopyAction extends Action("Copy - Ignored") {
        enabled = false
        onSelectionChanged(sel => enabled = sel.nonEmpty)
        def apply() {
          transferHandler.exportToClipboard(viewer.getTable, HackClipboard, COPY)
        }
      }

    }

  }

  // Our toolbar
  object toolbar extends StdToolbar with Bound.Self[Model] {

    // Configure content, defined below
    add(add)
    add(delete)
    addFlexibleSpace()
    add(importTargets)
    add(exportTargets)

    // Enablement
    override def refresh(m:Option[Model]) {
      add.enabled = canEdit
      importTargets.enabled = canEdit
      exportTargets.refresh()
    }

    // Add Target Button
    object add extends ToolButton(SharedIcons.ADD, SharedIcons.ADD_DISABLED, "Add Target") {
      def apply() {
        for {
          m             <- model
          t             <- TargetEditor.open(semester, None, canEdit, panel)
          ts             = targetLens.get(m)
        } model = Some(targetLens.set(m, t :: ts))
      }
    }

    // Delete
    object delete extends ToolButton(SharedIcons.REMOVE, SharedIcons.REMOVE_DISABLED, "Delete Target") {
      enabled = false
      def apply() {
        for {
          m <- model
          inUse = m.proposal.observations.flatMap(_.target).distinct
          toDelete = listView.selection
          if confirmDelete(toDelete.exists(inUse.contains))
          ts = targetLens.get(m)
        } model = Some(targetLens.set(m, ts.filterNot(toDelete.contains)))
      }
    }

    import JOptionPane._

    private def confirmDelete(doIt:Boolean) = (!doIt) || YES_OPTION == showConfirmDialog(panel.peer,
      "Selection contains target(s) that are in use. Continue with delete?\nTarget(s) will be removed from associated observations.",
      "Confirm Delete", WARNING_MESSAGE, YES_NO_OPTION)


    object importTargets extends ToolButton(SharedIcons.ICON_IMPORT, SharedIcons.ICON_IMPORT_DIS, "Import Targets...") {
      enabled = false
      def apply() {
        for {
          m <- model
          existing = targetLens.get(m)
          toAdd <- TargetImporter.open(this)
        } {
          model = Some(targetLens.set(m, existing ++ toAdd))
        }
      }
    }

    object exportTargets extends ToolButton(SharedIcons.ICON_EXPORT, SharedIcons.ICON_EXPORT_DIS, "Export Targets...") {
      enabled = false
      def refresh() {
        enabled = model exists {m => targetLens.get(m).exists(TargetExporter.isExportable)}
      }
      def apply() {
        model foreach {m => TargetExporter.open(this, targetLens.get(m))}
      }
    }

  }

}

