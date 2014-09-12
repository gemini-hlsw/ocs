//
// $
//

package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.data.AbstractDataObject;


import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * An observation sequence node and its children.
 */
public final class SequenceNode extends AbstractNodeContext<ISPSeqComponent, AbstractDataObject> {
    private final List<SequenceNode> children;

    public SequenceNode(ISPSeqComponent node)  {
        super(node);

        List<SequenceNode> tmp = new ArrayList<SequenceNode>();
        List<ISPNode> lst = node.getChildren();
        for (ISPNode child : lst) {
             tmp.add(new SequenceNode((ISPSeqComponent) child));
        }
        children = Collections.unmodifiableList(tmp);
    }

    public List<SequenceNode> getChildren() {
        return children;
    }
}
