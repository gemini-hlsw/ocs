package edu.gemini.spModel.gemini.trecs;

import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.obscomp.InstConstants;

import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Map;

/**
 * InstTReCSCB is the configuration builder for the InstTReCS data
 * object.
 */
public class InstTReCSCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public InstTReCSCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstTReCSCB result = (InstTReCSCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        InstTReCS dataObj = (InstTReCS) getDataObject();
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

        // SCT-85: add the observing wavelength
        InstTReCS.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }

}
