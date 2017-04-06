package jsky.app.ot.gemini.gmos;

import edu.gemini.spModel.gemini.gmos.*;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.Disperser;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.Filter;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.type.SpTypeUtil;

import java.util.regex.Pattern;

/**
 * This is the editor for the GMOS South instrument component.
 */
public class EdCompInstGMOSSouth extends EdCompInstGMOS<InstGmosSouth> {

    // GMOS filter info for the mask making software
    private static final GMOSInfo[] FILTER_INFO = new GMOSInfo[]{
            //                 filter                       lambda1   lambda2
            new GMOSInfo(FilterSouth.u_G0332, 300, 420),
            new GMOSInfo(FilterSouth.g_G0325, 398, 552),
            new GMOSInfo(FilterSouth.r_G0326, 562, 698),
            new GMOSInfo(FilterSouth.i_G0327, 706, 850),
            new GMOSInfo(FilterSouth.z_G0328, 848, 1100),
            new GMOSInfo(FilterSouth.GG455_G0329, 460, 1100),
            new GMOSInfo(FilterSouth.OG515_G0330, 520, 1100),
            new GMOSInfo(FilterSouth.RG610_G0331, 615, 1100),
            new GMOSInfo(FilterSouth.CaT_G0333, 745, 950),
            new GMOSInfo(FilterSouth.g_G0325_GG455_G0329, 460, 552),
            new GMOSInfo(FilterSouth.g_G0325_OG515_G0330, 520, 552),
            new GMOSInfo(FilterSouth.r_G0326_RG610_G0331, 615, 698),
            new GMOSInfo(FilterSouth.i_G0327_CaT_G0333, 745, 850),
            new GMOSInfo(FilterSouth.z_G0328_CaT_G0333, 745, 1100)
    };

    // GMOS grating info for the mask making software
    private static final GMOSInfo[] GRATING_INFO = new GMOSInfo[]{
            //                 grating             lambda1   lambda2
            new GMOSInfo(DisperserSouth.B1200_G5321, 300, 1100),
            new GMOSInfo(DisperserSouth.R831_G5322, 498, 1100),
            new GMOSInfo(DisperserSouth.B600_G5323, 320, 1100),
            new GMOSInfo(DisperserSouth.R600_G5324, 530, 1100),
            new GMOSInfo(DisperserSouth.R400_G5325, 520, 1100),
            new GMOSInfo(DisperserSouth.R150_G5326, 430, 1100)
    };

    private static final Pattern OBSOLETE = Pattern.compile("^\\* .*");

    public EdCompInstGMOSSouth() {
        super();
    }

    // Initialize controls based on gmos specific information.
    protected void _updateControlVisibility() {
        super._updateControlVisibility();
        // Apply differences in translation stage (follow options) between GMOSN and GMOSS
        _w.transFollowXYButton.setVisible(false);
        _w.transFollowXYZButton.setVisible(true);
        _w.transFollowZButton.setVisible(true);

        if (!OBSOLETE.matcher(_w.transFollowXYButton.getText()).matches())
            _w.transFollowXYButton.setText("* " + _w.transFollowXYButton.getText());
        if (OBSOLETE.matcher(_w.transFollowXYZButton.getText()).matches())
            _w.transFollowXYZButton.setText(_w.transFollowXYZButton.getText().replaceAll("\\* ", ""));
        if (OBSOLETE.matcher(_w.transFollowZButton.getText()).matches())
            _w.transFollowZButton.setText(_w.transFollowZButton.getText().replaceAll("\\* ", ""));
    }

    /**
     * Return true if the given value is valid for the central wavelength
     * and display an error or warning message if needed.
     */
    protected boolean _checkWavelength(double lambda_cent) {
        _clearWarning(_w.warning1);

        if (lambda_cent < MIN_LAMBDA_CENT || lambda_cent > MAX_LAMBDA_CENT) {
            _setWarning(_w.warning1, "Central wavelength outside useful range for GMOS ("
                    + MIN_LAMBDA_CENT + ", " + MAX_LAMBDA_CENT + ")");
            return false;
        }


        GmosCommonType.Disperser disperser = (GmosCommonType.Disperser) getDataObject().getDisperser();
        if (disperser == DisperserSouth.B1200_G5321 && lambda_cent > 595.) {
            _setWarning(_w.warning1, "Central wavelength too large (max 595), GMOS camera overfilled");
            return true;
        }
        if (disperser == DisperserSouth.R831_G5322 && lambda_cent > 860.) {
            _setWarning(_w.warning1, "Central wavelength too large (max 860), GMOS camera overfilled");
            return true;
        }

        GmosCommonType.Filter filter = (GmosCommonType.Filter) getDataObject().getFilter();
        double lambda1_filter = 0., lambda2_filter = 0.;
        boolean found = false;
        for (int i = 0; i < FILTER_INFO.length; i++) {
            if (filter == FILTER_INFO[i].type) {
                lambda1_filter = FILTER_INFO[i].lambda1;
                lambda2_filter = FILTER_INFO[i].lambda2;
                found = true;
            }
        }

        if (found) {
            for (int i = 0; i < GRATING_INFO.length; i++) {
                if (disperser == GRATING_INFO[i].type) {
                    double lambda1_grating = GRATING_INFO[i].lambda1;
                    double lambda2_grating = GRATING_INFO[i].lambda2;
                    double minVal = Math.max(lambda1_filter, lambda1_grating);
                    double maxVal = Math.min(lambda2_filter, lambda2_grating);
                    if (lambda_cent < minVal || lambda_cent > maxVal) {
                        _setWarning(_w.warning1, "Central wavelength outside wavelength range ("
                                + minVal + ", " + maxVal + ")");
                    }
                    break;
                }
            }
        }

        return true;
    }

    /**
     * Return the FPUnitSouth for the given index
     */
    protected GmosCommonType.FPUnit _getFPUnitByIndex(int index) {
        return GmosSouthType.FPUnitSouth.getFPUnitByIndex(index);
    }

    protected Class getFilterClass() {
        return FilterSouth.class;
    }

    protected Class getDisperserClass() {
        return DisperserSouth.class;
    }

    /**
     * Return the Filter for the given index
     */
    protected GmosCommonType.Filter _getFilterByIndex(int index) {
        return FilterSouth.getFilterByIndex(index);
    }

    /**
     * Check the selected disperser value and display a warning if needed.
     */
    protected void _checkDisperserValue() {
        _clearWarning(_w.warning1);
        Disperser disperser = (Disperser) getDataObject().getDisperser();
        Filter filter = (Filter) getDataObject().getFilter();

        if (filter == FilterSouth.g_G0325
                && (disperser == DisperserSouth.R831_G5322
                || disperser == DisperserSouth.R600_G5324
                || disperser == DisperserSouth.R400_G5325)) {
            _setWarning(_w.warning1, "Grating-filter combination gives very small wavelength coverage");
            return;
        }

        _checkWavelength(getDataObject().getDisperserLambda());
    }

    /**
     * Return an array of mask names
     */
    protected String[] _getFPUnits() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(GmosSouthType.FPUnitSouth.class);
    }

    /**
     * Return an array of disperser names
     */
    protected String[] _getDispersers() {
        return SpTypeUtil.getFormattedDisplayValueAndDescriptions(DisperserSouth.class);
    }


    /**
     * Return the Disperser for the given index
     */
    protected Disperser _getDisperserByIndex(int index) {
        return DisperserSouth.getDisperserByIndex(index);
    }

    @Override protected void init() {
        super.init();

        // Add the property change listeners defined in InstGmosCommon.
        final InstGmosSouth inst = getDataObject();
        inst.addPropertyChangeListener(InstGmosSouth.FPUNIT_PROP.getName(), updateParallacticAnglePCL);
        inst.addPropertyChangeListener(InstGmosSouth.FPUNIT_PROP.getName(), updateUnboundedAnglePCL);
    }

    @Override protected void cleanup() {
        super.cleanup();

        final InstGmosSouth inst = getDataObject();
        inst.removePropertyChangeListener(InstGmosSouth.FPUNIT_PROP.getName(), updateParallacticAnglePCL);
        inst.removePropertyChangeListener(InstGmosSouth.FPUNIT_PROP.getName(), updateUnboundedAnglePCL);
    }
}

