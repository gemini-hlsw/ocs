package edu.gemini.itc.service

import edu.gemini.itc.acqcam.AcqCamRecipe
import edu.gemini.itc.base._
import edu.gemini.itc.flamingos2.Flamingos2Recipe
import edu.gemini.itc.gmos.GmosRecipe
import edu.gemini.itc.gnirs.GnirsRecipe
import edu.gemini.itc.gsaoi.GsaoiRecipe
import edu.gemini.itc.nifs.NifsRecipe
import edu.gemini.itc.niri.NiriRecipe
import edu.gemini.itc.operation.ImagingS2NMethodACalculation
import edu.gemini.itc.shared._

import scalaz._
import Scalaz._

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
    case e: Throwable => ItcResult.forException(e)
  }

  // === Imaging

  private def calculateImaging(source: SourceDefinition, obs: ObservationDetails, cond: ObservingConditions, tele: TelescopeDetails, ins: InstrumentDetails): Result =
    ins match {
      case i: MichelleParameters          => ItcResult.forMessage ("Imaging not implemented.")
      case i: TRecsParameters             => ItcResult.forMessage ("Imaging not implemented.")
      case i: AcquisitionCamParameters    => imagingResult        (new AcqCamRecipe(source, obs, cond, tele, i))
      case i: Flamingos2Parameters        => imagingResult        (new Flamingos2Recipe(source, obs, cond, i, tele))
      case i: GmosParameters              => imagingResult        (new GmosRecipe(source, obs, cond, i, tele))
      case i: GsaoiParameters             => imagingResult        (new GsaoiRecipe(source, obs, cond, i, tele))
      case i: NiriParameters              => imagingResult        (new NiriRecipe(source, obs, cond, i, tele))
      case _                              => ItcResult.forMessage ("Imaging with this instrument is not supported by ITC.")
    }

  private def imagingResult(recipe: ImagingRecipe): Result = {
    val r = recipe.calculateImaging()
    ItcResult.forResult(ItcImagingResult(List(toImgData(r)), r.warnings))
  }

  private def imagingResult(recipe: ImagingArrayRecipe): Result = {
    val r = recipe.calculateImaging()
    ItcResult.forResult(ItcImagingResult(r.map(toImgData).toList, combineWarnings(r.toList)))
  }

  private def toImgData(result: ImagingResult): ImgData = result.is2nCalc match {
    case i: ImagingS2NMethodACalculation  => ImgData(i.singleSNRatio(), i.totalSNRatio(), result.peakPixelCount)
    case _                                => throw new NotImplementedError
  }

  // === Spectroscopy

  private def calculateSpectroscopy(source: SourceDefinition, obs: ObservationDetails, cond: ObservingConditions, tele: TelescopeDetails, ins: InstrumentDetails): Result =
    ins match {
      case i: MichelleParameters          => ItcResult.forMessage ("Spectroscopy not implemented.")
      case i: TRecsParameters             => ItcResult.forMessage ("Spectroscopy not implemented.")
      case i: Flamingos2Parameters        => spectroscopyResult   (new Flamingos2Recipe(source, obs, cond, i , tele))
      case i: GmosParameters              => spectroscopyResult   (new GmosRecipe(source, obs, cond, i, tele))
      case i: GnirsParameters             => spectroscopyResult   (new GnirsRecipe(source, obs, cond, i, tele))
      case i: NifsParameters              => spectroscopyResult   (new NifsRecipe(source, obs, cond, i, tele))
      case i: NiriParameters              => spectroscopyResult   (new NiriRecipe(source, obs, cond, i, tele))
      case _                              => ItcResult.forMessage ("Spectroscopy with this instrument is not supported by ITC.")

    }

  private def spectroscopyResult(recipe: SpectroscopyRecipe): Result = {
    val r = recipe.calculateSpectroscopy()
    ItcResult.forResult(ItcSpectroscopyResult(r._1.charts, r._2.warnings))
  }

  private def spectroscopyResult(recipe: SpectroscopyArrayRecipe): Result = {
    val r = recipe.calculateSpectroscopy()
    val c = r._1.charts
    val w = combineWarnings(r._2.toList)
    ItcResult.forResult(ItcSpectroscopyResult(c, w))
  }

  // combine all warnings for the different CCDs and prepend a "CCD x:" in front of them
  private def combineWarnings[A <: edu.gemini.itc.base.Result](rs: List[A]): List[ItcWarning] =
    if (rs.size > 1)
      rs.zipWithIndex.flatMap { case (r, i) => r.warnings.map(w => new ItcWarning(s"CCD $i: ${w.msg}")) }
    else
      rs.head.warnings


}
