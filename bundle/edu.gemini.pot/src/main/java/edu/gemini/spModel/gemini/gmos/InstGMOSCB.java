// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: InstGMOSCB.java 47181 2012-08-02 16:40:03Z swalker $
//

package edu.gemini.spModel.gemini.gmos;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import static edu.gemini.spModel.guide.DefaultGuideOptions.Value.on;
import edu.gemini.spModel.guide.GuideOption;
import static edu.gemini.spModel.guide.StandardGuideOptions.Value.*;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.target.obsComp.TargetObsCompConstants;
import edu.gemini.spModel.target.offset.OffsetPos;
import edu.gemini.spModel.target.offset.OffsetPosList;

import java.util.Collection;
import java.util.Map;


/**
 * InstGMOSCB is the configuration builder for the InstGMOS data
 * object.
 */
public class InstGMOSCB extends AbstractObsComponentCB {

    // The following are needed for the N/S config.  Needs fixing
    private static final String BEAM_LABELS = "ABCDEFG";
    private static final int MAX_BEAM_LABELS = BEAM_LABELS.length();

    private transient ISysConfig _sysConfig;
    private transient InstGmosCommon _dataObj;

    public InstGMOSCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstGMOSCB result = (InstGMOSCB) super.clone();
        result._sysConfig = null;
        result._dataObj   = null;
        return result;
    }

    protected void thisReset(Map options) {
        _dataObj = (InstGmosCommon) getDataObject();
        if (_dataObj == null) {
            System.out.println("It's null!");
        }
        _sysConfig = _dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        if (_sysConfig == null) {
            return false;
        }
        return (_sysConfig.getParameterCount() > 0);
    }

    private String _getParamName(int i, String paramName) {
        if (i > MAX_BEAM_LABELS) {
            return "OUT OF RANGE";
        }

        char c = BEAM_LABELS.charAt(i);
        return "nsBeam" + c + "-" + paramName;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        String systemName    = _sysConfig.getSystemName();
        Collection sysConfig = _sysConfig.getParameters();

        for (Object aSysConfig : sysConfig) {
            IParameter param = (IParameter) aSysConfig;
            config.putParameter(systemName, param);
        }

        config.putParameter(systemName, StringParameter.getInstance(
                InstConstants.INSTRUMENT_NAME_PROP, _dataObj.getReadable()));

        // Add same generated items as in the sequence
        boolean isNorth = !(_dataObj instanceof InstGmosSouth);
        if (isNorth) {
            InstGmosNorth.WAVELENGTH_INJECTOR.inject(config, prevFull);
            InstGmosNorth.GAIN_SETTING_INJECTOR.inject(config, prevFull);
        } else {
            InstGmosSouth.WAVELENGTH_INJECTOR.inject(config, prevFull);
            InstGmosSouth.GAIN_SETTING_INJECTOR.inject(config, prevFull);
        }

        if (_dataObj.useNS()) {
            OffsetPosList posList = _dataObj.getPosList();

            // Do the offsets for N/S
            for (int i = 0, size = posList.size(); i < size; i++) {
                OffsetPos op = (OffsetPos) posList.getPositionAt(i);

                config.putParameter(systemName, StringParameter.getInstance(
                        _getParamName(i, "p"), op.getXAxisAsString()));
                config.putParameter(systemName, StringParameter.getInstance(
                        _getParamName(i, "q"), op.getYAxisAsString()));

                GuideOption curOIWFS = op.getLink(GmosOiwfsGuideProbe.instance);
                // FR30465
                if (curOIWFS == null) curOIWFS = op.getDefaultGuideOption() == on ? guide : freeze;

                config.putParameter(systemName, StringParameter.getInstance(
                        _getParamName(i, TargetObsCompConstants.GUIDE_WITH_OIWFS_PROP),
                        curOIWFS.name()));
            }
        }
    }
}
