package edu.gemini.itc.web.html

import edu.gemini.itc.shared.SourceDefinition
import edu.gemini.spModel.core._

/**
 * HTML Utils
 * Move replacement code for Java HtmlPrinter here.
 */
object HtmlUtil {

  // string representation for the spectral distribution
  def sourceDistributionString(sd: SourceDefinition): String = sd.distribution match {

    case EmissionLine(wavelength, width, flux, cont) =>
      s"n emission line, at a wavelength of $wavelength, and with a width of $width.\n" +
      s"It's total flux is $flux on a flat continuum of flux density $cont."

    case BlackBody(temp) =>
      s" ${temp}K Blackbody, at ${sd.norm} ${sd.units.displayValue} in the ${sd.normBand.name} band."

    case l: LibraryStar =>
      s" ${sd.norm} ${sd.units.displayValue} ${l.sedSpectrum} star in the ${sd.normBand.name} band."

    case l: LibraryNonStar =>
      s" ${sd.norm} ${sd.units.displayValue} ${l.sedSpectrum} in the ${sd.normBand.name} band."

    case u: UserDefined =>
      s" ${sd.norm} ${sd.units.displayValue} user defined spectrum in the ${sd.normBand.name} band with the name: ${u.name}"

    case PowerLaw(index) =>
      s" Power Law Spectrum, with an index of $index and ${sd.norm} mag in the ${sd.normBand.name} band."

  }

  // string representation for the spatial profile
  def sourceProfileString(profile: SpatialProfile): String = profile match {
    case PointSource  => "point source"
    case _            => "extended source"
  }

}
