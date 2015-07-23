package edu.gemini.catalog.ui

import javax.swing.SwingConstants
import javax.swing.table.{DefaultTableCellRenderer, TableRowSorter, AbstractTableModel}

import edu.gemini.ags.api.AgsRegistrar
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.catalog.votable.VoTableClient
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.gui.{GlassLabel, SizePreference, SortableTable}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.OT
import jsky.app.ot.tpe.TpeContext

import scala.swing._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.event.{ButtonClicked, UIElementMoved, UIElementResized}

import scalaz._
import Scalaz._

trait PreferredSizeFrame { this: Window =>
  def adjustSize() {
    size = SizePreference.getDimension(this.getClass).getOrElse {
      // Set initial based on the desktop's size, the code below will work with multiple desktops
      val screenSize = java.awt.GraphicsEnvironment
        .getLocalGraphicsEnvironment
        .getDefaultScreenDevice
        .getDefaultConfiguration.getBounds
      new Dimension(screenSize.getWidth.intValue() * 3 / 4, (2f / 3f * screenSize.getHeight).intValue())
    }
  }
}

object QueryResultsWindow {

  private object table {

    sealed trait TableColumn[T] {
      val title: String

      def lens: PLens[Target, T]

      def displayValue(t: T): String = t.toString

      def render(target: Target): String = ~lens.get(target).map(displayValue)
    }

    case class IdColumn(title: String) extends TableColumn[String] {
      override val lens = Target.name
    }

    case class RAColumn(title: String) extends TableColumn[RightAscension] {
      override val lens = Target.coords >=> Coordinates.ra.partial

      override def displayValue(t: RightAscension) = t.toAngle.formatHMS
    }

    case class DECColumn(title: String) extends TableColumn[Declination] {
      override val lens = Target.coords >=> Coordinates.dec.partial

      override def displayValue(t: Declination) = t.formatDMS
    }

    case class PMRAColumn(title: String) extends TableColumn[RightAscensionAngularVelocity] {
      override val lens = Target.pm >=> ProperMotion.deltaRA.partial

      override def displayValue(t: RightAscensionAngularVelocity): String = f"${t.velocity.masPerYear}%.2f"
    }

    case class PMDecColumn(title: String) extends TableColumn[DeclinationAngularVelocity] {
      override val lens = Target.pm >=> ProperMotion.deltaDec.partial

      override def displayValue(t: DeclinationAngularVelocity): String = f"${t.velocity.masPerYear}%.2f"
    }

    case class MagnitudeColumn(band: MagnitudeBand) extends TableColumn[Magnitude] {
      override val title = band.name
      // Lens from list of magnitudes to the band's magnitude if present
      val bLens: List[Magnitude] @?> Magnitude = PLens(ml => ml.find(_.band === band).map(m => Store(b => sys.error("read-only lens"), m)))
      override val lens = Target.magnitudes >=> bLens

      override def displayValue(t: Magnitude): String = f"${t.value}%.2f"
    }

    val baseColumnNames: List[TableColumn[_]] = List(IdColumn("Id"), RAColumn("RA"), DECColumn("DE"))
    val pmColumns: List[TableColumn[_]] = List(PMRAColumn("pmRA"), PMDecColumn("pmDEC"))
    val magColumns = MagnitudeBand.all.map(MagnitudeColumn)

    case class TargetsModel(targets: List[SiderealTarget]) extends AbstractTableModel {

      // Available columns from the list of targets
      val columns = {
        val bandsInTargets = targets.flatMap(_.magnitudes).map(_.band).distinct
        val hasPM = targets.exists(_.properMotion.isDefined)
        val pmCols = if (hasPM) pmColumns else Nil
        val magCols = magColumns.filter(m => bandsInTargets.contains(m.band))

        baseColumnNames ::: pmCols ::: magCols
      }

      override def getRowCount = targets.length

      override def getColumnCount = columns.size

      override def getColumnName(column: Int): String =
        columns(column).title

      override def getValueAt(rowIndex: Int, columnIndex: Int) = (targets.isDefinedAt(rowIndex) option {
        val target = targets(rowIndex)
        columns.zipWithIndex.find(_._2 == columnIndex).map { c =>
          c._1.render(target)
        }
      }).flatten.orNull

    }

    lazy val resultsLabel = new Label("Query") {
      horizontalAlignment = Alignment.Center

      def updateCount(c: Int):Unit = {
        text = s"Query results: $c"
      }
    }

    case class QueryResultsFrame(table: Table) extends Frame with PreferredSizeFrame {
      title = "Query Results"

      def closeFrame(): Unit = {
        this.visible = false
      }

      private val closeButton = new Button("Close") {
        reactions += {
          case ButtonClicked(_) => closeFrame()
        }
      }

      contents = new MigPanel(LC().fill().insets(0).debug(100)) {
        // Results Table
        add(resultsLabel, CC().growX().wrap())
        // Results Table
        add(new ScrollPane() {
          contents = table
        }, CC().grow().pushY())
        // Command buttons at the bottom
        add(new MigPanel(LC().fillX().insets(10.px)) {
          add(closeButton, CC().alignX(RightAlign))
        }, CC().growX().dockSouth())
      }
      adjustSize()
      SizePreference.getPosition(this.getClass).foreach { p =>
        location = p
      }

      listenTo(this)
      reactions += {
        case _: UIElementResized =>
          SizePreference.setDimension(getClass, Some(this.size))
        case _: UIElementMoved =>
          SizePreference.setPosition(getClass, Some(this.location))
      }
    }

    val resultsTable = new Table() with SortableTable {
      private val m = TargetsModel(Nil)
      model = model
      val sorter = new TableRowSorter[TargetsModel](m)
      peer.setRowSorter(sorter)
      peer.getRowSorter.toggleSortOrder(0)

      // Align Right
      peer.setDefaultRenderer(classOf[String], new DefaultTableCellRenderer() {
        setHorizontalAlignment(SwingConstants.RIGHT)
      })

    }
    val frame = QueryResultsFrame(resultsTable)
  }

  private def reloadSearchData(query: CatalogQuery) {
    import QueryResultsWindow.table._

    VoTableClient.catalog(query).onComplete {
      case _: scala.util.Failure[_] =>
        GlassLabel.hide(frame.peer.getRootPane) // TODO Display error
      case scala.util.Success(x) if x.result.containsError =>
        GlassLabel.hide(frame.peer.getRootPane) // TODO Display error
      case scala.util.Success(x) =>
        Swing.onEDT {
          // Controller code in MVC-style
          GlassLabel.hide(frame.peer.getRootPane)
          // Update the table
          val model = TargetsModel(x.result.targets.rows)
          resultsTable.model = model

          // The sorting logic may change if the list of magnitudes changes
          val sorter = new TableRowSorter[TargetsModel](model)
          resultsTable.peer.setRowSorter(sorter)
          resultsTable.peer.getRowSorter.toggleSortOrder(0)

          // Update the count of tables
          resultsLabel.updateCount(x.result.targets.rows.length)
        }
    }
  }

  // Public interface
  val instance = this

  private def showTable(q: CatalogQuery):Unit = Swing.onEDT {
    import QueryResultsWindow.table._

    frame.visible = true
    frame.peer.toFront()
    GlassLabel.show(frame.peer.getRootPane, "Downloading...")
    reloadSearchData(q)
  }

  def showOn(n: ISPNode) {
    TpeContext.apply(n).obsContext.foreach { obsCtx =>
      // TODO The user should be able to select the strategy OCSADV-403
      AgsRegistrar.currentStrategy(obsCtx).foreach { strategy =>
        // TODO Use only the first query, GEMS isn't supported yet OCSADV-242, OCSADV-239
        strategy.catalogQueries(obsCtx, OT.getMagnitudeTable).headOption.foreach { q =>
          showTable(q)
        }
      }
    }
  }

}