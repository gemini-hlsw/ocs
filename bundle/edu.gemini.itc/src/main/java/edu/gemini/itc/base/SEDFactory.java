package edu.gemini.itc.base;

import edu.gemini.itc.flamingos2.Flamingos2;
import edu.gemini.itc.gnirs.Gnirs;
import edu.gemini.itc.gsaoi.Gsaoi;
import edu.gemini.itc.nifs.Nifs;
import edu.gemini.itc.niri.Niri;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.ObservingConditions;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.itc.shared.TelescopeDetails;
import edu.gemini.spModel.core.BlackBody;
import edu.gemini.spModel.core.EmissionLine;
import edu.gemini.spModel.core.Library;
import edu.gemini.spModel.core.LibraryNonStar;
import edu.gemini.spModel.core.LibraryStar;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.PowerLaw;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.core.UserDefinedSpectrum;
import edu.gemini.spModel.core.Wavelength;
import scala.Option;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Timestamp;

/**
 * This class encapsulates the process of creating a Spectral Energy
 * Distribution (SED).  (e.g. from a data file)
 * As written it demands a certain format to the data file.
 * Each row must contain two doubles separated by whitespace or comma,
 * the first is a wavelength in nanometers, the second is the energy in
 * arbitrary units.  Since a SED will be normalized before it is used,
 * the scale is arbitrary.
 */
public final class SEDFactory {

    /**
     * The resulting source intensity, sky intensity and, if applicable, the halo produced by the AO system.
     * The source is redshifted; all values are in photons/s/nm.
     */
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
    private static VisitableSampledSpectrum getUserSED(final UserDefinedSpectrum userSED, final double start, final double end, final double wavelengthInterval, final double redshift) {
        try {
            final DefaultArraySpectrum as = DefaultArraySpectrum.fromUserSpectrum(userSED.spectrum());
            return new DefaultSampledSpectrum(as, start, end, wavelengthInterval, redshift);

        } catch (final Exception e) {
            throw new IllegalArgumentException("Could not parse user SED " + userSED.name() + ": " + e.getMessage());
        }
    }

    private static VisitableSampledSpectrum getSED(final SourceDefinition sdp, final Instrument instrument) {

        final VisitableSampledSpectrum temp;
        if (sdp.distribution() instanceof BlackBody) {
            return BlackBodySpectrum.apply(
                    ((BlackBody) sdp.distribution()).temperature(),
                    instrument.getSampling(),
                    sdp.norm(),
                    sdp.units(),
                    sdp.normBand(),
                    sdp.redshift());

        } else if (sdp.distribution() instanceof EmissionLine) {
            final EmissionLine eLine = (EmissionLine) sdp.distribution();
            return new EmissionLineSpectrum(
                    // Wavelength is a value class, on the Java side wavelength() therefore returns just a Length
                    // and not a Wavelength object, so we need to repackage it to make it a wavelength. Blech.
                    new Wavelength(eLine.wavelength()),
                    eLine.width(),
                    eLine.flux(),
                    eLine.continuum(),
                    sdp.redshift(),
                    instrument.getSampling());

        } else if (sdp.distribution() instanceof PowerLaw) {
            return new PowerLawSpectrum(
                    ((PowerLaw) sdp.distribution()).index(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    instrument.getSampling(),
                    sdp.redshift());

        } else if (sdp.distribution() instanceof UserDefinedSpectrum) {
            final UserDefinedSpectrum userDefined = (UserDefinedSpectrum) sdp.distribution();
            final MagnitudeBand band = sdp.normBand();
            Double inst_start_wave = instrument.getObservingStart();
            Double inst_end_wave = instrument.getObservingEnd();

            // GNIRS overrides getObservingStart and getObservingEnd for each order,
            // so use the full GNIRS XD wavelength range:
            if (instrument instanceof Gnirs) {
                if (((Gnirs) instrument).XDisp_IsUsed()) {
                    inst_start_wave = 750.0;
                    inst_end_wave = 2600.0;
                }
            }

            // The user-supplied SED must cover the range of the instrument configuration AND the normalization band:
            temp = getUserSED(userDefined,
                    Math.min(inst_start_wave, band.start().toNanometers()),
                    Math.max(inst_end_wave, band.end().toNanometers()),
                    instrument.getSampling(),
                    sdp.redshift().z());
            temp.applyWavelengthCorrection();
            return temp;

        } else if (sdp.distribution() instanceof LibraryStar) {
            temp = getSED(getLibraryResource(STELLAR_LIB, sdp).toLowerCase(), instrument.getSampling());
            temp.applyWavelengthCorrection();
            return temp;

        } else if (sdp.distribution() instanceof LibraryNonStar) {
            temp = getSED(getLibraryResource(NON_STELLAR_LIB, sdp), instrument.getSampling());
            temp.applyWavelengthCorrection();
            return temp;

        } else {
            throw new Error("invalid spectral distribution type");
        }
    }

    private static String getLibraryResource(final String prefix, final SourceDefinition sdp) {
        return prefix + "/" + ((Library) sdp.distribution()).sedSpectrum() + SED_FILE_EXTENSION;
    }

    public static SourceResult calculate(final Instrument instrument, final SourceDefinition sdp, final ObservingConditions odp, final TelescopeDetails tp) {
        return calculate(instrument, sdp, odp, tp, Option.apply((AOSystem) null));
    }

    public static void creatingFile(double sampling,  VisitableSampledSpectrum sed, String fName) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String fileName = fName + "_"+ sampling + "_" + timestamp.toString().replace(' ', '_') +".dat";

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            double[][] data = sed.getData();
            for (int i = 0; i < data[0].length; i++) {
                if (data[0][i] < 1200)
                   writer.append(data[0][i] + "\t" + data[1][i] + "\n");
            }
            writer.close();
            System.out.println("File created " + fileName);
        } catch (Exception e) {
            System.out.println("Error creating the file " + fName);
        }
    }

    public static SourceResult calculate(final Instrument instrument, final SourceDefinition sdp, final ObservingConditions odp, final TelescopeDetails tp, final Option<AOSystem> ao) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifted SED

        final VisitableSampledSpectrum sed = SEDFactory.getSED(sdp, instrument);
        final SampledSpectrumVisitor redshift = new RedshiftVisitor(sdp.redshift());
        sed.accept(redshift);

        // Must check to see if the redshift has moved the spectrum beyond
        // useful range. The shifted spectrum must completely overlap
        // both the normalization waveband and the observation waveband
        // (filter region).

        final MagnitudeBand band = sdp.normBand();
        final double start = band.start().toNanometers();
        final double end = band.end().toNanometers();

        // TODO: which instruments need this check, why only some and others not? Do all near-ir instruments need it?
        // TODO: what about Nifs and Gnirs (other near-ir instruments)?
        if (instrument instanceof Gsaoi || instrument instanceof Niri || instrument instanceof Flamingos2) {
            if (sed.getStart() > instrument.getObservingStart() || sed.getEnd() < instrument.getObservingEnd()) {
                throw new IllegalArgumentException(String.format("Redshifted SED (%.1f - %.1f nm) does not cover range of instrument configuration (%.1f - %.1f nm).",
                        sed.getStart(), sed.getEnd(), instrument.getObservingStart(), instrument.getObservingEnd()));
            }
        }

        //System.out.println("sed-Start: " + sed.getStart() + " sedEnd: " + sed.getEnd() + " instrStart " + instrument.getObservingStart() + " inst-end: " + instrument.getObservingEnd());

        // any sed except BBODY and ELINE have normalization regions
        if (!(sdp.distribution() instanceof EmissionLine) && !(sdp.distribution() instanceof BlackBody)) {
            if (sed.getStart() > start || sed.getEnd() < end) {
                throw new IllegalArgumentException(String.format("Redshifted SED (%.1f - %.1f nm) does not cover the specified normalization waveband (%.1f - %.1f nm).",
                        sed.getStart(), sed.getEnd(), start, end));
            }
        }

        // Module 2
        // Convert input into standard internally-used units.
        //
        // inputs: instrument,redshifted SED, waveband, normalization flux,
        // units
        // calculates: normalized SED, resampled SED, SED adjusted for aperture
        // output: SED in common internal units
        if (!(sdp.distribution() instanceof EmissionLine)) {
            final SampledSpectrumVisitor norm = new NormalizeVisitor(
                    sdp.normBand(),
                    sdp.norm(),
                    sdp.units());
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

        final SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(odp.javaCc());
        sed.accept(clouds);

        final SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
                instrument,
                odp.wv(),
                odp.airmass(),
                getWater(instrument));

        sed.accept(water);

        // Background spectrum is introduced here.
        final VisitableSampledSpectrum sky = SEDFactory.getSED(getSky(instrument, odp), instrument.getSampling());
        Option<VisitableSampledSpectrum> halo = Option.empty();

        // Apply telescope transmission to both sed and sky
        final SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(tp);
        sed.accept(t);
        sky.accept(t);

        // Create and Add background for the telescope.
        final SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(instrument, tp);
        sky.accept(tb);

        // FOR GSAOI and NIRI ADD AO STUFF HERE
        if (instrument instanceof Gsaoi || instrument instanceof Niri || instrument instanceof Gnirs) {
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

        //double[][] data = sed.getData();

        if (!(instrument instanceof Gsaoi) && !(instrument instanceof Niri) && !(instrument instanceof Gnirs)) {
            // TODO: for any instrument other than GSAOI and NIRI convolve here, why?
            instrument.convolveComponents(sed);
        }

        /*double[][] data2 = sed.getData();
        System.out.println("**** sed   beforeConvolveEleemnt   afterConvolveElement **** ");
        for (int i = 0; i < data[0].length; i++) {
            if (data[0][i] > 500 && data[0][i] < 510)
                System.out.println(data[0][i] + " -> " + data[1][i] + ";  "+ data2[1][i] );
        }

        System.out.println("******************************************************** ");

         */

        creatingFile(instrument.getSampling(),  sed, "BcompCalc");
        instrument.convolveComponents(sky);
        creatingFile(instrument.getSampling(),  sed, "compCalc");
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

    private static String getWater(final Instrument instrument) {
        switch (instrument.getBands()) {
            case VISIBLE:  return "skytrans_";
            case NEAR_IR:  return "nearIR_trans_";
            case MID_IR:   return "midIR_trans_";
            default:       throw new Error("invalid band");
        }
    }

    private static String getSky(final Instrument instrument, final ObservingConditions ocp) {
        switch (instrument.getBands()) {
            case VISIBLE:
                return ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.OPTICAL_SKY_BACKGROUND_FILENAME_BASE
                        + "_"
                        + ocp.sb().sequenceValue()
                        + "_" + airmassCategory(ocp.airmass())
                        + ITCConstants.DATA_SUFFIX;
            case NEAR_IR:
                return "/"
                        + ITCConstants.HI_RES + (instrument.getSite().equals(Site.GN) ? "/mk" : "/cp")
                        + instrument.getBands().getDirectory() + ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.NEAR_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                        + ocp.wv().sequenceValue() + "_"
                        + airmassCategory(ocp.airmass())
                        + ITCConstants.DATA_SUFFIX;
            case MID_IR:
                return "/"
                        + ITCConstants.HI_RES + (instrument.getSite().equals(Site.GN) ? "/mk" : "/cp")
                        + instrument.getBands().getDirectory() + ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.MID_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                        + ocp.wv().sequenceValue() + "_"
                        + airmassCategory(ocp.airmass())
                        + ITCConstants.DATA_SUFFIX;
            default:
                throw new Error("invalid band");
        }
    }

    private static String airmassCategory(final double airmass) {
        if (airmass <= 1.25)
            return "10";
        else if (airmass > 1.25 && airmass <= 1.75)
            return "15";
        else
            return "20";
    }

}
