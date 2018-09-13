// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: GNIRSConstants.java 5548 2004-11-24 17:45:09Z brighton $
//
package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.obscomp.InstConstants;

public final class GNIRSConstants extends InstConstants {

    public static final String BIAS_LEVEL_PROP = "biasLevel";
    public static final String CROSS_DISPERSED_PROP = "crossDispersed";
    public static final String READ_MODE_PROP = "readMode";
    public static final String DIGITAL_AVERAGES_PROP = "digitalAverages";
    public static final String DISPERSER_PROP = "disperser";
    public static final String LOW_NOISE_READS_PROP = "lowNoiseReads";
    public static final String PIXEL_SCALE_PROP = "pixelScale";
    public static final ItemKey PIXEL_SCALE_KEY = key(PIXEL_SCALE_PROP);
    public static final String SLIT_WIDTH_PROP = "slitWidth";
    public static final ItemKey SLIT_WIDTH_KEY = key(SLIT_WIDTH_PROP);
    public static final String DECKER_PROP = "decker";
    public static final String CAMERA_PROP = "camera";
    public static final ItemKey CAMERA_KEY = key(CAMERA_PROP);
    public static final String WOLLASTON_PRISM_PROP = "wollastonPrism";
    public static final String CENTRAL_WAVELENGTH_PROP = "centralWavelength";
    public static final ItemKey CENTRAL_WAVELENGTH_KEY = key(CENTRAL_WAVELENGTH_PROP);
    public static final String CENTRAL_WAVELENGTH_ORDER_N_PROP = "centralWavelengthOrderN";
    public static final String FILTER_PROP = "filter";
    public static final ItemKey FILTER_KEY = key(FILTER_PROP);
    public static final String ACQUISITION_MIRROR_PROP = "acquisitionMirror";
    public static final ItemKey ACQUISITION_MIRROR_KEY = key(ACQUISITION_MIRROR_PROP);

    public static final double DEF_EXPOSURE_TIME = 17.0; // sec (by default settings)
    public static final double DEF_CENTRAL_WAVELENGTH = 2.2; // um (band=K)

    // The name displayed for the cross-dispersed wavelength, instead of the value
    public static final String CROSS_DISPERSED_NAME = "Cross-dispersed";

    // The name of the GNIRS instrument configuration
    public static final String INSTRUMENT_NAME_PROP = "GNIRS";

    private static ItemKey key(String propName) {
        return new ItemKey("instrument:" + propName);
    }
}
