// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
//
package edu.gemini.spModel.gemini.altair;

public final class AltairConstants {

    /**
     * The name of the Altair instrument configuration
     */
    public static final String SYSTEM_NAME_PROP = "Altair";

    public static final String WAVELENGTH_PROP = "wavelength";
    public static final String ADC_PROP = "adc";
    public static final String CASS_ROTATOR_PROP = "cassRotator";
    public static final String ND_FILTER_PROP = "ndFilter";
    public static final String MODE_PROP = "mode";              // includes field lens, guide star type and guider

    // these values are not needed for persistence anymore but are still needed for system configuration
    // the values for field lens and guide star type are both incorporated into the mode (UX-1423)
    public static final String FIELD_LENSE_PROP = "fieldLens";
    public static final String GUIDESTAR_TYPE_PROP = "guideStarType";
}
