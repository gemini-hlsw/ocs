//
// $Id$
//

package edu.gemini.p2checker.api;

import edu.gemini.pot.sp.ISPProgramNode;

import java.io.Serializable;

/**
     * A Problem that represents a collection of Problems.
 * Provides a way to easily summarize the content of a container node.
 * <code>ProblemRepresentative</code> objects are associated commonly to nodes that
 * contain other nodes with actual <code>Problem</code>s
 */
public final class ProblemRollup extends Problem implements Serializable {

    private int _representedCount;

    public ProblemRollup(Type type, String id, String description, ISPProgramNode node, int representedCount) {
        super (type, id, description, node);
        _representedCount = representedCount;
    }

    public int getRepresentedCount() {
        return _representedCount;
    }
}
