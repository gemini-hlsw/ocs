package edu.gemini.p2checker.rules.gmos;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.util.SPTreeUtil;

import java.util.List;

public class GmosOffsetIteratorRule implements IRule {

    private static final String PREFIX = "GmosOffsetIteratorRule_";
    private static final String MSG = "Offset iterators are not permitted with nod and shuffle.";

    @Override
    public IP2Problems check(ObservationElements elements) {
        final P2Problems ps = new P2Problems();
        switch (elements.getInstrument().getType()) {
            case INSTRUMENT_GMOS:
            case INSTRUMENT_GMOSSOUTH:
                if (((InstGmosCommon) elements.getInstrument()).useNS()) {
                    final List<ISPSeqComponent> cs = SPTreeUtil.findSeqComponents(elements.getObservationNode(), SPComponentType.ITERATOR_OFFSET);
                    for (ISPSeqComponent c : cs) {
                        ps.addError(PREFIX + "ERROR", MSG, c);
                    }
                }
        }
        return ps;
    }

}
