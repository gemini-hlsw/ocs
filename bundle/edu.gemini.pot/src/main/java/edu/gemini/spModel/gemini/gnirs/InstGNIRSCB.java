// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: InstGNIRSCB.java 27568 2010-10-25 18:03:42Z swalker $
//

package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
 * InstGNIRSCB is the configuration builder for the InstGNIRS data
 * object.
 */
public class InstGNIRSCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;
    private transient InstGNIRS _dataObj;

    public InstGNIRSCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstGNIRSCB result = (InstGNIRSCB) super.clone();
        result._sysConfig = null;
        result._dataObj   = null;
        return result;
    }

    protected void thisReset(Map options) {
        _dataObj = (InstGNIRS) getDataObject();
        _sysConfig = _dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        if (_sysConfig == null)
            return false;
        return (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        String systemName = _sysConfig.getSystemName();
        Collection sysConfig = _sysConfig.getParameters();

        Iterator it = sysConfig.iterator();
        while (it.hasNext()) {
            IParameter param = (IParameter) it.next();
            config.putParameter(systemName, param);

            // insert the observing wavelength
            Object value = param.getValue();
            if (value != null) {
                String name = param.getName();
                if (name.equals(InstGNIRS.CENTRAL_WAVELENGTH_PROP.getName())) {
                    config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME,
                            DefaultParameter.getInstance(GNIRSConstants.OBSERVING_WAVELENGTH_PROP, value));
                }
            }
        }

        config.putParameter(systemName,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP, _dataObj.getReadable()));

    }
}
