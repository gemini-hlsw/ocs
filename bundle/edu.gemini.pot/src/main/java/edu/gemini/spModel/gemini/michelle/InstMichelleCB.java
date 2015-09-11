// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: InstMichelleCB.java 27615 2010-10-26 20:14:30Z swalker $
//

package edu.gemini.spModel.gemini.michelle;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;

import java.util.Collection;
import java.util.Map;


/**
 * InstMichelleCB is the configuration builder for the InstMichelle data
 * object.
 */
public class InstMichelleCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public InstMichelleCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstMichelleCB result = (InstMichelleCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    protected void thisReset(Map options) {
        InstMichelle dataObj = (InstMichelle) getDataObject();
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
                                                        InstMichelle.INSTRUMENT_NAME_PROP));

        // SCT-85: Add the observing wavelength
        InstMichelle.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }

}
