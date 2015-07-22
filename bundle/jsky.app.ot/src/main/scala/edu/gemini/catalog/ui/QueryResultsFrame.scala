package edu.gemini.catalog.ui

import javax.swing.BorderFactory._
import javax.swing.border.Border
import javax.swing.SwingConstants
import javax.swing.table._

import edu.gemini.ags.api.AgsRegistrar
import edu.gemini.catalog.api._
import edu.gemini.catalog.votable.VoTableClient
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.gui.textComponent.{TextRenderer, NumberField}
import edu.gemini.shared.gui.{GlassLabel, SizePreference, SortableTable}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.OT
import jsky.app.ot.tpe.TpeContext

import scala.swing.Reactions.Reaction
import scala.swing._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.event.{ValueChanged, ButtonClicked, UIElementMoved, UIElementResized}

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

/**
 * Support to calculate the columnt widths and adjust them to the outer width
 */
trait TableColumnsAdjuster { this: Table =>
  autoResizeMode = Table.AutoResizeMode.Off

  val minSpacing: Int = 20

  def adjustColumns(containerWidth: Int): Unit = {
    import scala.math.max

    def updateTableColumn(column: TableColumn, width: Int):Unit = {
      if (column.getResizable) {
        this.peer.getTableHeader.setResizingColumn(column)
        column.setPreferredWidth(width)
        column.setWidth(width)
      }
    }

    def calculateColumnWidth(column: TableColumn): Int = {

      def calculateColumnHeaderWidth: Int = {
        val value = column.getHeaderValue
        val renderer = Option(column.getHeaderRenderer).getOrElse(this.peer.getTableHeader.getDefaultRenderer)

        val c = renderer.getTableCellRendererComponent(this.peer, value, false, false, -1, column.getModelIndex)
        c.getPreferredSize.width
      }

      def cellDataWidth(row: Int, column: Int): Int = {
        val cellRenderer = this.peer.getCellRenderer(row, column)
        val c = this.peer.prepareRenderer(cellRenderer, row, column)
        c.getPreferredSize.width + this.peer.getIntercellSpacing.width
      }

      def calculateColumnDataWidth: Int = {
        (0 until this.model.getRowCount).foldLeft(0) { (currMax, i) =>
          max(currMax, cellDataWidth(i, column.getModelIndex))
        }
      }

      val columnHeaderWidth = calculateColumnHeaderWidth
      val columnDataWidth = calculateColumnDataWidth

      max(columnHeaderWidth, columnDataWidth)
    }

    val tcm = this.peer.getColumnModel

    // Calculate the width
    val cols = for {
        i <- 0 until tcm.getColumnCount
      } yield (tcm.getColumn(i), calculateColumnWidth(tcm.getColumn(i)))
    val initialWidth = cols.map(_._2).sum

    // Adjust space to fit on the outer width
    val spacing = max(minSpacing, (containerWidth - initialWidth) / tcm.getColumnCount)
    // Add the rounding error to the first col
    val initialOffset = max(0, containerWidth - cols.map(_._2 + spacing).sum)
    // Set width + spacing
    cols.zipWithIndex.foreach {
      case ((c, w), i) if i == 0 => updateTableColumn(c, w + spacing + initialOffset) // Add rounding error to the first col
      case ((c, w), i)           => updateTableColumn(c, w + spacing)
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

    val baseColumnNames: List[TableColumn[_]] = List(IdColumn("Id"), RAColumn("RA"), DECColumn("Dec"))
    val pmColumns: List[TableColumn[_]] = List(PMRAColumn("pmRA"), PMDecColumn("pmDec"))
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

      val scrollPane = new ScrollPane() {
        contents = table
      }

      case object QueryForm extends MigPanel(LC().fill().insets(0.px).debug(0)) {
        /** Create a titled border with inner and outer padding. */
        def titleBorder(title: String): Border =
          createCompoundBorder(
            createEmptyBorder(2,2,2,2),
            createCompoundBorder(
              createTitledBorder(title),
              createEmptyBorder(2,2,2,2)))

        border = titleBorder("Query Params")

        case class MagnitudeFilterControls(faintess: NumberField, saturation: NumberField, bandCB: ComboBox[MagnitudeBand])

        private val queryButton = new Button("Query") {
          reactions += {
            case ButtonClicked(_) =>
              // Hit the catalog with a new query
              buildQuery.foreach(reloadSearchData)
          }
        }

        val queryButtonEnabling:Reaction = {
          case ValueChanged(a) =>
            queryButton.enabled = a match {
              case f: AngleTextField[_] => f.valid
              case f: NumberField       => f.valid
              case _                    => true
            }
        }

        lazy val ra = new RATextField(RightAscension.zero) {
          reactions += queryButtonEnabling
          def updateRa(ra: RightAscension): Unit = {
            value = ra
          }
        }

        lazy val dec = new DecTextField(Declination.zero) {
          reactions += queryButtonEnabling
          def updateDec(dec: Declination): Unit = {
            value = dec
          }
        }

        lazy val radiusStart, radiusEnd = new NumberField(None) {
          reactions += queryButtonEnabling
          def updateAngle(angle: Angle): Unit = {
            text = f"${angle.toArcmins}%2.2f"
          }
        }

        // Contains the list of controls on the UI to make magnitude filters
        val magnitudeControls = mutable.ListBuffer.empty[MagnitudeFilterControls]

        // Supported bands, remove R-Like duplicates
        val bands = MagnitudeBand.all.collect {
          case MagnitudeBand.R  => MagnitudeBand._r
          case MagnitudeBand.UC => MagnitudeBand._r
          case b                => b
        }.distinct

        // Make a combo box out of the supported bands
        def bandsComboBox(bandsList: BandsList): ComboBox[MagnitudeBand] = new ComboBox(bands) with TextRenderer[MagnitudeBand] {
          bandsList match {
            case RBandsList       => selection.item = MagnitudeBand._r
            case SingleBand(band) => selection.item = band
          }

          override def text(a: MagnitudeBand) = a.name
        }

        // Make a query out of the form parameters
        def buildQuery: Option[CatalogQuery] = {
          queryButton.enabled option {
            // No validation here, the Query button is disabled unless all the controls are valid
            val coordinates = Coordinates(ra.value, dec.value)
            val radius = RadiusConstraint.between(Angle.fromArcmin(radiusStart.text.toDouble), Angle.fromArcmin(radiusEnd.text.toDouble))

            val filters = magnitudeControls.map {
              case MagnitudeFilterControls(faintess, saturation, bandCB) =>
                MagnitudeConstraints(BandsList.bandList(bandCB.selection.item), FaintnessConstraint(faintess.text.toDouble), SaturationConstraint(saturation.text.toDouble).some)
            }
            CatalogQuery(None, coordinates, radius, filters.toList, ucac4)
          }
        }

        def buildLayout(filters: List[MagnitudeQueryFilter]): Unit = {
          _contents.clear()

          add(new Label("RA"), CC().cell(0, 0))
          add(ra, CC().cell(1, 0).spanX(3).growX())
          add(new Label("Dec"), CC().cell(0, 1))
          add(dec, CC().cell(1, 1).spanX(3).growX())
          add(new Label("J2000") {
            verticalAlignment = Alignment.Center
          }, CC().cell(4, 0).spanY(2))
          add(new Label("Radial Range"), CC().cell(0, 2))
          add(radiusStart, CC().cell(1, 2).minWidth(50.px).growX())
          add(new Label("-"), CC().cell(2, 2))
          add(radiusEnd, CC().cell(3, 2).minWidth(50.px).growX())
          add(new Label("arcmin"), CC().cell(4, 2))

          // Replace current magnitude filters
          magnitudeControls.clear()
          magnitudeControls ++= filters.map { f =>
            val faint = new NumberField(f.mc.faintnessConstraint.brightness.some) {
                          reactions += queryButtonEnabling
                        }
            val sat = new NumberField(f.mc.saturationConstraint.map(_.brightness)) {
                            reactions += queryButtonEnabling
                          }
            val cb = bandsComboBox(RBandsList)
            MagnitudeFilterControls(faint, sat, cb)
          }
          // Add magnitude filters list
          val startIndex = 3
          magnitudeControls.zipWithIndex.map(v => v.copy(_2 = v._2 + startIndex)).foreach {
            case (MagnitudeFilterControls(faintness, saturation, cb), i) =>
              add(new Label("Magnitudes"), CC().cell(0, i))
              add(faintness, CC().cell(1, i).minWidth(50.px).growX())
              add(new Label("-"), CC().cell(2, i))
              add(saturation, CC().cell(3, i).minWidth(50.px).growX())
              add(cb, CC().cell(4, i).growX())
          }

          add(queryButton, CC().cell(0, startIndex + filters.length + 1).span(5).pushX().alignX(RightAlign).gapTop(10.px))
        }

        def updateQuery(query: CatalogQuery): Unit = {
          // Update the RA
          ra.updateRa(query.base.ra)
          dec.updateDec(query.base.dec)

          // Update radius constraint
          radiusStart.updateAngle(query.radiusConstraint.minLimit)
          radiusEnd.updateAngle(query.radiusConstraint.maxLimit)

          buildLayout(query.filters.list.collect {case q: MagnitudeQueryFilter => q})
        }
      }

      QueryForm.buildLayout(Nil)
      contents = new MigPanel(LC().fill().insets(0).debug(0)) {
        // Query Form
        add(QueryForm, CC().spanY(2).alignY(TopAlign).minWidth(250.px))
        // Results Table
        add(resultsLabel, CC().alignX(CenterAlign).gapTop(5.px).wrap())
        // Results Table
        add(scrollPane, CC().grow().pushY().pushX())
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

    val resultsTable = new Table() with SortableTable with TableColumnsAdjuster {
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

    GlassLabel.show(frame.peer.getRootPane, "Downloading...")
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
          sorter.sort()

          // Update the count of rows
          resultsLabel.updateCount(x.result.targets.rows.length)

          // Update the magnitude constraints
          frame.QueryForm.updateQuery(x.query)

          // Adjust the width of the columns
          val insets = frame.scrollPane.border.getBorderInsets(frame.scrollPane.peer)
          resultsTable.adjustColumns(frame.scrollPane.bounds.width - insets.left - insets.right)
        }
    }
  }

  // Public interface
  val instance = this

  protected [ui] def showTable(q: CatalogQuery):Unit = Swing.onEDT {
    import QueryResultsWindow.table._

    frame.visible = true
    frame.peer.toFront()
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

object CatalogQueryDemo extends SwingApplication {
  import QueryResultsWindow.instance
  import jsky.util.gui.Theme
  import javax.swing.UIManager

  val query = CatalogQuery(None,Coordinates(RightAscension.fromAngle(Angle.fromDegrees(3.1261166666666895)),Declination.fromAngle(Angle.fromDegrees(337.93268333333333)).getOrElse(Declination.zero)),RadiusConstraint.between(Angle.zero,Angle.fromDegrees(0.16459874517619255)),List(MagnitudeConstraints(RBandsList,FaintnessConstraint(16.0),Some(SaturationConstraint(3.1999999999999993)))),ucac4)

  def startup(args: Array[String]) {
    System.setProperty("apple.awt.antialiasing", "on")
    System.setProperty("apple.awt.textantialiasing", "on")
    Theme.install()

    UIManager.put("Button.defaultButtonFollowsFocus", true)

    instance.showTable(query)
  }

}
