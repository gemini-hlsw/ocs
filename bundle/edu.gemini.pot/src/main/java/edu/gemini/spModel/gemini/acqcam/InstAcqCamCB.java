// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: InstAcqCamCB.java 27568 2010-10-25 18:03:42Z swalker $
//

package edu.gemini.spModel.gemini.acqcam;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;

import java.util.Collection;
import java.util.Iterator;
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
        if (_sysConfig == null) return false;
        return (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig fullPrev) {
        String systemName = _sysConfig.getSystemName();
        Collection sysConfig = _sysConfig.getParameters();

        Iterator it = sysConfig.iterator();
        while (it.hasNext()) {
            IParameter param = (IParameter) it.next();
            config.putParameter(systemName,
                                DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP, InstAcqCam.INSTRUMENT_NAME_PROP));
    }

}
