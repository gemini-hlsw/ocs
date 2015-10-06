package edu.gemini.catalog.ui

import java.awt.Color
import javax.swing.BorderFactory._
import javax.swing.border.Border
import javax.swing.{UIManager, DefaultComboBoxModel}

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.{AgsGuideQuality, AgsRegistrar}
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.api._
import edu.gemini.catalog.votable._
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.gui.textComponent.{SelectOnFocus, TextRenderer, NumberField}
import edu.gemini.shared.gui.{ButtonFlattener, GlassLabel, SizePreference, SortableTable}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.core._
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.gemini.editor.targetComponent.GuidingIcon
import jsky.app.ot.tpe.TpeContext
import jsky.app.ot.util.Resources

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

  private class GuidingFeedbackRenderer extends Table.AbstractRenderer[AgsGuideQuality, Label](new Label) {
    override def configure(t: Table, sel: Boolean, foc: Boolean, value: AgsGuideQuality, row: Int, col: Int): Unit = {
      component.icon = GuidingIcon(value, enabled = true)
      component.text = ""
      component.preferredSize = new Dimension(GuidingIcon.sideLength + 2, GuidingIcon.sideLength + 2)
      component.maximumSize = new Dimension(GuidingIcon.sideLength  + 2, GuidingIcon.sideLength + 2)
    }
  }

  private lazy val resultsTable = new Table() with SortableTable with TableColumnsAdjuster {

    override def rendererComponent(isSelected: Boolean, focused: Boolean, row: Int, column: Int) =
      // Note that we need to use the same conversions as indicated on SortableTable to get the value
      (model, model.getValueAt(viewToModelRow(row), viewToModelColumn(column))) match {
        case (m: TargetsModel, q:AgsGuideQuality) =>
          new GuidingFeedbackRenderer().componentFor(this, isSelected, focused, q, row, column)
        case (m: TargetsModel, value) =>
          // Delegate rendering to the model
          m.rendererComponent(value ,isSelected, focused, row, column).getOrElse(super.rendererComponent(isSelected, focused, row, column))
      }
  }

  private lazy val closeButton = new Button("Close") {
    reactions += {
      case ButtonClicked(_) => QueryResultsFrame.visible = false
    }
  }

  private lazy val scrollPane = new ScrollPane() {
    contents = resultsTable
  }

  private lazy val resultsLabel = new Label("Query") {
    horizontalAlignment = Alignment.Center

    def updateCount(c: Int): Unit = {
      text = s"Query results: $c"
    }
  }

  private lazy val errorLabel = new Label("") {
    horizontalAlignment = Alignment.Left
    foreground = Color.red

    def reset(): Unit = text = ""
  }

  title = "Query Results"
  QueryForm.buildLayout(Nil)
  contents = new MigPanel(LC().fill().insets(0).gridGap("0px", "0px").debug(0)) {
    // Query Form
    add(QueryForm, CC().alignY(TopAlign).minWidth(280.px))
    // Results Table
    add(new BorderPanel() {
      border = titleBorder(title)
      add(scrollPane, BorderPanel.Position.Center)
    }, CC().grow().spanX(2).pushY().pushX())
    // Labels and command buttons at the bottom
    add(resultsLabel, CC().alignX(LeftAlign).alignY(BaselineAlign).newline().gap(10.px, 10.px, 10.px, 10.px))
    add(errorLabel, CC().alignX(LeftAlign).alignY(BaselineAlign).gap(10.px, 10.px, 10.px, 10.px))
    add(closeButton, CC().alignX(RightAlign).alignY(BaselineAlign).gap(10.px, 10.px, 10.px, 10.px))
  }

  // Set initial size
  adjustSize()
  // Update location according to the last save position
  SizePreference.getPosition(this.getClass).foreach { p =>
    location = p
  }

  // Save position and dimensions
  listenTo(this)
  reactions += {
    case _: UIElementResized =>
      SizePreference.setDimension(getClass, Some(this.size))
    case _: UIElementMoved =>
      SizePreference.setPosition(getClass, Some(this.location))
  }


  /** Create a titled border with inner and outer padding. */
  def titleBorder(title: String): Border =
    createCompoundBorder(
      createEmptyBorder(2, 2, 2, 2),
      createCompoundBorder(
        createTitledBorder(title),
        createEmptyBorder(2, 2, 2, 2)))

  /**
   * Show error message at the bottom line
   */
  def displayError(error: String): Unit = {
    errorLabel.text = error
  }

  /**
   * Called after a name search completes
   */
  def updateName(search: String, targets: List[SiderealTarget]): Unit = {
    QueryForm.updateName(targets.headOption)
    targets.headOption.ifNone {
      errorLabel.text = s"Target '$search' not found..."
    }
  }

  /**
   * Called after  a query completes to update the UI according to the results
   */
  def updateResults(info: Option[ObservationInfo], queryResult: QueryResult): Unit = {
    queryResult.query match {
      case q: ConeSearchCatalogQuery =>
        val model = TargetsModel(info, q.base, queryResult.result.targets.rows)
        resultsTable.model = model

        // The sorting logic may change if the list of magnitudes changes
        resultsTable.peer.setRowSorter(model.sorter)

        // Adjust the width of the columns
        val insets = scrollPane.border.getBorderInsets(scrollPane.peer)
        resultsTable.peer.getColumnModel.getColumn(0).setResizable(false)
        resultsTable.adjustColumns(scrollPane.bounds.width - insets.left - insets.right)

        // Update the count of rows
        resultsLabel.updateCount(queryResult.result.targets.rows.length)

        // Update the query form
        QueryForm.updateQuery(info, q)
      case _ =>
    }
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

    border = titleBorder("Query Params")

    lazy val queryButton = new Button("Query") {
      reactions += {
        case ButtonClicked(_) =>
          // Hit the catalog with a new query
          buildQuery.foreach(Function.tupled(reloadSearchData))
      }
    }

    // Action to disable the query button if there are invalid fields
    val queryButtonEnabling: Reaction = {
      case ValueChanged(a) =>
        queryButton.enabled = a match {
          case f: AngleTextField[_] => f.valid
          case f: NumberField       => f.valid
          case _                    => true
        }
    }

    lazy val objectName = new TextField("") with SelectOnFocus {
      val foregroundColor = UIManager.getColor("TextField.foreground")
      listenTo(keys)
      reactions += {
        case KeyPressed(_, Key.Enter, _, _) if text.nonEmpty =>
          doNameSearch(text)
        case KeyTyped(_, _, _, _)                            =>
          errorLabel.reset()
          foreground = foregroundColor
      }
    }
    lazy val searchByName = new Button("") {
      icon = Resources.getIcon("eclipse/search.gif")
      ButtonFlattener.flatten(peer)
      reactions += {
        case ButtonClicked(_) if objectName.text.nonEmpty =>
          doNameSearch(objectName.text)
      }
    }
    lazy val instrumentName = new Label("")
    lazy val catalogBox = new ComboBox(List[CatalogName](UCAC4, PPMXL)) with TextRenderer[CatalogName] {
      override def text(a: CatalogName) = ~Option(a).map(_.displayName)
    }
    lazy val guider = new ComboBox(List.empty[SupportedStrategy]) with TextRenderer[SupportedStrategy] {
      override def text(a: SupportedStrategy) = ~Option(a).map(_.strategy.key.displayName)

      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          updateGuideSpeedText()
      }
    }
    lazy val sbBox = new ComboBox(List(SPSiteQuality.SkyBackground.values(): _*)) with TextRenderer[SPSiteQuality.SkyBackground] {
      override def text(a: SPSiteQuality.SkyBackground) = a.displayValue()
    }
    lazy val ccBox = new ComboBox(List(SPSiteQuality.CloudCover.values(): _*)) with TextRenderer[SPSiteQuality.CloudCover] {
      override def text(a: SPSiteQuality.CloudCover) = a.displayValue()
    }
    lazy val iqBox = new ComboBox(List(SPSiteQuality.ImageQuality.values(): _*)) with TextRenderer[SPSiteQuality.ImageQuality] {
      override def text(a: SPSiteQuality.ImageQuality) = a.displayValue()
    }
    lazy val limitsLabel = new Label() {
      font = font.deriveFont(font.getSize2D * 0.8f)
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
      add(instrumentName, CC().spanX(3))
      add(new Label("Guider"), CC().spanX(2).newline())
      add(guider, CC().spanX(3).growX())
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

      add(queryButton, CC().newline().span(7).pushX().alignX(RightAlign).gapTop(10.px))
    }

    /**
     * Updates the text containing the limits for the currently selected guider
     */
    def updateGuideSpeedText(): Unit =
      for {
        sel <- guider.selection.item.some
        probeLimit <- sel.limits
      } limitsLabel.text = probeLimit.detailRange

    /**
     * Update query form according to the passed values
     */
    def updateQuery(info: Option[ObservationInfo], query: ConeSearchCatalogQuery): Unit = {
      info.foreach { i =>
        objectName.text = ~i.objectName
        instrumentName.text = ~i.instrumentName
        // Update guiders box model
        val guiderModel = new DefaultComboBoxModel[SupportedStrategy](new java.util.Vector((~info.map(_.validStrategies)).asJava))
        val selected = for {
          in <- info
          s  <- in.strategy
          it <- in.validStrategies.find(_.strategy == s)
        } yield it
        selected.foreach(guiderModel.setSelectedItem)
        guider.peer.setModel(guiderModel)
        // Update conditions
        i.conditions.foreach { c =>
          sbBox.selection.item = c.sb
          ccBox.selection.item = c.cc
          iqBox.selection.item = c.iq
        }
        updateGuideSpeedText()
      }
      // Update the RA
      ra.updateRa(query.base.ra)
      dec.updateDec(query.base.dec)

      // Update radius constraint
      radiusStart.updateAngle(query.radiusConstraint.minLimit)
      radiusEnd.updateAngle(query.radiusConstraint.maxLimit)

      buildLayout(query.filters.list.collect { case q: MagnitudeQueryFilter => q.mc })
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
    private def bandsBoxes(bandsList: BandsList): List[ComboBox[MagnitudeBand]] = {
      def bandComboBox(band: MagnitudeBand) = new ComboBox(bands) with TextRenderer[MagnitudeBand] {
        selection.item = band

        override def text(a: MagnitudeBand) = a.name
      }

      bandsList match {
        case RBandsList       => List(bandComboBox(MagnitudeBand._r)) // TODO Should we represent the R-Family as a separate entry on the combo box?
        case NiciBandsList    => List(bandComboBox(MagnitudeBand._r), bandComboBox(MagnitudeBand.V))
        case SingleBand(band) => List(bandComboBox(band))
      }
    }

    // Read the GUI values and constructs the constrains
    private def currentFilters: List[MagnitudeConstraints] =
      magnitudeControls.map {
        case MagnitudeFilterControls(_, faintess, _, saturation, bandCB, _) =>
          MagnitudeConstraints(BandsList.bandList(bandCB.selection.item), FaintnessConstraint(faintess.text.toDouble), SaturationConstraint(saturation.text.toDouble).some)
      }.toList

    // Make a query out of the form parameters
    private def buildQuery: Option[(Option[ObservationInfo], CatalogQuery)] = {
      queryButton.enabled option {
        // No validation here, the Query button is disabled unless all the controls are valid
        val coordinates = Coordinates(ra.value, dec.value)
        val radiusConstraint = RadiusConstraint.between(Angle.fromArcmin(radiusStart.text.toDouble), Angle.fromArcmin(radiusEnd.text.toDouble))

        val guiders = for {
          i <- 0 until guider.peer.getModel.getSize
        } yield guider.peer.getModel.getElementAt(i)

        // TODO Change the search query for different conditions OCSADV-416
        val conditions = Conditions.NOMINAL.sb(sbBox.selection.item).cc(ccBox.selection.item).iq(iqBox.selection.item)

        val selectedCatalog = catalogBox.selection.item


        val inst = ObservationInfo.toInstrument(instrumentName.text)
        val info = ObservationInfo(None, objectName.text.some, coordinates.some, inst, Option(guider.selection.item.strategy), guiders.toList, conditions.some, selectedCatalog, ProbeLimitsTable.loadOrThrow())
        val defaultQuery = CatalogQuery(coordinates, radiusConstraint, currentFilters, selectedCatalog)
        // Start with the guider's query and update it with the values on the UI
        val calculatedQuery = guider.selection.item.query.headOption.collect {
          case c: ConeSearchCatalogQuery if currentFilters.nonEmpty => c.copy(base = coordinates, radiusConstraint = radiusConstraint, magnitudeConstraints = currentFilters, catalog = selectedCatalog)
          case c: ConeSearchCatalogQuery                            => c.copy(base = coordinates, radiusConstraint = radiusConstraint, catalog = selectedCatalog) // Use the magnitude constraints from the guider
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
      val faint = new NumberField(mc.faintnessConstraint.brightness.some) {
        reactions += queryButtonEnabling
      }
      val sat = new NumberField(mc.saturationConstraint.map(_.brightness)) {
        reactions += queryButtonEnabling
      }
      bandsBoxes(mc.searchBands).map(MagnitudeFilterControls(addMagnitudeRowButton(index), faint, new Label("-"), sat, _, removeMagnitudeRowButton(index)))
    }

  }

  private def catalogSearch(query: CatalogQuery, backend: VoTableBackend, message: String, onSuccess: (QueryResult) => Unit): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    GlassLabel.show(peer.getRootPane, message)
    VoTableClient.catalog(query, backend).onComplete {
      case scala.util.Failure(f)                           =>
        GlassLabel.hide(peer.getRootPane)
        displayError(s"Exception: ${f.getMessage}")
      case scala.util.Success(x) if x.result.containsError =>
        GlassLabel.hide(peer.getRootPane)
        displayError(s"Error: ${x.result.problems.head.displayValue}")
      case scala.util.Success(x)                           =>
        Swing.onEDT {
          GlassLabel.hide(peer.getRootPane)
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
  val instance = this

  def showOn(n: ISPNode) {
    TpeContext.apply(n).obsContext.foreach(showOn)
  }

  def showOn(obsCtx: ObsContext) {
    // TODO The user should be able to select the strategy OCSADV-403
    AgsRegistrar.currentStrategy(obsCtx).foreach { strategy =>
      val mt = ProbeLimitsTable.loadOrThrow()
      // TODO Use only the first query, GEMS isn't supported yet OCSADV-242, OCSADV-239
      strategy.catalogQueries(obsCtx, mt).headOption.foreach {
        case q: ConeSearchCatalogQuery =>
          // OCSADV-403 Display all the rows, removing the magnitude constraints
          showWithQuery(obsCtx, mt, q.copy(magnitudeConstraints = Nil))
        case _ =>
        // Ignore named queries
      }
    }
  }
}
