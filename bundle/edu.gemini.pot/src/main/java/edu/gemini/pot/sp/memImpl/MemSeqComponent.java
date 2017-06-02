package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;


import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

/**
 * This class implements an in-memory, non-persistent
 * <code>ISPSeqComponent</code> object.
 */
public final class MemSeqComponent extends MemAbstractContainer implements ISPSeqComponent {
    private static final Logger LOG = Logger.getLogger(MemSeqComponent.class.getName());

    private final MemProgram _program;
    private final SPComponentType _type;

    // The list of sequence components
    private final List<ISPSeqComponent> _compList = new ArrayList<>();

    MemSeqComponent(MemProgram prog, SPComponentType type, SPNodeKey key) {
        super(prog.getDocumentData(), key);
        _program = prog;
        _type = type;
    }

    MemSeqComponent(MemProgram prog, ISPFactory fact,
                    ISPSeqComponent sc, boolean preserveKeys) {
        super(prog.getDocumentData(), sc, preserveKeys);
        _program = prog;
        _type = sc.getType();

        // Copy the components.
        List<ISPSeqComponent> compListCopy = sc.getSeqComponents();
        ListIterator<ISPSeqComponent> lit = compListCopy.listIterator();
        while (lit.hasNext()) {
            try {
                ISPSeqComponent scChild = lit.next();
                lit.set(fact.createSeqComponentCopy(prog, scChild, preserveKeys));
            } catch (SPUnknownIDException ex) {
                // This should never happen since the factory handed in this
                // ProgramData
                throw new RuntimeException("Bug 1 in MemSeqComponent(ProgramData, ISPFactory, ISPSeqComponent).");
            }
        }
        getProgramWriteLock(); // wouldn't strictly be required in this situation,
        // but updateChildren requires it
        try {
            ISPConflictFolder cf = sc.getConflictFolder();
            if (cf != null) setConflictFolder(fact.createConflictFolderCopy(prog, cf, preserveKeys));
            updateChildren(_compList, compListCopy);
        } catch (SPException ex) {
            // This should never happen since we just created the components
            // locally and they have never been added to any observation yet.
            throw new RuntimeException("Bug 2 in MemSeqComponent(ProgramData, ISPFactory, ISPSeqComponent).");
        } finally {
            returnProgramWriteLock();
        }
    }

    public SPComponentType getType() {
        return _type;
    }

    public List<ISPSeqComponent> getSeqComponents() {
        // Make a list copy so the client gets a stable version.
        getProgramReadLock();
        try {
            return new ArrayList<>(_compList);
        } finally {
            returnProgramReadLock();
        }
    }

    public void setSeqComponents(List<? extends ISPSeqComponent> newCompList) throws SPNodeNotLocalException, SPTreeStateException {
        checkChildTypes(newCompList, ISPSeqComponent.class);

        List<ISPSeqComponent> newCopy = new ArrayList<>(newCompList);
        getProgramWriteLock();
        try {
            List<ISPSeqComponent> oldCopy = new ArrayList<>(_compList);
            updateChildren(_compList, newCopy);
            firePropertyChange(SEQ_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(SEQ_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addSeqComponent(ISPSeqComponent seqComp) throws SPNodeNotLocalException, SPTreeStateException {
        // Get the local component (throwing an SPNodeNotLocalException if not
        // local).
        MemSeqComponent node = (MemSeqComponent) seqComp;
        getProgramWriteLock();
        try {
            List<ISPSeqComponent> oldCopy = new ArrayList<>(_compList);
            node.attachTo(this);
            _compList.add(node);
            List<ISPSeqComponent> newCopy = new ArrayList<>(_compList);
            firePropertyChange(SEQ_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(SEQ_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addSeqComponent(int index, ISPSeqComponent seqComp) throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException {
        // Get the local component (throwing an SPNodeNotLocalException if not
        // local).
        MemSeqComponent node = (MemSeqComponent) seqComp;
        getProgramWriteLock();
        try {
            List<ISPSeqComponent> oldCopy = new ArrayList<>(_compList);
            node.attachTo(this);
            _compList.add(index, node);
            List<ISPSeqComponent> newCopy = new ArrayList<>(_compList);
            firePropertyChange(SEQ_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(SEQ_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void removeSeqComponent(ISPSeqComponent seqComp) {
        MemSeqComponent node = (MemSeqComponent) seqComp;
        getProgramWriteLock();
        try {
            int index = _compList.indexOf(node);
            if (index == -1) {
                LOG.warning("Component was not located and can't be removed.");
                return;
            }
            List<ISPSeqComponent> oldCopy = new ArrayList<>(_compList);
            node.detachFrom(this);
            _compList.remove(index);
            List<ISPSeqComponent> newCopy = new ArrayList<>(_compList);
            firePropertyChange(SEQ_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(SEQ_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    @Override public int getStepCount() {
        final Object obj     = getDataObject();
        final int multiplier = (obj instanceof ISPSeqObject) ? ((ISPSeqObject) obj).getStepCount() : 0;

        if (getType().broadType == SPComponentBroadType.OBSERVER) {
            // Leaf nodes produce datasets but have no children.  Probably want
            // to make this determination from a method in ISPSeqObject rather
            // than checking the broadType?
            return multiplier;
        } else {
            // multiplier * ((0/:getSeqComponents) { _ + _.getStepCount })
            int childStepCount = 0;
            for (ISPSeqComponent child : getSeqComponents()) {
                childStepCount += child.getStepCount();
            }
            return multiplier * childStepCount;
        }
    }

    public void accept(ISPProgramVisitor visitor) {
        visitor.visitSeqComponent(this);
    }

    public List<ISPNode> getChildren() {
        getProgramReadLock();
        try {
            final List<ISPNode> children = new ArrayList<>();
            if (getConflictFolder() != null) children.add(getConflictFolder());
            children.addAll(getSeqComponents());
            return children;
        } finally {
            returnProgramReadLock();
        }
    }

    private static final Class<?>[] VALID_CHILD_TYPES = {
        MemConflictFolder.class,
        MemSeqComponent.class,
    };

    protected void setTypedChildren(MemAbstractContainer.TypedChildren tc)
            throws SPNodeNotLocalException, SPTreeStateException {
        tc.verify(VALID_CHILD_TYPES);
        setConflictFolder(tc.getOnlyChild(MemConflictFolder.class));
        setSeqComponents(tc.getChildren(MemSeqComponent.class));
    }

    @Override public MemProgram getProgram() { return _program; }
}
