// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id Gsaoi.java,v 1.2 1999/10/14 16:37:00 cvs-tuc Exp $
//
package edu.gemini.itc.gsaoi;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.Filter;
import edu.gemini.itc.shared.Detector;
import edu.gemini.itc.shared.FixedOptics;
import edu.gemini.itc.parameters.ObservationDetailsParameters;

import java.util.Iterator;

/**
 * Gsaoi specification class
 */
public class Gsaoi extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "gsaoi";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "gsaoi" + getSuffix();

    // ITC-2:
    // Please use the following read noises:
    // Bright object: 28e-
    // Faint object: 13e-
    // Very faint object: 10e-
    private static final double BRIGHT_OBJECTS__READ_NOISE = 28.0; // e-
    private static final double FAINT_OBJECTS_READ_NOISE = 13.0;
    private static final double VERY_FAINT_OBJECTS_READ_NOISE = 10.0;

    private Filter _filter;
    private String _filterUsed;
    private String _camera;
    private String _readMode;
    private String _mode;

    // These are the limits of observable wavelength with this configuration.
    private double _observingStart;
    private double _observingEnd;

    /**
     * construct an Gsaoi with specified Broadband filter or Narrowband filter
     * and camera type.
     */
    public Gsaoi(GsaoiParameters np, ObservationDetailsParameters odp) throws Exception {
        super(INSTR_DIR, FILENAME);
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _observingStart = super.getStart();
        _observingEnd = super.getEnd();

        _readMode = np.getReadMode();
        _filterUsed = np.getFilter();
        _camera = np.getCamera();
        _mode = odp.getCalculationMode();

        // Note for designers of other instruments:
        // Other instruments may not have filters and may just use
        // the range given in their instrument file.
        if (!(_filterUsed.equals("none"))) {
            _filter = new Filter(getPrefix(), _filterUsed, getDirectory() + "/", Filter.CALC_EFFECTIVE_WAVELEN);
            _observingStart = _filter.getStart();
            _observingEnd = _filter.getEnd();
            addComponent(_filter);
        }

        FixedOptics test = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(test);

        addComponent(new Camera(getDirectory() + "/"));

        addComponent(new Detector(getDirectory() + "/", getPrefix(), "detector",
                "2048x2048 HAWAII-2RG HgCdTe"));
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        return (int) _filter.getEffectiveWavelength();

    }

    public double getReadNoise() {
        if (_readMode.equals(GsaoiParameters.BRIGHT_OBJECTS_READ_MODE))
            return BRIGHT_OBJECTS__READ_NOISE;
        else if (_readMode.equals(GsaoiParameters.FAINT_OBJECTS_READ_MODE))
            return FAINT_OBJECTS_READ_NOISE;
        else return VERY_FAINT_OBJECTS_READ_NOISE;
    }

    public String getCamera() {
        return _camera;
    }

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    public double getObservingStart() {
        return _observingStart;
    }

    public double getObservingEnd() {
        return _observingEnd;
    }

    public double getPixelSize() {
        return 0.02; //*bin
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public String toString() {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (Object o : getComponents()) {
            if (!(o instanceof Camera)) {
                s += "<LI>" + o.toString() + "<BR>";
            }
        }
        s += "<BR>";
        s += "Pixel Size: " + getPixelSize() + "<BR>";

        return s;
    }

}
