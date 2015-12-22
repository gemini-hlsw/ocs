package edu.gemini.spModel.gemini.gems;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.ao.AOConstants;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;


import java.util.Collection;
import java.util.Map;

/**
 * The {@link edu.gemini.spModel.config.IConfigBuilder configuration builder}
 * for Gems. Responsible for adding bits of configuration information into
 * the sequence.
 */
public final class GemsCB extends AbstractObsComponentCB {

    private transient ISysConfig sysConfig;

    public GemsCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        GemsCB res = (GemsCB) super.clone();
        res.sysConfig = null;
        return res;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        Gems dataObj = (Gems) getDataObject();
        if (dataObj == null) throw new RuntimeException("Missing GemsCB data object");
        sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        return (sysConfig != null) && (sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull)  {
        String sysName = sysConfig.getSystemName();
        Collection<IParameter> params = sysConfig.getParameters();

        for (IParameter param : params) {
            config.putParameter(sysName, DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(sysName, StringParameter.getInstance(AOConstants.AO_SYSTEM_PROP, Gems.SYSTEM_NAME));
    }
}
