package edu.gemini.spModel.gemini.visitor;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.util.Map;

/**
 * Configuration builder for Visitor Instruments
 */
public class VisitorInstrumentCB extends AbstractObsComponentCB {
    private transient ISysConfig _sysConfig;

    public VisitorInstrumentCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    @Override
    public Object clone() {
        VisitorInstrumentCB result = (VisitorInstrumentCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        // No configuration
        VisitorInstrument dataObj = (VisitorInstrument) getDataObject();
        if (dataObj == null)
            throw new IllegalArgumentException("The data object for Visitor can not be null");
        _sysConfig = dataObj.getSysConfig();
    }

    @Override
    protected boolean thisHasConfiguration() {
        return _sysConfig != null && (_sysConfig.getParameterCount() > 0);
    }

    @Override
    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        // No configuration
        String systemName = _sysConfig.getSystemName();

        for (IParameter param : _sysConfig.getParameters()) {
            config.putParameter(systemName,
                    DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                                                        VisitorInstrument.INSTRUMENT_NAME_PROP));
        // Add the observing wavelength, which should be the same as the wavelength.
        Double centralWavelength = (Double) config.getParameterValue(SeqConfigNames.INSTRUMENT_CONFIG_NAME, VisitorInstrument.WAVELENGTH_PROP.getName());
        config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME, DefaultParameter.getInstance(InstConstants.OBSERVING_WAVELENGTH_PROP, centralWavelength));
    }
}
