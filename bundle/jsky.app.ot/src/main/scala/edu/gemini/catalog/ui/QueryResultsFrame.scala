package edu.gemini.catalog.ui

import java.awt.Color
import java.io._
import java.nio.charset.Charset
import javax.swing.BorderFactory._
import javax.swing.border.Border
import javax.swing.{BorderFactory, DefaultComboBoxModel, JOptionPane, UIManager}

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.{AgsGuideQuality, AgsRegistrar}
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.api._
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay
import edu.gemini.catalog.votable._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.shared.gui.textComponent.{NumberField, SelectOnFocus, TextRenderer}
import edu.gemini.shared.gui.{ButtonFlattener, GlassLabel, SortableTable}
import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.core._
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.gemini.editor.targetComponent.GuidingIcon
import jsky.app.ot.gemini.editor.targetComponent.TargetGuidingFeedback.ProbeLimits
import jsky.app.ot.tpe.{TpeContext, TpeManager}
import jsky.app.ot.util.OtColor
import jsky.catalog.gui.{SymbolSelectionEvent, SymbolSelectionListener}
import jsky.util.gui.{DialogUtil, Resources}

import scala.swing.Reactions.Reaction
import scala.swing._
import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.swing.event._
import scalaz._
import Scalaz._

/**
 * Frame to display the Query controls and results
 */
object QueryResultsFrame extends Frame with PreferredSizeFrame {
  private sealed trait PlotState {
    def flipAction: String
  }
  private case object PlottedState extends PlotState {
    override val flipAction = "Unplot"
  }
  private case object UnplottedState extends PlotState {
    override val flipAction = "Plot"
  }

  private object GuidingFeedbackRenderer {
    val icons = AgsGuideQuality.All.fproduct(GuidingIcon(_, enabled = true)).toMap
    val dimensions = new Dimension(GuidingIcon.sideLength + 2, GuidingIcon.sideLength + 2)
  }

  private class GuidingFeedbackRenderer extends Table.AbstractRenderer[AgsGuideQuality, Label](new Label) {
    override def configure(t: Table, sel: Boolean, foc: Boolean, value: AgsGuideQuality, row: Int, col: Int): Unit = {
      component.icon = GuidingFeedbackRenderer.icons.get(value).orNull
      component.text = ""
      component.preferredSize = GuidingFeedbackRenderer.dimensions
      component.maximumSize = GuidingFeedbackRenderer.dimensions
    }
  }

  private lazy val resultsTable = new Table() with SortableTable with TableColumnsAdjuster {
    selection.elementMode = Table.ElementMode.Row
    listenTo(selection)

    reactions += {
      case TableRowsSelected(source, _, false) =>
        selectResults(source.selection.rows.toSet.map(viewToModelRow))
    }
    Option(TpeManager.get()).foreach(_.getImageWidget.plotter.addSymbolSelectionListener(new SymbolSelectionListener {
      override def symbolDeselected(e: SymbolSelectionEvent): Unit = {
        selection.rows -= modelToViewRow(e.getRow)
      }

      override def symbolSelected(e: SymbolSelectionEvent): Unit = {
        selection.rows += modelToViewRow(e.getRow)
      }
    }))

    override def rendererComponent(isSelected: Boolean, focused: Boolean, row: Int, column: Int): Component =
      // Note that we need to use the same conversions as indicated on SortableTable to get the value
      (model, model.getValueAt(viewToModelRow(row), viewToModelColumn(column))) match {
        case (_, q: AgsGuideQuality) =>
          new GuidingFeedbackRenderer().componentFor(this, isSelected, focused, q, row, column)
        case (m: TargetsModel, value) =>
          // Delegate rendering to the model
          m.rendererComponent(value, isSelected, focused, row, column, this.peer).getOrElse(super.rendererComponent(isSelected, focused, row, column))
      }
  }

  private lazy val closeButton = new Button("Close") {
    reactions += {
      case ButtonClicked(_) => QueryResultsFrame.visible = false
    }
  }

  private lazy val unplotButton = new Button(PlottedState.flipAction) {
    reactions += {
      case ButtonClicked(_) =>
        text match {
          case PlottedState.flipAction =>
            unplotCurrent()
            text = UnplottedState.flipAction
          case UnplottedState.flipAction =>
            plotResults()
            text = PlottedState.flipAction
        }
    }
  }

  private lazy val exportButton = new Button("Save as...") {
    reactions += {
      case ButtonClicked(_) =>
        val fc = new FileChooser(new File("."))
        val r = fc.showSaveDialog(this)
        def canOverwrite(f: File) = {
          if (f.exists) {
            val msg = s"The file ${f.getName} already exists. Do you want to overwrite it?"
            DialogUtil.confirm(msg) == JOptionPane.YES_OPTION
          } else {
            true
          }
        }
        (r, fc.selectedFile) match {
          case (FileChooser.Result.Approve, file) if canOverwrite(file) =>
            exportToText(file)
          case _                          => // Ignore
        }
    }
  }

  private lazy val scrollPane = new ScrollPane() {
    contents = resultsTable
  }

  private lazy val errorLabel = new Label("") {
    horizontalAlignment = Alignment.Left
    foreground = Color.darkGray
    background = OtColor.LIGHT_SALMON
    border = BorderFactory.createEmptyBorder(2, 2, 2, 2)

    def reset(): Unit = {
      text = ""
      opaque = false
    }

    def show(msg: String):Unit = {
      text = msg
      opaque = true
    }
  }

  title = "Catalog Query Tool"
  val tableBorder: BorderPanel = new BorderPanel() {
    border = titleBorder("Results")
    add(scrollPane, BorderPanel.Position.Center)
  }
  QueryForm.buildLayout(Nil)
  contents = new MigPanel(LC().fill().insets(0).gridGap("0px", "0px").debug(0)) {
    // Query Form
    add(QueryForm, CC().alignY(TopAlign).minWidth(320.px))
    // Results Table
    add(tableBorder, CC().grow().spanX(4).pushY().pushX())
    // Labels and command buttons at the bottom
    add(errorLabel, CC().alignX(LeftAlign).alignY(BaselineAlign).gap(10.px, 10.px, 10.px, 10.px).newline().skip(1).grow())
    add(unplotButton, CC().alignX(RightAlign).alignY(BaselineAlign).gap(10.px, 10.px, 10.px, 10.px))
    add(exportButton, CC().alignX(RightAlign).alignY(BaselineAlign).gap(10.px, 10.px, 10.px, 10.px))
    add(closeButton, CC().alignX(RightAlign).alignY(BaselineAlign).gap(10.px, 10.px, 10.px, 10.px))
  }

  // Set initial size
  adjustSize(false)

  /** Create a titled border with inner and outer padding. */
  def titleBorder(title: String): Border =
    createCompoundBorder(
      createEmptyBorder(2, 2, 2, 2),
      createCompoundBorder(
        createTitledBorder(title),
        createEmptyBorder(2, 2, 2, 2)))

  /**
   * Called after a name search completes
   */
  def updateName(search: String, targets: List[SiderealTarget]): Unit = {
    QueryForm.updateName(targets.headOption)
    targets.headOption.ifNone {
      errorLabel.show(s"Target '$search' not found...")
    }
  }

  def closing[A <: {def close(): Unit}, B](param: A)(f: A => B): B =
    try f(param) finally param.close()

  private def exportToText(f: File): Unit = {
    val ColumnSeparator = "\t"
    val NewLine = System.getProperty("line.separator")
    val model = resultsTable.model match {
      case m: TargetsModel => Some(m)
      case _               => None
    }
    model.foreach { m =>
      // Note that we don't export the guiding quality column
      val colIds = (0 until m.getColumnCount).collect {
        case i if resultsTable.model.getColumnClass(i) != classOf[AgsGuideQuality] => i
      }
      val header = colIds.map(m.getColumnName)
      val dashedLine = header.map(i => "-" * i.length)

      val data = for {
        r <- 0 until m.getRowCount
      } yield (for {
        c <- colIds
      } yield ~m.renderAt(r, c)).mkString(ColumnSeparator)

      val encoder = Charset.forName("UTF-8").newEncoder()
      val output = List(header.mkString(ColumnSeparator), dashedLine.mkString(ColumnSeparator), data.mkString(NewLine)).mkString(NewLine)
      closing(new OutputStreamWriter(new FileOutputStream(f), encoder)) { w =>
        w.write(output)
      }
    }
  }

  private def plotResults(): Unit = {
    resultsTable.model match {
      case t: TargetsModel =>
        Option(TpeManager.open()).foreach(p => TpePlotter(p.getImageWidget).plot(t))
      case _               => // Ignore, it shouldn't happen
    }
  }

  private def selectResults(selected: Set[Int]): Unit = {
    resultsTable.model match {
      case t: TargetsModel =>
        val tpe = TpeManager.get()
        Option(tpe).foreach(p => TpePlotter(p.getImageWidget).select(t, selected))
      case _               => // Ignore, it shouldn't happen
    }
  }

  private def unplotCurrent(): Unit = {
    resultsTable.model match {
      case t: TargetsModel =>
        val tpe = TpeManager.get()
        Option(tpe).foreach(p => TpePlotter(p.getImageWidget).unplot(t))
      case _               => // Ignore, it shouldn't happen
    }
  }

  /**
   * Called after a query completes to update the UI according to the results
   */
  def updateResults(info: Option[ObservationInfo], queryResult: QueryResult): Unit = {
    errorLabel.reset()
    queryResult.query match {
      case q: ConeSearchCatalogQuery =>
        unplotCurrent()

        val model = TargetsModel(info, q.base, q.radiusConstraint, queryResult.result.targets.rows)
        updateResultsModel(model)

        // Update the count of rows
        tableBorder.border = titleBorder(s"Results - ${queryResult.result.targets.rows.length} results found")

        // Update the query form
        QueryForm.updateQuery(info, q)

        // Plot the results when they arrive
        plotResults()

        // Reset the state of the plot button
        unplotButton.text = PlottedState.flipAction
      case _ =>
    }
  }

  private def updateResultsModel(model: TargetsModel): Unit = {
    resultsTable.model = model

    // The sorting logic may change if the list of magnitudes changes
    resultsTable.peer.setRowSorter(model.sorter)

    // Adjust the width of the columns
    val insets = scrollPane.border.getBorderInsets(scrollPane.peer)
    resultsTable.peer.getColumnModel.getColumn(0).setResizable(false)
    resultsTable.adjustColumns(scrollPane.bounds.width - insets.left - insets.right)
  }

  protected def revalidateFrame(): Unit = {
    contents.headOption.foreach(_.revalidate())
  }

  /**
   * Contains the data used to create the right-hand side form to do queries
   */
  private case object QueryForm extends MigPanel(LC().fill().insets(0.px).debug(0)) {

    // Represents a magnitude filter containing the controls that make the row
    case class MagnitudeFilterControls(addButton: Button, faintess: NumberField, separator: Label, saturation: NumberField, bandCB: ComboBox[MagnitudeBand], removeButton: Button)

    border = titleBorder("Query Parameters")

    lazy val queryButton: Button = new Button("Query") {
      reactions += {
        case ButtonClicked(_) =>
          // Hit the catalog with a new query
          buildQuery.foreach(Function.tupled(reloadSearchData))
      }
    }
    lazy val fromImageButton: Button = new Button("Reset") {
      reactions += {
        case ButtonClicked(_) =>
          // Reload target info from the OT
          Option(TpeManager.get()).flatMap(_.getImageWidget.getObsContext.asScalaOpt).foreach(QueryResultsFrame.instance.showOn)
      }
    }

    // Action to disable the query button if there are invalid fields
    val queryButtonEnabling: Reaction = {
      case ValueChanged(a) =>
        val controls = List(radiusStart, radiusEnd, ra, dec) ++ magnitudeControls.flatMap(f => List(f.faintess, f.saturation))
        queryButton.enabled = controls.forall {
          case f: AngleTextField[_] => f.valid
          case f: NumberField       => f.valid
          case _                    => true
        }
    }

    lazy val objectName: TextField with SelectOnFocus {
      def resetName(n: String): Unit
    } = new TextField("") with SelectOnFocus {
      val foregroundColor: Color = UIManager.getColor("TextField.foreground")
      listenTo(keys)
      reactions += {
        case KeyPressed(_, Key.Enter, _, _) if text.nonEmpty =>
          doNameSearch(text)
        case KeyTyped(_, _, _, _)                            =>
          errorLabel.reset()
          foreground = foregroundColor
      }

      def resetName(n: String): Unit = {
        text = n
        foreground = foregroundColor
      }
    }
    lazy val searchByName: Button = new Button("") {
      icon = Resources.getIcon("eclipse/search.gif")
      ButtonFlattener.flatten(peer)
      reactions += {
        case ButtonClicked(_) if objectName.text.nonEmpty =>
          doNameSearch(objectName.text)
      }
    }
    lazy val instrumentBox: ComboBox[SPComponentType] with TextRenderer[SPComponentType] {
      def text(a: SPComponentType): String
    } = new ComboBox[SPComponentType](ObservationInfo.InstList.map(_._2)) with TextRenderer[SPComponentType] {
      override def text(a: SPComponentType): String = ~Option(a).flatMap(t => ObservationInfo.InstMap.get(t))
      selection.item = ObservationInfo.DefaultInstrument
      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          // Update the guiders box with the newly selected instrument
          val i = observationInfoFromForm.toContext
          val defaultStrategy = i.flatMap(AgsRegistrar.defaultStrategy)
          val mt = ProbeLimitsTable.loadOrThrow()
          val strategies = ~i.map(c => AgsRegistrar.validStrategies(c).map(ObservationInfo.toSupportedStrategy(c, _, mt)))

          val selected = for {
              s <- defaultStrategy
              c <- strategies.find(_.strategy == s)
            } yield c
          selected.foreach { s =>
            updateGuidersModel(s, strategies)
            updateGuideSpeedText()
            magnitudeFiltersFromControls(s.query.headOption)
          }
      }
    }
    lazy val catalogBox: ComboBox[CatalogName] with TextRenderer[CatalogName] {
      def text(a: CatalogName): String
    } = new ComboBox(List[CatalogName](UCAC4, PPMXL)) with TextRenderer[CatalogName] {
      override def text(a: CatalogName): String = ~Option(a).map(_.displayName)

      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          val supportedBands = selection.item.supportedBands
          // Go through the magnitude selectors and remove those not supported
          val toRemove = magnitudeControls.filter(mc => !supportedBands.contains(mc.bandCB.selection.item))
          magnitudeControls --= toRemove
          buildLayout(currentFilters)
          revalidateFrame()
      }
    }
    lazy val guider: ComboBox[SupportedStrategy] with TextRenderer[SupportedStrategy] {
      def text(a: SupportedStrategy): String
    } = new ComboBox(List.empty[SupportedStrategy]) with TextRenderer[SupportedStrategy] {
      override def text(a: SupportedStrategy): String = ~Option(a).map(s => {
        if (s.altairMode == AltairParams.Mode.LGS_P1.some) {
          AltairParams.Mode.LGS_P1.displayValue
        } else {
          s.strategy.key.displayName + ~s.altairMode.map(m => s"+${m.displayValue()}")
        }
      })

      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          // REL-2910 Set the radius range to the selected guider settings
          selection.item.query.headOption.foreach {
            case ConeSearchCatalogQuery(_, _, rc, _, _) =>
              radiusStart.updateAngle(rc.minLimit)
              radiusEnd.updateAngle(rc.maxLimit)
            case _                                      =>
          }
          updateGuideSpeedText()
          magnitudeFiltersFromControls(selection.item.query.headOption)
      }
    }

    // PA and offsets must be mutable, the rest of the model lives on the UI
    var pa: Angle = Angle.zero
    var allowPAFlip = false
    var offsets = Set.empty[Offset]
    var originalConditions: Option[Conditions] = ObservationInfo.zero.conditions

    def conditionsRenderer[A <: Comparable[A]](get: Option[Conditions] => Option[A], text: TextRenderer[A]): ListView.AbstractRenderer[A, Label] {
      def configure(list: ListView[_], isSelected: Boolean, focused: Boolean, a: A, index: Int): Unit
    } = new ListView.AbstractRenderer[A, Label](new Label()) {
      override def configure(list: ListView[_], isSelected: Boolean, focused: Boolean, a: A, index: Int): Unit = {
        component.text = text.text(a)
        component.horizontalAlignment = Alignment.Left
        get(originalConditions) match {
          case Some(i) if i.compareTo(a) != 0 =>
            component.foreground = Color.red
          case _                             =>
            component.foreground = Color.black
        }
      }
    }

    lazy val sbBox: ComboBox[SPSiteQuality.SkyBackground] with TextRenderer[SPSiteQuality.SkyBackground] {
      def text(a: SPSiteQuality.SkyBackground): String
    } = new ComboBox(List(SPSiteQuality.SkyBackground.values(): _*)) with TextRenderer[SPSiteQuality.SkyBackground] {
      renderer = conditionsRenderer(_.map(_.sb), this)

      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          foreground = originalConditions.map(_.sb).contains(selection.item) ? Color.black | Color.red
          resultsTable.model match {
            case t: TargetsModel =>
              Swing.onEDT {
                unplotCurrent()
                updateResultsModel(t.copy(info = t.info.map(i => i.copy(ctx = None, conditions = i.conditions.map(_.sb(selection.item))))))
                plotResults()
                updateGuideSpeedText()
              }
          }
      }

      override def text(a: SPSiteQuality.SkyBackground): String = a.displayValue()
    }

    lazy val ccBox: ComboBox[SPSiteQuality.CloudCover] with TextRenderer[SPSiteQuality.CloudCover] {
      def text(a: SPSiteQuality.CloudCover): String
    } = new ComboBox(List(SPSiteQuality.CloudCover.values().filter(!_.isObsolete): _*)) with TextRenderer[SPSiteQuality.CloudCover] {
      renderer = conditionsRenderer(_.map(_.cc), this)

      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          foreground = originalConditions.map(_.cc).contains(selection.item) ? Color.black | Color.red
          resultsTable.model match {
            case t: TargetsModel =>
              Swing.onEDT {
                unplotCurrent()
                updateResultsModel(t.copy(info = t.info.map(i => i.copy(ctx = None, conditions = i.conditions.map(_.cc(selection.item))))))
                plotResults()
                updateGuideSpeedText()
              }
          }
      }

      override def text(a: SPSiteQuality.CloudCover): String = a.displayValue()
    }

    lazy val iqBox: ComboBox[SPSiteQuality.ImageQuality] with TextRenderer[SPSiteQuality.ImageQuality] {
      def text(a: SPSiteQuality.ImageQuality): String
    } = new ComboBox(List(SPSiteQuality.ImageQuality.values(): _*)) with TextRenderer[SPSiteQuality.ImageQuality] {
      renderer = conditionsRenderer(_.map(_.iq), this)

      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          foreground = originalConditions.map(_.iq).contains(selection.item) ? Color.black | Color.red
          resultsTable.model match {
            case t: TargetsModel =>
              Swing.onEDT {
                unplotCurrent()
                updateResultsModel(t.copy(info = t.info.map(i => i.copy(ctx = None, conditions = i.conditions.map(_.iq(selection.item))))))
                plotResults()
                updateGuideSpeedText()
              }
          }
      }

      override def text(a: SPSiteQuality.ImageQuality): String = a.displayValue()
    }

    lazy val limitsLabel: Label = new Label() {
      font = font.deriveFont(font.getSize2D * 0.8f)
    }

    lazy val ra: RATextField {
      def updateRa(ra: RA): Unit
    } = new RATextField(RightAscension.zero) {
      reactions += queryButtonEnabling

      def updateRa(ra: RightAscension): Unit = {
        value = ra
      }
    }

    lazy val dec: DecTextField {
      def updateDec(dec: Dec): Unit
    } = new DecTextField(Declination.zero) {
      reactions += queryButtonEnabling

      def updateDec(dec: Declination): Unit = {
        value = dec
      }
    }

    lazy val radiusStart, radiusEnd = new NumberField(None, allowEmpty = false) {
      reactions += queryButtonEnabling

      def updateAngle(angle: Angle): Unit = {
        text = f"${angle.toArcmins}%2.2f"
      }

      override def valid(d: Double): Boolean = d >= 0
    }

    // Contains the list of controls on the UI to make magnitude filters
    val magnitudeControls: ListBuffer[MagnitudeFilterControls] = mutable.ListBuffer.empty[MagnitudeFilterControls]

    /**
     * Reconstructs the layout depending on the magnitude constraints
     */
    def buildLayout(filters: List[MagnitudeConstraints]): Unit = {
      _contents.clear()

      add(catalogBox, CC().spanX(7).alignX(CenterAlign))
      add(new Separator(Orientation.Horizontal), CC().spanX(7).growX().newline())
      add(new Label("Object"), CC().spanX(2).newline())
      add(objectName, CC().spanX(3).growX())
      add(searchByName, CC().spanX(4))
      add(new Label("RA"), CC().spanX(2).newline())
      add(ra, CC().spanX(3).growX())
      add(new Label("J2000") {
        verticalAlignment = Alignment.Center
      }, CC().spanY(2).spanX(2))
      add(new Label("Dec"), CC().spanX(2).newline())
      add(dec, CC().spanX(3).growX())
      add(new Separator(Orientation.Horizontal), CC().spanX(7).growX().newline())
      add(new Label("Instrument"), CC().spanX(2).newline())
      add(instrumentBox, CC().spanX(3).growX())
      add(new Label("Guider"), CC().spanX(2).newline())
      add(guider, CC().spanX(3).growX().pushX())
      add(new Label("Sky Background"), CC().spanX(2).newline())
      add(sbBox, CC().spanX(3).growX())
      add(new Label("Cloud Cover"), CC().spanX(2).newline())
      add(ccBox, CC().spanX(3).growX())
      add(new Label("Image Quality"), CC().spanX(2).newline())
      add(iqBox, CC().spanX(3).growX())
      add(limitsLabel, CC().spanX(7).growX().newline())
      add(new Separator(Orientation.Horizontal), CC().spanX(7).growX().newline())
      add(new Label("Radial Range"), CC().spanX(2).newline())
      add(radiusStart, CC().minWidth(50.px).growX())
      add(new Label("-"), CC())
      add(radiusEnd, CC().minWidth(50.px).growX())
      add(new Label("arcmin"), CC().spanX(3))
      add(new Label("Magnitudes"), CC().spanX(2).newline())
      add(new Label("Bright."), CC().spanX(2))
      add(new Label("Faint."), CC())

      if (filters.isEmpty) {
        add(addMagnitudeRowButton(0), CC().spanX(2).alignX(RightAlign).newline())
      } else {
        // Replace current magnitude filters
        magnitudeControls.clear()
        magnitudeControls ++= filters.zipWithIndex.flatMap(Function.tupled(filterControls))

        // Add magnitude filters list
        magnitudeControls.foreach {
          case MagnitudeFilterControls(addButton, faintness, separator, saturation, cb, removeButton) =>
            add(addButton, CC().spanX(2).newline().alignX(RightAlign))
            add(saturation, CC().minWidth(50.px).growX())
            add(separator, CC())
            add(faintness, CC().minWidth(50.px).growX())
            add(cb, CC().grow())
            add(removeButton, CC())
        }
      }

      add(new Separator(Orientation.Horizontal), CC().spanX(7).growX().newline())
      add(fromImageButton, CC().newline().span(6).pushX().alignX(RightAlign).gapTop(10.px))
      add(queryButton, CC().alignX(RightAlign).gapTop(10.px))
    }

    /**
     * Updates the text containing the limits for the currently selected guider
     */
    def updateGuideSpeedText(): Unit = {
      val i = observationInfoFromForm
      val ctx = i.toContext
      for {
        sel        <- guider.selection.item.some
        c          <- ctx
        s          <- sel.strategy.magnitudes(c, i.mt).map(k => ProbeLimits(sel.strategy.probeBands, c, k._2))
      } limitsLabel.text = ~s.map(_.detailRange)
    }

    /**
      * Updates the magnitude filters when the controls change
      */
    def magnitudeFiltersFromControls(query: Option[CatalogQuery]): Unit = {
      query.collect {
        case ConeSearchCatalogQuery(_, _, _, mc, _) =>
          magnitudeControls.clear()
          magnitudeControls ++= mc.zipWithIndex.flatMap(Function.tupled(filterControls))
      }
      buildLayout(currentFilters)
      revalidateFrame()
    }

    /**
     * Update query form according to the passed values
     */
    def updateQuery(info: Option[ObservationInfo], query: ConeSearchCatalogQuery): Unit = {
      info.foreach { i =>
        objectName.resetName(~i.objectName)
        // Skip listening or the selection update will trickle down to the other controls
        instrumentBox.deafTo(instrumentBox.selection)
        instrumentBox.selection.item = i.instrument.getOrElse(ObservationInfo.DefaultInstrument)
        instrumentBox.listenTo(instrumentBox.selection)
        val selected = for {
          s <- i.strategy
          c <- i.validStrategies.find(p => p.strategy == s.strategy && p.altairMode == s.altairMode)
        } yield c
        selected.foreach { s =>
          updateGuidersModel(s, i.validStrategies)
        }
        // Update conditions
        i.ctx.map(_.getConditions).foreach(c => originalConditions = Option(c))
        // Don't listen to updates or we'll replot foreach selection
        List(sbBox, ccBox, iqBox).foreach(i => i.deafTo(i.selection))
        // Reset the foreground color if it matches the original
        if (originalConditions.map(_.sb) == i.conditions.map(_.sb)) {
          sbBox.foreground = Color.black
        }
        if (originalConditions.map(_.cc) == i.conditions.map(_.cc)) {
          ccBox.foreground = Color.black
        }
        if (originalConditions.map(_.iq) == i.conditions.map(_.iq)) {
          iqBox.foreground = Color.black
        }
        i.conditions.foreach { c =>
          sbBox.selection.item = c.sb
          ccBox.selection.item = c.cc
          iqBox.selection.item = c.iq
        }
        List(sbBox, ccBox, iqBox).foreach(i => i.listenTo(i.selection))
        i.ctx.map(_.getPositionAngle).foreach(a => pa = a)
        i.ctx.map(_.getSciencePositions).foreach(o => offsets = o.asScala.map(_.toNewModel).toSet)
        updateGuideSpeedText()
      }
      // Update the RA
      ra.updateRa(query.base.ra)
      dec.updateDec(query.base.dec)

      // Update radius constraint
      radiusStart.updateAngle(query.radiusConstraint.minLimit)
      radiusEnd.updateAngle(query.radiusConstraint.maxLimit)

      errorLabel.reset()

      buildLayout(query.filters.toList.collect { case q: MagnitudeQueryFilter => q.mc })
    }

    def updateGuidersModel(selected: SupportedStrategy, strategies: List[SupportedStrategy]): Unit = {
      // Update guiders box model
      new DefaultComboBoxModel[SupportedStrategy](new java.util.Vector(strategies.asJava)) <| {_.setSelectedItem(selected)} |> guider.peer.setModel
    }

    def updateName(t: Option[SiderealTarget]): Unit = {
      t.foreach { i =>
        // Don't update the name, Simbad often has many names for the same target
        ra.updateRa(i.coordinates.ra)
        dec.updateDec(i.coordinates.dec)
      }
      t.ifNone {
        objectName.foreground = Color.red
      }

    }

    // Makes a combo box out of the supported bands
    private def bandsBoxes(catalog: CatalogName, bandsList: BandsList): List[ComboBox[MagnitudeBand]] = {
      def bandComboBox(band: MagnitudeBand) = new ComboBox(catalog.supportedBands) with TextRenderer[MagnitudeBand] {
        selection.item = band

        override def text(a: MagnitudeBand): String = a.name.padTo(2, " ").mkString("")
      }

      val rRepresentative = catalog.rBand

      bandsList match {
        case RBandsList       => List(bandComboBox(rRepresentative)) // TODO Should we represent the R-Family as a separate entry on the combo box?
        case NiciBandsList    => List(bandComboBox(rRepresentative), bandComboBox(MagnitudeBand.V))
        case SingleBand(band) => List(bandComboBox(band))
        case NoBands          => Nil
      }
    }

    // Read the GUI values and constructs the constrains
    private def currentFilters: List[MagnitudeConstraints] =
      magnitudeControls.map {
        case MagnitudeFilterControls(_, faintess, _, saturation, bandCB, _) =>
          MagnitudeConstraints(SingleBand(bandCB.selection.item), FaintnessConstraint(faintess.text.toDouble), SaturationConstraint(saturation.text.toDouble).some)
      }.toList

    def observationInfoFromForm: ObservationInfo = {
      val selectedCatalog = catalogBox.selection.item

      val guiders = for {
        i <- 0 until guider.peer.getModel.getSize
      } yield guider.peer.getModel.getElementAt(i)

      val conditions = Conditions.NOMINAL.sb(sbBox.selection.item).cc(ccBox.selection.item).iq(iqBox.selection.item)

      val coordinates = Coordinates(ra.value, dec.value)
      ObservationInfo(None, Option(objectName.text), coordinates.some, Option(instrumentBox.selection.item), Option(guider.selection.item), guiders.toList, conditions.some, pa, allowPAFlip, offsets, selectedCatalog, ProbeLimitsTable.loadOrThrow())
    }

    // Make a query out of the form parameters
    private def buildQuery: Option[(Option[ObservationInfo], CatalogQuery)] = {
      queryButton.enabled option {
        // No validation here, the Query button is disabled unless all the controls are valid
        val radiusConstraint = RadiusConstraint.between(Angle.fromArcmin(radiusStart.text.toDouble), Angle.fromArcmin(radiusEnd.text.toDouble))
        val selectedCatalog = catalogBox.selection.item

        val coordinates = Coordinates(ra.value, dec.value)
        val info = observationInfoFromForm
        val defaultQuery = CatalogQuery(coordinates, radiusConstraint, currentFilters, selectedCatalog)

        // Start with the guider's query and update it with the values on the UI
        val calculatedQuery = guider.selection.item.query.headOption.collect {
          case c: ConeSearchCatalogQuery => c.copy(base = coordinates, radiusConstraint = radiusConstraint, magnitudeConstraints = currentFilters, catalog = selectedCatalog)
        }
        (info.some, calculatedQuery.getOrElse(defaultQuery))
      }
    }

    // Plus button to add a new row of magnitude filter
    private def addMagnitudeRowButton(index: Int) = new Button("+") {
      reactions += {
        case ButtonClicked(_) =>
          // Make a copy of the current row
          val mc = magnitudeControls.lift(index).map { mc =>
            List(mc.copy())
          }.getOrElse {
            filterControls(MagnitudeConstraints(RBandsList, FaintnessConstraint(99), SaturationConstraint(-99).some), 0)
          }
          magnitudeControls.insertAll(index, mc)
          buildLayout(currentFilters)
          // Important to re-layout the parent
          revalidateFrame()
      }
    }

    // Minus button to remove a row of magnitude filters
    private def removeMagnitudeRowButton(index: Int) = new Button("-") {
      reactions += {
        case ButtonClicked(s) =>
          magnitudeControls.lift(index).foreach { _ =>
            magnitudeControls.remove(index)
            buildLayout(currentFilters)
            // Important to re-layout the parent
            revalidateFrame()
          }
      }
    }

    // Make GUI controls for a Magnitude Constraint
    private def filterControls(mc: MagnitudeConstraints, index: Int): List[MagnitudeFilterControls] = {
      val faint = new NumberField(mc.faintnessConstraint.brightness.some, allowEmpty = false) {
        reactions += queryButtonEnabling
      }
      val sat = new NumberField(mc.saturationConstraint.map(_.brightness), allowEmpty = false) {
        reactions += queryButtonEnabling
      }
      bandsBoxes(catalogBox.selection.item, mc.searchBands).map(MagnitudeFilterControls(addMagnitudeRowButton(index), faint, new Label("-"), sat, _, removeMagnitudeRowButton(index)))
    }

  }

  private def catalogSearch(query: CatalogQuery, backend: VoTableBackend, message: String, onSuccess: (QueryResult) => Unit): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val glass = GlassLabel.show(peer, message)
    VoTableClient.catalog(query, backend)(global).onComplete {
      case scala.util.Failure(f)                           =>
        glass.foreach(_.hide())
        errorLabel.show(s"Exception: ${f.getMessage}")
      case scala.util.Success(x) if x.result.containsError =>
        glass.foreach(_.hide())
        errorLabel.show(s"Error: ${x.result.problems.head.displayValue}")
      case scala.util.Success(x)                           =>
        Swing.onEDT {
          glass.foreach(_.hide())
          onSuccess(x)
        }
    }
  }

  private def doNameSearch(search: String): Unit = {
    catalogSearch(CatalogQuery(search), SimbadNameBackend, "Searching...", { x =>
      updateName(search, x.result.targets.rows)
    })
  }

  private def reloadSearchData(obsInfo: Option[ObservationInfo], query: CatalogQuery) {
    catalogSearch(query, ConeSearchBackend, "Searching...", { x =>
      updateResults(obsInfo, x)
    })
  }

  // Shows the frame and loads the query
  protected[ui] def showWithQuery(ctx: ObsContext, mt: MagnitudeTable, q: CatalogQuery): Unit = Swing.onEDT {
    visible = true
    peer.toFront()
    reloadSearchData(ObservationInfo(ctx, mt).some, q)
  }

  // Public interface
  val instance: QueryResultsFrame.type = this

  def showOn(i: CatalogImageDisplay, n: TpeContext) {
    Option(n).flatMap(_.obsContext).foreach(showOn)
  }

  def showOn(obsCtx: ObsContext) {
    AgsRegistrar.currentStrategy(obsCtx).foreach { strategy =>
      val mt = ProbeLimitsTable.loadOrThrow()
      // TODO Use only the first query, GEMS isn't supported yet OCSADV-242, OCSADV-239
      strategy.catalogQueries(obsCtx, mt).headOption.foreach {
        case q: ConeSearchCatalogQuery =>
          showWithQuery(obsCtx, mt, q.copy(magnitudeConstraints = Nil))
        case _ =>
        // Ignore named queries
      }
    }
  }
}
