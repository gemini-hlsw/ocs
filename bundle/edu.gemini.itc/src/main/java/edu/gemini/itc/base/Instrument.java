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

/**
 * The Instrument class is the class that any instrument should extend.
 * It defines the common properties of any given Instrumnet.
 * <p/>
 * The important piece of data is the _list. This is a linked list
 * that contains all of the Components that make up the instrument.
 */
public abstract class Instrument {

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

    // The filter
    public Option<Filter> filter;
    // The grating
    public Option<GratingOptics> grating;
    // The grism
    public Option<GrismOptics> grism;

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
        this.grating     = Option.empty();
        this.grism       = Option.empty();  // TODO: difference grism vs grating??
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
        for (final TransmissionElement te : components) {
            sed.accept(te);
        }
    }

    /**
     * Add a filter to the light path.
     * Fiters limit the start and/or end value of the observable wavelengths.
     * @param f
     */
    protected void addFilter(Filter f) {
        if (filter.isDefined()) throw new IllegalStateException(); // TODO: make sure filter is passed into constructor?
        filter = new Some<>(f);
        components.add(f);
        validate();
    }

    /**
     * Add a grating to the light path.
     * Gratings limit the start and/or end value of the observable wavelengths.
     * @param g
     */
    protected void addGrating(GratingOptics g) {
        if (grating.isDefined()) throw new IllegalStateException(); // TODO: make sure grating is passed into constructor?
        grating = new Some<>(g);
        components.add(g);
        validate();
    }

    /**
     * Add a grism to the light path.
     * Grisms limit the start and/or end value of the observable wavelengths.
     * @param g
     */
    protected void addGrism(GrismOptics g) {
        if (grism.isDefined()) throw new IllegalStateException(); // TODO: make sure grating is passed into constructor?
        grism = new Some<>(g);
        components.add(g);
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
        if (grating.isDefined() && filter.isDefined()) {
            final Filter f = filter.get();
            final GratingOptics g = grating.get();
            if ((f.getStart() >= g.getEnd()) || (f.getEnd() <= g.getStart())) {
                throw new RuntimeException("The " + f + " filter" +
                        " and the " + g +
                        " do not overlap with the requested wavelength.\n" +
                        " Please select a different filter, grating or wavelength." + f.getStart() +
                        " " + f.getEnd() + " " + g.getStart() + " " + g.getEnd());
            }
        }
        if (grism.isDefined() && filter.isDefined()) { // TODO grism vs grating ??
            final Filter f = filter.get();
            final GrismOptics g = grism.get();
            if ((f.getStart() >= g.getEnd()) || (f.getEnd() <= g.getStart())) {
                throw new RuntimeException("The " + f + " filter" +
                        " and the " + g +
                        " do not overlap.\nTo continue with " +
                        "Spectroscopy mode " +
                        "either deselect the filter or choose " +
                        "one that overlaps with the grism.");
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
        if (grating.isDefined()) return grating.get().getStart();
        double s = getStart();
        //s = grating.isDefined() ? Math.max(grating.get().getStart(), s) : s;
        s = grism.isDefined()   ? Math.max(grism.get().getStart(),   s) : s;
        s = filter.isDefined()  ? Math.max(filter.get().getStart(),  s) : s;
        return s;
    }

    // find min end of all limiting elements
    // TODO: Verify with science and change as appropriate. Shouldn't we return the min of *all* values?
    public double getObservingEnd() {
        // From original code (Michelle, TReCS, Nifs and GMOS): grating trumps everything else, is this correct?
        // Note that F2, Gnirs, Niri and Nifs behave differently again and override this method.
        if (grating.isDefined()) return grating.get().getEnd();
        double e = getEnd();
        //e = grating.isDefined() ? Math.min(grating.get().getEnd(), e) : e;
        e = grism.isDefined()   ? Math.min(grism.get().getEnd(),   e) : e;
        e = filter.isDefined()  ? Math.min(filter.get().getEnd(),  e) : e;
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

    // === Warnings

    public List<LimitWarning> warnings() {
        return Collections.emptyList();
    }

    public List<ItcWarning> imagingWarnings(final ImagingResult result) {
        return Collections.emptyList();
    }

    public List<ItcWarning> spectroscopyWarnings(final SpectroscopyResult result) {
        return Collections.emptyList();
    }
}


