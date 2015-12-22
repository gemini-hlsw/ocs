package edu.gemini.spModel.gemini.niri;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;

import java.util.Collection;
import java.util.Map;

/**
 * InstNIRICB is the configuration builder for the InstNIRI data
 * object.
 */
public class InstNIRICB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public InstNIRICB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstNIRICB result = (InstNIRICB) super.clone();
        result._sysConfig = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        InstNIRI dataObj = (InstNIRI) getDataObject();
        if (dataObj == null) {
            throw new IllegalArgumentException("The data objectfor NIRI can not be null");
        }
        _sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        return _sysConfig != null && (_sysConfig.getParameterCount() > 0);
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
                                                        InstNIRI.INSTRUMENT_NAME_PROP));

        // SCT-85: add the observing wavelength
        InstNIRI.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }

}
