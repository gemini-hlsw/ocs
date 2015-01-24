// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: AcquisitionCamera.java,v 1.4 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.acqcam;

import edu.gemini.itc.shared.*;

/**
 * Aquisition Camera specification class
 */
public class AcquisitionCamera extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "acqcam";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "acquisition_camera" + getSuffix();

    // Well Depth
    private static final double WELL_DEPTH = 98940.0;


    // Keep a reference to the color filter to ask for effective wavelength
    private Filter _colorFilter;

    // These are the limits of observable wavelength with this configuration.
    private double _observingStart;
    private double _observingEnd;

    /**
     * construct an AcquisitionCamera with specified color filter and ND filter.
     */
    public AcquisitionCamera(String filterBand, String ndFilter)
            throws Exception {
        super(INSTR_DIR, FILENAME);
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.
        // Since this instrument always has a filter, the filter-passed
        // range is used for _observingStart, _observingEnd.
        // old way of getting the observing start and end
        //_observingStart = WavebandDefinition.getStart(filterBand);
        //_observingEnd = WavebandDefinition.getEnd(filterBand);
        // Note for designers of other instruments:
        // Other instruments may not have filters and may just use
        // the range given in their instrument file.
        //_colorFilter = new ColorFilter(filterBand, getDirectory()+"/");
        _colorFilter = Filter.fromFile(getPrefix(), "colfilt_" + filterBand, getDirectory() + "/");

        addComponent(_colorFilter);
        addComponent(new NDFilterWheel(ndFilter, getDirectory() + "/"));
        addComponent(new FixedOptics(getDirectory() + "/", getPrefix()));
        addComponent(new Detector(getDirectory() + "/", getPrefix(), "detector",
                "1024x1024 CCD47 Chip"));
        //New way (Directly from the filter.
        _observingStart = _colorFilter.getStart();
        _observingEnd = _colorFilter.getEnd();
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        return (int) _colorFilter.getEffectiveWavelength();
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

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public double getWellDepth() {
        return WELL_DEPTH;
    }
}
