package jsky.app.ot.editor.seq

import javax.swing.table.AbstractTableModel
import javax.swing.{Icon, ListSelectionModel}

import edu.gemini.ags.api.AgsRegistrar
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.spModel.`type`.DisplayableSpType
import edu.gemini.spModel.config2.{Config, ConfigSequence, ItemKey}
import edu.gemini.spModel.core.{Peer, Wavelength}
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable.asScalaOpt
import edu.gemini.spModel.target.system.ITarget
import jsky.app.ot.userprefs.observer.ObservingPeer
import jsky.app.ot.util.OtColor

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing._
import scalaz.Scalaz._
import scalaz._
/**
 * A table to display ITC calculation results to users.
 */
trait ItcTable extends Table {

  import ItcUniqueConfig._

  val parameters: ItcParametersProvider

  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence): ItcTableModel

  def selected: Option[Future[ItcService.Result]] = selection.rows.headOption.map(model.asInstanceOf[ItcTableModel].res)

  def selectedResult(): Option[ItcSpectroscopyResult] =
    selection.rows.headOption.flatMap(result)

  def result(row: Int): Option[ItcSpectroscopyResult] =
    model.asInstanceOf[ItcSpectroscopyTableModel].result(row)

  import jsky.app.ot.editor.seq.Keys._

  // set this to the same values as in SequenceTableUI
  autoResizeMode = Table.AutoResizeMode.Off
  background = OtColor.VERY_LIGHT_GREY
  focusable = false

  // allow selection of single rows, this will display the charts
  peer.setRowSelectionAllowed(true)
  peer.setColumnSelectionAllowed(false)
  peer.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  peer.getTableHeader.setReorderingAllowed(false)

  def update() = {
    val seq = parameters.sequence
    val allKeys = seq.getStaticKeys.toSeq ++ seq.getIteratedKeys.toSeq
    val showKeys = seq.getIteratedKeys.toSeq.
      filterNot(k => ExcludedParentKeys.contains(k.getParent)).
      filterNot(ExcludedKeys.contains).
      filterNot(_.equals(INST_EXP_TIME_KEY)).   // exposure time will always be shown, don't repeat it in case it is part of the dynamic configuration
      sortBy(_.getPath)

    // update table model while keeping the current selection
    restoreSelection {
      model = tableModel(showKeys, seq)
    }

    // make all columns as wide as needed
    SequenceTabUtil.resizeTableColumns(this.peer, this.model)

  }

  // implement our own renderer that deals with alignment, formatting of double numbers, background colors etc.
  override def rendererComponent(sel: Boolean, foc: Boolean, row: Int, col: Int): Component = {

    def cellBg(m: ItcTableModel) = {
      // Use SequenceCellRenderer based background color for key columns.
      val keyBg = m.key(col).map(SequenceCellRenderer.lookupColor)
      keyBg.fold {
        if (sel) peer.getSelectionBackground else peer.getBackground
      } {
        bg => if (sel) bg.darker else bg
      }
    }

    // represent whatever is thrown at us with a label, try to stay close to layout of other sequence tables
    val m = model.asInstanceOf[ItcTableModel]
    val l = model.getValueAt(row, col) match {
      case (null,      str: String)  => new Label(str,              null, Alignment.Left)
      case (ico: Icon, str: String)  => new Label(str,              ico,  Alignment.Left)
      case d: DisplayableSpType      => new Label(d.displayValue(), null, Alignment.Left)
      case Some(i: Int)              => new Label(i.toString,       null, Alignment.Right)
      case Some(d: Double)           => new Label(f"$d%.2f",        null, Alignment.Right)
      case Some(s: String)           => new Label(s,                null, Alignment.Left)
      case None | null               => new Label("")
      case s: String                 => new Label(s,                null, Alignment.Left)
      case x                         => new Label(x.toString,       null, Alignment.Left)
    }

    // adapt label as needed
    l <|
      (_.opaque      = true)          <|
      (_.background  = cellBg(m))     <|
      (_.tooltip     = m.tooltip(col))

  }

  protected def calculateSpectroscopy(peer: Peer, instrument: SPComponentType, c: ItcUniqueConfig): Future[ItcService.Result] =
    calculate(peer, instrument, c, SpectroscopySN(c.count, c.singleExposureTime, 1.0))

  protected def calculateImaging(peer: Peer, instrument: SPComponentType, c: ItcUniqueConfig): Future[ItcService.Result] =
    calculate(peer, instrument, c, ImagingSN(c.count, c.singleExposureTime, 1.0))

  protected def calculate(peer: Peer, instrument: SPComponentType, c: ItcUniqueConfig, cm: CalculationMethod): Future[ItcService.Result] = {
    val s = for {
      analysis  <- parameters.analysisMethod
      cond      <- parameters.conditions
      port      <- parameters.instrumentPort
      targetEnv <- parameters.targetEnvironment
      probe     <- extractGuideProbe()
      src       <- extractSource(targetEnv.getBase.getTarget, c)
      tele      <- ConfigExtractor.extractTelescope(port, probe, targetEnv, c.config)
      ins       <- ConfigExtractor.extractInstrumentDetails(instrument, probe, targetEnv, c.config)
    } yield {
        doServiceCall(peer, c, src, ins, tele, cond, new ObservationDetails(cm, analysis))
    }

    s match {
      case -\/(l) => Future {
        List(l).fail
      }
      case \/-(r) => r
    }

  }

  protected def doServiceCall(peer: Peer, c: ItcUniqueConfig, src: SourceDefinition, ins: InstrumentDetails, tele: TelescopeDetails, cond: ObservingConditions, obs: ObservationDetails): Future[ItcService.Result] = {
    // Do the service call
    ItcService.calculate(peer, src, obs, cond, tele, ins).

      // whenever service call is finished notify table to update its contents
      andThen {
      case _ => Swing.onEDT {

        // notify table of data update while keeping the current selection
        restoreSelection {
          this.peer.getModel.asInstanceOf[AbstractTableModel].fireTableDataChanged()
        }

        // make all columns as wide as needed
        SequenceTabUtil.resizeTableColumns(this.peer, this.model)
      }
    }

  }

  // execute a table update while making sure that the selected row is kept (or row 0 is chosen as default)
  private def restoreSelection(updateTable: => Unit): Unit = {
    val selected = peer.getSelectedRow

    updateTable

    if (peer.getRowCount > 0) {
      val toSelect = if (selected < 0 || selected >= peer.getRowCount) 0 else selected
      peer.setRowSelectionInterval(toSelect, toSelect)
    }
  }

  private def extractGuideProbe(): String \/ GuideProbe = {
    val o = for {
      observation <- parameters.observation
      obsContext  <- ObsContext.create(observation).asScalaOpt
      agsStrategy <- AgsRegistrar.currentStrategy(obsContext)

    // Except for Gems we have only one guider, so in order to decide the "type" (AOWFS, OIWFS, PWFS)
    // we take a shortcut here and just look at the first guider we get from the strategy.
    } yield agsStrategy.guideProbes.headOption

    o.flatten.fold("Could not identify ags strategy or guide probe type".left[GuideProbe])(_.right)
  }

  private def extractSource(target: ITarget, c: ItcUniqueConfig): String \/ SourceDefinition = {
    for {
      (mag, band)     <- extractSourceMagnitude(target, c.config)
      srcProfile      <- parameters.spatialProfile
      srcDistribution <- parameters.spectralDistribution
      srcRedshift     <- parameters.redshift
    } yield {
      new SourceDefinition(srcProfile, srcDistribution, mag, BrightnessUnit.MAG, band, srcRedshift)
    }
  }

  private def extractSourceMagnitude(target: ITarget, c: Config): String \/ (Double, WavebandDefinition) = {

    def closestBand(bands: List[Magnitude], wl: Wavelength) =
      // note, at this point we've filtered out all bands without a wavelength
      bands.minBy(m => Math.abs(m.getBand.getWavelengthMidPoint.getValue.toNanometers - wl.toNanometers))

    def mags(wl: Wavelength): String \/ Magnitude = {
      val bands = target.getMagnitudes.toList.asScala.toList.
        filter(_.getBand.getWavelengthMidPoint.isDefined).// ignore bands with unknown wavelengths (currently AP only)
        filterNot(_.getBand == Magnitude.Band.UC).        // ignore UC magnitudes
        filterNot(_.getBand == Magnitude.Band.AP)         // ignore AP magnitudes
      if (bands.isEmpty) "No standard magnitudes for target defined; ITC can not use UC and AP magnitudes.".left[Magnitude]
      else closestBand(bands, wl).right[String]
    }

    for {
      wl  <- ConfigExtractor.extractObservingWavelength(c)
      mag <- mags(wl)
    } yield {
      val b = mag.getBrightness
      mag.getBand match {
        // TODO: unify band definitions from spModel core and itc shared so that we don't need this translation anymore
        case Magnitude.Band.u  => (b, WavebandDefinition.U)
        case Magnitude.Band.g  => (b, WavebandDefinition.g)
        case Magnitude.Band.r  => (b, WavebandDefinition.r)
        case Magnitude.Band.i  => (b, WavebandDefinition.i)
        case Magnitude.Band.z  => (b, WavebandDefinition.z)

        case Magnitude.Band.U  => (b, WavebandDefinition.U)
        case Magnitude.Band.B  => (b, WavebandDefinition.B)
        case Magnitude.Band.V  => (b, WavebandDefinition.V)
        case Magnitude.Band.R  => (b, WavebandDefinition.R)
        case Magnitude.Band.I  => (b, WavebandDefinition.I)
        case Magnitude.Band.Y  => (b, WavebandDefinition.z)
        case Magnitude.Band.J  => (b, WavebandDefinition.J)
        case Magnitude.Band.H  => (b, WavebandDefinition.H)
        case Magnitude.Band.K  => (b, WavebandDefinition.K)
        case Magnitude.Band.L  => (b, WavebandDefinition.L)
        case Magnitude.Band.M  => (b, WavebandDefinition.M)
        case Magnitude.Band.N  => (b, WavebandDefinition.N)
        case Magnitude.Band.Q  => (b, WavebandDefinition.Q)

        // UC and AP are not taken into account for ITC calculations
        case Magnitude.Band.UC => throw new Error()
        case Magnitude.Band.AP => throw new Error()
      }
    }
  }
}

class ItcImagingTable(val parameters: ItcParametersProvider) extends ItcTable {
  private val emptyTable: ItcImagingTableModel = new ItcGenericImagingTableModel(Seq(), Seq(), Seq())

  /** Creates a new table model for the current context (instrument) and config sequence.
    * Note that GMOS has a different table model with separate columns for its three CCDs. */
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence): ItcImagingTableModel = {
    ObservingPeer.getOrPrompt.fold(emptyTable) { peer =>
      parameters.instrument.fold(emptyTable) { ins =>
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

class ItcSpectroscopyTable(val parameters: ItcParametersProvider) extends ItcTable {
  private val emptyTable: ItcGenericSpectroscopyTableModel = new ItcGenericSpectroscopyTableModel(Seq(), Seq(), Seq())

  /** Creates a new table model for the current context and config sequence. */
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence) =
    ObservingPeer.getOrPrompt.fold(emptyTable) { peer =>
      parameters.instrument.fold(emptyTable) { ins =>
        val uniqueConfigs = ItcUniqueConfig.spectroscopyConfigs(seq)
        val results = uniqueConfigs.map(calculateSpectroscopy(peer, ins, _))
        new ItcGenericSpectroscopyTableModel(keys, uniqueConfigs, results)
      }
  }

}
