package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.logging.Logger;

/**
 * This class provides an in-memory, non-persistent implementation of the
 * ISPGroup remote interface.
 */
public final class MemGroup extends MemAbstractContainer implements ISPGroup {
    private static final Logger LOG = Logger.getLogger(MemGroup.class.getName());
    private final MemProgram _program;

    // The list of observation components
    private final List<ISPObsComponent> _compList = new ArrayList<>();

    // The list of observations
    private final List<ISPObservation> _obsList = new ArrayList<>();

    public MemGroup(MemProgram prog, SPNodeKey key)  {
        super(prog.getDocumentData(), key);
        _program = prog;
    }

    public MemGroup(MemProgram prog, ISPFactory fact, ISPGroup group,
                      boolean preserveKeys)  {
        super(prog.getDocumentData(), group, preserveKeys);
        _program = prog;

        // Copy the components.
        List<ISPObsComponent> compListCopy = group.getObsComponents();
        ListIterator<ISPObsComponent> lit = compListCopy.listIterator();
        while (lit.hasNext()) {
            try {
                lit.set(fact.createObsComponentCopy(prog, lit.next(), preserveKeys));
            } catch (SPUnknownIDException ex) {
                // This should never happen since the factory handed in this
                // ProgramData
                throw new RuntimeException(ex);
            }
        }

        // Copy the observations.
        List<ISPObservation> obsListCopy = group.getObservations();
        ListIterator<ISPObservation> lit2 = obsListCopy.listIterator();
        while (lit2.hasNext()) {
            try {
                lit2.set(fact.createObservationCopy(prog, lit2.next(), preserveKeys));
            } catch (SPException ex) {
                // This should never happen since the factory just created this
                // program!
                throw new RuntimeException(ex);
            }
        }

        getProgramWriteLock();  // wouldn't strictly be required in this situation,
        // but updateChildren requires it
        try {
            ISPConflictFolder cf = group.getConflictFolder();
            if (cf != null) setConflictFolder(fact.createConflictFolderCopy(prog, cf, preserveKeys));

            // Update the observation components.
            updateChildren(_compList, compListCopy);
            updateChildren(_obsList, obsListCopy);
        } catch (SPException ex) {
            // This should never happen since we just created the observations
            // locally and they have never been added to any program yet.
            throw new RuntimeException(ex);
        } finally {
            returnProgramWriteLock();
        }
    }

    public List<ISPObsComponent> getObsComponents() {
        // Make a list copy so the client gets a stable version
        getProgramReadLock();
        try {
            return new ArrayList<>(_compList);
        } finally {
            returnProgramReadLock();
        }
    }

    public void setObsComponents(List<? extends ISPObsComponent> newCompList) throws SPNodeNotLocalException, SPTreeStateException {
        checkChildTypes(newCompList, ISPObsComponent.class);

        // Convert the stubs in the newCompList into local object references.
        // This will throw SPNodeNotLocalException if any are not in fact local.
        List<ISPObsComponent> newCopy = new ArrayList<>(newCompList);
        getProgramWriteLock();
        try {
            List<ISPObsComponent> oldCopy = new ArrayList<>(_compList);
            updateChildren(_compList, newCopy);
            firePropertyChange(OBS_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addObsComponent(ISPObsComponent obsComp) throws SPNodeNotLocalException, SPTreeStateException {
        // Get the local component (throwing an SPNodeNotLocalException if not
        // local).
        MemObsComponent node = (MemObsComponent) obsComp;
        getProgramWriteLock();
        try {
            List<ISPObsComponent> oldCopy = new ArrayList<>(_compList);
            node.attachTo(this);
            _compList.add(node);
            List<ISPObsComponent> newCopy = new ArrayList<>(_compList);
            firePropertyChange(OBS_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addObsComponent(int index, ISPObsComponent obsComp) throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException {
        // Get the local component (throwing an SPNodeNotLocalException if not
        // local).
        MemObsComponent node = (MemObsComponent) obsComp;
        getProgramWriteLock();
        try {
            List<ISPObsComponent> oldCopy = new ArrayList<>(_compList);
            node.attachTo(this);
            _compList.add(index, node);
            List<ISPObsComponent> newCopy = new ArrayList<>(_compList);
            firePropertyChange(OBS_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void removeObsComponent(ISPObsComponent obsComp) {
        MemObsComponent node = (MemObsComponent) obsComp;
        if (node == null) {
            return; // wasn't local, isn't in this program
        }
        getProgramWriteLock();
        try {
            int index = _compList.indexOf(node);
            if (index == -1) {
                LOG.warning("Component was not located and can't be removed.");
                return;
            }
            List<ISPObsComponent> oldCopy = new ArrayList<>(_compList);
            node.detachFrom(this);
            _compList.remove(index);
            List<ISPObsComponent> newCopy = new ArrayList<>(_compList);
            firePropertyChange(OBS_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public List<ISPObservation> getObservations() {
        getProgramReadLock();
        try {
            return new ArrayList<>(_obsList);
        } finally {
            returnProgramReadLock();
        }
    }

    public List<ISPObservation> getAllObservations()  {
        return getObservations();
    }

    public void setObservations(List<? extends ISPObservation> newObsList) throws SPNodeNotLocalException, SPTreeStateException {
        checkChildTypes(newObsList, ISPObservation.class);

        final List<ISPObservation> newCopy = new ArrayList<>(newObsList);

        getProgramWriteLock();
        try {
            SPAssert.setsNoDuplicateObs(this, newCopy);
            final List<ISPObservation> oldCopy = new ArrayList<>(_obsList);
            updateChildren(_obsList, newCopy);
            firePropertyChange(OBSERVATIONS_PROP, oldCopy, newCopy);
            fireStructureChange(OBSERVATIONS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addObservation(ISPObservation obs) throws SPNodeNotLocalException, SPTreeStateException {
        // Get the local observation (throwing an SPNodeNotLocalException if not
        // local).
        final MemObservation node = (MemObservation) obs;
        getProgramWriteLock();
        try {
            SPAssert.addsNoDuplicateObs(this, obs);
            final List<ISPObservation> oldCopy = new ArrayList<>(_obsList);
            node.attachTo(this);
            _obsList.add(node);
            final List<ISPObservation> newCopy = new ArrayList<>(_obsList);
            firePropertyChange(OBSERVATIONS_PROP, oldCopy, newCopy);
            fireStructureChange(OBSERVATIONS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addObservation(int pos, ISPObservation obs) throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException {
        // Get the local observation (throwing an SPNodeNotLocalException if not
        // local).
        MemObservation node = (MemObservation) obs;
        getProgramWriteLock();
        try {
            SPAssert.addsNoDuplicateObs(this, obs);
            List<ISPObservation> oldCopy = new ArrayList<>(_obsList);
            node.attachTo(this);
            _obsList.add(pos, node);
            List<ISPObservation> newCopy = new ArrayList<>(_obsList);
            firePropertyChange(OBSERVATIONS_PROP, oldCopy, newCopy);
            fireStructureChange(OBSERVATIONS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void removeObservation(ISPObservation obs) {
        MemObservation node = (MemObservation) obs;
        getProgramWriteLock();
        try {
            int index = _obsList.indexOf(node);
            if (index == -1) {
                //System.out.println("Observation not located and can't be removed.");
                return;
            }
            List<ISPObservation> oldCopy = new ArrayList<>(_obsList);
            node.detachFrom(this);
            _obsList.remove(index);
            List<ISPObservation> newCopy = new ArrayList<>(_obsList);
            firePropertyChange(OBSERVATIONS_PROP, oldCopy, newCopy);
            fireStructureChange(OBSERVATIONS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void accept(ISPProgramVisitor visitor) {
        visitor.visitGroup(this);
    }

    public List<ISPNode> getChildren() {
        getProgramReadLock();
        try {
            List<ISPNode> res = new ArrayList<>();
            if (getConflictFolder() != null) res.add(getConflictFolder());
            res.addAll(getObsComponents());
            res.addAll(getObservations());
            return res;
        } finally {
            returnProgramReadLock();
        }
    }

    private static final Class<?>[] VALID_CHILD_TYPES = {
        MemConflictFolder.class,
        MemObsComponent.class,
        MemObservation.class,
    };

    protected void setTypedChildren(MemAbstractContainer.TypedChildren tc)
            throws SPNodeNotLocalException, SPTreeStateException {
        tc.verify(VALID_CHILD_TYPES);
        setConflictFolder(tc.getOnlyChild(MemConflictFolder.class));
        setObsComponents(tc.getChildren(MemObsComponent.class));
        setObservations(tc.getChildren(MemObservation.class));
    }

    @Override public MemProgram getProgram() { return _program; }
}
