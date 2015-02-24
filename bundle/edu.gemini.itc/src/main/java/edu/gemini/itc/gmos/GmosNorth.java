package edu.gemini.itc.gmos;

import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.shared.Detector;
import edu.gemini.itc.shared.Filter;
import edu.gemini.itc.shared.FixedOptics;
import edu.gemini.itc.shared.Instrument;

import java.awt.*;

/**
 * Gmos specification class
 */
public class GmosNorth extends Gmos {

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "gmos_n_";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "gmos_n" + getSuffix();

    // Detector data files (see REL-478)
    // The GMOS team has decided that the GMOS-N focal plane will be composed of:
    // CCDr -> BB
    // CCDg -> HSC
    // CCDb -> BB
    private static final String[] DETECTOR_CCD_FILES = {"ccd_hamamatsu_bb", "ccd_hamamatsu_hsc", "ccd_hamamatsu_bb"};

    // Detector display names corresponding to the detectorCcdIndex
    private static final String[] DETECTOR_CCD_NAMES = {"BB(B)", "HSC", "BB(R)"};

    public GmosNorth(GmosParameters gp, ObservationDetailsParameters odp, int detectorCcdIndex) throws Exception {
        super(gp, odp, FILENAME, INSTR_PREFIX, detectorCcdIndex);

        if (gp.getFocalPlaneMask().equals(GmosParameters.IFU))
            _IFUUsed = true;
        else _IFUUsed = false;

        /// !!!!!!!!NEED to Edit all of this !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // Note for designers of other instruments:
        // Other instruments may not have filters and may just use
        // the range given in their instrument file.
        if (!(gp.getFilter().equals("none"))) {

            _Filter = Filter.fromWLFile(getPrefix(), gp.getFilter(), getDirectory() + "/");
            if (_Filter.getStart() >= _observingStart)
                _observingStart = _Filter.getStart();
            if (_Filter.getEnd() <= _observingEnd)
                _observingEnd = _Filter.getEnd();
            addComponent(_Filter);

        }


        FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);


        //Test to see that all conditions for Spectroscopy are met
        if (odp.getMethod().isSpectroscopy()) {
            if (gp.getGrating().equals("none"))
                throw new Exception("Spectroscopy calculation method is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (gp.getFocalPlaneMask().equals(GmosParameters.NO_SLIT))
                throw new Exception("Spectroscopy calculation method is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
        }

        if (odp.getMethod().isImaging()) {
            if (gp.getFilter().equals("none"))
                throw new Exception("Imaging calculation method is selected but a filter" +
                        " is not.\n  Please select a filter and resubmit the " +
                        "form to continue.");
            if (!gp.getGrating().equals("none"))
                throw new Exception("Imaging calculation method is selected but a grating" +
                        " is also selected.\nPlease deselect the " +
                        "grating or change the method to spectroscopy.");
            if (!gp.getFocalPlaneMask().equals("none"))
                throw new Exception("Imaging calculation method is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the method to spectroscopy.");
            if (_IFUUsed)
                throw new Exception("Imaging calculation method is selected but an IFU" +
                        " is also selected.\nPlease deselect the IFU or" +
                        " change the method to spectroscopy.");
        }

        //Original vs Hamamatsu CCD's
        _CCDtype = gp.getCCDtype();

        //Choose correct CCD QE curve
        // REL-760, REL-478
        // The following QE files correspond to each option
        // 0. gmos_n_E2V4290DDmulti3.dat      => EEV DD array
        // 1. gmos_n_cdd_red.dat              => EEV legacy
        // 2. gmos_n_CCD-{R,G,B}.dat          =>  Hamamatsu (R,G,B)
        if (_CCDtype.equals("0")) {
            _detector = new Detector(getDirectory() + "/", getPrefix(), "E2V4290DDmulti3", "EEV DD array");
            _detector.setDetectorPixels(DETECTOR_PIXELS);
            if (detectorCcdIndex == 0) _instruments = new Gmos[]{this};
        } else if (_CCDtype.equals("1")) {
            _detector = new Detector(getDirectory() + "/", getPrefix(), "ccd_red", "EEV legacy array");
            _detector.setDetectorPixels(DETECTOR_PIXELS);
            if (detectorCcdIndex == 0) _instruments = new Gmos[]{this};
        } else if (_CCDtype.equals("2")) {
            String fileName = DETECTOR_CCD_FILES[detectorCcdIndex];
            String name = DETECTOR_CCD_NAMES[detectorCcdIndex];
            Color color = DETECTOR_CCD_COLORS[detectorCcdIndex];
            _detector = new Detector(getDirectory() + "/", getPrefix(), fileName, "Hamamatsu array", name, color);
            _detector.setDetectorPixels(DETECTOR_PIXELS);
            if (detectorCcdIndex == 0)
                _instruments = new Gmos[]{this, new GmosNorth(gp, odp, 1), new GmosNorth(gp, odp, 2)};
        }

        if (detectorCcdIndex == 0) {
            _dtv = new DetectorsTransmissionVisitor(gp.getSpectralBinning(),
                    getDirectory() + "/" + getPrefix() + "ccdpix_red" + Instrument.getSuffix());
        }

        if (_IFUUsed) {
            if (gp.getIFUMethod().equals(GmosParameters.SINGLE_IFU)) {
                _IFU_IsSingle = true;
                _IFU = new IFUComponent(getPrefix(), gp.getIFUOffset());
            }
            if (gp.getIFUMethod().equals(GmosParameters.RADIAL_IFU)) {
                _IFU = new IFUComponent(getPrefix(), gp.getIFUMinOffset(), gp.getIFUMaxOffset());
            }
            addComponent(_IFU);
        }


        if (!(gp.getGrating().equals("none"))) {

            _gratingOptics = new GmosGratingOptics(getDirectory() + "/" + getPrefix(), gp.getGrating(), _detector,
                    gp.getCentralWavelength(),
                    _detector.getDetectorPixels(),
                    gp.getSpectralBinning());
            _sampling = _gratingOptics.getGratingDispersion_nmppix();
            _observingStart = _gratingOptics.getStart();
            _observingEnd = _gratingOptics.getEnd();

            if (!(gp.getGrating().equals("none")) && !(gp.getFilter().equals("none")))
                if ((_Filter.getStart() >= _gratingOptics.getEnd()) ||
                        (_Filter.getEnd() <= _gratingOptics.getStart()))

                {
                    throw new Exception("The " + gp.getFilter() + " filter" +
                            " and the " + gp.getGrating() +
                            " do not overlap with the requested wavelength.\n" +
                            " Please select a different filter, grating or wavelength." + _Filter.getStart() +
                            " " + _Filter.getEnd() + " " + _gratingOptics.getStart() + " " + _gratingOptics.getEnd());
                }
            addComponent(_gratingOptics);
        }


        addComponent(_detector);
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public double getPixelSize() {
        //Temp method of returning correct GMOS-N pixel size.
        //Cannot change the value directly since pixel_size is a private
        //member variable of the superclass Instrument.

        // REL-477: XXX FIXME
        if (_CCDtype.equals("0") || _CCDtype.equals("1")) {
            return ORIG_PLATE_SCALE * gp.getSpatialBinning();
        } else {
            return HAM_PLATE_SCALE * gp.getSpatialBinning();
        }
    }
}
