package jsky.app.ot.editor.seq

import edu.gemini.spModel.config2.{ItemKey, Config, ConfigSequence}
import edu.gemini.spModel.gemini.acqcam.InstAcqCam
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{GmosNorthType, GmosSouthType, InstGmosNorth, InstGmosSouth}
import edu.gemini.spModel.gemini.gnirs.GNIRSConstants
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.michelle.{InstMichelle, MichelleParams}
import edu.gemini.spModel.gemini.nifs.InstNIFS
import edu.gemini.spModel.gemini.niri.{InstNIRI, Niri}
import edu.gemini.spModel.gemini.trecs.{InstTReCS, TReCSParams}
import edu.gemini.spModel.obscomp.InstConstants
import jsky.app.ot.editor.seq.Keys._

import scalaz.Scalaz._

/**
 * Unique configurations represent sets of observes in a sequence which are done with the same
 * instrument configuration and exposure times.
 */
case class ItcUniqueConfig(count: Int, labels: String, config: Config) {

  /** Single exposure time is either the given exposure time or the time on source divided by the number of images. */
  def singleExposureTime: Double = singleTime.getOrElse(totalTime.orZero / count)

  /** Total exposure time is either total time on source (TReCS/Michelle) or the single exposure time times the number of images. */
  def totalExposureTime: Double = totalTime.getOrElse(singleTime.orZero * count)

  /** TReCS and Michelle have an optional total time on source. */
  private val totalTime = Option(config.getItemValue(INST_TIME_ON_SRC_KEY)).map(_.asInstanceOf[Double])

  /** Instruments other than TReCS and Michelle have a defined exposure time per image. */
  private val singleTime = Option(config.getItemValue(INST_EXP_TIME_KEY)).map(_.asInstanceOf[Double])

}

/**
 * Utility functions to get unique configurations from ConfigurationSequence objects.
 */
object ItcUniqueConfig {

  /** The following keys are for configuration values which are not relevant for ITC calculations
    * and must be excluded when deciding on which configurations are unique with respect to ITC. */
  val ExcludedParentKeys = Set (
      CALIBRATION_KEY,
      TELESCOPE_KEY,                                // this includes P- and Q offsets
      OBSERVE_KEY                                   // this includes the data label
    )
  val ExcludedKeys = Set(
      // == device specific exceptions
      new ItemKey("instrument:dtaXOffset")          // DTA X-Offset (GMOS)
    )


  /** Gets all unique science imaging configurations from the given sequence. */
  def imagingConfigs(seq: ConfigSequence): Seq[ItcUniqueConfig] =
    uniqueConfigs(seq, c => isScience(c) && isImaging(c))

  /** Gets all unique spectroscopy configurations from the given sequence. */
  def spectroscopyConfigs(seq: ConfigSequence): Seq[ItcUniqueConfig] =
    uniqueConfigs(seq, c => isScience(c) && isSpectroscopy(c))

  // Checks if a config is science or not; only science observations are relevant for ITC
  private def isScience(c: Config): Boolean =
    Option(c.getItemValue(OBS_TYPE_KEY)).fold(false)(_.equals(InstConstants.SCIENCE_OBSERVE_TYPE))

  // Decides if a configuration is for spectroscopy or not.
  private def isSpectroscopy(c: Config): Boolean = !isImaging(c)

  // Decides if a configuration is for imaging or spectroscopy based on the presence of a disperser element.
  private def isImaging(c: Config): Boolean = c.getItemValue(INST_INSTRUMENT_KEY) match {
    case InstAcqCam.INSTRUMENT_NAME_PROP    => true  // Acq cam is imaging only
    case Flamingos2.INSTRUMENT_NAME_PROP    => c.getItemValue(INST_DISPERSER_KEY).equals(Flamingos2.Disperser.NONE)
    case InstGmosNorth.INSTRUMENT_NAME_PROP => c.getItemValue(INST_DISPERSER_KEY).equals(GmosNorthType.DisperserNorth.MIRROR)
    case InstGmosSouth.INSTRUMENT_NAME_PROP => c.getItemValue(INST_DISPERSER_KEY).equals(GmosSouthType.DisperserSouth.MIRROR)
    case GNIRSConstants.INSTRUMENT_NAME_PROP=> false // GNIRS is spectroscopy only
    case Gsaoi.INSTRUMENT_NAME_PROP         => true  // Gsaoi is imaging only
    case InstMichelle.INSTRUMENT_NAME_PROP  => c.getItemValue(INST_DISPERSER_KEY).equals(MichelleParams.Disperser.MIRROR)
    case InstNIFS.INSTRUMENT_NAME_PROP      => false // NIFS is spectroscopy only
    case InstNIRI.INSTRUMENT_NAME_PROP      => c.getItemValue(INST_DISPERSER_KEY).equals(Niri.Disperser.NONE)
    case InstTReCS.INSTRUMENT_NAME_PROP     => c.getItemValue(INST_DISPERSER_KEY).equals(TReCSParams.Disperser.MIRROR)
  }

  // Gets all "unique configs" (i.e. configs that are relevant for ITC) from the given sequence. The predicate
  // defines which steps have to be taken into account, i.e. spectroscopy vs imaging and no calibrations.
  private def uniqueConfigs(seq: ConfigSequence, predicate: Config => Boolean): Seq[ItcUniqueConfig] = {
    val steps        = seq.getAllSteps.toSeq.filter(predicate)
    val groupedSteps = steps.groupBy(hash).toSeq
    val mappedSteps  = groupedSteps.map{case (h, cs) => ItcUniqueConfig(cs.size, labels(cs), cs.head)}
    mappedSteps.sortBy(_.config.getItemValue(DATALABEL_KEY).toString)
  }

  // Creates a hash for the given config step taking into account only the values that are relevant for ITC
  // calculations, e.g. ignoring offsets and data labels.
  private def hash(step: Config): Int = step.getKeys.
    filterNot(k => ExcludedParentKeys(k.getParent)).
    filterNot(ExcludedKeys).
    filterNot(step.getItemValue(_) == null).      // occasionally we get null values..
    map(step.getItemValue(_).hashCode()).
    foldLeft(17)((acc, h) => 37*acc + h)

  // Creates a text label based on the spans of steps covered by the steps.
  // E.g. ((1,2,3),(10,11),(15)) is turned into "001-003, 010-011, 015"
  private def labels(cs: Seq[Config]): String = {
    val ls = cs.map(labelIndex).sorted
    findSpans(ls).map {
      case i :: Nil => f"$i%03d"
      case i :: is  => f"$i%03d-${is.last}%03d"
    }.mkString(", ")
  }

  // Only the number at the very end is relevant
  private def labelIndex(c: Config): Int = {
    val label = c.getItemValue(DATALABEL_KEY).asInstanceOf[String]
    label.split('-').last.toInt
  }

  // Finds spans of int values that immediately follow each other.
  // E.g. (1,2,3,10,11,15) is turned into ((1,2,3),(10,11),(15)).
  // Note, this is done backwards, so we can always prepend the newest element.
  private def findSpans(ns: Seq[Int]): Seq[Seq[Int]] = {
    val e: Seq[Seq[Int]] = Seq()
    ns.foldRight(e) {
      case (i, a) if a.isEmpty           => Seq(Seq(i))
      case (i, a) if a.head.head == i+1  => (i +: a.head) +: a.tail
      case (i, a)                        => Seq(i) +: a
    }
  }

}
