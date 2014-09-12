package edu.gemini.spModel.gemini.trecs;

import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

//
// Gemini Observatory/AURA
// $Id: InstEngTReCSCB.java 27568 2010-10-25 18:03:42Z swalker $
//

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

    protected void thisReset(Map options) {
        InstEngTReCS dataObj = (InstEngTReCS) getDataObject();
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        if (_sysConfig == null) return false;
        return (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        String systemName = _sysConfig.getSystemName();
        Collection sysConfig = _sysConfig.getParameters();

        Iterator it = sysConfig.iterator();
        while (it.hasNext()) {
            IParameter param = (IParameter) it.next();
            config.putParameter(systemName,
                                DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                                                        InstTReCS.INSTRUMENT_NAME_PROP));
    }

}


