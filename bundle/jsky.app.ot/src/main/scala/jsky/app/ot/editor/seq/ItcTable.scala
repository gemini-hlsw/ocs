package jsky.app.ot.editor.seq

import javax.swing.Icon
import javax.swing.table.AbstractTableModel

import edu.gemini.ags.api.AgsRegistrar
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.{Config, ConfigSequence, ItemKey}
import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable.asScalaOpt
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.system.ITarget
import edu.gemini.spModel.telescope.IssPort
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


  val owner: EdIteratorFolder

  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence, conditions: ObservingConditions): ItcTableModel

  import jsky.app.ot.editor.seq.Keys._

  // set this to the same values as in SequenceTableUI
  autoResizeMode = Table.AutoResizeMode.Off
  background = OtColor.VERY_LIGHT_GREY
  focusable = false
  peer.setRowSelectionAllowed(false)
  peer.setColumnSelectionAllowed(false)
  peer.getTableHeader.setReorderingAllowed(false)

  def update(conditions: ObservingConditions) = {
    val seq = sequence()
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

    model = tableModel(showKeys, seq, conditions)

  }

  override def rendererComponent(sel: Boolean, foc: Boolean, row: Int, col: Int) = {
    model.getValueAt(row, col) match {
      case Some(i: Int)    => IntRenderer.componentFor(this, sel, foc, i, row, col)
      case Some(d: Double) => DoubleRenderer.componentFor(this, sel, foc, d, row, col)
      case v               => AnyRenderer.componentFor(this, sel, foc, v, row, col)
    }
  }

  private def sequence() = Option(owner.getContextObservation).fold(new ConfigSequence) {
    ConfigBridge.extractSequence(_, null, ConfigValMapInstances.IDENTITY_MAP, true)
  }

  protected def calculateSpectroscopy(peer: Peer, c: ItcUniqueConfig, cond: ObservingConditions): Future[ItcService.Result] =
    Future {
      List("Not Implemented Yet").fail
    }

  protected def calculateImaging(peer: Peer, instrument: SPComponentType, c: ItcUniqueConfig, cond: ObservingConditions): Future[ItcService.Result] = {
    val s = for {
      port      <- extractPort()
      targetEnv <- extractTargetEnv()
      probe     <- extractGuideProbe()
      src       <- extractSource(targetEnv.getBase.getTarget, c)
      tele      <- ConfigExtractor.extractTelescope(port, probe, targetEnv, c.config)
      ins       <- ConfigExtractor.extractInstrumentDetails(instrument, probe, targetEnv, c.config)
    } yield {
        calculateImaging(peer, c, src, ins, tele, cond)
      }

    s match {
      case -\/(l) => Future {
        List(l).fail
      }
      case \/-(r) => r
    }

  }

  protected def calculateImaging(peer: Peer, c: ItcUniqueConfig, src: SourceDefinition, ins: InstrumentDetails, tele: TelescopeDetails, cond: ObservingConditions): Future[ItcService.Result] = {
    val obs = new ObservationDetails(ImagingSN(c.count, c.singleExposureTime, 1.0), AutoAperture(5.0))
//    val qual = owner.getContextSiteQuality
//    val cond = new ObservingConditions(qual.getImageQuality, qual.getCloudCover, qual.getWaterVapor, qual.getSkyBackground, 1.5)

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

  private def extractPort(): String \/ IssPort =
    Option(owner.getContextIssPort).fold("No port information available".left[IssPort])(_.right)

  private def extractTargetEnv(): String \/ TargetEnvironment =
    Option(owner.getContextTargetEnv).fold("No target environment available".left[TargetEnvironment])(_.right)

  private def extractGuideProbe(): String \/ GuideProbe = {
    val o = for {
      observation <- Option(owner.getContextObservation)
      obsContext  <- ObsContext.create(observation).asScalaOpt
      agsStrategy <- AgsRegistrar.currentStrategy(obsContext)

    // Except for Gems we have only one guider, so in order to decide the "type" (AOWFS, OIWFS, PWFS)
    // we take a shortcut here and just look at the first guider we get from the strategy.
    } yield agsStrategy.guideProbes.headOption

    o.flatten.fold("Could not identify ags strategy or guide probe type".left[GuideProbe])(_.right)
  }

  private def extractSource(target: ITarget, c: ItcUniqueConfig): String \/ SourceDefinition = {
    for {
      (mag, band) <- extractSourceMagnitude(target, c.config)
    } yield {
      // TODO: definition of spectral profile and redshift
      new SourceDefinition(PointSource(mag, BrightnessUnit.MAG), /*TODO*/LibraryStar("A0V"), band, /*TODO*/0.0)
    }
  }

  private def extractSourceMagnitude(target: ITarget, c: Config): String \/ (Double, WavebandDefinition) = {

    def closestBand(bands: List[Magnitude], wl: Double) =
      bands.minBy(m => Math.abs(m.getBand.getWavelengthMidPoint.getValue.toDouble - wl))

    def mags(wl: Double): String \/ Magnitude = {
      val bands = target.getMagnitudes.toList.asScala.toList.
        filter(_.getBand.getWavelengthMidPoint.isDefined).// ignore bands with unknown wavelengths (currently AP only)
        filterNot(_.getBand == Magnitude.Band.UC).        // ignore UC magnitudes
        filterNot(_.getBand == Magnitude.Band.AP)         // ignore AP magnitudes
      if (bands.isEmpty) "No standard magnitudes for target defined; ITC can not use UC and AP magnitudes.".left[Magnitude]
      else closestBand(bands, wl).right[String]
    }

    for {
      wl  <- ConfigExtractor.extractObservingWavelength(c)
      mag <- mags(wl*1000) // convert wavelength from micrometer [um] to nanometer [nm]
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

class ItcImagingTable(val owner: EdIteratorFolder) extends ItcTable {
  private val emptyTable: ItcImagingTableModel = new ItcGenericImagingTableModel(Seq(), Seq(), Seq())

  /** Creates a new table model for the current context (instrument) and config sequence.
    * Note that GMOS has a different table model with separate columns for its three CCDs. */
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence, conditions: ObservingConditions): ItcImagingTableModel = {
    ObservingPeer.getOrPrompt.fold(emptyTable) { peer =>
      Option(owner.getContextInstrument).map(_.getType).fold(emptyTable) { ins =>
        val uniqConfigs = ItcUniqueConfig.imagingConfigs(seq)
        val results     = uniqConfigs.map(calculateImaging(peer, ins, _, conditions))
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
  def tableModel(keys: Seq[ItemKey], seq: ConfigSequence, conditions: ObservingConditions) =
    ObservingPeer.getOrPrompt.fold(emptyTable) { peer =>
      val uniqueConfigs = ItcUniqueConfig.spectroscopyConfigs(seq)
      val results       = uniqueConfigs.map(calculateSpectroscopy(peer, _, conditions))
      new ItcGenericSpectroscopyTableModel(keys, uniqueConfigs, results)
  }

}
