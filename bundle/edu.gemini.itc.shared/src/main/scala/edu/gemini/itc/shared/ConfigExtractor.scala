package edu.gemini.itc.shared

import edu.gemini.spModel.config2.{Config, ItemKey}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosCommonType

import scala.reflect.ClassTag
import scalaz.Scalaz._
import scalaz._

/**
 * A helper class that translates OT instrument configs into parameter objects that can be consumed by the ITC.
 */
object ConfigExtractor {

  private val InstrumentKey       = new ItemKey("instrument:instrument")
  private val FilterKey           = new ItemKey("instrument:filter")
  private val FpuKey              = new ItemKey("instrument:fpu")
  private val DisperserKey        = new ItemKey("instrument:disperser")
  private val CcdXBinKey          = new ItemKey("instrument:ccdXBinning")
  private val CcdYBinKey          = new ItemKey("instrument:ccdYBinning")
  private val ReadModeKey         = new ItemKey("instrument:readMode")
  private val CcdManufacturerKey  = new ItemKey("instrument:detectorManufacturer")

  def extractF2(config: Config): \/[Throwable, Flamingos2Parameters] = {
    import Flamingos2._
    for {
      filter      <- extract[Filter]    (config, FilterKey)
      grism       <- extract[Disperser] (config, DisperserKey)
      mask        <- extract[FPUnit]    (config, FpuKey)
      readMode    <- extract[ReadMode]  (config, ReadModeKey)
    } yield {
      Flamingos2Parameters(filter, grism, mask, readMode)
    }
  }

  def extractGmos(config: Config): \/[Throwable, GmosParameters] = {
    import GmosCommonType._
    for {
      filter      <- extract[Filter]    (config, FilterKey)
      grating     <- extract[Disperser] (config, DisperserKey)
      fpmask      <- extract[FPUnit]    (config, FpuKey)
      spatBin     <- extract[Binning]   (config, CcdXBinKey)
      specBin     <- extract[Binning]   (config, CcdYBinKey)
      ccdType     <- extract[DetectorManufacturer](config, CcdManufacturerKey)
      siteString  <- extract[String]    (config, InstrumentKey)
    } yield {
      val wavelen     = 500.0 // TODO
      val ifuMethod   =  None // TODO
      val site = if (siteString.equals("GMOS-N")) Site.GN else Site.GS
      GmosParameters(filter, grating, wavelen, fpmask, spatBin.getValue, specBin.getValue, ifuMethod, ccdType, site)
    }

  }

  // Helper method that enforces that whatever we get from the config
  // for the given key is not null and matches the type we expect.
  private def extract[A](config: Config, key: ItemKey)(implicit ev: ClassTag[A]): \/[Throwable, A] = {
    Option(config.getItemValue(key)).fold(nullValue[A]) { v =>
      \/.fromTryCatch(ev.runtimeClass.cast(v).asInstanceOf[A])
    }
  }

  private def nullValue[A]: \/[Throwable, A] = new NullPointerException().left

}
