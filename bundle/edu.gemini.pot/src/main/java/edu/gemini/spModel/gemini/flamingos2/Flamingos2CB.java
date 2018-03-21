package edu.gemini.spModel.gemini.flamingos2;

import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Collection;
import java.util.Map;

/**
 * The configuration builder for the Flamingos2 data object.
 */
public class Flamingos2CB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public Flamingos2CB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        Flamingos2CB result = (Flamingos2CB) super.clone();
        result._sysConfig = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        Flamingos2 dataObj = (Flamingos2) getDataObject();
        if (dataObj == null)
            throw new IllegalArgumentException("The data object for Flamingos2 can not be null");
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
                                                        Flamingos2.INSTRUMENT_NAME_PROP));

        // SCT-85: add the observing wavelength
        Flamingos2.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }

}
