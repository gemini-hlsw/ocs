package jsky.app.ot.editor.seq

import java.awt.Color

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.{ConfigSequence, ItemKey}
import jsky.app.ot.util.OtColor

import scala.swing.Table

/**
 * A table to display ITC calculation results to users.
 */
trait ItcTable extends Table {

  val owner: EdIteratorFolder
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence): ItcTableModel

  import jsky.app.ot.editor.seq.Keys._

  // set this to the same values as in SequenceTableUI
  autoResizeMode = Table.AutoResizeMode.Off
  background     = OtColor.VERY_LIGHT_GREY
  focusable      = false
  peer.setRowSelectionAllowed(false)
  peer.setColumnSelectionAllowed(false)
  peer.getTableHeader.setReorderingAllowed(false)

  def update() = {
    val seq       = sequence()
    val allKeys   = seq.getStaticKeys.toSeq ++ seq.getIteratedKeys.toSeq
    val showKeys  = seq.getIteratedKeys.toSeq.
      filterNot(_.equals(DATALABEL_KEY)).       // don't show the original data label (step number)
      filterNot(_.equals(OBS_TYPE_KEY)).        // don't show the type (science vs. calibration)
      filterNot(_.equals(OBS_EXP_TIME_KEY)).    // don't show observe exp time
      filterNot(_.equals(INST_EXP_TIME_KEY)).   // don't show instrument exp time
      filterNot(_.equals(TEL_P_KEY)).           // don't show offsets
      filterNot(_.equals(TEL_Q_KEY)).           // don't show offsets
      filterNot(_.getParent().equals(CALIBRATION_KEY)). // calibration settings are not relevant
      sortBy(_.getPath)

    val itcModel  = tableModel(showKeys, seq)
    val renderer  = new ItcCellRenderer(itcModel)
    peer.setDefaultRenderer(classOf[java.lang.Object], renderer)
    model = itcModel

    // make all columns as wide as needed
    SequenceTabUtil.resizeTableColumns(this.peer, itcModel)

  }

  private def sequence() = Option(owner.getContextObservation).fold(new ConfigSequence) {
    ConfigBridge.extractSequence(_, null, ConfigValMapInstances.IDENTITY_MAP, true)
  }

}

class ItcImagingTable(val owner: EdIteratorFolder) extends ItcTable {
  private val emptyTable: ItcImagingTableModel = new ItcGenericImagingTableModel(Seq(), Seq())

  /**
   * Creates a new table model for the current context (instrument) and config sequence.
   * Note that GMOS has a different table model with separate columns for its three CCDs.
   */
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence): ItcImagingTableModel =
    Option(owner.getContextInstrument).fold(emptyTable) {
    _.getType match {
      case SPComponentType.INSTRUMENT_GMOS        => new ItcGmosImagingTableModel(keys, UniqueConfig.imagingConfigs(seq))
      case SPComponentType.INSTRUMENT_GMOSSOUTH   => new ItcGmosImagingTableModel(keys, UniqueConfig.imagingConfigs(seq))
      case _                                      => new ItcGenericImagingTableModel(keys, UniqueConfig.imagingConfigs(seq))
    }
  }
}

class ItcSpectroscopyTable(val owner: EdIteratorFolder) extends ItcTable {

  /** Creates a new table model for the current context and config sequence. */
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence) = new ItcGenericSpectroscopyTableModel(keys, UniqueConfig.spectroscopyConfigs(seq))

}


/**
 * Cell renderer based on the sequence cell renderer used for other sequence tables. This gives us coherent
 * formatting and color coding throughout the different tables in the sequence node.
 * @param model
 */
private class ItcCellRenderer(model: ItcTableModel) extends SequenceCellRenderer {
  override def lookupColor(row: Int, col: Int): Color = model.getKeyAt(col) match {
    case Some(k)  => SequenceCellRenderer.lookupColor(k)
    case None     => Color.LIGHT_GRAY
  }
}
