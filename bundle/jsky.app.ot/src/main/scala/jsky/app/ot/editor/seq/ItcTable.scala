package jsky.app.ot.editor.seq

import java.awt.Color
import javax.swing.table.AbstractTableModel

import edu.gemini.itc.shared.TelescopeDetails.Wfs
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.{ConfigSequence, ItemKey}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.gmos.GmosCommonType
import jsky.app.ot.OT
import jsky.app.ot.userprefs.observer.ObservingPeer
import jsky.app.ot.util.OtColor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.{Swing, Table}

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

  protected def callService(c: ItcUniqueConfig): Future[ItcService.Result] = {

    ObservingPeer.get.map { peer =>
      val port = owner.getContextIssPort
      //val wfs  = ??? TODO
      val src  = new SourceDefinition(PointSource(20.0, BrightnessUnit.MAG), LibraryStar("A0V"), WavebandDefinition.R, 0.0)
      val obs  = new ObservationDetails(ImagingSN(c.count, c.singleExposureTime, 1.0), AutoAperture(5.0))
      val tele = new TelescopeDetails(TelescopeDetails.Coating.SILVER, port, Wfs.OIWFS)

      val qual = owner.getContextSiteQuality
      val cond = new ObservingConditions(qual.getImageQuality, qual.getCloudCover, qual.getWaterVapor, qual.getSkyBackground, 1.5)

      // get the instrument configuration
      val filter    = c.config.getItemValue(new ItemKey("instrument:filter")).asInstanceOf[GmosCommonType.Filter]
      val grating   = c.config.getItemValue(new ItemKey("instrument:disperser")).asInstanceOf[GmosCommonType.Disperser]
      val wavelen   = 500.0 // ??? TODO
      val fpmask    = c.config.getItemValue(new ItemKey("instrument:fpu")).asInstanceOf[GmosCommonType.FPUnit]
      val spatBin   = c.config.getItemValue(new ItemKey("instrument:ccdXBinning")).asInstanceOf[GmosCommonType.Binning]
      val specBin   = c.config.getItemValue(new ItemKey("instrument:ccdYBinning")).asInstanceOf[GmosCommonType.Binning]
      val ccdType   = c.config.getItemValue(new ItemKey("instrument:detectorManufacturer")).asInstanceOf[GmosCommonType.DetectorManufacturer]
      val ifuMethod = None
      val site      = if (c.config.getItemValue(new ItemKey("instrument:instrument")).equals("GMOS-N")) Site.GN else Site.GS
      val ins       = GmosParameters(filter, grating, wavelen, fpmask, spatBin.getValue, specBin.getValue, ifuMethod, ccdType, site)

      // Do the service call
      ItcService.calculate(OT.getKeyChain, peer, src, obs, cond, tele, ins).
      // whenever service call is finished notify table to update its contents
      andThen {
        case _ => Swing.onEDT(this.peer.getModel.asInstanceOf[AbstractTableModel].fireTableDataChanged())
      }

    }.get  // TODO: what to do if no peer is present?

  }

}

class ItcImagingTable(val owner: EdIteratorFolder) extends ItcTable {
  private val emptyTable: ItcImagingTableModel = new ItcGenericImagingTableModel(Seq(), Seq(), Seq())

  /**
   * Creates a new table model for the current context (instrument) and config sequence.
   * Note that GMOS has a different table model with separate columns for its three CCDs.
   */
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence): ItcImagingTableModel = {
    val uniqConfigs = ItcUniqueConfig.imagingConfigs(seq)
    val results     = uniqConfigs.map(callService)
    Option(owner.getContextInstrument).fold(emptyTable) {
      _.getType match {
        case SPComponentType.INSTRUMENT_GMOS      => new ItcGmosImagingTableModel(keys, uniqConfigs, results)
        case SPComponentType.INSTRUMENT_GMOSSOUTH => new ItcGmosImagingTableModel(keys, uniqConfigs, results)
        case _                                    => new ItcGenericImagingTableModel(keys, uniqConfigs, results)
      }
    }
  }
}

class ItcSpectroscopyTable(val owner: EdIteratorFolder) extends ItcTable {

  /** Creates a new table model for the current context and config sequence. */
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence) = new ItcGenericSpectroscopyTableModel(keys, ItcUniqueConfig.spectroscopyConfigs(seq), Seq())

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
