package edu.gemini.spModel.gemini.bhros;

import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import edu.gemini.pot.sp.ISPObsComponent;

import java.util.Collection;
import java.util.Map;
//$Id: InstBHROSCB.java 27568 2010-10-25 18:03:42Z swalker $

public class InstBHROSCB extends AbstractObsComponentCB {

    private transient ISysConfig sysConfig;

    public InstBHROSCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        InstBHROSCB result = (InstBHROSCB) super.clone();
        result.sysConfig = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        InstBHROS dataObj = (InstBHROS) getDataObject();
        if (dataObj == null)
            throw new IllegalArgumentException("The data object for BHROS can not be null");
        sysConfig = dataObj.getSysConfig();
    }

    protected boolean thisHasConfiguration() {
        return sysConfig != null && (sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        String systemName = sysConfig.getSystemName();
        Collection<IParameter> sysConfigParams = sysConfig.getParameters();
        for (IParameter param : sysConfigParams) {
            config.putParameter(systemName, DefaultParameter.getInstance(
                    param.getName(),
                    param.getValue()));
        }

        config.putParameter(systemName, StringParameter.getInstance(
                InstConstants.INSTRUMENT_NAME_PROP,
                InstBHROS.INSTRUMENT_NAME_PROP));

        // Add the observing wavelength, which should be the same as the central wavelength.
        Double centralWavelength = (Double) config.getParameterValue(SeqConfigNames.INSTRUMENT_CONFIG_NAME, InstBHROS.CENTRAL_WAVELENGTH_PROP.getName());
        config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME, DefaultParameter.getInstance(InstConstants.OBSERVING_WAVELENGTH_PROP, centralWavelength));

    }

}
