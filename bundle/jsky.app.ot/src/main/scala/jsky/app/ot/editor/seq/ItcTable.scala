package jsky.app.ot.editor.seq

import java.awt.event.MouseEvent
import javax.swing.table.{AbstractTableModel, JTableHeader}
import javax.swing.{Icon, ListSelectionModel}

import edu.gemini.ags.api.AgsRegistrar
import edu.gemini.catalog.ui.TableColumnsAdjuster
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.spModel.`type`.DisplayableSpType
import edu.gemini.spModel.config2.{Config, ConfigSequence, ItemKey}
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.rich.shared.immutable.asScalaOpt
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.{Option => GOption}
import jsky.app.ot.userprefs.observer.ObservingPeer
import jsky.app.ot.util.OtColor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing._
import scalaz._
import Scalaz._

/**
 * A table to display ITC calculation results to users.
 */
trait ItcTable extends Table with TableColumnsAdjuster {

  import ItcUniqueConfig._

  val parameters: ItcParametersProvider

  def tableModel(keys: List[ItemKey], seq: ConfigSequence): ItcTableModel

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

  def update(): Unit = {
    val seq      = parameters.sequence
    val showKeys = seq.getIteratedKeys.toList.
      filterNot(k => ExcludedParentKeys(k.getParent)).
      filterNot(ExcludedKeys).
      filterNot(_ == INST_EXP_TIME_KEY).   // exposure time will always be shown, don't repeat in case it is part of the dynamic configuration
      filterNot(_ == INST_COADDS_KEY).     // coadds will always be shown for instruments that support them, don't repeat in case it is part of the dynamic configuration
      sortBy(_.getPath)

    // update table model while keeping the current selection
    restoreSelection {
      model = tableModel(showKeys, seq)
    }

    // make all columns as wide as needed
    adjustColumns(peer.getWidth - peer.getInsets.left - peer.getInsets.right)
  }

  // Add headers to the table columns.
  val newHeader: JTableHeader = {
    val oldHeader = peer.getTableHeader
    new JTableHeader() {
      override def getToolTipText(e: MouseEvent): String = {
        val idx     = columnModel.getColumnIndexAtX(e.getPoint.x)
        val realIdx = columnModel.getColumn(idx).getModelIndex
        model.asInstanceOf[ItcTableModel].tooltip(realIdx)
      }
    } <| (_.setReorderingAllowed(false)) <| (_.setColumnModel(oldHeader.getColumnModel))
  }
  peer.setTableHeader(newHeader)

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
      case Some(l: Long)             => new Label(l.toString,       null, Alignment.Right)
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

  protected def extractInputs(instrument: SPInstObsComp, when: GOption[java.lang.Long], uc: ItcUniqueConfig, method: Double => CalculationMethod): String \/ ItcParameters =
    for {
      cond      <- parameters.conditions
      port      <- parameters.instrumentPort
      targetEnv <- parameters.targetEnvironment
      analysis  <- parameters.analysisMethod
      probe     <- extractGuideProbe()
      t         <- parameters.selectedTarget
      src       <- extractSource(t, uc)
      tele      <- ConfigExtractor.extractTelescope(port, probe, targetEnv, uc.config)
      ins       <- ConfigExtractor.extractInstrumentDetails(instrument, probe, targetEnv, when, uc.config, cond)
      srcFrac   <- extractSourceFraction(uc, instrument)

    } yield ItcParameters(src, ObservationDetails(method(srcFrac), analysis), cond, tele, ins)

  protected def doServiceCall(peer: Peer, inputs: String \/ ItcParameters): Future[ItcService.Result] = inputs match {

    case -\/(err) =>
      Future.successful(ItcError(err).left).andThen { case _ => updateResults() }

    case \/-(inp) =>
      // Do the service call
      ItcService.calculate(peer, inp).andThen       { case _ => updateResults() }

    }

  // whenever service call is finished notify table to update its contents
  protected def updateResults(): Unit = Swing.onEDT {
      // notify table of data update while keeping the current selection
      restoreSelection {
        this.peer.getModel.asInstanceOf[AbstractTableModel].fireTableDataChanged()
      }

      // make all columns as wide as needed
      adjustColumns(peer.getWidth - peer.getInsets.left - peer.getInsets.right)
    }

  // lacking a simple way to decide on the "closest" (i.e. "fastest to reach") peer we talk to the observing peer
  // if defined and GN or GS otherwise, if no site is defined you're out of luck and the ITC tables will stay empty
  protected def itcServicePeer: Option[Peer] =
    ObservingPeer.get orElse ObservingPeer.peerFor(Site.GN) orElse ObservingPeer.peerFor(Site.GS) orElse None

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

  private def extractSource(target: Target, c: ItcUniqueConfig): String \/ SourceDefinition =
    for {
      (mag, band, system) <- extractSourceMagnitude(target, c.config)
      srcProfile          <- parameters.spatialProfile
      srcDistribution     <- parameters.spectralDistribution
      srcRedshift         <- parameters.redshift
    } yield SourceDefinition(srcProfile, srcDistribution, mag, system, band, srcRedshift)

  private def extractSourceMagnitude(target: Target, c: Config): String \/ (Double, MagnitudeBand, BrightnessUnit) = {

    def closestBand(bands: List[Magnitude], wl: Wavelength) =
      // note, at this point we've filtered out all bands without a wavelength
      bands.minBy(m => Math.abs(m.band.center.toNanometers - wl.toNanometers))

    def mags(wl: Wavelength): String \/ Magnitude = {
      val bands = Target.magnitudes.get(target).
        orZero.
        filterNot(_.band == MagnitudeBand.UC).        // ignore UC magnitudes
        filterNot(_.band == MagnitudeBand.AP)         // ignore AP magnitudes
      if (bands.isEmpty) "No standard magnitudes for target defined; ITC does not support UC and AP magnitudes.".left[Magnitude]
      else closestBand(bands, wl).right[String]
    }

    for {
      wl  <- ConfigExtractor.extractObservingWavelength(c)
      mag <- mags(wl)
    } yield {
      val value  = mag.value
      (value, mag.band, mag.system)
    }
  }

  // Makes an educated guesstimate of the fraction of images of the given set that are on source.
  // For imaging the field of view is the science area, for spectroscopy the slit width of the mask is used as width.
  // Then, for every image we check if the p or q offsets are more than 0.5 times the horizontal or vertical
  // field of view, if so, we assume that image is off-source, otherwise we assume it is on-source.
  private def extractSourceFraction(uc: ItcUniqueConfig, instrument: SPInstObsComp): String \/ Double = {

    // get science area / field of view in arcsecs of the instrument
    // this already takes care to replace the width with the slit width of the mask for spectroscopy
    val scienceArea = instrument.getScienceArea
    val (w, h)      = (scienceArea(0), scienceArea(1))

    // check how many images are "off-source" which is defined as having an offset > 1/2 the size of the fov
    // this is an educated guesstimate only; ROIs are not taken into account at all (yet)
    val offSource = uc.configs.toList.count { c =>
      val p = ConfigExtractor.extractDoubleFromString(c, TEL_P_KEY).getOrElse(0.0)
      val q = ConfigExtractor.extractDoubleFromString(c, TEL_Q_KEY).getOrElse(0.0)
      Math.abs(p) > w/2.0 || Math.abs(q) > h/2.0
    }

    ((uc.count - offSource.toDouble) / uc.count.toDouble).right

  }

}

class ItcImagingTable(val parameters: ItcParametersProvider) extends ItcTable {
  private val emptyTable: ItcImagingTableModel = ItcGenericImagingTableModel(List(), List(), List(), List())

  /** Creates a new table model for the current context (instrument) and config sequence.
    * Note that GMOS has a different table model with separate columns for its three CCDs. */
  def tableModel(keys: List[ItemKey], seq: ConfigSequence): ItcImagingTableModel = {

    val table = for {
      peer        <- itcServicePeer
      instrument  <- parameters.instrument

    } yield {
      val uniqueConfigs = ItcUniqueConfig.imagingConfigs(seq)
      val inputs        = uniqueConfigs.map(uc => extractInputs(instrument, parameters.schedulingBlockStart, uc, frac => ImagingS2N(uc.count * uc.coadds.getOrElse(1), uc.singleExposureTime, frac)))
      val results       = uniqueConfigs.zip(inputs).map { case (uc, i) => doServiceCall(peer, i) }

      instrument.getType match {
        case INSTRUMENT_GMOS | INSTRUMENT_GMOSSOUTH =>
          ItcGmosImagingTableModel(keys, uniqueConfigs, inputs, results)

        case INSTRUMENT_GSAOI                       =>
          ItcGsaoiImagingTableModel(keys, uniqueConfigs, inputs, results)

        case INSTRUMENT_NIRI | INSTRUMENT_GNIRS     =>
          ItcGenericImagingTableModel(keys, uniqueConfigs, inputs, results, showCoadds = true)

        case _                                      =>
          ItcGenericImagingTableModel(keys, uniqueConfigs, inputs, results, showCoadds = false)
      }

    }

    table.getOrElse(emptyTable)
  }

}

class ItcSpectroscopyTable(val parameters: ItcParametersProvider) extends ItcTable {
  private val emptyTable: ItcGenericSpectroscopyTableModel = new ItcGenericSpectroscopyTableModel(List(), List(), List(), List())

  /** Creates a new table model for the current context and config sequence. */
  def tableModel(keys: List[ItemKey], seq: ConfigSequence) = {

    val table = for {
      peer        <- itcServicePeer
      instrument  <- parameters.instrument

    } yield {
      val uniqueConfigs = ItcUniqueConfig.spectroscopyConfigs(seq)
      val inputs        = uniqueConfigs.map(uc => extractInputs(instrument, parameters.schedulingBlockStart, uc, frac => SpectroscopyS2N(uc.count * uc.coadds.getOrElse(1), uc.singleExposureTime, frac)))
      val results       = uniqueConfigs.zip(inputs).map { case (uc, i) => doServiceCall(peer, i) }

      instrument.getType match {
        case INSTRUMENT_GNIRS                                     =>
          val xDisp = instrument match {
            case i: InstGNIRS => i.checkCrossDispersed()
            case _            => false
          }
          // ITC results are slightly different depending on cross dispersion mode
          new ItcGnirsSpectroscopyTableModel(keys, uniqueConfigs, inputs, results, xDisp)

        case INSTRUMENT_GSAOI | INSTRUMENT_NIFS | INSTRUMENT_NIRI =>
          new ItcGenericSpectroscopyTableModel(keys, uniqueConfigs, inputs, results, showCoadds = true)

        case _                                                    =>
          new ItcGenericSpectroscopyTableModel(keys, uniqueConfigs, inputs, results, showCoadds = false)
      }
    }

    table.getOrElse(emptyTable)
  }

}
