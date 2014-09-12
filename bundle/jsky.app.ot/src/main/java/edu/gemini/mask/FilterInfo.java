/**
 * $Id: FilterInfo.java 7064 2006-05-25 19:48:25Z shane $
 */

package edu.gemini.mask;

import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;

/**
 * Defines min/max wavelength for GMOS filters (north and south)
 */
class FilterInfo {

    private static final FilterInfo[] FILTER_INFO = new FilterInfo[]{

        // These values were originally taken from the gmmps filters.lut file
        // and the GMOS web page.

        // From Marcel Bergman:
        //The GMOS Hartmann filters should not be included in the gmmps list.
        //They are only used for instrument engineering (actually, for focusing
        //gmos), and are not available for science use.

        // Rodrigo tells me that the DS816 filter does not exist any longer,
        // so it should definitely not  be included in the gmmps filter list.

        //The GG455_G0305, OG515_G0306 and RG610_G0307 are longpass filters,
        //which don't have an upper wavelength cutoff.  The upper wavelength is
        //determined by the rest of the system (basically the CCD).  1050 should
        //be fine to use for them.
        //For i_G0327_RG780_G0334 use these values:
        //
        //i_G0327_RG780_G0334  780.   850.
        //
        //Looking at the table one more time, I realize that there are two lines
        //which are not quite correct.  Please update these two as well (these
        //are specific to gmmps, and won't be quite the same as what you see in
        //the webpages because the webpages list the regions of peak throughput,
        //and these wavelengths here include all the wavlengths where the
        //filters transmit any light):
        //
        //i_and_CaT      750.      850.
        //z_and_CaT      848.      950.


        // GMOS-N
        new FilterInfo(FilterNorth.NONE, 300, 1200),
        new FilterInfo(FilterNorth.g_G0301, 398, 552),
        new FilterInfo(FilterNorth.r_G0303, 562, 698),
        new FilterInfo(FilterNorth.i_G0302, 706, 850),
        new FilterInfo(FilterNorth.z_G0304, 848, 1050),
        new FilterInfo(FilterNorth.CaT_G0309, 750, 950),
        new FilterInfo(FilterNorth.GG455_G0305, 460, 1050),
        new FilterInfo(FilterNorth.OG515_G0306, 520, 1050),
        new FilterInfo(FilterNorth.RG610_G0307, 615, 1050),
        new FilterInfo(FilterNorth.Ha_G0310, 654.2, 661.1),
        new FilterInfo(FilterNorth.HaC_G0311, 659.8, 666.6),
        new FilterInfo(FilterNorth.DS920_G0312, 912.8, 931.4),
        new FilterInfo(FilterNorth.g_G0301_GG455_G0305, 460, 552),
        new FilterInfo(FilterNorth.g_G0301_OG515_G0306, 520, 552),
        new FilterInfo(FilterNorth.r_G0303_RG610_G0307, 615, 698),
        new FilterInfo(FilterNorth.i_G0302_CaT_G0309, 780, 850),
        new FilterInfo(FilterNorth.z_G0304_CaT_G0309, 848, 933),

        // GMOS-S
        new FilterInfo(FilterSouth.NONE, 300, 1200),
        new FilterInfo(FilterSouth.u_G0332, 336, 385),
        new FilterInfo(FilterSouth.g_G0325, 398, 552),
        new FilterInfo(FilterSouth.r_G0326, 562, 698),
        new FilterInfo(FilterSouth.i_G0327, 706, 850),
        new FilterInfo(FilterSouth.z_G0328, 848, 1200),
        new FilterInfo(FilterSouth.GG455_G0329, 460, 1200),
        new FilterInfo(FilterSouth.OG515_G0330, 520, 1200),
        new FilterInfo(FilterSouth.RG610_G0331, 615, 1200),
        new FilterInfo(FilterSouth.RG780_G0334, 780, 1200),
        new FilterInfo(FilterSouth.CaT_G0333, 780, 933),
        new FilterInfo(FilterSouth.Ha_G0336, 653.9, 660.0),
        new FilterInfo(FilterSouth.g_G0325_GG455_G0329, 460, 552),
        new FilterInfo(FilterSouth.g_G0325_OG515_G0330, 520, 552),
        new FilterInfo(FilterSouth.r_G0326_RG610_G0331, 615, 698),
        new FilterInfo(FilterSouth.i_G0327_RG780_G0334, 780, 850),
        new FilterInfo(FilterSouth.i_G0327_CaT_G0333, 750, 850),
        new FilterInfo(FilterSouth.z_G0328_CaT_G0333, 848, 950),
        new FilterInfo(FilterSouth.SII_G0335, 669.4, 673.7),
        new FilterInfo(FilterSouth.HaC_G0337, 658.1, 664.9),
        new FilterInfo(FilterSouth.OIII_G0338, 496.5, 501.5),
        new FilterInfo(FilterSouth.OIIIC_G0339, 509.0, 519.0),
    };

    private final GmosCommonType.Filter filter;
    private final double lambda1;
    private final double lambda2;

    FilterInfo(GmosCommonType.Filter filter, double lambda1, double lambda2) {
        this.filter = filter;
        this.lambda1 = lambda1;
        this.lambda2 = lambda2;
    }

    static FilterInfo getFilterInfo(GmosCommonType.Filter f) {
        for (FilterInfo aFILTER_INFO : FILTER_INFO) {
            if (aFILTER_INFO.filter == f) {
                return aFILTER_INFO;
            }
        }
        throw new IllegalArgumentException("The chosen filter: " + f.name()
                + " is not supported. Please select another filter.");
    }

    public GmosCommonType.Filter getFilter() {
        return filter;
    }

    public double getLambda1() {
        return lambda1;
    }

    public double getLambda2() {
        return lambda2;
    }
}


