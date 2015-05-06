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

    public final int representedCount;

    public ProblemRollup(Type type, String id, String description, ISPProgramNode node, int representedCount) {
        super (type, id, description, node);
        this.representedCount = representedCount;
    }

    public int getRepresentedCount() {
        return representedCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ProblemRollup that = (ProblemRollup) o;
        return representedCount == that.representedCount;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + representedCount;
        return result;
    }
}
