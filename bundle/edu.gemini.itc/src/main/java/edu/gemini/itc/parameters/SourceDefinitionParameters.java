package edu.gemini.itc.parameters;

import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.type.DisplayableSpType;


/**
 * This class holds the information from the Source Definition section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class SourceDefinitionParameters extends ITCParameters {

    public static enum BrightnessUnit implements DisplayableSpType {
        // TODO: The "displayable" units are pretty ugly, but we have to keep them for
        // TODO: now in order to be backwards compatible for regression testing.
        MAG                 ("mag"),
        ABMAG               ("ABmag"),
        JY                  ("Jy"),
        WATTS               ("watts_fd_wavelength"),
        ERGS_WAVELENGTH     ("ergs_fd_wavelength"),
        ERGS_FREQUENCY      ("ergs_fd_frequency"),
        // -- TODO: same in blue but per area, can we unify those two sets of values?
        MAG_PSA             ("mag_per_sq_arcsec"),
        ABMAG_PSA           ("ABmag_per_sq_arcsec"),
        JY_PSA              ("jy_per_sq_arcsec"),
        WATTS_PSA           ("watts_fd_wavelength_per_sq_arcsec"),
        ERGS_WAVELENGTH_PSA ("ergs_fd_wavelength_per_sq_arcsec"),
        ERGS_FREQUENCY_PSA  ("ergs_fd_frequency_per_sq_arcsec")
        ;
        private final String displayValue;
        private BrightnessUnit(final String displayName) { this.displayValue = displayName; }
        public String displayValue() {return displayValue;}
    }

    public static enum Recession {
        REDSHIFT,
        VELOCITY
    }

    public static enum SourceGeometry {
        POINT,
        EXTENDED
    }

    public static enum ExtSourceType {
        UNIFORM,
        GAUSSIAN
    }

    public static enum SourceType {
        POINT,
        EXTENDED_UNIFORM,
        EXTENDED_GAUSSIAN
    }

    public static enum SpectralDistribution {
        LIBRARY_STAR,
        LIBRARY_NON_STAR,
        BBODY,
        ELINE,
        PLAW,
        USER_DEFINED
    }

    public static final String WATTS = "watts_fd_wavelength";
    public static final String WATTS_FLUX = "watts_flux";
    public static final String ERGS_FLUX = "ergs_flux";

    public static final String SED_FILE_EXTENSION = ".nm";

    /**
     * Location of SED data files
     */
    public static final String STELLAR_LIB = ITCConstants.SED_LIB + "/stellar";
    public static final String NON_STELLAR_LIB = ITCConstants.SED_LIB + "/non_stellar";

    // Data members
    private final SourceType _sourceType;  // point or extended
    private final double _sourceNorm;  // 19.3 or 2e-17
    private final BrightnessUnit _units; // unit code
    private final double _fwhm;
    private final WavebandDefinition _normBand; // U, V, B, ...
    private final double _bBTemp;
    private final double _eLineWavelength;
    private final double _eLineWidth;
    private final double _eLineFlux;
    private final double _eLineContinuumFlux;
    private final String _eLineFluxUnits; // units for eline flux
    private final String _eLineContinuumFluxUnits;
    private final double _pLawIndex;
    private final SpectralDistribution _sourceSpec;
    private final String _specType;
    private final String _userDefinedSedString;

    // resource name of library spectrum
    private String _sedSpectrum;  // /lib/stellar/KOIII.nm

    private final double _redshift;  // z

    /**
     * Constructs a SourceDefinitionParameters from a servlet request
     *
     * @throws Exception if input data is not parsable.
     */
    public SourceDefinitionParameters(SourceType sourceType,
                                      double sourceNorm,
                                      BrightnessUnit units,
                                      double fwhm,
                                      WavebandDefinition normBand,
                                      double redshift,
                                      String spectrumResource,
                                      double bBTemp,
                                      double eLineWavelength,
                                      double eLineWidth,
                                      double eLineFlux,
                                      double eLineContinuumFlux,
                                      String eLineFluxUnits,
                                      String eLineContinuumFluxUnits,
                                      double pLawIndex,
                                      SpectralDistribution sourceSpec,
                                      String userDefinedString,
                                      String specType) {
        _sourceType = sourceType;
        _sourceNorm = sourceNorm;
        _units = units;
        _fwhm = fwhm;
        _normBand = normBand;
        _redshift = redshift;
        _sedSpectrum = spectrumResource;
        _bBTemp = bBTemp;
        _eLineWavelength = eLineWavelength;
        _eLineWidth = eLineWidth;
        _eLineFlux = eLineFlux;
        _eLineContinuumFlux = eLineContinuumFlux;
        _eLineFluxUnits = eLineFluxUnits;
        _eLineContinuumFluxUnits = eLineContinuumFluxUnits;
        _pLawIndex = pLawIndex;
        _sourceSpec = sourceSpec;
        _userDefinedSedString = userDefinedString;
        _specType = specType; // TODO: used??

        if ((sourceType == SourceType.EXTENDED_GAUSSIAN) && (_fwhm < 0.1)) {
            throw new IllegalArgumentException("Please use a Gaussian FWHM greater than 0.1");
        }

    }

    public SourceType getSourceType() {
        return _sourceType;
    }

    public boolean sourceIsUniform() {
        return _sourceType == SourceType.EXTENDED_UNIFORM;
    }

    public String getSourceGeometryStr() {
        switch (_sourceType) {
            case POINT: return "point source";
            default:    return "extended source";
        }
    }

    public double getSourceNormalization() {
        return _sourceNorm;
    }

    public BrightnessUnit getUnits() {
        return _units;
    }

    public double getFWHM() {
        return _fwhm;
    }

    public WavebandDefinition getNormBand() {
        return _normBand;
    }

    public double getRedshift() {
        return _redshift;
    }

    public SpectralDistribution getSourceSpec() {
        return _sourceSpec;
    }

    public String getSpecType() {
        return _specType;
    }

    public String getSpectrumResource() {
        return _sedSpectrum;
    }

    public double getBBTemp() {
        return _bBTemp;
    }

    public double getELineWavelength() {
        return _eLineWavelength;
    }

    public double getELineWidth() {
        return _eLineWidth;
    }

    public double getELineFlux() {
        return _eLineFlux;
    }

    public double getELineContinuumFlux() {
        return _eLineContinuumFlux;
    }

    public String getELineFluxUnits() {
        return _eLineFluxUnits;
    }

    public String getELineContinuumFluxUnits() {
        return _eLineContinuumFluxUnits;
    }

    public double getPowerLawIndex() {
        return _pLawIndex;
    }

    public boolean isSedUserDefined() {
        return _sourceSpec.equals(SpectralDistribution.USER_DEFINED);
    }

    public String getUserDefinedSpectrum() {
        return _userDefinedSedString;
    }

    /**
     * Return a human-readable string for debugging
     * NOTE: toString() is also used by NiciRecipe to create final html output.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Source Geometry:\t" + sourceGeometry() + "\n");
        sb.append("Extended source type:\t" + extendedSourceType() + "\n");
        sb.append("Source Normalization:\t" + getSourceNormalization() + "\n");
        sb.append("Units:\t\t\t" + getUnits().displayValue() + "\n");
        sb.append("Gaussian FWHM:\t" + getFWHM() + "\n");
        sb.append("Normalization Type:\tfilter\n");
        sb.append("Normalization WaveBand:\t" + getNormBand().name + "\n");
        sb.append("Normalization Wavelen:\t0.0\n");
        sb.append("Redshift:\t\t" + getRedshift() + "\n");
        sb.append("Spectrum Resource:\t" + getSpectrumResource() + "\n");
        sb.append("Black Body Temp:\t" + getBBTemp() + "\n");
        sb.append("Emission Line Central Wavelen:\t" + getELineWavelength() + "\n");
        sb.append("Emission Line Width:\t" + getELineWidth() + "\n");
        sb.append("Emission Line Flux:\t" + getELineFlux() + "\n");
        sb.append("Emission Line Continuum Flux:\t" + getELineContinuumFlux() + "\n");
        sb.append("Emission Line Units:" + getELineFluxUnits() + "\n");
        sb.append("Emission Line Cont Units:" + getELineContinuumFluxUnits() + "\n");
        sb.append("Power Law Index:" + getPowerLawIndex() + "\n");
        sb.append("\n");
        return sb.toString();
    }

    // only needed for regression testing June release 2015, remove asap
    private String sourceGeometry() {
        switch (_sourceType) {
            case POINT:                 return "pointSource";
            case EXTENDED_GAUSSIAN:     return "extendedSource";
            case EXTENDED_UNIFORM:      return "extendedSource";
            default: throw new IllegalArgumentException();
        }
    }

    // only needed for regression testing June release 2015, remove asap
    private String extendedSourceType() {
        switch (_sourceType) {
            case POINT:                 return "uniform"; // TODO: set to "null"
            case EXTENDED_GAUSSIAN:     return "gaussian";
            case EXTENDED_UNIFORM:      return "uniform";
            default: throw new IllegalArgumentException();
        }
    }

    public String printParameterSummary() {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(4);  // four decimal places
        device.clear();

        sb.append("Source spatial profile, brightness, and spectral distribution: \n");
        sb.append("  The z = ");
        sb.append(getRedshift());
        sb.append(" ");
        sb.append(getSourceGeometryStr());
        sb.append(" is a");
        switch (getSourceSpec()) {
            case ELINE:
                sb.append("n emission line, at a wavelength of " + device.toString(getELineWavelength()));
                device.setPrecision(2);
                device.clear();
                sb.append(" microns, and with a width of " + device.toString(getELineWidth()) + " km/s.\n  It's total flux is " +
                        device.toString(getELineFlux()) + " " + getELineFluxUnits() + " on a flat continuum of flux density " +
                        device.toString(getELineContinuumFlux()) + " " + getELineContinuumFluxUnits() + ".");
                break;
            case BBODY:
                sb.append(" " + getBBTemp() + "K Blackbody, at " + getSourceNormalization() +
                        " " + _units.displayValue() + " in the " + getNormBand().name + " band.");
                break;
            case LIBRARY_STAR:
                sb.append(" " + getSourceNormalization() + " " + _units.displayValue() + " " + getSpecType() +
                        " star in the " + getNormBand().name + " band.");
                break;
            case LIBRARY_NON_STAR:
                sb.append(" " + getSourceNormalization() + " " + _units.displayValue() + " " + getSpecType() +
                        " in the " + getNormBand().name + " band.");
                break;
            case USER_DEFINED:
                sb.append(" a user defined spectrum with the name: " + getSpectrumResource());
                break;
            case PLAW:
                sb.append(" Power Law Spectrum, with an index of " + getPowerLawIndex()
                        + " and " + getSourceNormalization() + " mag in the " + getNormBand().name + " band.");
                break;
        }
        sb.append("\n");
        return sb.toString();

    }

}
