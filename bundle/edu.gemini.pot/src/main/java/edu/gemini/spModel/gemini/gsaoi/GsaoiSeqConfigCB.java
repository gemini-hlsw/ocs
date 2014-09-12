//
// $
//

package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.util.Map;

/**
 * Configuration builder for the GSAOI iterator.
 */
public final class GsaoiSeqConfigCB extends HelperSeqCompCB {

    public GsaoiSeqConfigCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        GsaoiSeqConfigCB result = (GsaoiSeqConfigCB) super.clone();
        return result;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        super.thisApplyNext(config, prevFull);
        config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME,
                StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                        Gsaoi.SP_TYPE.narrowType));

        Gsaoi.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }

    public void thisReset(Map options) {
        super.thisReset(options);
    }
}
