//
// $
//

package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;



/**
 * The observation constraints associated with the observation.
 */
public final class ConstraintsNode extends AbstractNodeContext<ISPObsComponent, SPSiteQuality> {
    public ConstraintsNode(ISPObsComponent node)  {
        super(node);
    }
}
