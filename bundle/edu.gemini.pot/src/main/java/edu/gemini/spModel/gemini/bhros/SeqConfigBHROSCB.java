package edu.gemini.spModel.gemini.bhros;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.util.Map;

//$Id: SeqConfigBHROSCB.java 27568 2010-10-25 18:03:42Z swalker $
public final class SeqConfigBHROSCB extends HelperSeqCompCB {

    public transient Double previousCentralWavelength;

    public SeqConfigBHROSCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqConfigBHROSCB result = (SeqConfigBHROSCB) super.clone();
        result.previousCentralWavelength = null;
        return result;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        super.thisApplyNext(config, prevFull);

        // Always add the instrument name, for some reason.
        config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME,
            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP, InstBHROS.INSTRUMENT_NAME_PROP));

        // Add the observing wavelength, which should be the same as the central wavelength.
        Double centralWavelength = (Double) config.getParameterValue(SeqConfigNames.INSTRUMENT_CONFIG_NAME, InstBHROS.CENTRAL_WAVELENGTH_PROP.getName());
        if (centralWavelength != null && !centralWavelength.equals(previousCentralWavelength)) {
            config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME,
                DefaultParameter.getInstance(InstConstants.OBSERVING_WAVELENGTH_PROP, centralWavelength));
        }
        previousCentralWavelength = centralWavelength;

    }

    protected void thisReset(Map<String, Object> options) {
        super.thisReset(options);
        previousCentralWavelength = null;
    }

}
