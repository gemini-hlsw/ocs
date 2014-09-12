// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: InstTReCSCB.java 27669 2010-10-28 17:44:55Z swalker $
//

package edu.gemini.spModel.gemini.trecs;

import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.obscomp.InstConstants;

import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Collection;
import java.util.Iterator;
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

    protected void thisReset(Map options) {
        InstTReCS dataObj = (InstTReCS) getDataObject();
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

        // SCT-85: add the observing wavelength
        InstTReCS.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }

}
