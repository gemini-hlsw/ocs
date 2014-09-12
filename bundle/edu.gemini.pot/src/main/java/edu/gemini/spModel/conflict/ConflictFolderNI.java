package edu.gemini.spModel.conflict;

import edu.gemini.pot.sp.Conflict;
import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPNode;

public final class ConflictFolderNI implements ISPNodeInitializer {
    public void initNode(ISPFactory factory, ISPNode node) {
        ConflictFolder obj = new ConflictFolder();
        node.setDataObject(obj);
        node.addConflictNote(new Conflict.ConflictFolder(node.getNodeKey()));
    }

    public void updateNode(ISPNode node) {
    }
}
