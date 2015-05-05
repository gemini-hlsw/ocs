package jsky.app.ot.editor.seq

import javax.swing.table.AbstractTableModel
import javax.swing.{Icon, ListSelectionModel}

import edu.gemini.ags.api.AgsRegistrar
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.spModel.config2.{Config, ConfigSequence, ItemKey}
import edu.gemini.spModel.core.{Wavelength, Peer}
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable.asScalaOpt
import edu.gemini.spModel.target.system.ITarget
import jsky.app.ot.editor.seq.ItcTable.{AnyRenderer, DoubleRenderer, IntRenderer}
import jsky.app.ot.userprefs.observer.ObservingPeer
import jsky.app.ot.util.OtColor

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.Table.LabelRenderer
import scala.swing._
import scalaz.Scalaz._
import scalaz._

object ItcTable {

  // Generic renderer for labels; deals with alignment, background color and tooltip text
  abstract sealed class Renderer[A](alignment: Alignment.Value, f: A => (Icon, String)) extends LabelRenderer[A](f)  {
    override def componentFor(table : Table, isSelected : Boolean, hasFocus : Boolean, a : A, row : Int, column : Int) : Component = {
      val c = super.componentFor(table, isSelected, hasFocus, a, row, column)
      val model = table.model.asInstanceOf[ItcTableModel]
      // Use SequenceCellRenderer based background color. This gives us coherent color coding throughout
      // the different tables in the sequence node.
      val bg = model.key(column).map(SequenceCellRenderer.lookupColor)
      val tt = model.tooltip(column)
      // set label horizontal alignment, bg color and tooltip as needed
      c.asInstanceOf[Label] <| { l =>
        l.horizontalAlignment = alignment
        l.background = bg.getOrElse(l.background)
        l.tooltip = tt
      }
    }
  }

  // Render anything by turning it into a string (or ignore it if empty)
  case object AnyRenderer extends Renderer(Alignment.Left, (o: AnyRef) => (null, o match {
    case null             => ""
    case None             => ""
    case Some(s)          => s.toString
    case x                => x.toString
  }))

  // Render an int value
  case object IntRenderer extends Renderer[Int](Alignment.Right, i => (null, i.toString))

  // Render a double value with two decimal digits
  case object DoubleRenderer extends Renderer[Double](Alignment.Right, d => (null, f"$d%.2f"))

}

/**
 * A table to display ITC calculation results to users.
 */
trait ItcTable extends Table {

  val parameters: ItcParametersProvider

  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence): ItcTableModel

  def selectedResult(): Option[ItcSpectroscopyResult] =
    selection.rows.headOption.flatMap(result)

  def result(row: Int): Option[ItcSpectroscopyResult] =
    model.asInstanceOf[ItcSpectroscopyTableModel].result(row)

  import jsky.app.ot.editor.seq.Keys._

  // set this to the same values as in SequenceTableUI
  autoResizeMode = Table.AutoResizeMode.Off
  background = OtColor.VERY_LIGHT_GREY
  focusable = false
  peer.setRowSelectionAllowed(false)
  peer.setColumnSelectionAllowed(false)
  peer.getTableHeader.setReorderingAllowed(false)

  def update() = {
    val seq = parameters.sequence
    val allKeys = seq.getStaticKeys.toSeq ++ seq.getIteratedKeys.toSeq
    val showKeys = seq.getIteratedKeys.toSeq.
      filterNot(_.equals(DATALABEL_KEY)). // don't show the original data label (step number)
      filterNot(_.equals(OBS_TYPE_KEY)). // don't show the type (science vs. calibration)
      filterNot(_.equals(OBS_EXP_TIME_KEY)). // don't show observe exp time
      filterNot(_.equals(INST_EXP_TIME_KEY)). // don't show instrument exp time
      filterNot(_.equals(TEL_P_KEY)). // don't show offsets
      filterNot(_.equals(TEL_Q_KEY)). // don't show offsets
      filterNot(_.getParent().equals(CALIBRATION_KEY)). // calibration settings are not relevant
      sortBy(_.getPath)

    // update model with new one
    model = tableModel(showKeys, seq)

  }

  override def rendererComponent(sel: Boolean, foc: Boolean, row: Int, col: Int) = {
    model.getValueAt(row, col) match {
      case Some(i: Int)    => IntRenderer.componentFor(this, sel, foc, i, row, col)
      case Some(d: Double) => DoubleRenderer.componentFor(this, sel, foc, d, row, col)
      case v               => AnyRenderer.componentFor(this, sel, foc, v, row, col)
    }
  }

  protected def calculateSpectroscopy(peer: Peer, instrument: SPComponentType, c: ItcUniqueConfig): Future[ItcService.Result] = {
    val obs = new ObservationDetails(SpectroscopySN(c.count, c.singleExposureTime, 1.0), parameters.analysisMethod)
    calculate(peer, instrument, c, obs)
  }

  protected def calculateImaging(peer: Peer, instrument: SPComponentType, c: ItcUniqueConfig): Future[ItcService.Result] = {
    val obs = new ObservationDetails(ImagingSN(c.count, c.singleExposureTime, 1.0), parameters.analysisMethod)
    calculate(peer, instrument, c, obs)
  }

  protected def calculate(peer: Peer, instrument: SPComponentType, c: ItcUniqueConfig, obs: ObservationDetails): Future[ItcService.Result] = {
    val s = for {
      cond      <- parameters.conditions
      port      <- parameters.instrumentPort
      targetEnv <- parameters.targetEnvironment
      probe     <- extractGuideProbe()
      src       <- extractSource(targetEnv.getBase.getTarget, c)
      tele      <- ConfigExtractor.extractTelescope(port, probe, targetEnv, c.config)
      ins       <- ConfigExtractor.extractInstrumentDetails(instrument, probe, targetEnv, c.config)
    } yield {
        doServiceCall(peer, c, src, ins, tele, cond, obs)
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
        this.peer.getModel.asInstanceOf[AbstractTableModel].fireTableDataChanged()
        // make all columns as wide as needed
        SequenceTabUtil.resizeTableColumns(this.peer, this.model)
      }
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
      srcProfile      <- parameters.spatialProfile(mag)
      srcDistribution <- parameters.spectralDistribution
      srcRedshift     <- parameters.redshift
    } yield {
      new SourceDefinition(srcProfile, srcDistribution, band, srcRedshift)
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

  // allow selection of single rows, this will display the charts
  peer.setRowSelectionAllowed(true)
  peer.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

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
