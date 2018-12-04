package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.wdba.glue.api.WdbaGlueException;

/**
 * Gemini Observatory/AURA
 * $Id: AOConfig.java 756 2007-01-08 18:01:24Z gillies $
 */
public class AOConfig extends ParamSet {

    // Parameters set only in AOConfig (and hence not in TccNames
    public static final String ADJUSTADC = "adjustAdc";
    public static final String DEPLOYADC = "deplyAdc";
    public static final String FLDLENS = "fldLens";
    public static final String GSMAG = "gsmag";
    public static final String LGS = "lgs";
    public static final String NDFILTER = "ndFilter";
    public static final String SEEING = "seeing";
    public static final String WINDSPEED = "windspeed";
    public static final String WAVELENGTH = "wavelength";

    public static final String LGS_CONFIG_NAME = "LGS+TTGS";
    public static final String NGS_CONFIG_NAME = "NGS";

    public static final String GEMS_GAOS_ADC         = "adc";
    public static final String GEMS_GAOS_DICHROIC    = "dichroicBeamsplitter";
    public static final String GEMS_GAOS_ASTROMETRIC = "astrometricMode";

    // Default values
    private static final String DEFAULT_NGS_GSMAG = "11.0";
    private static final String DEFAULT_LGS_GSMAG = "11.0";
    private static final String DEFAULT_SEEING = "0.23";
    private static final String DEFAULT_WINDSPEED = "20";

    private ObservationEnvironment _oe;
    private String _configName;

    public AOConfig(ObservationEnvironment oe) {
        super("");
        if (oe == null) throw new NullPointerException("ObservationEnvironment");
        _oe = oe;
        _configName = TccNames.NO_AO;
    }

    public String getConfigName() {
        return _configName;
    }

    private void _setConfigName(String configName) {
        addAttribute(NAME, configName);
        _configName = configName;
    }

    public boolean build() throws WdbaGlueException {
        // Create a  gaos config

        // If in south, no AO yet
        if (_oe.isSouth()) {
            addAttribute(TYPE, "gems");
            buildGems();
        } else {
            addAttribute(TYPE, TccNames.GAOS);
            buildAltair();
        }
        return true;
    }

    private void buildGems() throws WdbaGlueException {
        Gems gems = _oe.getGemsConfig();
        if (gems == null) {
            _setConfigName(TccNames.NO_AO);
            return;
        }

        _setConfigName(TccNames.GEMS_GAOS);
        putParameter(GEMS_GAOS_ADC,         gems.getAdc().sequenceValue());
        putParameter(GEMS_GAOS_DICHROIC,    gems.getDichroicBeamsplitter().sequenceValue());
        putParameter(GEMS_GAOS_ASTROMETRIC, gems.getAstrometricMode().sequenceValue());
    }

    private void buildAltair() throws WdbaGlueException {
        InstAltair altair = _oe.getAltairConfig();
        // IF there is no Altair in the north, "No AO"
        if (altair == null) {
            _setConfigName(TccNames.NO_AO);
            return;
        }
        // There is an Altair so we need a special AO Config
        AltairParams.GuideStarType guideStarType = altair.getGuideStarType();
        if (guideStarType == AltairParams.GuideStarType.NGS) {
            _setConfigName(NGS_CONFIG_NAME);
            putParameter(LGS, TccNames.OFF);
            putParameter(GSMAG, DEFAULT_NGS_GSMAG);
        } else if (guideStarType == AltairParams.GuideStarType.LGS) {
            _setConfigName(LGS_CONFIG_NAME);
            putParameter(LGS, TccNames.ON);
            putParameter(GSMAG, DEFAULT_LGS_GSMAG);
        } else {
            _setConfigName(TccNames.NO_AO);
            return;
        }

        // Commmon parameters
        putParameter(SEEING, DEFAULT_SEEING);
        putParameter(WINDSPEED, DEFAULT_WINDSPEED);

        putParameter(FLDLENS, altair.getFieldLens().sequenceValue());
        putParameter(NDFILTER, altair.getNdFilter().sequenceValue());
        putParameter(WAVELENGTH, altair.getWavelength().sequenceValue());

        // If the adc is on, set both the  ao config things to in, not documented, we will see
        putParameter(ADJUSTADC, altair.getAdc() == AltairParams.ADC.ON ? TccNames.ON : TccNames.OFF);
        putParameter(DEPLOYADC, altair.getAdc() == AltairParams.ADC.ON ? "IN" : "OUT");
    }
}
