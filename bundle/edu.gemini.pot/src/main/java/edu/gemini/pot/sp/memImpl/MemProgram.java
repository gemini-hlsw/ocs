package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.shared.util.VersionVector;
import edu.gemini.spModel.core.SPProgramID;

import java.util.*;

/**
 * This class provides an in-memory, non-persistent implementation of the
 * ISPProgram remote interface.
 */
public final class MemProgram extends MemAbstractContainer implements ISPProgram {

    /**
     * Creates a new empty program with the given key and program id.
     */
    public static MemProgram create(SPNodeKey key, SPProgramID progId, UUID databaseId) {
        if (key == null) key = new SPNodeKey();
        return new MemProgram(new ProgramData(key, progId, databaseId, LifespanId.random()));
    }

    /**
     * Creates a copy of the given program with new keys and a new program id.
     * The copy is identical in structure but each node has unique keys and the
     * program itself has the given (presumably different) key and program id.
     */
    public static MemProgram copyWithNewKeys(ISPProgram that, SPProgramID newProgId, UUID databaseId, ISPFactory factory) {
        return new MemProgram(new ProgramData(new SPNodeKey(), newProgId, databaseId, LifespanId.random()), factory, that, false);
    }

    /**
     * Creates an exact copy of the given program using the same keys and
     * program id.
     */
    public static MemProgram copyWithSameKeys(ISPProgram that, UUID databaseId, ISPFactory factory) {
        return duplicate(that, databaseId, factory, that.getLifespanId());
    }

    public static MemProgram copyWithNewLifespanId(ISPProgram that, UUID databaseId, ISPFactory factory) {
        return duplicate(that, databaseId, factory, LifespanId.random());
    }

    public static MemProgram duplicate(ISPProgram that, UUID databaseId, ISPFactory factory, LifespanId lifespanId) {
        final ProgramData pd = new ProgramData(that.getNodeKey(), that.getProgramID(), databaseId, lifespanId);
        return new MemProgram(pd, factory, that, true);
    }

    private MemTemplateFolder templateFolder;

    // The list of observation components
    private final List<ISPObsComponent> _compList = new ArrayList<>();

    // The list of observations
    private final List<ISPObservation> _obsList = new ArrayList<>();

    // The list of observation groups
    private final List<ISPGroup> _groupList = new ArrayList<>();

    private MemProgram(ProgramData progData)  {
        super(progData, progData.getDocumentKey());
    }

    private MemProgram(ProgramData progData, ISPFactory fact, ISPProgram prog, boolean preserve) {
        super(progData, prog, preserve);

        // Copy the components.
        List<ISPObsComponent> compListCopy = prog.getObsComponents();
        ListIterator<ISPObsComponent> lit = compListCopy.listIterator();
        while (lit.hasNext()) {
            try {
                lit.set(fact.createObsComponentCopy(this, lit.next(), preserve));
            } catch (SPUnknownIDException ex) {
                throw new RuntimeException(ex);
            }
        }

        // Copy the observations.
        List<ISPObservation> obsListCopy = prog.getObservations();
        ListIterator<ISPObservation> lit2 = obsListCopy.listIterator();
        while (lit2.hasNext()) {
            try {
                lit2.set(fact.createObservationCopy(this, lit2.next(), preserve));
            } catch (SPException ex) {
                throw new RuntimeException(ex);
            }
        }

        // Copy the groups.
        List<ISPGroup> groupListCopy = prog.getGroups();
        ListIterator<ISPGroup> lit3 = groupListCopy.listIterator();
        while (lit3.hasNext()) {
            try {
                lit3.set(fact.createGroupCopy(this, lit3.next(), preserve));
            } catch (SPUnknownIDException ex) {
                throw new RuntimeException(ex);
            }
        }

        getProgramWriteLock();  // wouldn't strictly be required in this situation,
        // but updateChildren requires it
        try {
            final ISPConflictFolder cf = prog.getConflictFolder();
            if (cf != null) setConflictFolder(fact.createConflictFolderCopy(this, cf, preserve));

            final ISPTemplateFolder tf = prog.getTemplateFolder();
            if (tf != null) setTemplateFolder(fact.createTemplateFolderCopy(this, tf, preserve));

            // Update the observation components.
            updateChildren(_compList, compListCopy);
            updateChildren(_obsList, obsListCopy);
            updateChildren(_groupList, groupListCopy);
        } catch (SPException ex) {
            // This should never happen since we just created the observations
            // locally and they have never been added to any program yet.
            throw new RuntimeException(ex);
        } finally {
            returnProgramWriteLock();
        }
    }

    public scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> getVersions() {
        return getDocumentData().getVersions();
    }

    public void setVersions(scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> v) {
        getDocumentData().setVersions(v);
    }

    public VersionVector<LifespanId, Integer> getVersions(SPNodeKey key) {
        return getDocumentData().versionVector(key);
    }

    public void setVersions(SPNodeKey key, VersionVector<LifespanId, Integer> vv) {
        getDocumentData().setVersionVector(key, vv);
    }

    public long lastModified() {
        return getDocumentData().lastModified();
    }

    public ISPTemplateFolder getTemplateFolder() {
        getProgramReadLock();
        try {
            return templateFolder;
        } finally {
            returnProgramReadLock();
        }
    }

    public void setTemplateFolder(ISPTemplateFolder templateFolder) throws SPNodeNotLocalException, SPTreeStateException {
        if (templateFolder == this.templateFolder) return;
        if (templateFolder != null && templateFolder.getParent() == this) return;

        MemTemplateFolder node = (MemTemplateFolder) templateFolder;

        getProgramWriteLock();
        try {
            MemTemplateFolder oldValue = this.templateFolder;
            this.templateFolder = node;
            updateParentLinks(oldValue, node);
            firePropertyChange(TEMPLATE_FOLDER_PROP, oldValue, node);
            fireStructureChange(TEMPLATE_FOLDER_PROP, this, oldValue, node);
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
        getProgramWriteLock();
        try {
            int index = _compList.indexOf(node);
            if (index == -1) {
                System.out.println("Component was not located and can't be removed.");
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
        List<ISPObservation> result = getObservations();
        for (ISPGroup group : getGroups()) {
            result.addAll(group.getObservations());
        }
        return result;
    }

    public void setObservations(List<? extends ISPObservation> newObsList) throws SPNodeNotLocalException, SPTreeStateException {
        checkChildTypes(newObsList, ISPObservation.class);

        List<ISPObservation> newCopy = new ArrayList<>(newObsList);

        // Check for duplicate sequence ids in the new obs list.
        Set<Integer> taken = new HashSet<>(newCopy.size());
        for (ISPObservation aNewCopy : newCopy) {
            MemObservation obs = (MemObservation) aNewCopy;
            Integer obsNum = obs.getObservationNumber();
            if (taken.contains(obsNum)) {
                throw new SPTreeStateException("There are at least two observations with number: " + obsNum);
            }
            taken.add(obsNum);
        }
        getProgramWriteLock();
        try {
            List<ISPObservation> oldCopy = new ArrayList<>(_obsList);
            updateChildren(_obsList, newCopy);
            firePropertyChange(OBSERVATIONS_PROP, oldCopy, newCopy);
            fireStructureChange(OBSERVATIONS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    private void checkForDuplicate(MemObservation localObs) throws SPTreeStateException {
        // Check for duplicate observation id.
        int newObsNum = localObs.getObservationNumber();
        for (ISPObservation ispObservation : getAllObservations()) {
            MemObservation obs = (MemObservation) ispObservation;
            int obsNum = obs.getObservationNumber();
            if (newObsNum == obsNum) {
                throw new SPTreeStateException("There is an existing observation with number: " + obsNum);
            }
        }
    }

    public void addObservation(ISPObservation obs) throws SPNodeNotLocalException, SPTreeStateException {
        // Get the local observation (throwing an SPNodeNotLocalException if not
        // local).
        MemObservation node = (MemObservation) obs;
        checkForDuplicate(node);
        getProgramWriteLock();
        try {
            List<ISPObservation> oldCopy = new ArrayList<>(_obsList);
            node.attachTo(this);
            _obsList.add(node);
            List<ISPObservation> newCopy = new ArrayList<>(_obsList);
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
        checkForDuplicate(node);
        getProgramWriteLock();
        try {
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

    public List<ISPGroup> getGroups() {
        getProgramReadLock();
        try {
            return new ArrayList<>(_groupList);
        } finally {
            returnProgramReadLock();
        }
    }

    public void setGroups(List<? extends ISPGroup> newGroupList) throws SPNodeNotLocalException, SPTreeStateException {
        checkChildTypes(newGroupList, ISPGroup.class);

        List<ISPGroup> newCopy = new ArrayList<>(newGroupList);
        getProgramWriteLock();
        try {
            List<ISPGroup> oldCopy = new ArrayList<>(_groupList);
            updateChildren(_groupList, newCopy);
            firePropertyChange(OBS_GROUP_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_GROUP_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addGroup(ISPGroup group) throws SPNodeNotLocalException, SPTreeStateException {
        // Get the local group (throwing an SPNodeNotLocalException if not local).
        MemGroup node = (MemGroup) group;
        getProgramWriteLock();
        try {
            List<ISPGroup> oldCopy = new ArrayList<>(_groupList);
            node.attachTo(this);
            _groupList.add(node);
            List<ISPGroup> newCopy = new ArrayList<>(_groupList);
            firePropertyChange(OBS_GROUP_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_GROUP_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addGroup(int pos, ISPGroup group) throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException {
        // Get the local group (throwing an SPNodeNotLocalException if not
        // local).
        MemGroup node = (MemGroup) group;
        getProgramWriteLock();
        try {
            List<ISPGroup> oldCopy = new ArrayList<>(_groupList);
            node.attachTo(this);
            _groupList.add(pos, node);
            List<ISPGroup> newCopy = new ArrayList<>(_groupList);
            firePropertyChange(OBS_GROUP_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_GROUP_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void removeGroup(ISPGroup group) {
        MemGroup node = (MemGroup) group;
        getProgramWriteLock();
        try {
            int index = _groupList.indexOf(node);
            if (index == -1) {
                //System.out.println("Group not located and can't be removed.");
                return;
            }
            List<ISPGroup> oldCopy = new ArrayList<>(_groupList);
            node.detachFrom(this);
            _groupList.remove(index);
            List<ISPGroup> newCopy = new ArrayList<>(_groupList);
            firePropertyChange(OBS_GROUP_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_GROUP_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void accept(ISPProgramVisitor visitor) {
        visitor.visitProgram(this);
    }

    public List<ISPNode> getChildren() {
        getProgramReadLock();
        try {
            List<ISPNode> res = new ArrayList<>(getObsComponents());
            if (getConflictFolder() != null) res.add(getConflictFolder());
            if (templateFolder != null) res.add(templateFolder);
            res.addAll(getObservations());
            res.addAll(getGroups());
            return res;
        } finally {
            returnProgramReadLock();
        }
    }

    private static final Class<?>[] VALID_CHILD_TYPES = {
        MemConflictFolder.class,
        MemObsComponent.class,
        MemTemplateFolder.class,
        MemGroup.class,
        MemObservation.class,
    };

    protected void setTypedChildren(MemAbstractContainer.TypedChildren tc)
            throws SPNodeNotLocalException, SPTreeStateException {
        tc.verify(VALID_CHILD_TYPES);
        setConflictFolder(tc.getOnlyChild(MemConflictFolder.class));
        setObsComponents(tc.getChildren(MemObsComponent.class));
        setTemplateFolder(tc.getOnlyChild(MemTemplateFolder.class));
        setObservations(tc.getChildren(MemObservation.class));
        setGroups(tc.getChildren(MemGroup.class));
    }

    @Override public MemProgram getProgram() { return this; }

    // Gets a map of observation key to observation number for the given
    // program.
    private static Map<SPNodeKey, Integer> mapNumbers(ISPProgram prog) {
        final Map<SPNodeKey, Integer> res = new HashMap<>();
        for (ISPObservation obs : new ObservationIterator(prog)) {
            res.put(obs.getNodeKey(), obs.getObservationNumber());
        }
        return res;
    }

    public void renumberObservationsToMatch(ISPProgram that) {
        // Renumber the existing observations in target to match those in the
        // input program.  Keep up with any "new" observations that don't
        // appear in the input program.
        final Map<SPNodeKey, Integer> inMap = mapNumbers(that);
        final List<MemObservation> newObsList = new ArrayList<>();
        for (ISPObservation obs : new ObservationIterator(this)) {
            MemObservation memObs = (MemObservation) obs;
            Integer inObsNumber = inMap.get(obs.getNodeKey());
            if (inObsNumber == null) {
                newObsList.add(memObs);
            } else {
                memObs.setObservationNumber(inObsNumber);
            }
        }

        // Get the biggest observation number that was known in the incoming
        // program.  This + 1 is where the numbers for the "new" observations
        // in this program will start
        Integer max = (inMap.size() == 0) ? 0 : Collections.max(inMap.values());

        // Sort the "new" observations by their old observation number.  That
        // way when we renumber them at least they stay in the same order.
        Collections.sort(newObsList, new Comparator<MemObservation>() {
            @Override public int compare(MemObservation o1, MemObservation o2) {
                return o1.getObservationNumber() - o2.getObservationNumber();
            }
        });

        // Renumber the "new" observations starting with max + 1
        for (MemObservation obs : newObsList) {
            final int cur = obs.getObservationNumber();
            if (cur >= max+1) {
                max = cur; // no need to renumber it, already a new number.
            } else {
                obs.setObservationNumber(++max);
            }
        }

        // Remember where to start counting again if we make any new
        // observations.
        ((ProgramData) getDocumentData()).updateNextObsNumber(max);
    }
}

