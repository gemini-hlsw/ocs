/**
 * $Id: InstEngNifsCB.java 27568 2010-10-25 18:03:42Z swalker $
 */

package edu.gemini.spModel.gemini.nifs;

import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Collection;
import java.util.Map;

public class InstEngNifsCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public InstEngNifsCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstEngNifsCB result = (InstEngNifsCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    protected void thisReset(Map options) {
        InstEngNifs dataObj = (InstEngNifs) getDataObject();
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        if (_sysConfig == null) return false;
        return (_sysConfig.getParameterCount() > 0);
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
                                                        InstNIFS.INSTRUMENT_NAME_PROP));
    }
}
