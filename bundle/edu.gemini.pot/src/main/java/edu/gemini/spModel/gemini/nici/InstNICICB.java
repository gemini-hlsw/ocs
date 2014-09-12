package edu.gemini.spModel.gemini.nici;

import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Collection;
import java.util.Map;

import static edu.gemini.spModel.obscomp.InstConstants.OBSERVING_WAVELENGTH_PROP;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME;

public class InstNICICB extends AbstractObsComponentCB {

    private transient ISysConfig sysConfig;

    public InstNICICB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstNICICB result = (InstNICICB) super.clone();
        result.sysConfig = null;
        return result;
    }

    protected void thisReset(Map options) {
        InstNICI dataObj = (InstNICI) getDataObject();
        if (dataObj == null)
            throw new IllegalArgumentException("The data object for NICI can not be null");
        sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        if (sysConfig == null)
            return false;
        return (sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        String systemName = sysConfig.getSystemName();
        Collection sysConfigParams = sysConfig.getParameters();
        for (Object sysConfigParam : sysConfigParams) {
            IParameter param = (IParameter) sysConfigParam;
            config.putParameter(systemName, DefaultParameter.getInstance(
                    param.getName(),
                    param.getValue()));
        }

        config.putParameter(systemName, StringParameter.getInstance(
                InstConstants.INSTRUMENT_NAME_PROP,
                InstNICI.INSTRUMENT_NAME_PROP));

        injectWavelength(config, prevFull);
    }

    static void injectWavelength(IConfig config, IConfig prevFull) {
        InstNICI.WAVELENGTH_INJECTOR.inject(config, prevFull);

        // But only if present in config (which implies a new value has been
        // set).
        Object val = config.getParameterValue(INSTRUMENT_CONFIG_NAME, OBSERVING_WAVELENGTH_PROP);
        if (val != null) {
            config.putParameter(INSTRUMENT_CONFIG_NAME,
                    DefaultParameter.getInstance(InstNICI.CENTRAL_WAVELENGTH_PROP, val));
        }
    }

}
