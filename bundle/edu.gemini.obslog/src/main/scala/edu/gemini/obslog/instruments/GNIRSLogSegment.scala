package edu.gemini.obslog.instruments

import java.util.logging.Logger

import edu.gemini.obslog.config.model.OlLogItem
import edu.gemini.obslog.core.OlSegmentType
import edu.gemini.obslog.obslog.{ConfigMap, InstrumentLogSegment, OlLogOptions}
import edu.gemini.spModel.gemini.gnirs.{GNIRSParams, InstGNIRS}
import edu.gemini.spModel.obscomp.InstConstants

import GNIRSParams._
import GNIRSLogSegment._

import scalaz._
import Scalaz._

class GNIRSLogSegment(logItems: java.util.List[OlLogItem], obsLogOptions: OlLogOptions) extends InstrumentLogSegment(SegmentType, logItems, obsLogOptions) {

  override def decorateObservationData(map: ConfigMap): Unit = Option(map).foreach(m => {
    decorateFilter(m)
    decorateSlitWidth(m)
    decorateDisperser(m)
    decorateAcquisitionMirror(m)
    decorateCamera(m)
  })

  override def getSegmentCaption = SegmentCaption

  private def decorateFilter(map: ConfigMap): Unit = {
    // This is a hideous hack. I don't know how to improve it.
    def determineFilter: Option[String] = (for {
      v <- map.sgetOpt(FilterKey)
      f <- Option(Filter.getFilter(v, null))
    } yield map.sgetOpt(TypeKey).map {
      case InstConstants.DARK_OBSERVE_TYPE => InstConstants.DARK_OBSERVE_TYPE
      case _ => f.logValue()
    }).flatten

    def determineFilterFromWavelength: Option[String] = map.sgetOpt(CrossDispersedKey).flatMap { c =>
      if (yesOrNotNo(c)) "XD".some
      else map.sgetOpt(WavelengthKey).map { l => \/.fromTryCatchNonFatal {
          val lambda = l.toDouble
          FilterOrder.find(_.getMaxWavelength > lambda).getOrElse(GNIRSParams.Order.ONE).getBand
        }.getOrElse("?") }
      }

    // If we can determine the filter in one of the two ways, then adjust it.
    determineFilter.orElse(determineFilterFromWavelength).foreach(map.put(FilterKey, _))
  }

  // Replace the disperser value with disperser/wavelength.
  private def decorateDisperser(map: ConfigMap): Unit = for {
    v <- map.sgetOpt(DisperserKey)
    d <- Option(Disperser.getDisperser(v)).map(_.logValue())
    w <- map.sgetOpt(WavelengthKey)
  } map.put(DisperserKey, s"$d/$w")

  // This method looks at the coadds, readmode, and exposureTime to synthesize the new complicated exposure time
  private def decorateExposureTime(map: ConfigMap): Unit = {
    def lowNoiseReads: Option[Int] = {
      val lnrs = map.sgetOpt(ReadModeKey).map(ReadMode.getReadMode).map(_.getLowNoiseReads)
      if (lnrs.isEmpty) Log.severe("No readMode found in GNIRS items")
      lnrs
    }
    def extractWithLog(key: String, err: String, default: String): String = {
      val value = map.sgetOpt(key)
      if (value.isEmpty) Log.severe(err)
      value.getOrElse(default)
    }

    lowNoiseReads.foreach { lnrs =>
      val expTime = extractWithLog(ExposureTimeKey, "No exposureTime property in GNIRS items", "1")
      val coadds = extractWithLog(CoaddsKey, "No coaddsValue found in GNIRS items", "1")

      map.put(ExposureTimeKey, s"$expTime/$lnrs/$coadds")
    }
  }

  // Replace the slitwidth with the log value.
  private def decorateSlitWidth(map: ConfigMap): Unit = for {
    w <- map.sgetOpt(SlitWidthKey)
    s <- Option(SlitWidth.getSlitWidth(w))
  } map.put(SlitWidthKey, s.logValue())

  // This changes the acquisition mirror from "in" to "Y"; otherwise nothing.
  private def decorateAcquisitionMirror(map: ConfigMap): Unit = {
    val newValue = map.sgetOpt(AcqMirrorKey).collect {
      case "in" => "Y"
    }.getOrElse("")
    map.put(AcqMirrorKey, newValue)
  }

  private def decorateCamera(map: ConfigMap): Unit = {
    for {
      k <- map.sgetOpt(WavelengthKey)
      p <- map.sgetOpt(PixelScaleKey)
      c <- map.sgetOpt(CrossDispersedKey)
    } yield {
      val ps = PixelScale.getPixelScale(p)

      // Camera
      val cameraName = \/.fromTryCatchNonFatal {
        val lambda = k.toDouble
        ps match {
          case PixelScale.PS_015 if lambda > 2.5 => Camera.SHORT_RED
          case PixelScale.PS_015                 => Camera.SHORT_BLUE
          case _                 if lambda > 2.5 => Camera.LONG_RED
          case _                                 => Camera.LONG_BLUE
        }
      }.getOrElse("?")

      // Prism
      val prismName = yesOrNotNo(c) ? (ps match {
        case PixelScale.PS_015 => CrossDispersed.SXD
        case _                 => CrossDispersed.LXD
      }).displayValue() | "MIR"

      map.put(CameraKey, s"$cameraName/$prismName")
    }
  }
}

object GNIRSLogSegment {
  val Log = Logger.getLogger(classOf[GNIRSLogSegment].getName)
  val SegmentType = new OlSegmentType(InstGNIRS.SP_TYPE.narrowType)
  val SegmentCaption = "GNIRS Observing Log"

  val AcqMirrorKey      = "acqMirror"
  val CameraKey         = "camera"
  val CoaddsKey         = "coadds"
  val CrossDispersedKey = "crossDispersed"
  val DisperserKey      = "disperser"
  val ExposureTimeKey   = "exposureTime"
  val FilterKey         = "filter"
  val PixelScaleKey     = "pixelScale"
  val ReadModeKey       = "readMode"
  val SlitWidthKey      = "slitwidth"
  val TypeKey           = "type"
  val WavelengthKey     = "wavelength"

  // An order of filters to try.
  val FilterOrder = List(
    GNIRSParams.Order.SIX,
    GNIRSParams.Order.FIVE,
    GNIRSParams.Order.FOUR,
    GNIRSParams.Order.THREE,
    GNIRSParams.Order.TWO
  )

  // This is to handle the hideousness of cross dispersers.
  // Yes used to be a supported option, and now it is not: it is divided into SXD and LXD.
  // Thus, for backward compatibility, we determine if the cross disperser is yes (legacy) or not no.
  def yesOrNotNo(s: String): Boolean = s.equalsIgnoreCase("yes") || !s.equalsIgnoreCase("no")
}
