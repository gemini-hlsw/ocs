package edu.gemini.itc.service

import edu.gemini.auxfile.client.AuxFileClient
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
import edu.gemini.spModel.core.{AuxFileSpectrum, UserDefinedSpectrum, SpectralDistribution, SPProgramID}

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

  def calculate(p: ItcParameters): Result = try {

    // Get the SED data from an aux file. For now we can assume that the ITC service is running on the same
    // machine as the database (localhost). In case this setup changes, we need to change this here, too.
    def auxFileDistribution(id: String, name: String): SpectralDistribution = {
      val programId     = SPProgramID.toProgramID(id)
      val spectrumBytes = new AuxFileClient("localhost", 8443).fetchToMemory(programId, name)
      val spectrum      = new String(spectrumBytes)
      UserDefinedSpectrum(name, spectrum)
    }

    // if a user defined source distribution is involved we need to read the aux file
    val src = p.source.distribution match {
      case AuxFileSpectrum.Undefined    => throw new RuntimeException("The user SED is undefined.")       // "User Defined", but no SED file was available
      case AuxFileSpectrum(anId, aName) => p.source.copy(distribution = auxFileDistribution(anId, aName)) // "User Defined", we need to replace placeholder with aux file
      case _                            => p.source                                                       // for all other cases we can go ahead
    }

    p.observation.getMethod match {
      case _: Imaging       => calculateImaging(p)
      case _: Spectroscopy  => calculateSpectroscopy(p)
    }

  } catch {
    case e: Throwable => ItcResult.forException(e)
  }

  // === Imaging

  private def calculateImaging(p: ItcParameters): Result =
    p.instrument match {
      case _: MichelleParameters          => ItcResult.forMessage ("Imaging not implemented.")
      case _: TRecsParameters             => ItcResult.forMessage ("Imaging not implemented.")
      case i: AcquisitionCamParameters    => imagingResult        (new AcqCamRecipe(p, i))
      case i: Flamingos2Parameters        => imagingResult        (new Flamingos2Recipe(p, i))
      case i: GmosParameters              => imagingResult        (new GmosRecipe(p, i))
      case i: GsaoiParameters             => imagingResult        (new GsaoiRecipe(p, i))
      case i: NiriParameters              => imagingResult        (new NiriRecipe(p, i))
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

  private def calculateSpectroscopy(p: ItcParameters): Result =
    p.instrument match {
      case i: MichelleParameters          => ItcResult.forMessage ("Spectroscopy not implemented.")
      case i: TRecsParameters             => ItcResult.forMessage ("Spectroscopy not implemented.")
      case i: Flamingos2Parameters        => spectroscopyResult   (new Flamingos2Recipe(p, i))
      case i: GmosParameters              => spectroscopyResult   (new GmosRecipe(p, i))
      case i: GnirsParameters             => spectroscopyResult   (new GnirsRecipe(p, i))
      case i: NifsParameters              => spectroscopyResult   (new NifsRecipe(p, i))
      case i: NiriParameters              => spectroscopyResult   (new NiriRecipe(p, i))
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
