package edu.gemini.itc.service

import edu.gemini.auxfile.client.AuxFileClient
import edu.gemini.itc.acqcam.AcqCamRecipe
import edu.gemini.itc.base._
import edu.gemini.itc.flamingos2.Flamingos2Recipe
import edu.gemini.itc.ghost.GHostRecipe
import edu.gemini.itc.gmos.GmosRecipe
import edu.gemini.itc.gnirs.GnirsRecipe
import edu.gemini.itc.gsaoi.GsaoiRecipe
import edu.gemini.itc.igrins2.Igrins2Recipe
import edu.gemini.itc.nifs.NifsRecipe
import edu.gemini.itc.niri.NiriRecipe
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

  def calculate(p: ItcParameters, includeCharts: Boolean): Result = try {

    // update parameters sent from client with stuff that needs to be done on the server
    val updatedParams: ItcParameters = {

      // Get the SED data from an aux file. For now we can assume that the ITC service is running on the same
      // machine as the database (localhost). In case this setup changes, we need to change this here, too.
      def readAuxFile(id: String, name: String): SpectralDistribution = {
        val programId     = SPProgramID.toProgramID(id)
        val spectrumBytes = new AuxFileClient("localhost", 8443).fetchToMemory(programId, name)
        val spectrum      = new String(spectrumBytes)
        UserDefinedSpectrum(name, spectrum)
      }

      // if a user defined source distribution is involved we need to read the aux file and update the parameters accordingly
      def updatedSrc(s: SourceDefinition) = s.distribution match {
        case AuxFileSpectrum.Undefined    => throw new RuntimeException("The user SED is undefined.")   // "User Defined", but no SED file was available
        case AuxFileSpectrum(anId, aName) => s.copy(distribution = readAuxFile(anId, aName))            // "User Defined", we need to replace placeholder with aux file
        case _                            => s                                                          // for all other cases we can use what's there
      }

      // do any updates necessary
      p.copy(source = updatedSrc(p.source))
    }

    // execute ITC service call with updated parameters
    updatedParams.observation.calculationMethod match {
      case _: Imaging       => calculateImaging(updatedParams)
      case _: Spectroscopy  => calculateSpectroscopy(updatedParams, includeCharts)
    }

  } catch {
    case e: Throwable => ItcResult.forException(e)
  }

  def calculateCharts(p: ItcParameters): Result = try {
    // execute ITC service call with updated parameters
    p.observation.calculationMethod match {
      case _: Imaging       => ItcResult.forMessage ("Imaging not implemented.")
      case _: Spectroscopy  => calculateSpectroscopy(p, excludeCharts = false)
    }

  } catch {
    case e: Throwable => ItcResult.forException(e)
  }

  // === Imaging
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
      case i: GnirsParameters             => imagingResult        (new GnirsRecipe(p, i))
      case _                              => ItcResult.forMessage (s"Imaging with this instrument: ${p.instrument} is not supported by ITC.")
    }

  private def imagingResult(recipe: ImagingRecipe): Result = {
    val r = recipe.calculateImaging()
    val s = recipe.serviceResult(r)
    ItcResult.forResult(s)
  }

  private def imagingResult(recipe: ImagingArrayRecipe): Result = {
    val r = recipe.calculateImaging()
    val s = recipe.serviceResult(r)
    ItcResult.forResult(s)
  }


  // === Spectroscopy

  private def calculateSpectroscopy(p: ItcParameters, excludeCharts: Boolean): Result =
    p.instrument match {
      case i: Flamingos2Parameters        => spectroscopyResult   (new Flamingos2Recipe(p, i),               excludeCharts)
      case i: GmosParameters              => spectroscopyResult   (new GmosRecipe(p, i),                     excludeCharts)
      case i: GnirsParameters             => spectroscopyResult   (new GnirsRecipe(p, i),                    excludeCharts)
      case i: NifsParameters              => spectroscopyResult   (new NifsRecipe(p, i),                     excludeCharts)
      case i: NiriParameters              => spectroscopyResult   (new NiriRecipe(p, i),                     excludeCharts)
      case i: Igrins2Parameters           => spectroscopyResult   (new Igrins2Recipe(paramsInMicrons(p), i), excludeCharts)
      case i: GhostParameters             => ghostSpectroscopyResult(new GHostRecipe(p, i),                    excludeCharts)
      case _                              => ItcResult.forMessage (s"Spectroscopy with this instrument: ${p.instrument} is not supported by ITC.")

    }

  private def spectroscopyResult(recipe: SpectroscopyRecipe, excludeCharts: Boolean): Result = {
    val r = recipe.calculateSpectroscopy()
    val s = recipe.serviceResult(r, excludeCharts)
    ItcResult.forResult(s.withSignificantFigures(6))
  }

  private def spectroscopyResult(recipe: SpectroscopyArrayRecipe, excludeCharts: Boolean): Result = {
    val r = recipe.calculateSpectroscopy()
    val s = recipe.serviceResult(r, excludeCharts)
    ItcResult.forResult(s.withSignificantFigures(6))
  }

  private def ghostSpectroscopyResult(recipe: GHostRecipe, excludeCharts: Boolean): Result = {
    val r = recipe.calculateSpectroscopy()
    // we use a special method to not produce S2NChartPerRes charts or the combined charts.
    val s = recipe.serviceResult(r, excludeCharts, true)
    ItcResult.forResult(s.copy(chartGroups = s.chartGroups.take(2)).withSignificantFigures(6))
  }

  // The web page has the fields in microns and they are passed directly as doubles so the recipe expects micros
  // but json takes nanometers thus we need to convert them here.
  // The correct way should be to include units but we want this to be a limited change and avoid impacting old itc too much
  private def paramsInMicrons(p: ItcParameters): ItcParameters = {
    val obs = p.observation
    val micros = obs.calculationMethod match {
      case s: SpectroscopyIntegrationTime =>
        obs.copy(calculationMethod = s.copy(wavelengthAt = s.wavelengthAt / 1000.0))
      case s: SpectroscopyS2N =>
        obs.copy(calculationMethod = s.copy(wavelengthAt = s.wavelengthAt.map(_ / 1000.0)))
      case other => obs
    }
    p.copy(observation = micros)
  }

}
