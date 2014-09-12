//
// $
//

package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.data.AbstractDataObject;



/**
 * The adaptive optics component in the observation.
 */
public final class AoNode extends AbstractNodeContext<ISPObsComponent, AbstractDataObject> {
    public AoNode(ISPObsComponent node)  {
        super(node);
    }
}
