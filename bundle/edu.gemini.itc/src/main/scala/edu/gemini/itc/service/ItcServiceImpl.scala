package edu.gemini.itc.service

import edu.gemini.itc.acqcam.AcqCamRecipe
import edu.gemini.itc.flamingos2.Flamingos2Recipe
import edu.gemini.itc.gmos.GmosRecipe
import edu.gemini.itc.gnirs.GnirsParameters
import edu.gemini.itc.gsaoi.GsaoiRecipe
import edu.gemini.itc.michelle.MichelleParameters
import edu.gemini.itc.nifs.NifsParameters
import edu.gemini.itc.niri.NiriRecipe
import edu.gemini.itc.operation.ImagingS2NMethodACalculation
import edu.gemini.itc.shared._
import edu.gemini.itc.trecs.TRecsParameters

/**
 * The ITC service implementation.
 *
 * Note that all results are repacked in simplified Scala case classes in order not to leak out any of the
 * implementation details of the underlying ITC functionality.
 */
class ItcServiceImpl extends ItcService {

  import ItcService._

  def calculate(source: SourceDefinition, obs: ObservationDetails, cond: ObservingConditions, tele: TelescopeDetails, ins: InstrumentDetails): Result = try {

    if (obs.getMethod.isImaging)  calculateImaging(source, obs, cond, tele, ins)
    else                          calculateSpectroscopy(source, obs, cond, tele, ins)

  } catch {
    // TODO: for now in most cases where a validation problem should be reported to the user the ITC code throws an exception instead
    case e: Throwable =>
      e.printStackTrace()
      ItcResult.forException(e)
  }

  // === Imaging

  private def calculateImaging(source: SourceDefinition, obs: ObservationDetails, cond: ObservingConditions, tele: TelescopeDetails, ins: InstrumentDetails): Result =
    ins match {
      case i: AcquisitionCamParameters    => imagingResult(new AcqCamRecipe(source, obs, cond, tele, i))
      case i: Flamingos2Parameters        => imagingResult(new Flamingos2Recipe(source, obs, cond, i, tele))
      case i: GmosParameters              => imagingArrayResult(new GmosRecipe(source, obs, cond, i, tele))
      case i: GsaoiParameters             => imagingResult(new GsaoiRecipe(source, obs, cond, i, tele))
      case i: MichelleParameters          => ItcResult.forMessage ("Imaging not implemented.")
      case i: NiriParameters              => imagingResult(new NiriRecipe(source, obs, cond, i, tele))
      case i: TRecsParameters             => ItcResult.forMessage ("Imaging not implemented.")
      case _                              => ItcResult.forMessage("This instrument does not support imaging.")
    }

  private def imagingResult(recipe: ImagingRecipe): Result =
    ItcResult.forCcd(imgResult(recipe.calculateImaging()))

  private def imagingArrayResult(recipe: ImagingArrayRecipe): Result =
    ItcResult.forCcds(recipe.calculateImaging().map(imgResult))

  private def imgResult(result: ImagingResult): ItcCalcResult = result.is2nCalc match {
    case i: ImagingS2NMethodACalculation  => ItcImagingResult(result.source, i.singleSNRatio(), i.totalSNRatio(), result.peakPixelCount)
    case _                                => throw new NotImplementedError
  }

  // === Spectroscopy

  private def calculateSpectroscopy(source: SourceDefinition, obs: ObservationDetails, cond: ObservingConditions, tele: TelescopeDetails, ins: InstrumentDetails): Result =
    ins match {
      case i: Flamingos2Parameters        => ItcResult.forMessage ("Spectroscopy not yet implemented.")
      case i: GmosParameters              => ItcResult.forMessage ("Spectroscopy not yet implemented.")
      case i: GnirsParameters             => ItcResult.forMessage ("Spectroscopy not yet implemented.")
      case i: MichelleParameters          => ItcResult.forMessage ("Spectroscopy not implemented.")
      case i: NifsParameters              => ItcResult.forMessage ("Spectroscopy not yet implemented.")
      case i: NiriParameters              => ItcResult.forMessage ("Spectroscopy not yet implemented.")
      case i: TRecsParameters             => ItcResult.forMessage ("Spectroscopy not implemented.")
      case _                              => ItcResult.forMessage ("This instrument does not support spectroscopy.")

    }

}
