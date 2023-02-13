package edu.gemini.itc.base;

import edu.gemini.itc.niri.GrismOptics;
import edu.gemini.itc.shared.ItcWarning;
import edu.gemini.spModel.core.Site;
import scala.Option;
import scala.Some;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * The Instrument class is the class that any instrument should extend.
 * It defines the common properties of any given Instrumnet.
 * <p/>
 * The important piece of data is the _list. This is a linked list
 * that contains all of the Components that make up the instrument.
 */
public abstract class Instrument {
    private static final Logger Log = Logger.getLogger(Instrument.class.getName());

    public enum Bands {
        VISIBLE("03-08"),
        NEAR_IR("1-5"),
        MID_IR("7-26");

        private final String directory;

        Bands(final String directory) {
            this.directory = directory;
        }

        public String getDirectory() {
            return directory;
        }
    }

    public static final String DATA_SUFFIX = ITCConstants.DATA_SUFFIX;

    // The site of the instrument
    private final Site site;
    // Type of instrument: visible, near IR or mid IR
    private final Bands bands;
    // Instrument parameters from dat file
    private final DatFile.Instrument params;
    // List of Components
    private final List<TransmissionElement> components;
    // Each Instrument adds its own background.
    private ArraySpectrum background;

    // The filter (if any)
    public Option<Filter> filter;
    // The disperser (if any)
    public Option<Disperser> disperser;

    /**
     * All instruments have data files of the same format.
     * Note that one instrument accesses two files.
     * One gives instrument info, the other has transmission curve.
     *
     * @param subdir   The subdirectory under lib where files are located
     * @param filename The filename of the instrument data file
     */
    // Automatically loads the background data.
    protected Instrument(final Site site, final Bands bands, final String subdir, final String filename) {
        final String dir = ITCConstants.LIB + "/" + subdir + "/";
        this.site        = site;
        this.bands       = bands;
        this.params      = DatFile.instruments().apply(dir + filename);
        this.components  = new LinkedList<>();
        this.background  = new DefaultArraySpectrum(dir + params.backgroundFile());
        this.filter      = Option.empty();
        this.disperser   = Option.empty();
    }

    /**
     * Method adds the instrument background flux to the specified spectrum.
     */
    public void addBackground(ArraySpectrum sky) {
        for (int i = 0; i < sky.getLength(); i++) {
            sky.setY(i, background.getY(sky.getX(i)) + sky.getY(i));
        }
    }

    /**
     * Method to iterate through the Components list and apply the
     * accept method of each component to a sed.
     */
    public void convolveComponents(VisitableSampledSpectrum sed) {
        Log.fine("Applying each instrument component...");
        for (final TransmissionElement te : components) {
            Log.fine("Accepting " + te._file);
            sed.accept(te);
        }
    }

    /**
     * Add a filter to the light path.
     * Fiters limit the start and/or end value of the observable wavelengths.
     * @param f
     */
    protected void addFilter(Filter f) {
        if (filter.isDefined()) throw new IllegalStateException();
        filter = new Some<>(f);
        components.add(f);
        validate();
    }

    /**
     * Add a disperser to the light path.
     * Dispersers limit the start and/or end value of the observable wavelengths.
     * @param d
     */
    protected void addDisperser(final Disperser d) {
        if (disperser.isDefined()) throw new IllegalStateException();
        disperser = new Some<>(d);
        // we know that all dispersers are transmission elements, it would be nice to reflect this in the object
        // hierarchy but that's a refactoring for a later time
        components.add((TransmissionElement) d);
        validate();
    }


    protected void addComponent(TransmissionElement c) {
        components.add(c);
    }

    /**
     * Checks some conditions which must hold true.
     * Throws an exception if the instrument configuration is invalid
     * TODO: call this in constructor at some point?
     */
    private void validate() {
        if (disperser.isDefined() && filter.isDefined()) {
            final Filter f = filter.get();
            final Disperser d = disperser.get();
            if ((f.getStart() >= d.getEnd()) || (f.getEnd() <= d.getStart())) {
                throw new RuntimeException("The " + f + " filter" +
                        " and the " + d +
                        " do not overlap with the requested wavelength.\n" +
                        " Please select a different filter, grating or wavelength." + f.getStart() +
                        " " + f.getEnd() + " " + d.getStart() + " " + d.getEnd());
            }
        }
    }

    // Accessor methods
    public Site getSite() {
        return site;
    }

    public Bands getBands() {
        return bands;
    }

    public String getName() {
        return params.name();
    }

    public double getStart() {
        return params.start();
    }

    public double getEnd() {
        return params.end();
    }

    // find max start of all limiting elements
    // TODO: Verify with science and change as appropriate. Shouldn't we return the max of *all* values?
    public double getObservingStart() {
        // From original code (Michelle, TReCS, Nifs and GMOS): grating trumps everything else, is this correct?
        // Note that F2, Gnirs, Niri and Nifs behave differently again and override this method.
        if (disperser.isDefined()) return disperser.get().getStart();
        double s = getStart();
        s = filter.isDefined() ? Math.max(filter.get().getStart(),  s) : s;
        return s;
    }

    // find min end of all limiting elements
    // TODO: Verify with science and change as appropriate. Shouldn't we return the min of *all* values?
    public double getObservingEnd() {
        // From original code (Michelle, TReCS, Nifs and GMOS): grating trumps everything else, is this correct?
        // Note that F2, Gnirs, Niri and Nifs behave differently again and override this method.
        if (disperser.isDefined()) return disperser.get().getEnd();
        double e = getEnd();
        e = filter.isDefined() ? Math.min(filter.get().getEnd(),  e) : e;
        return e;
    }

    public double getSampling() {
        return params.sampling();
    }

    public double getPixelSize() {
        return params.plateScale();
    }

    public double getReadNoise() {
        return params.readNoise();
    }

    public double getDarkCurrent() {
        return params.darkCurrent();
    }

    protected void resetBackGround(String subdir, String filename_prefix) {
        final String dir = ITCConstants.LIB + "/" + subdir + "/";
        background = new DefaultArraySpectrum(dir + filename_prefix + params.backgroundFile());
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public abstract int getEffectiveWavelength();

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public abstract String getDirectory();

    /**
     * The suffix on instrument data files.
     */
    public static String getSuffix() {
        return DATA_SUFFIX;
    }

    public List<TransmissionElement> getComponents() {
        return new ArrayList<>(components);
    }


    // === Common values (should be defined per CCD)

    // Well depth and gain are actually properties of the different CCDs of the instrument, so in the future these
    // values should be defined on a CCD data structure of which each instrument can have more than one; currently
    // ITC uses copies of the instrument to implement several CCDs for GMOS, however this is a very peculiar design
    // to deal with one instrument that has several CCDs, if I may say so.
    public abstract double wellDepth();
    public abstract double gain();

    // === Warnings

    public List<WarningRule> warnings() {
        return Collections.emptyList();
    }

    public List<ItcWarning> imagingWarnings(final ImagingResult result) {
        return Collections.emptyList();
    }

    public List<ItcWarning> spectroscopyWarnings(final SpectroscopyResult result) {
        return Collections.emptyList();
    }
}


