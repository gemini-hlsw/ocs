package edu.gemini.spModel.config;

import edu.gemini.pot.sp.ISPObsComponent;

import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IConfigProvider;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;

import java.util.Map;

public class HelperObsCompCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public HelperObsCompCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        HelperObsCompCB result = (HelperObsCompCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    protected void thisReset(Map options) {
        IConfigProvider dataObj = (IConfigProvider) getDataObject();
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        return _sysConfig != null && (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        String systemName = _sysConfig.getSystemName();
        for (IParameter param : _sysConfig.getParameters()) {
            config.putParameter(systemName, param);
        }
    }

}
