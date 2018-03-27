package edu.gemini.spModel.conflict;

import edu.gemini.pot.sp.*;

public enum ConflictFolderNI implements ISPNodeInitializer<ISPConflictFolder, ConflictFolder> {
    instance;

    @Override
    public SPComponentType getType() {
        return SPComponentType.CONFLICT_FOLDER;
    }

    @Override
    public ConflictFolder createDataObject() {
        return new ConflictFolder();
    }

    @Override
    public void initNode(ISPFactory factory, ISPConflictFolder node) {
        node.setDataObject(createDataObject());
        node.addConflictNote(new Conflict.ConflictFolder(node.getNodeKey()));
    }
}
