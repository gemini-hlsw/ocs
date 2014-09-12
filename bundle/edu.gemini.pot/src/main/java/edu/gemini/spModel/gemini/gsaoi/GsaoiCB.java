//
// $
//

package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;


import java.util.Collection;
import java.util.Map;

/**
 * Configuration Builder for GSAOI.
 */
public final class GsaoiCB extends AbstractObsComponentCB {

    private transient ISysConfig sysConfig;

    public GsaoiCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        GsaoiCB result = (GsaoiCB) super.clone();
        result.sysConfig = null;
        return result;
    }

    protected void thisReset(Map options)  {
        Gsaoi dataObj = (Gsaoi) getDataObject();
        if (dataObj == null) {
            throw new RuntimeException("Gsaoi data object is null");
        }
        sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration()  {
        if (sysConfig == null) return false;
        return (sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull)  {
        String systemName = sysConfig.getSystemName();

        Collection<IParameter> params = sysConfig.getParameters();
        for (IParameter param : params) {
            config.putParameter(systemName,
                    DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP, Gsaoi.SP_TYPE.narrowType));

        Gsaoi.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }
}
