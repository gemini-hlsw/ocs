package edu.gemini.spModel.gemini.trecs;

import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Map;

public class InstEngTReCSCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public InstEngTReCSCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstEngTReCSCB result = (InstEngTReCSCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        InstEngTReCS dataObj = (InstEngTReCS) getDataObject();
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        return _sysConfig != null && (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        String systemName = _sysConfig.getSystemName();

        for (IParameter param : _sysConfig.getParameters()) {
            config.putParameter(systemName,
                    DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                                                        InstTReCS.INSTRUMENT_NAME_PROP));
    }

}


