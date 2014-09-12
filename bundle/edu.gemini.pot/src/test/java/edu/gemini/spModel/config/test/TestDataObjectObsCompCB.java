// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TestDataObjectObsCompCB.java 27568 2010-10-25 18:03:42Z swalker $
//

package edu.gemini.spModel.config.test;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


public class TestDataObjectObsCompCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public TestDataObjectObsCompCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        TestDataObjectObsCompCB result = (TestDataObjectObsCompCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    protected void thisReset(Map options) {
        TestDataObject dataObj = (TestDataObject) getDataObject();
        _sysConfig = dataObj.getSysConfig();
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
            IParameter param2 = DefaultParameter.getInstance(param.getName(), param.getValue());
            config.putParameter(systemName, param2);
        }
    }

}
