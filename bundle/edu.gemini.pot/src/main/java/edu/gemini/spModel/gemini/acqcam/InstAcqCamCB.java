package edu.gemini.spModel.gemini.acqcam;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;

import java.util.Map;

/**
 * InstAcqCamCB is the configuration builder for the InstAcqCam data
 * object.
 */
public class InstAcqCamCB extends AbstractObsComponentCB {
    private transient ISysConfig _sysConfig;

    public InstAcqCamCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstAcqCamCB result = (InstAcqCamCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    protected void thisReset(Map options) {
        InstAcqCam dataObj = (InstAcqCam) getDataObject();
        if (dataObj == null) throw new IllegalArgumentException("The data objectfor AcqCam can not be null");
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        return _sysConfig != null && (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig fullPrev) {
        String systemName = _sysConfig.getSystemName();

        for (IParameter param : _sysConfig.getParameters()) {
            config.putParameter(systemName,
                    DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP, InstAcqCam.INSTRUMENT_NAME_PROP));
    }

}
