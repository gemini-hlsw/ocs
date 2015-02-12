package edu.gemini.itc.shared;

import edu.gemini.itc.parameters.SourceDefinitionParameters;

/**
 * This class encapsulates the process of creating a Spectral Energy
 * Distribution (SED).  (e.g. from a data file)
 * As written it demands a certain format to the data file.
 * Each row must contain two doubles separated by whitespace or comma,
 * the first is a wavelength in nanometers, the second is the energy in
 * arbitrary units.  Since a SED will be normalized before it is used,
 * the scale is arbitrary.
 * <p/>
 * Programmer's note: There is no need for a factory.  A factory is for
 * creating something when the client does not know which concrete type
 * to create.  Since we don't have different types of SEDs at this point,
 * we could directly create a SED.
 * Maybe this is for future support of data files in different units.
 */
public class SEDFactory {
    /**
     * Returns a SED constructed with specified values.
     */
    public static VisitableSampledSpectrum getSED(double[] flux, double wavelengthStart, double wavelengthInterval) {
        return new DefaultSampledSpectrum(flux, wavelengthStart, wavelengthInterval);
    }

    /**
     * Returns a SED read from specified data file.
     * The format of the file is as follows:
     * Lines containing two doubles separated by whitespace or commas.
     * The first is wavelength in nm.  The second is flux in arbitrary units.  e.g.
     * <pre>
     * # The data, wavelengths are in nm, flux units unknown
     *  115.0  0.181751
     *  115.5  0.203323
     *  116.0  0.142062
     *  ...
     * </pre>
     */
    public static VisitableSampledSpectrum getSED(String fileName, double wavelengthInterval) {
        // values <= 0 used to trigger different behavior in an older version but seems not be used anymore
        assert wavelengthInterval > 0.0;
        final DefaultArraySpectrum as = new DefaultArraySpectrum(fileName);
        return new DefaultSampledSpectrum(as, wavelengthInterval);
    }


    /**
     * Returns a SED read from a user submitted Data file.
     * The format of the file is as follows:
     * A line containing a double specifying the wavelength interval
     * followed by lines containing two doubles
     * separated by whitespace or commas.  The first is wavelength
     * in nm.  The second is flux in arbitrary units.  e.g.
     * <pre>
     * # Wavelength sampling size in nm
     * 0.5
     * # The data, wavelengths are in nm, flux units unknown
     *  115.0  0.181751
     *  115.5  0.203323
     *  116.0  0.142062
     *  ...
     * </pre>
     */
    public static VisitableSampledSpectrum getSED(String fileName, String userSED, double wavelengthInterval) {
        // values <= 0 used to trigger different behavior in an older version but seems not be used anymore
        assert wavelengthInterval > 0.0;
        final DefaultArraySpectrum as = DefaultArraySpectrum.fromUserSpectrum(userSED);
        return new DefaultSampledSpectrum(as, wavelengthInterval);
    }


    public static VisitableSampledSpectrum getSED(SourceDefinitionParameters sdp, Instrument instrument) {

        switch (sdp.getSourceSpec()) {
            case BBODY:
                return new BlackBodySpectrum(sdp.getBBTemp(),
                        instrument.getSampling(),
                        sdp.getSourceNormalization(),
                        sdp.getUnits(),
                        sdp.getNormBand(),
                        sdp.getRedshift());

            case ELINE:
                return new EmissionLineSpectrum(sdp.getELineWavelength(),
                        sdp.getELineWidth(),
                        sdp.getELineFlux(),
                        sdp.getELineContinuumFlux(),
                        sdp.getELineFluxUnits(),
                        sdp.getELineContinuumFluxUnits(),
                        sdp.getRedshift(),
                        instrument.getSampling());

            case PLAW:
                return new PowerLawSpectrum(sdp.getPowerLawIndex(),
                        instrument.getObservingStart(),
                        instrument.getObservingEnd(),
                        instrument.getSampling(),
                        sdp.getRedshift());

            default:
                final VisitableSampledSpectrum temp;
                if (sdp.isSedUserDefined()) {
                    temp = getSED(sdp.getSpectrumResource(),
                            sdp.getUserDefinedSpectrum(),
                            instrument.getSampling());
                } else {
                    temp = getSED(sdp.getSpectrumResource(),
                            instrument.getSampling());
                }
                temp.applyWavelengthCorrection();

                return temp;
        }
    }


    //Added to allow creation of an SED spanning more than one filter for NICI
    public static VisitableSampledSpectrum getSED(SourceDefinitionParameters sdp, double sampling, double observingStart, double observingEnd) {

        switch (sdp.getSourceSpec()) {
            case BBODY:
                return new BlackBodySpectrum(sdp.getBBTemp(),
                        sampling,
                        sdp.getSourceNormalization(),
                        sdp.getUnits(),
                        sdp.getNormBand(),
                        sdp.getRedshift());

            case ELINE:
                return new EmissionLineSpectrum(sdp.getELineWavelength(),
                        sdp.getELineWidth(),
                        sdp.getELineFlux(),
                        sdp.getELineContinuumFlux(),
                        sdp.getELineFluxUnits(),
                        sdp.getELineContinuumFluxUnits(),
                        sdp.getRedshift(),
                        sampling);

            case PLAW:
                return new PowerLawSpectrum(sdp.getPowerLawIndex(),
                        observingStart,
                        observingEnd,
                        sampling,
                        sdp.getRedshift());

            default:
                final VisitableSampledSpectrum temp;
                if (sdp.getSourceSpec() == SourceDefinitionParameters.SpectralDistribution.USER_DEFINED) {
                    temp = getSED(sdp.getSpectrumResource(),
                            sdp.getUserDefinedSpectrum(),
                            sampling);
                } else {
                    temp = getSED(sdp.getSpectrumResource(),
                            sampling);
                }
                temp.applyWavelengthCorrection();
                return temp;
        }
    }

}
