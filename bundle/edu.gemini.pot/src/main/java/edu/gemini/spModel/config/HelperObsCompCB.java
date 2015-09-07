// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: HelperObsCompCB.java 27568 2010-10-25 18:03:42Z swalker $
//

package edu.gemini.spModel.config;

import edu.gemini.pot.sp.ISPObsComponent;

import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IConfigProvider;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;

import java.util.Collection;
import java.util.Iterator;
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
        Collection sysConfig = _sysConfig.getParameters();

        Iterator it = sysConfig.iterator();
        while (it.hasNext()) {
            IParameter param = (IParameter) it.next();
            config.putParameter(systemName, param);
        }
    }

}
