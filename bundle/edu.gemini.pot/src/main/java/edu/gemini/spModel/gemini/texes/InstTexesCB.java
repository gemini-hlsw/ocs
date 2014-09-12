package edu.gemini.spModel.gemini.texes;

import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Collection;
import java.util.Map;

/**
 * InstPhoenixCB is the configuration builder for the InstTexes data
 * object.
 */
public class InstTexesCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public InstTexesCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstTexesCB result = (InstTexesCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    protected void thisReset(Map options) {
        InstTexes dataObj = (InstTexes) getDataObject();
        if (dataObj == null)
            throw new IllegalArgumentException("The data objectfor Texes can not be null");
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

        for (Object sc : sysConfig) {
            IParameter param = (IParameter) sc;
            config.putParameter(systemName,
                    DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                                                        InstTexes.INSTRUMENT_NAME_PROP));
        // Add the observing wavelength, which should be the same as the wavelength.
        Double centralWavelength = (Double) config.getParameterValue(SeqConfigNames.INSTRUMENT_CONFIG_NAME, InstTexes.WAVELENGTH_PROP.getName());
        config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME, DefaultParameter.getInstance(InstConstants.OBSERVING_WAVELENGTH_PROP, centralWavelength));

    }

}
