package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MemTemplateFolder extends MemAbstractContainer implements ISPTemplateFolder {
    private static final Logger LOG = Logger.getLogger(MemTemplateFolder.class.getName());

    private final MemProgram program;
    private final List<ISPTemplateGroup> templates = new ArrayList<>();

    public MemTemplateFolder(MemProgram prog, SPNodeKey key)  {
        super(prog.getDocumentData(), key);
        this.program = prog;
    }

    public MemTemplateFolder(MemProgram prog, ISPFactory fact, ISPTemplateFolder that, boolean preserveKeys)  {
        super(prog.getDocumentData(), that, preserveKeys);
        this.program = prog;

        List<ISPTemplateGroup> thatList = that.getTemplateGroups();
        List<ISPTemplateGroup> newList  = new ArrayList<>(thatList.size());
        for (ISPTemplateGroup tg : thatList) {
            try {
                newList.add(fact.createTemplateGroupCopy(prog, tg, preserveKeys));
            } catch (SPUnknownIDException ex) {
                LOG.log(Level.SEVERE, "Bug in MemTemplateFolder", ex);
                throw new RuntimeException("Bug in MemTemplateFolder");
            }
        }

        getProgramWriteLock();
        try {
            ISPConflictFolder cf = that.getConflictFolder();
            if (cf != null) setConflictFolder(fact.createConflictFolderCopy(prog, cf, preserveKeys));

            updateChildren(templates, newList);
        } catch (SPException ex) {
            LOG.log(Level.SEVERE, "Bug in MemTemplateFolder", ex);
            throw new RuntimeException("Bug in MemTemplateFolder");
        } finally {
            returnProgramWriteLock();
        }
    }

    private static final Class<?>[] VALID_CHILD_TYPES = {
        MemConflictFolder.class,
        MemTemplateGroup.class,
    };

    @Override protected void setTypedChildren(TypedChildren tc) throws SPNodeNotLocalException, SPTreeStateException {
        tc.verify(VALID_CHILD_TYPES);
        setConflictFolder(tc.getOnlyChild(MemConflictFolder.class));
        setTemplateGroups(tc.getChildren(MemTemplateGroup.class));
    }

    @Override public List<ISPNode> getChildren()  {
        getProgramReadLock();
        try {
            List<ISPNode> children = new ArrayList<>();
            if (getConflictFolder() != null) children.add(getConflictFolder());
            children.addAll(templates);
            return children;
        } finally {
            returnProgramReadLock();
        }
    }

    @Override
    public List<ISPTemplateGroup> getTemplateGroups() {
        getProgramReadLock();
        try {
            return new ArrayList<>(templates);
        } finally {
            returnProgramReadLock();
        }
    }

    @Override
    public void setTemplateGroups(List<? extends ISPTemplateGroup> newGroupList) throws SPNodeNotLocalException, SPTreeStateException {
        List<ISPTemplateGroup> newCopy = new ArrayList<>(newGroupList);
        getProgramWriteLock();
        try {
            SPAssert.setsNoDuplicateObs(this, newCopy);
            List<ISPTemplateGroup> oldCopy = new ArrayList<>(templates);
            updateChildren(templates, newCopy);
            firePropertyChange(TEMPLATE_GROUP_PROP, oldCopy, newCopy);
            fireStructureChange(TEMPLATE_GROUP_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    @Override
    public void addTemplateGroup(ISPTemplateGroup group) throws SPNodeNotLocalException, SPTreeStateException {
        addTemplateGroup(-1, group);
    }

    @Override
    public void addTemplateGroup(int index, ISPTemplateGroup group) throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException {
        MemTemplateGroup node = (MemTemplateGroup) group;
        getProgramWriteLock();
        try {
            SPAssert.addsNoDuplicateObs(this, group);
            List<ISPTemplateGroup> oldCopy = new ArrayList<>(templates);
            node.attachTo(this);
            if (index >= 0) templates.add(index, node); else templates.add(node);
            List<ISPTemplateGroup> newCopy = new ArrayList<>(templates);
            firePropertyChange(TEMPLATE_GROUP_PROP, oldCopy, newCopy);
            fireStructureChange(TEMPLATE_GROUP_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    @Override
    public void removeTemplateGroup(ISPTemplateGroup group) {
        MemTemplateGroup node = (MemTemplateGroup) group;

        getProgramWriteLock();
        try {
            int index = templates.indexOf(node);
            if (index == -1) {
                LOG.warning("template group was not located and cannot be removed");
                return;
            }
            List<ISPTemplateGroup> oldCopy = new ArrayList<>(templates);
            node.detachFrom(this);
            templates.remove(index);
            List<ISPTemplateGroup> newCopy = new ArrayList<>(templates);
            firePropertyChange(TEMPLATE_GROUP_PROP, oldCopy, newCopy);
            fireStructureChange(TEMPLATE_GROUP_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    @Override
    public void accept(ISPProgramVisitor visitor)  {
        visitor.visitTemplateFolder(this);
    }

    @Override public MemProgram getProgram() { return program; }
}
