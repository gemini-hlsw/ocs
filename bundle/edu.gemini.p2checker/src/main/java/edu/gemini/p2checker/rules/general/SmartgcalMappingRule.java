//
// $Id$
//

package edu.gemini.p2checker.rules.general;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.p2checker.util.P2CheckerUtil;
import edu.gemini.spModel.gemini.seqcomp.smartgcal.SmartgcalMappingErrorMessage;



/**
 * A rule that checks for smart gcal mapping errors.
 */
public class SmartgcalMappingRule implements IRule {
    private static final String PREFIX = "SmartgcalMappingRule_";
    public IP2Problems check(ObservationElements elements)  {
        IP2Problems probs = P2CheckerUtil.NO_PROBLEMS;
        String msg = SmartgcalMappingErrorMessage.get(elements.getSequence(), null);
        if (msg != null) {
            probs = new P2Problems();
            probs.addError(PREFIX+"SmartgcalMappingRule", msg, elements.getSeqComponentNode());
        }
        return probs;
    }
}
