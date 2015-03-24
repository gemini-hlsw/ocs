package jsky.app.ot.editor.seq

import javax.swing.BorderFactory

import edu.gemini.pot.sp.SPComponentType

import scala.swing.ScrollPane.BarPolicy._
import scala.swing.{Label, GridBagPanel, ScrollPane}

object ItcPanel {

  /** Creates a panel for ITC imaging results. */
  def forImaging(owner: EdIteratorFolder)       = new ItcImagingPanel(owner, new ItcImagingTable(owner))

  /** Creates a panel for ITC spectroscopy results. */
  def forSpectroscopy(owner: EdIteratorFolder)  = new ItcSpectroscopyPanel(owner, new ItcSpectroscopyTable(owner))

}

/** Base trait for different panels which are used to present ITC calculation results to the users. */
sealed trait ItcPanel extends GridBagPanel {
  val owner: EdIteratorFolder
  val table: ItcTable
  def visibleFor(t: SPComponentType): Boolean

  def update() = table.update()
}

/** Panel holding the ITC imaging calculation result table. */
class ItcImagingPanel(val owner: EdIteratorFolder, val table: ItcImagingTable) extends ItcPanel {

  border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  private val scrollPane = new ScrollPane(table) {
    verticalScrollBarPolicy = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }

  layout(scrollPane) = new Constraints {
    gridx = 0
    gridy = 0
    weightx = 1
    weighty = 1
    fill = GridBagPanel.Fill.Both
  }

  /** True for all instruments which support ITC calculations for imaging. */
  def visibleFor(t: SPComponentType): Boolean = t match {
    case SPComponentType.INSTRUMENT_ACQCAM      => true
    case SPComponentType.INSTRUMENT_FLAMINGOS2  => true
    case SPComponentType.INSTRUMENT_GMOS        => true
    case SPComponentType.INSTRUMENT_GMOSSOUTH   => true
    case SPComponentType.INSTRUMENT_GSAOI       => true
    case SPComponentType.INSTRUMENT_MICHELLE    => true
    case SPComponentType.INSTRUMENT_NIRI        => true
    case SPComponentType.INSTRUMENT_TRECS       => true
    case _                                      => false
  }
}

/** Panel holding the ITC spectroscopy calculation result table and charts. */
class ItcSpectroscopyPanel(val owner: EdIteratorFolder, val table: ItcSpectroscopyTable) extends ItcPanel {

  border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  private val scrollPane = new ScrollPane(table) {
    verticalScrollBarPolicy = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }

  private val charts = new ItcChartsPanel()

  layout(scrollPane) = new Constraints {
    gridx   = 0
    gridy   = 0
    weightx = 1
    weighty = 0.5
    fill    = GridBagPanel.Fill.Both
  }
  layout(charts) = new Constraints {
    gridx   = 0
    gridy   = 1
    weightx = 1
    weighty = 0.5
    fill    = GridBagPanel.Fill.Both
  }

  /** True for all instruments which support ITC calculations for spectroscopy. */
  def visibleFor(t: SPComponentType): Boolean = t match {
    case SPComponentType.INSTRUMENT_FLAMINGOS2  => true
    case SPComponentType.INSTRUMENT_GMOS        => true
    case SPComponentType.INSTRUMENT_GMOSSOUTH   => true
    case SPComponentType.INSTRUMENT_GNIRS       => true
    case SPComponentType.INSTRUMENT_MICHELLE    => true
    case SPComponentType.INSTRUMENT_NIFS        => true
    case SPComponentType.INSTRUMENT_NIRI        => true
    case SPComponentType.INSTRUMENT_TRECS       => true
    case _                                      => false
  }
}

/** Panel holding spectroscopy charts. */
private class ItcChartsPanel extends GridBagPanel {

  // TODO...
  private val label = new Label("Spectroscopy charts will go here..")

  layout(label) = new Constraints {
    gridx   = 0
    gridy   = 0
    weightx = 1
    weighty = 1
    fill    = GridBagPanel.Fill.Both
  }
}
