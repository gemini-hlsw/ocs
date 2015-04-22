package jsky.app.ot.editor.seq

import java.awt.Color
import javax.swing.table.AbstractTableModel

import edu.gemini.ags.api.AgsRegistrar
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.{ConfigSequence, ItemKey}
import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable.asScalaOpt
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort
import jsky.app.ot.userprefs.observer.ObservingPeer
import jsky.app.ot.util.OtColor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.{Swing, Table}
import scalaz.Scalaz._
import scalaz._

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

  protected def calculateSpectroscopy(peer: Peer, c: ItcUniqueConfig): Future[ItcService.Result] =
    Future {
      List("Not Implemented Yet").fail
    }

  protected def calculateImaging(peer: Peer, instrument: SPComponentType, c: ItcUniqueConfig): Future[ItcService.Result] = {
    val s = for {
      port        <- extractPort()
      targetEnv   <- extractTargetEnv()
      probe       <- extractGuideProbe()
      tele        <- ConfigExtractor.extractTelescope(port, probe, targetEnv, c.config)
      ins         <- ConfigExtractor.extractInstrumentDetails(instrument, probe, targetEnv, c.config)
    } yield {
      calculateImaging(peer, c, ins, tele)
    }

    s match {
      case -\/(l) => Future { List(l).fail }
      case \/-(r) => r
    }

  }

  protected def calculateImaging(peer: Peer, c: ItcUniqueConfig, ins: InstrumentDetails, tele: TelescopeDetails): Future[ItcService.Result] = {
    val src  = new SourceDefinition(PointSource(20.0, BrightnessUnit.MAG), LibraryStar("A0V"), WavebandDefinition.R, 0.0)
    val obs  = new ObservationDetails(ImagingSN(c.count, c.singleExposureTime, 1.0), AutoAperture(5.0))
    val qual = owner.getContextSiteQuality
    val cond = new ObservingConditions(qual.getImageQuality, qual.getCloudCover, qual.getWaterVapor, qual.getSkyBackground, 1.5)

    // Do the service call
    ItcService.calculate(peer, src, obs, cond, tele, ins).

    // whenever service call is finished notify table to update its contents
    andThen {
      case _ => Swing.onEDT(this.peer.getModel.asInstanceOf[AbstractTableModel].fireTableDataChanged())
    }

  }

  private def extractPort(): String \/ IssPort =
    Option(owner.getContextIssPort).fold("No port information available".left[IssPort])(_.right)

  private def extractTargetEnv(): String \/ TargetEnvironment =
    Option(owner.getContextTargetEnv).fold("No target environment available".left[TargetEnvironment])(_.right)

  private def extractGuideProbe(): String \/ GuideProbe = {
    val o = for {
      observation         <- Option(owner.getContextObservation)
      obsContext          <- ObsContext.create(observation).asScalaOpt
      agsStrategy         <- AgsRegistrar.currentStrategy(obsContext)

    // Except for Gems we have only one guider, so in order to decide the "type" (AOWFS, OIWFS, PWFS)
    // we take a shortcut here and just look at the first guider we get from the strategy.
    } yield agsStrategy.guideProbes.headOption

    o.flatten.fold("Could not identify ags strategy or guide probe type".left[GuideProbe])(_.right)
  }

}

class ItcImagingTable(val owner: EdIteratorFolder) extends ItcTable {
  private val emptyTable: ItcImagingTableModel = new ItcGenericImagingTableModel(Seq(), Seq(), Seq())

  /** Creates a new table model for the current context (instrument) and config sequence.
    * Note that GMOS has a different table model with separate columns for its three CCDs. */
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence): ItcImagingTableModel = {
    ObservingPeer.getOrPrompt.fold(emptyTable) { peer =>
      Option(owner.getContextInstrument).map(_.getType).fold(emptyTable) { ins =>
        val uniqConfigs = ItcUniqueConfig.imagingConfigs(seq)
        val results     = uniqConfigs.map(calculateImaging(peer, ins, _))
        ins match {
          case INSTRUMENT_GMOS | INSTRUMENT_GMOSSOUTH => new ItcGmosImagingTableModel(keys, uniqConfigs, results)
          case _                                      => new ItcGenericImagingTableModel(keys, uniqConfigs, results)
        }
      }
    }
  }
}

class ItcSpectroscopyTable(val owner: EdIteratorFolder) extends ItcTable {
  private val emptyTable: ItcGenericSpectroscopyTableModel = new ItcGenericSpectroscopyTableModel(Seq(), Seq(), Seq())

  /** Creates a new table model for the current context and config sequence. */
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence) =
    ObservingPeer.getOrPrompt.fold(emptyTable) { peer =>
      val uniqueConfigs = ItcUniqueConfig.spectroscopyConfigs(seq)
      val results       = uniqueConfigs.map(calculateSpectroscopy(peer, _))
      new ItcGenericSpectroscopyTableModel(keys, uniqueConfigs, results)
  }

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
