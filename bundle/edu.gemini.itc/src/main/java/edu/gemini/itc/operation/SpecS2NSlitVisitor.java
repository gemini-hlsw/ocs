package edu.gemini.itc.operation;

import java.util.logging.Logger;
import edu.gemini.itc.base.Disperser;
import edu.gemini.itc.base.SampledSpectrum;
import edu.gemini.itc.base.SampledSpectrumVisitor;
import edu.gemini.itc.base.VisitableSampledSpectrum;
import edu.gemini.itc.shared.*;

/**
 * The SpecS2NSlitVisitor is used to calculate the s2n of spectroscopy observation using a slit.
 * Note that instances of this object are "recycled" in some recipes, in particular to handle different orders
 * (GNIRS) or IFUs; this is why this class has a bunch of setters and not all variables are final. Not all
 * relevant values are set in the constructor, e.g. halo information if used with an AO system. Ideally this
 * could be changed so that instances of this class become immutable.
 */
public class SpecS2NSlitVisitor implements SampledSpectrumVisitor, SpecS2N {

    private static final Logger Log = Logger.getLogger( SpecS2NSlitVisitor.class.getName() );
    private final ObservationDetails odp;
    private final Slit slit;
    private Disperser disperser;
    private final double sourceFraction;
    private final double exposureTime;
    private final int coadds;
    private final double darkCurrent;
    private final double readNoise;
    private final int numberExposures;
    private double pixelWidth;
    private double obsStart;
    private double obsEnd;

    // signal and background
    private VisitableSampledSpectrum sourceFlux;
    private VisitableSampledSpectrum backgroundFlux;
    private final SlitThroughput throughput;
    private final double imgQuality;

    // halo for AO system (optional)
    private boolean haloIsUsed = false;
    private VisitableSampledSpectrum haloFlux;
    private SlitThroughput haloThroughput;
    private double haloImgQuality;

    // used for GMOS multi-CCD case
    private int firstCcdPixel = 0;
    private int lastCcdPixel = -1;

    // the results
    private VisitableSampledSpectrum resultSignal;
    private VisitableSampledSpectrum resultSqrtBackground;
    private VisitableSampledSpectrum resultS2NSingle;
    private VisitableSampledSpectrum resultS2NFinal;

    /**
     * Constructs SpecS2NVisitor.
     */
    public SpecS2NSlitVisitor(final Slit slit,
                              final Disperser disperser,
                              final SlitThroughput throughput,
                              final double pixelWidth,
                              final double obsWaveLow,
                              final double obsEnd,
                              final double imgQuality,
                              final double readNoise,
                              final double darkCurrent,
                              final ObservationDetails odp) {
        this.odp            = odp;
        this.slit           = slit;
        this.disperser      = disperser;
        this.throughput     = throughput;
        this.pixelWidth     = pixelWidth;
        this.obsStart       = obsWaveLow;
        this.obsEnd         = obsEnd;
        this.imgQuality     = imgQuality;
        this.darkCurrent    = darkCurrent;
        this.readNoise      = readNoise;

        // Currently SpectroscopySN is the only supported calculation method for spectroscopy.
        final CalculationMethod calcMethod = odp.calculationMethod();
        if (!(calcMethod instanceof SpectroscopyS2N)) throw new Error("Unsupported calculation method");
        this.coadds = calcMethod.coaddsOrElse(1);
        this.numberExposures = ((SpectroscopyS2N) calcMethod).exposures();
        this.sourceFraction  = calcMethod.sourceFraction();
        this.exposureTime    = calcMethod.exposureTime();

    }

    // Return index of last CCD pixel, if defined and in range
    private int lastCcdPixel(final int n) {
        if (lastCcdPixel == -1 || lastCcdPixel >= n) return n - 1;
        return lastCcdPixel;
    }

    /**
     * Implements the SampledSpectrumVisitor interface
     */
    @Override
    public void visit(final SampledSpectrum sed) {
        // step one: do some resampling and preprocessing
        resample();
        // step two: calculate S2N for single and final exposure for given slit
        calculateS2N();
        // step three: calculate signal and background for single pixel
        calculateSignal();
    }

    /** Resample source, background and - if applicable - halo flux as needed. */
    private void resample() {

        // calc the width of a spectral resolution element in nm
        final double resElement                 = disperser.resolution(slit, imgQuality);
        final double backgroundResElement       = disperser.resolution(slit);

        // and the data size in the spectral domain
        final double resElementData             = resElement / sourceFlux.getSampling();
        final double backgroundResElementData   = backgroundResElement / backgroundFlux.getSampling();

        // use the int value of spectral_pix as a smoothing element (at least 1)
        final int smoothingElement              = (int) Math.max(1.0, Math.round(resElementData));
        final int backgroundSmoothingElement    = (int) Math.max(1.0, Math.round(backgroundResElementData));
        ///////////////////////////////////////////////////////////////////////////////////////
        //  We Don't know why but using just the smoothing element is not enough to create the resolution
        //     that we expect.  Using a smoothing element of  = smoothingElement + 1
        //     May need to take this out in the future.
        ///////////////////////////////////////////////////////////////////////////////////////
        sourceFlux.smoothY(smoothingElement + 1);
        backgroundFlux.smoothY(backgroundSmoothingElement + 1);

        if (haloIsUsed) {
            // calc the width of a spectral resolution element in nm
            final double haloResElement       = disperser.resolution(slit, haloImgQuality);
            // and the data size in the spectral domain
            final double haloResElementData  = haloResElement / sourceFlux.getSampling();
            // use the int value of spectral_pix as a smoothing element (at least 1)
            final int haloSmoothingElement    = (int) Math.max(1.0, Math.round(haloResElementData));
            ///////////////////////////////////////////////////////////////////////////////////////
            //  We Don't know why but using just the smoothing element is not enough to create the resolution
            //     that we expect.  Using a smoothing element of  = smoothingElement + 1
            //     May need to take this out in the future.
            ///////////////////////////////////////////////////////////////////////////////////////
            haloFlux.smoothY(haloSmoothingElement + 1);

            final SampledSpectrumVisitor haloResample   = new ResampleWithPaddingVisitor(obsStart, obsEnd - 1, pixelWidth, 0);
            haloFlux.accept(haloResample);
        }


        // resample both sky and SED
        final SampledSpectrumVisitor sourceResample     = new ResampleWithPaddingVisitor(obsStart, obsEnd - 1, pixelWidth, 0);
        final SampledSpectrumVisitor backgroundResample = new ResampleWithPaddingVisitor(obsStart, obsEnd - 1, pixelWidth, 0);

        sourceFlux.accept(sourceResample);
        backgroundFlux.accept(backgroundResample);

    }

    /** Calculates single and final S2N. */
    private void calculateS2N() {

        // shot noise on dark current flux in aperture
        final double darkNoise = darkCurrent * slit.lengthPixels() * exposureTime;

        // readout noise in aperture
        final double readNoise = this.readNoise * this.readNoise * slit.lengthPixels();

        // signal and background for given slit and throughput
        final VisitableSampledSpectrum signal     = haloIsUsed ? signalWithHalo(throughput.throughput(), haloThroughput.throughput()) : signal(throughput.throughput());
        final VisitableSampledSpectrum background = background(slit);

        // -- calculate and assign s2n results

        // S2N for one exposure
        resultS2NSingle = singleS2N(signal, background, darkNoise, readNoise);

        // final S2N for all exposures
        resultS2NFinal = finalS2N(signal, background, darkNoise, readNoise);

    }

    /** Calculates signal and background per coadd. */
    private void calculateSignal() {
        Log.fine("Calculating signal and background...");

        // total source flux in the aperture
        final VisitableSampledSpectrum signal         = haloIsUsed ? signalWithHalo(throughput.onePixelThroughput(), haloThroughput.onePixelThroughput()) : signal(throughput.onePixelThroughput());
        final VisitableSampledSpectrum sqrtBackground = background(new OnePixelSlit(slit.width(), slit.pixelSize())); // background(slit); REL-508

        // create the Sqrt(Background) sed for plotting
        for (int i = firstCcdPixel; i <= lastCcdPixel(sqrtBackground.getLength()); ++i)
            sqrtBackground.setY(i, Math.sqrt(sqrtBackground.getY(i)));

        // -- assign results
        resultSignal = signal;
        resultSqrtBackground = sqrtBackground;

    }

    /** Calculates total source flux (signal) in the aperture per coadd. */
    private VisitableSampledSpectrum signal(final double throughput) {

        final VisitableSampledSpectrum signal = (VisitableSampledSpectrum) sourceFlux.clone();
        final int lastPixel = lastCcdPixel(signal.getLength());
        Log.fine("Calculating signal with " + throughput + " throughput on detector pixels " + firstCcdPixel + " - " + lastPixel);

        for (int i = 0; i < signal.getLength(); ++i) { signal.setY(i, 0); } // zero data array before use per REL-2992

        for (int i = firstCcdPixel; i <= lastPixel; ++i) {
            signal.setY(i, totalFlux(sourceFlux.getY(i), throughput));
        }

        return signal;
    }

    /** Calculates total source flux (signal) in the aperture. */
    private VisitableSampledSpectrum signalWithHalo(final double throughput, final double haloThroughput) {

        final VisitableSampledSpectrum signal = (VisitableSampledSpectrum) sourceFlux.clone();
        final int lastPixel = lastCcdPixel(signal.getLength());
        Log.fine("Calculating signal with halo with " + throughput + " throughput on detector pixels " + firstCcdPixel + " - " + lastPixel);

        for (int i = 0; i < signal.getLength(); ++i) { signal.setY(i, 0); }

        for (int i = firstCcdPixel; i <= lastPixel; ++i) {
            signal.setY(i, totalFlux(sourceFlux.getY(i), throughput) + totalFlux(haloFlux.getY(i), haloThroughput));
        }

        return signal;
    }


    /** Calculates the background in the aperture per coadd. */
    private VisitableSampledSpectrum background(final Slit slit) {

        final VisitableSampledSpectrum background = (VisitableSampledSpectrum) backgroundFlux.clone();
        final int lastPixel = lastCcdPixel(background.getLength());
        Log.fine("Calculating background in " + slit.widthPixels() + " x " + slit.lengthPixels() + " pix slit on detector pixels " + firstCcdPixel + " - " + lastPixel);

        for (int i = 0; i < background.getLength(); ++i) { background.setY(i, 0); }

        //Shot noise on background flux in aperture
        for (int i = firstCcdPixel; i <= lastPixel; ++i) {
            background.setY(i,
                    backgroundFlux.getY(i) *
                            slit.width() * slit.pixelSize() * slit.lengthPixels() *
                            exposureTime * disperser.dispersion());
        }

        return background;
    }

    /** Calculates the signal to noise ratio for a single exposure (per frame). */
    private VisitableSampledSpectrum singleS2N(final VisitableSampledSpectrum signal, final VisitableSampledSpectrum background, final double darkNoise, final double readNoise) {

        // total noise in the aperture
        final VisitableSampledSpectrum noise = (VisitableSampledSpectrum) sourceFlux.clone();
        for (int i = firstCcdPixel; i <= lastCcdPixel(noise.getLength()); ++i) {
            noise.setY(i, Math.sqrt(signal.getY(i)+ background.getY(i) + darkNoise + readNoise));
        }

        // calculate signal to noise
        final VisitableSampledSpectrum singleS2N = (VisitableSampledSpectrum) sourceFlux.clone();
        for (int i = firstCcdPixel; i <= lastCcdPixel(singleS2N.getLength()); ++i) {
            singleS2N.setY(i, Math.sqrt(coadds) * signal.getY(i) / noise.getY(i));
        }

        return singleS2N;
    }

    /** Calculates the final signal to noise ratio for all exposures. */
    private VisitableSampledSpectrum finalS2N(final VisitableSampledSpectrum signal, final VisitableSampledSpectrum background, final double darkNoise, final double readNoise) {

        // sky aper is either the aperture or the number of fibres in the IFU case
        final double skyAper;
        final AnalysisMethod analysisMethod = odp.analysisMethod();
        if (analysisMethod instanceof ApertureMethod) {
            skyAper = ((ApertureMethod) analysisMethod).skyAperture();
        } else if (analysisMethod instanceof IfuMethod) {
            skyAper = ((IfuMethod) analysisMethod).skyFibres();
        } else {
            throw new Error();
        }

        // calculate the noise factor for the given skyAper
        final double noiseFactor = 1 + (1 / skyAper);

        // the number of exposures measuring the source flux is
        final double spec_number_source_exposures = numberExposures * coadds * sourceFraction;

        // noise in aperture
        final VisitableSampledSpectrum spec_sourceless_noise = (VisitableSampledSpectrum) sourceFlux.clone();
        int spec_sourceless_noise_last = lastCcdPixel(spec_sourceless_noise.getLength());
        for (int i = firstCcdPixel; i <= spec_sourceless_noise_last; ++i) {
            spec_sourceless_noise.setY(i, Math.sqrt(background.getY(i) + darkNoise + readNoise));
        }

        final VisitableSampledSpectrum finalS2N = (VisitableSampledSpectrum) sourceFlux.clone();
        for (int i = firstCcdPixel; i <= lastCcdPixel(finalS2N.getLength()); ++i)
            finalS2N.setY(i, Math.sqrt(spec_number_source_exposures) *
                    signal.getY(i) /
                    Math.sqrt(signal.getY(i) + noiseFactor *
                            spec_sourceless_noise.getY(i) *
                            spec_sourceless_noise.getY(i)));

        return finalS2N;
    }

    private double totalFlux(final double flux, final double throughput) {
        return flux * throughput * exposureTime * disperser.dispersion();
    }



    public void setSourceSpectrum(final VisitableSampledSpectrum sed) {
        sourceFlux = sed;
    }

    public void setHaloSpectrum(final VisitableSampledSpectrum sed, final SlitThroughput throughput, final double imgQuality) {
        haloIsUsed      = true;
        haloFlux        = sed;
        haloThroughput  = throughput;
        haloImgQuality  = imgQuality;
    }

    public void setBackgroundSpectrum(final VisitableSampledSpectrum sed) {
        backgroundFlux = sed;
    }

    public void setCcdPixelRange(final int first, final int last) {
        firstCcdPixel = first;
        lastCcdPixel = last;
    }

    public void setDisperser(final Disperser disperser) {
        this.disperser = disperser;
    }

    public void setSpectralPixelWidth(final double pixelWidth) {
        this.pixelWidth = pixelWidth;
    }

    public void setStartWavelength(final double obsStart) {
        this.obsStart = obsStart;
    }

    public void setEndWavelength(final double obsEnd) {
        this.obsEnd = obsEnd;
    }

    public VisitableSampledSpectrum getSignalSpectrum() {
        return resultSignal;
    }

    public VisitableSampledSpectrum getBackgroundSpectrum() {
        return resultSqrtBackground;
    }

    public VisitableSampledSpectrum getExpS2NSpectrum() {
        return resultS2NSingle;
    }

    public VisitableSampledSpectrum getFinalS2NSpectrum() {
        return resultS2NFinal;
    }

}
