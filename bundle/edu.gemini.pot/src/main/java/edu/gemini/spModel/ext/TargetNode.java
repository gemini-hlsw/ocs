//
// $
//

package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.target.obsComp.TargetObsComp;



/**
 * {@link NodeContext} for the target environment.
 */
public final class TargetNode extends AbstractNodeContext<ISPObsComponent, TargetObsComp> {
    public TargetNode(ISPObsComponent node)  {
        super(node);
    }
}
