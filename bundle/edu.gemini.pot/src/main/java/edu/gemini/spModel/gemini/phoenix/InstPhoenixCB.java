package edu.gemini.spModel.gemini.phoenix;

import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.obscomp.InstConstants;

import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Collection;
import java.util.Map;

/**
 * InstPhoenixCB is the configuration builder for the InstPhoenix data
 * object.
 */
public class InstPhoenixCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public InstPhoenixCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstPhoenixCB result = (InstPhoenixCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    @SuppressWarnings("rawtypes")
    protected void thisReset(Map options) {
        InstPhoenix dataObj = (InstPhoenix) getDataObject();
        if (dataObj == null)
            throw new IllegalArgumentException("The data objectfor Phoenix can not be null");
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        return _sysConfig != null && (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        String systemName = _sysConfig.getSystemName();
        Collection<IParameter> sysConfig = _sysConfig.getParameters();

        for (IParameter param : sysConfig) {
            config.putParameter(systemName,
                    DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                                                        InstPhoenix.INSTRUMENT_NAME_PROP));
    }

}
