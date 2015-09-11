package edu.gemini.spModel.dataflow;

import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

/**
 * Support for adding archive information to the sequence so that the seqexec
 * can use it to add release date and proprietary metadata header keys.
 */
public enum GsaSequenceEditor {
    instance;

    public static final String PROPRIETARY_MD     = "propMd";
    public static final String PROPRIETARY_MONTHS = "propMonths";

    public void addProprietaryMetadata(IConfig c, GsaAspect gsa) {
        c.putParameter(
            SeqConfigNames.OBSERVE_CONFIG_NAME,
            DefaultParameter.getInstance(PROPRIETARY_MD, gsa.isHeaderPrivate())
        );
    }

    public void addProprietaryPeriod(IConfig c, GsaAspect gsa, ObsClass obsClass) {
        final int m = obsClass.shouldChargeProgram() ? gsa.getProprietaryMonths() : 0;
        c.putParameter(
            SeqConfigNames.OBSERVE_CONFIG_NAME,
            DefaultParameter.getInstance(PROPRIETARY_MONTHS, m)
        );
    }
}
