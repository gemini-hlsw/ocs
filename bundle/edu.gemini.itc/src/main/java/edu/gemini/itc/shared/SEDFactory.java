package edu.gemini.itc.shared;

import edu.gemini.itc.flamingos2.Flamingos2;
import edu.gemini.itc.gsaoi.Gsaoi;
import edu.gemini.itc.nifs.Nifs;
import edu.gemini.itc.niri.Niri;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.service.*;
import edu.gemini.spModel.core.Site;
import scala.Option;

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
public final class SEDFactory {

    /**
     * Location of SED data files
     */
    private static final String STELLAR_LIB = ITCConstants.SED_LIB + "/stellar";
    private static final String NON_STELLAR_LIB = ITCConstants.SED_LIB + "/non_stellar";
    private static final String SED_FILE_EXTENSION = ".nm";


    /**
     * Returns a SED constructed with specified values.
     */
    public static VisitableSampledSpectrum getSED(final double[] flux, final double wavelengthStart, final double wavelengthInterval) {
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
    private static VisitableSampledSpectrum getSED(final String fileName, final double wavelengthInterval) {
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
    private static VisitableSampledSpectrum getUserSED(final String userSED, final double wavelengthInterval) {
        final DefaultArraySpectrum as = DefaultArraySpectrum.fromUserSpectrum(userSED);
        return new DefaultSampledSpectrum(as, wavelengthInterval);
    }

    public static VisitableSampledSpectrum getSED(final SourceDefinition sdp, final Instrument instrument) {

        final VisitableSampledSpectrum temp;
        switch (sdp.getDistributionType()) {
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

            case USER_DEFINED:
                temp = getUserSED(sdp.getUserDefinedSpectrum(), instrument.getSampling());
                temp.applyWavelengthCorrection();
                return temp;

            case LIBRARY_STAR:
                temp = getSED(getLibraryResource(STELLAR_LIB, sdp).toLowerCase(), instrument.getSampling());
                temp.applyWavelengthCorrection();
                return temp;

            case LIBRARY_NON_STAR:
                temp = getSED(getLibraryResource(NON_STELLAR_LIB, sdp), instrument.getSampling());
                temp.applyWavelengthCorrection();
                return temp;

            default:
                throw new Error("invalid distribution type");
        }
    }

    private static String getLibraryResource(final String prefix, final SourceDefinition sdp) {
        return prefix + "/" + ((Library) sdp.distribution).sedSpectrum() + SED_FILE_EXTENSION;
    }


    // TODO: site and band could be moved to instrument(?)
    public static SourceResult calculate(final Instrument instrument, final Site site, final String bandStr, final SourceDefinition sdp, final ObservingConditions odp, final TelescopeDetails tp, final PlottingDetails pdp) {
        return calculate(instrument, site, bandStr, sdp, odp, tp, pdp, Option.apply((AOSystem) null));
    }

    public static SourceResult calculate(final Instrument instrument, final Site site, final String bandStr, final SourceDefinition sdp, final ObservingConditions odp, final TelescopeDetails tp, final PlottingDetails pdp, final Option<AOSystem> ao) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        final VisitableSampledSpectrum sed = SEDFactory.getSED(sdp, instrument);
        final SampledSpectrumVisitor redshift = new RedshiftVisitor(sdp.getRedshift());
        sed.accept(redshift);

        // Must check to see if the redshift has moved the spectrum beyond
        // useful range. The shifted spectrum must completely overlap
        // both the normalization waveband and the observation waveband
        // (filter region).

        final WavebandDefinition band = sdp.getNormBand();
        final double start = band.getStart();
        final double end = band.getEnd();

        // TODO: which instruments need this check, why only some and others not? Do all near-ir instruments need it?
        // TODO: what about Nifs and Gnirs (other near-ir instruments)?
        if (instrument instanceof Gsaoi || instrument instanceof Niri || instrument instanceof Flamingos2) {
            if (sed.getStart() > instrument.getObservingStart() || sed.getEnd() < instrument.getObservingEnd()) {
                throw new IllegalArgumentException("Shifted spectrum lies outside of observed wavelengths");
            }
        }

        // any sed except BBODY and ELINE have normalization regions
        switch (sdp.getDistributionType()) {
            case ELINE:
            case BBODY:
                break;
            default:
                if (sed.getStart() > start || sed.getEnd() < end) {
                    throw new IllegalArgumentException("Shifted spectrum lies outside of specified normalisation waveband.");
                }
        }

// TODO: This is only relevant for writeOutput() need to factor this out somehow!!
        if (pdp != null && pdp.getPlotLimits().equals(PlottingDetails.PlotLimits.USER)) {
            if (pdp.getPlotWaveL() > instrument.getObservingEnd() || pdp.getPlotWaveU() < instrument.getObservingStart()) {
                throw new IllegalArgumentException("User limits for plotting do not overlap with filter.");
            }
        }
// TODO: END

        // Module 2
        // Convert input into standard internally-used units.
        //
        // inputs: instrument,redshifted SED, waveband, normalization flux,
        // units
        // calculates: normalized SED, resampled SED, SED adjusted for aperture
        // output: SED in common internal units
        if (!sdp.getDistributionType().equals(SourceDefinition.Distribution.ELINE)) {
            final SampledSpectrumVisitor norm = new NormalizeVisitor(
                    sdp.getNormBand(),
                    sdp.getSourceNormalization(),
                    sdp.getUnits());
            sed.accept(norm);
        }

        final SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
        sed.accept(tel);

        // SED is now in units of photons/s/nm

        // Module 3b
        // The atmosphere and telescope modify the spectrum and
        // produce a background spectrum.
        //
        // inputs: SED, AIRMASS, sky emmision file, mirror configuration,
        // output: SED and sky background as they arrive at instruments

        final SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(odp.getSkyTransparencyCloud());
        sed.accept(clouds);

        final SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
                odp.getSkyTransparencyWater(),
                odp.getAirmass(),
                getWater(bandStr),
                instrument instanceof Flamingos2 ? Site.GN : site, // TODO: GN is **wrong** for F2, fix this and update regression test baseline!
                bandStr);
        sed.accept(water);

        // Background spectrum is introduced here.
        final VisitableSampledSpectrum sky = SEDFactory.getSED(getSky(instrument, bandStr, site, odp), instrument.getSampling());
        Option<VisitableSampledSpectrum> halo = Option.empty();
        if (instrument instanceof Flamingos2) {
            // TODO: F2 differs slightly from GMOS, GNIRS, Michelle, TRecs and Nifs in this (order of operations)
            // TODO: check with science if we can change this and adapt baseline for regression tests accordingly
            final SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(tp, Site.GS, ITCConstants.NEAR_IR);
            sky.accept(tb);
            final SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(tp);
            sed.accept(t);
            sky.accept(t);
            sky.accept(tel);
            halo = Option.empty();
        } else {
            // Apply telescope transmission to both sed and sky
            final SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(tp);
            sed.accept(t);
            sky.accept(t);
            // Create and Add background for the telescope.
            final SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(tp, site, bandStr);
            sky.accept(tb);

            // FOR GSAOI and NIRI ADD AO STUFF HERE
            if (instrument instanceof Gsaoi || instrument instanceof Niri) {
                // Moved section where sky/sed is convolved with instrument below Altair/Gems
                // section
                // Module 5b
                // The instrument with its detectors modifies the source and
                // background spectra.
                // input: instrument, source and background SED
                // output: total flux of source and background.
                // TODO: for GSAOI and NIRI convolve here, why??
                instrument.convolveComponents(sed);
                if (ao.isDefined()) {
                    halo = Option.apply(SEDFactory.applyAoSystem(ao.get(), sky, sed));
                }
            }

            sky.accept(tel);
        }

        // Add instrument background to sky background for a total background.
        // At this point "sky" is not the right name.
        instrument.addBackground(sky);

        // Module 4 AO module not implemented
        // The AO module affects source and background SEDs.

        // Module 5b
        // The instrument with its detectors modifies the source and
        // background spectra.
        // input: instrument, source and background SED
        // output: total flux of source and background.
        if (!(instrument instanceof Gsaoi) && !(instrument instanceof Niri)) {
            // TODO: for any instrument other than GSAOI and NIRI convolve here, why?
            instrument.convolveComponents(sed);
        }
        instrument.convolveComponents(sky);

        // TODO: AO (FOR NIFS DONE AT THE VERY END, WHY DIFFERENT FROM GSAOI/NIRI?)
        if (instrument instanceof Nifs && ao.isDefined()) {
            halo = Option.apply(SEDFactory.applyAoSystem(ao.get(), sky, sed));
        }

        // End of the Spectral energy distribution portion of the ITC.
        return new SourceResult(sed, sky, halo);
    }

    public static VisitableSampledSpectrum applyAoSystem(final AOSystem ao, final VisitableSampledSpectrum sky, final VisitableSampledSpectrum sed) {
        sky.accept(ao.getBackgroundVisitor());
        sed.accept(ao.getTransmissionVisitor());
        sky.accept(ao.getTransmissionVisitor());

        final VisitableSampledSpectrum halo = (VisitableSampledSpectrum) sed.clone();
        halo.accept(ao.getHaloFluxAttenuationVisitor());
        sed.accept(ao.getFluxAttenuationVisitor());

        return halo;
    }

    private static String getWater(final String band) {
        switch (band) {
            case ITCConstants.VISIBLE:  return "skytrans_";
            case ITCConstants.NEAR_IR:  return "nearIR_trans_";
            case ITCConstants.MID_IR:   return "midIR_trans_";
            default:                    throw new Error("invalid band");
        }
    }

    private static String getSky(final Instrument instrument, final String band, final Site site, final ObservingConditions ocp) {
        // TODO: F2 uses a peculiar path (?), fix this and update regression test baseline!
        if (instrument instanceof Flamingos2) {
            return ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.NEAR_IR_SKY_BACKGROUND_FILENAME_BASE
                        + "_"
                        + ocp.getSkyTransparencyWaterCategory() // REL-557
                        + "_" + ocp.getAirmassCategory()
                        + ITCConstants.DATA_SUFFIX;
        }
        // TODO: this is how all instruments should work:
        switch (band) {
            case ITCConstants.VISIBLE:
                return ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.OPTICAL_SKY_BACKGROUND_FILENAME_BASE
                        + "_"
                        + ocp.getSkyBackgroundCategory()
                        + "_" + ocp.getAirmassCategory()
                        + ITCConstants.DATA_SUFFIX;
            case ITCConstants.NEAR_IR:
                return "/"
                        + ITCConstants.HI_RES + (site.equals(Site.GN) ? "/mk" : "/cp")
                        + ITCConstants.NEAR_IR + ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.NEAR_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                        + ocp.getSkyTransparencyWaterCategory() + "_"
                        + ocp.getAirmassCategory()
                        + ITCConstants.DATA_SUFFIX;
            case ITCConstants.MID_IR:
                return "/"
                        + ITCConstants.HI_RES + (site.equals(Site.GN) ? "/mk" : "/cp")
                        + ITCConstants.MID_IR +ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.MID_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                        + ocp.getSkyTransparencyWaterCategory() + "_"
                        + ocp.getAirmassCategory()
                        + ITCConstants.DATA_SUFFIX;
            default:
                throw new Error("invalid band");
        }
    }

    public static final class SourceResult {
        public final VisitableSampledSpectrum sed;
        public final VisitableSampledSpectrum sky;
        public final Option<VisitableSampledSpectrum> halo;
        public SourceResult(final VisitableSampledSpectrum sed, final VisitableSampledSpectrum sky, final Option<VisitableSampledSpectrum> halo) {
            this.sed                = sed;
            this.sky                = sky;
            this.halo               = halo;
        }
    }


}
