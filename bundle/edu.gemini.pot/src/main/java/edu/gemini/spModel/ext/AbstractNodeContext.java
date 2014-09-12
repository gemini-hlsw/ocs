//
// $
//

package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;



/**
 * Implementation of the {@link NodeContext} interface.  Subclasses may extend
 * this class and simply specify the information that is specific to the
 * particular node type.  For example, the {@link ObservationNode} provides
 * access to its contained {@link TargetNode}, etc.
 */
public abstract class AbstractNodeContext<N extends ISPNode, D extends ISPDataObject> implements NodeContext<N, D> {

    private final N remoteNode;
    private final D dataObject;
    private final SPNodeKey nodeKey;
    private final SPProgramID progId;

    protected AbstractNodeContext(N node)  {
        remoteNode = node;
        dataObject = (D) node.getDataObject();
        nodeKey    = node.getNodeKey();
        progId     = node.getProgramID();
    }

    public final N getRemoteNode() {
        return remoteNode;
    }

    public final D getDataObject() {
        return dataObject;
    }

    public final SPNodeKey getKey() {
        return nodeKey;
    }

    public final SPProgramID getProgramId() {
        return progId;
    }
}
