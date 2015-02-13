package edu.gemini.itc.parameters;

import edu.gemini.itc.shared.*;
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

    public static enum Profile {
        POINT,
        EXTENDED_UNIFORM,
        EXTENDED_GAUSSIAN
    }

    public static enum Distribution {
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

    public final SpatialProfile profile;
    public final SpectralDistribution distribution;
    public final WavebandDefinition normBand;
    public final double redshift;
    // additional java enums in order to be able to use switch statements for Scala case classes
    public final Profile profileType;
    public final Distribution distributionType;

    /**
     * Constructs a SourceDefinitionParameters
     */
    public SourceDefinitionParameters(final SpatialProfile profile,
                                      final SpectralDistribution distribution,
                                      final WavebandDefinition normBand,
                                      final double redshift) {
        this.profile        = profile;
        this.distribution   = distribution;
        this.normBand       = normBand;
        this.redshift       = redshift;

        // provide an enum for the spatial profile types (helps with the Java implementation -> switch)
        if      (profile instanceof PointSource)          profileType = Profile.POINT;
        else if (profile instanceof GaussianSource)       profileType = Profile.EXTENDED_GAUSSIAN;
        else if (profile instanceof UniformSource)        profileType = Profile.EXTENDED_UNIFORM;
        else    throw new IllegalArgumentException();

        // provide an enum for the spectral distribution types (helps with the Java implementation -> switch)
        if      (distribution instanceof BlackBody)      distributionType = Distribution.BBODY;
        else if (distribution instanceof EmissionLine)   distributionType = Distribution.ELINE;
        else if (distribution instanceof LibraryNonStar) distributionType = Distribution.LIBRARY_NON_STAR;
        else if (distribution instanceof LibraryStar)    distributionType = Distribution.LIBRARY_STAR;
        else if (distribution instanceof PowerLaw)       distributionType = Distribution.PLAW;
        else if (distribution instanceof UserDefined)    distributionType = Distribution.USER_DEFINED;
        else    throw new IllegalArgumentException();

    }

    public Profile getProfileType() {
        return profileType;
    }

    public boolean isUniform() {
        return profileType == Profile.EXTENDED_UNIFORM;
    }

    public String getSourceGeometryStr() {
        switch (profileType) {
            case POINT: return "point source";
            default:    return "extended source";
        }
    }

    public double getSourceNormalization() {
        return profile.norm();
    }

    public BrightnessUnit getUnits() {
        return profile.units();
    }

    public double getFWHM() {
        return ((GaussianSource) profile).fwhm();
    }

    public WavebandDefinition getNormBand() {
        return normBand;
    }

    public double getRedshift() {
        return redshift;
    }

    public Distribution getDistributionType() {
        return distributionType;
    }

    public String getSpecType() {
        return ((Library) distribution).specType();
    }

    public String getSpectrumResource() {
        return ((Library) distribution).sedSpectrum();
    }

    public double getBBTemp() {
        return ((BlackBody) distribution).temperature();
    }

    public double getELineWavelength() {
        return ((EmissionLine) distribution).wavelength();
    }

    public double getELineWidth() {
        return ((EmissionLine) distribution).width();
    }

    public double getELineFlux() {
        return ((EmissionLine) distribution).flux();
    }

    public double getELineContinuumFlux() {
        return ((EmissionLine) distribution).continuum();
    }

    public String getELineFluxUnits() {
        return ((EmissionLine) distribution).fluxUnits();
    }

    public String getELineContinuumFluxUnits() {
        return ((EmissionLine) distribution).continuumUnits();
    }

    public double getPowerLawIndex() {
        return ((PowerLaw) distribution).index();
    }

    public boolean isSedUserDefined() {
        return distributionType.equals(Distribution.USER_DEFINED);
    }

    public String getUserDefinedSpectrum() {
        return ((UserDefined) distribution).spectrum();
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
        switch (getDistributionType()) {
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
                        " " + profile.units().displayValue() + " in the " + getNormBand().name + " band.");
                break;
            case LIBRARY_STAR:
                sb.append(" " + getSourceNormalization() + " " + profile.units().displayValue() + " " + getSpecType() +
                        " star in the " + getNormBand().name + " band.");
                break;
            case LIBRARY_NON_STAR:
                sb.append(" " + getSourceNormalization() + " " + profile.units().displayValue() + " " + getSpecType() +
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
