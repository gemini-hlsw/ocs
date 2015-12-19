package edu.gemini.spModel.config.test;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;

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

    @Override
    protected void thisReset(Map<String, Object> options) {
        TestDataObject dataObj = (TestDataObject) getDataObject();
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        return _sysConfig != null && (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        String systemName = _sysConfig.getSystemName();

        for (IParameter param : _sysConfig.getParameters()) {
            IParameter param2 = DefaultParameter.getInstance(param.getName(), param.getValue());
            config.putParameter(systemName, param2);
        }
    }

}
