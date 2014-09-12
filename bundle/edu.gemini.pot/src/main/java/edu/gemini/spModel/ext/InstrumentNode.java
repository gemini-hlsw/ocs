//
// $
//

package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.obscomp.SPInstObsComp;



/**
 * The instrument node associated with an observation.
 */
public final class InstrumentNode extends AbstractNodeContext<ISPObsComponent, SPInstObsComp> {
    public InstrumentNode(ISPObsComponent node)  {
        super(node);
    }
}
