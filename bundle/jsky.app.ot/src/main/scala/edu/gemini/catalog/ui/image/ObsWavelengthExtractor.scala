package edu.gemini.catalog.ui.image

import edu.gemini.pot.sp.{ISPObservation, SPComponentType}
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.{ConfigSequence, ItemKey}
import edu.gemini.spModel.core.Wavelength
import edu.gemini.spModel.core.WavelengthConversions._
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import jsky.app.ot.tpe.{InstrumentContext, TpeContext}

import scala.reflect.ClassTag
import scalaz._
import Scalaz._

/**
  * Can read the wavelength from a given observation
  */
object ObsWavelengthExtractor {
  private val ObsWavelengthKey    = new ItemKey("instrument:observingWavelength")
  private val ColorFilterKey      = new ItemKey("instrument:colorFilter")

  /**
    * Attempt to extract the Wavelength for the observation
    */
  def extractObsWavelength(tpe: TpeContext): Option[Wavelength] =
    tpe.obsShell.flatMap(extractObsWavelength(tpe.instrument, _))

  /**
    * Read the configuration of the current observation to find the Observing Wavelength
    */
  private def extractObsWavelength(ctx: InstrumentContext, obs: ISPObservation): Option[Wavelength] = {
    // Extract a double value from a string in the configuration
    def extractDoubleFromString(c: ConfigSequence, key: ItemKey): Throwable \/ Double =
      for {
        s <- extractAs[String](c, key)
        d <- \/.fromTryCatchNonFatal(s.toDouble)
      } yield d

    // Helper method that enforces that whatever we get from the config
    // for the given key is not null and matches the type we expect.
    def extractAs[A](cs: ConfigSequence, key: ItemKey)(implicit clazz: ClassTag[A]): Throwable \/ A = {
      def missingKey(key: ItemKey): \/[Throwable, A] =
        new RuntimeException(s"Missing config value for key ${key.getPath}").left[A]

      // Read item 0 corresponding to the static configuration
      Option(cs.getItemValue(0, key)).fold(missingKey(key)) { v =>
        \/.fromTryCatchNonFatal(clazz.runtimeClass.cast(v).asInstanceOf[A])
      }
    }

    def parseWavelength(cs: ConfigSequence): Throwable \/ Wavelength = {
      if (ctx.is(SPComponentType.INSTRUMENT_ACQCAM)) {
        extractAs[AcqCamParams.ColorFilter](cs, ColorFilterKey).map(_.getCentralWavelength.toDouble).map(_.microns)
      } else if (ctx.is(SPComponentType.INSTRUMENT_GNIRS)) {
        val u = extractAs[String](cs, ObsWavelengthKey).bimap(_ => extractAs[GNIRSParams.Wavelength](cs, ObsWavelengthKey).map(_.doubleValue()), _ => extractDoubleFromString(cs, ObsWavelengthKey))
        u.merge.map(_.microns)
      } else {
        // regular instrument
        extractDoubleFromString(cs, ObsWavelengthKey).map(_.microns)
      }
    }

    Option(ConfigBridge.extractSequence(obs, new java.util.HashMap(), ConfigValMapInstances.IDENTITY_MAP)).flatMap(parseWavelength(_).toOption)
  }

}
