// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: InstNIFSCB.java 27584 2010-10-25 21:18:41Z swalker $
//

package edu.gemini.spModel.gemini.nifs;

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
 * InstNIFSCB is the configuration builder for the InstNIFS data
 * object.
 */
public class InstNIFSCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public InstNIFSCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstNIFSCB result = (InstNIFSCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    protected void thisReset(Map options) {
        InstNIFS dataObj = (InstNIFS) getDataObject();
        if (dataObj == null)
            throw new IllegalArgumentException("The data objectfor NIFS can not be null");
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        if (_sysConfig == null)
            return false;
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

        // SCT-85: add the observing wavelength
        InstNIFS.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }

}
