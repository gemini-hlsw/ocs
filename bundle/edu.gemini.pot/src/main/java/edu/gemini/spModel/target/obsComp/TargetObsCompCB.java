package edu.gemini.spModel.target.obsComp;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.target.env.TargetEnvironment;

import java.util.Map;

/**
 * TargetEnvCB is the configuration builder for the TargetEnv as used
 * by Gemini.
 */
public class TargetObsCompCB extends AbstractObsComponentCB {
    // The name of the system
    private static final String SYSTEM_NAME = TargetObsCompConstants.CONFIG_NAME;
    private transient TargetObsComp _dataObj;

    public TargetObsCompCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        TargetObsCompCB result = (TargetObsCompCB) super.clone();
        result._dataObj = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        _dataObj = (TargetObsComp) getDataObject();
        if (_dataObj == null) throw new IllegalArgumentException("TargetEnv data object of null is not allowed");
    }

    protected boolean thisHasConfiguration() {
        return (_dataObj != null);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {

        // Fill in the current values.
        IParameter ip = StringParameter.getInstance(ISPDataObject.VERSION_PROP, _dataObj.getVersion());
        config.putParameter(SYSTEM_NAME, ip);

        config.putParameter(SYSTEM_NAME,
                DefaultParameter.getInstance(GuideSequence.GUIDE_STATE_PARAM, GuideSequence.GuideState.DEFAULT_ON));

        TargetEnvironment env = _dataObj.getTargetEnvironment();

        // Patch for a problem in 2009B.1.1.1.  The "telescope:Base:name" param
        // was missing, which caused the FITS OBJECT keyword to be incorrect.
        // TODO:ASTERISM: how do we handle multiples? For now just use an arbitrary target's name.
        String baseName = env.getArbitraryTargetFromAsterism().getName();
        if ((baseName != null) && !"".equals(baseName)) {
            DefaultConfigParameter cp = DefaultConfigParameter.getInstance("Base");
            ISysConfig sc = (ISysConfig) cp.getValue();
            ip = StringParameter.getInstance(TargetObsCompConstants.NAME_PROP, baseName);
            sc.putParameter(ip);
            config.putParameter(SYSTEM_NAME, cp);
        }
    }
}
