//
// $
//

package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;

import java.io.Serializable;

/**
 * This class combines a remote node with its data object in a single local
 * object.  It is used to work around a model problem in which obtaining
 * information about a node requires a remote method call.
 */
public interface NodeContext<N extends ISPNode, D extends ISPDataObject> extends Serializable {

    /**
     * Gets the unique key associated with the node.
     */
    SPNodeKey getKey();

    /**
     * Gets the program ID associated with this node.
     */
    SPProgramID getProgramId();

    /**
     * Gets the remote reference to the node in the ODB.
     */
    N getRemoteNode();

    /**
     * Gets the data object associated with the node.
     */
    D getDataObject();
}
