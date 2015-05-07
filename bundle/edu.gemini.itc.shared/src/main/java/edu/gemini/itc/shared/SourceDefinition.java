package edu.gemini.itc.shared;

import edu.gemini.spModel.core.Wavelength;

import java.io.Serializable;

/**
 * Container for source definition parameters.
 */
public final class SourceDefinition implements Serializable {

    public enum Recession {
        REDSHIFT,
        VELOCITY
    }

    public enum Profile {
        POINT,
        UNIFORM,
        GAUSSIAN
    }

    public enum Distribution {
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

    public final SpatialProfile profile;
    public final SpectralDistribution distribution;
    public final WavebandDefinition normBand;
    public final double norm;
    public final BrightnessUnit units;
    public final double redshift;
    // additional java enums in order to be able to use switch statements for Scala case classes
    public final Profile profileType;
    public final Distribution distributionType;

    /**
     * Constructs a SourceDefinitionParameters
     */
    public SourceDefinition(final SpatialProfile profile,
                            final SpectralDistribution distribution,
                            final double norm,
                            final BrightnessUnit units,
                            final WavebandDefinition normBand,
                            final double redshift) {
        this.profile        = profile;
        this.distribution   = distribution;
        this.norm           = norm;
        this.units          = units;
        this.normBand       = normBand;
        this.redshift       = redshift;

        // provide an enum for the spatial profile types (helps with the Java implementation -> switch)
        if      (profile instanceof PointSource)          profileType = Profile.POINT;
        else if (profile instanceof GaussianSource)       profileType = Profile.GAUSSIAN;
        else if (profile instanceof UniformSource)        profileType = Profile.UNIFORM;
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
        return profileType == Profile.UNIFORM;
    }

    public String getSourceGeometryStr() {
        switch (profileType) {
            case POINT: return "point source";
            default:    return "extended source";
        }
    }

    public double getSourceNormalization() {
        return norm;
    }

    public BrightnessUnit getUnits() {
        return units;
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

    public double getBBTemp() {
        return ((BlackBody) distribution).temperature();
    }

    public Wavelength getELineWavelength() {
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

    public String getUserDefinedSpectrum() {
        return ((UserDefined) distribution).spectrum();
    }

}
