package edu.gemini.spModel.gemini.altair;

import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;

import edu.gemini.spModel.ao.AOConstants;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsCompConstants;

import java.util.Map;

/**
 * InstAltairCB is the configuration builder for the InstAltair data
 * object.
 */
public class InstAltairCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public InstAltairCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstAltairCB result = (InstAltairCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        InstAltair dataObj = (InstAltair) getDataObject();
        if (dataObj == null)
            throw new IllegalArgumentException("The data object for Altair can not be null");
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        return _sysConfig != null && (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig fullPrev) {
        String systemName = _sysConfig.getSystemName();

        // REL-542 hack.  Seqexec not capable of figuring this out.
        InstAltair altair = (InstAltair) getDataObject();
        if ((altair != null) && ((altair.getMode() == AltairParams.Mode.LGS_P1) || (altair.getMode() == AltairParams.Mode.LGS_OI))) {
            GuideProbe guider = AltairAowfsGuider.instance;
            config.putParameter(
                    TargetObsCompConstants.CONFIG_NAME,
                    DefaultParameter.getInstance(
                            guider.getSequenceProp(),
                            guider.getGuideOptions().getDefaultActive()));
        }

        for (IParameter param : _sysConfig.getParameters()) {
            config.putParameter(systemName, DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName, StringParameter.getInstance(AOConstants.AO_SYSTEM_PROP, AltairConstants.SYSTEM_NAME_PROP));
    }

}
