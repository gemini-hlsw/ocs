package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MemTemplateGroup extends MemAbstractContainer implements ISPTemplateGroup {
    private static final Logger LOG = Logger.getLogger(MemTemplateGroup.class.getName());

    private final MemProgram program;
    private final List<MemObservation> obsList = new ArrayList<>();
    private final List<MemTemplateParameters> paramsList = new ArrayList<>();
    private final List<MemObsComponent> obsCompList = new ArrayList<>();

    public MemTemplateGroup(MemProgram prog, SPNodeKey key)  {
        super(prog.getDocumentData(), key);
        this.program = prog;
    }

    public MemTemplateGroup(MemProgram prog, ISPFactory fact, ISPTemplateGroup that, boolean preserveKeys)  {
        super(prog.getDocumentData(), that, preserveKeys);
        this.program = prog;

        // Make a copy of the obs list
        List<ISPObservation> thatObsList = that.getObservations();
        List<ISPObservation> newObsList  = new ArrayList<>(thatObsList.size());
        for (ISPObservation to : thatObsList) {
            try {
                newObsList.add(fact.createObservationCopy(prog, to, preserveKeys));
            } catch (SPException ex) {
                LOG.log(Level.SEVERE, "Bug in MemObservation");
                throw new RuntimeException("Bug in MemObservation");
            }
        }

        // Make a copy of the params list
        List<ISPTemplateParameters> thatParamsList = that.getTemplateParameters();
        List<ISPTemplateParameters> newParamsList  = new ArrayList<>(thatParamsList.size());
        for (ISPTemplateParameters tp : thatParamsList) {
            try {
                newParamsList.add(fact.createTemplateParametersCopy(prog, tp, preserveKeys));
            } catch (SPUnknownIDException ex) {
                LOG.log(Level.SEVERE, "Bug in MemTemplateParameters");
                throw new RuntimeException("Bug in MemTemplateParameters");
            }
        }

        // Make a copy of the obsComp list
        List<ISPObsComponent> thatObsCompList = that.getObsComponents();
        List<ISPObsComponent> newObsCompList  = new ArrayList<>(thatObsCompList.size());
        for (ISPObsComponent tp : thatObsCompList) {
            try {
                newObsCompList.add(fact.createObsComponentCopy(prog, tp, preserveKeys));
            } catch (SPUnknownIDException ex) {
                LOG.log(Level.SEVERE, "Bug in MemObsComponent");
                throw new RuntimeException("Bug in MemObsComponent");
            }
        }

        getProgramWriteLock();
        try {
            ISPConflictFolder cf = that.getConflictFolder();
            if (cf != null) setConflictFolder(fact.createConflictFolderCopy(prog, cf, preserveKeys));

            updateChildren(obsList, newObsList);
            updateChildren(paramsList, newParamsList);
            updateChildren(obsCompList, newObsCompList);
        } catch (SPException ex) {
            LOG.log(Level.SEVERE, "Bug copying MemTemplateGroup");
            throw new RuntimeException("Bug copying MemTemplateGroup");
        } finally {
            returnProgramWriteLock();
        }

    }

    private static final Class<?>[] VALID_CHILD_TYPES = {
        MemConflictFolder.class,
        MemObservation.class,
        MemObsComponent.class,
        MemTemplateParameters.class,
    };

    @Override
    protected void setTypedChildren(TypedChildren tc) throws SPNodeNotLocalException, SPTreeStateException {
        getProgramWriteLock();
        try {
            tc.verify(VALID_CHILD_TYPES);
            setConflictFolder(tc.getOnlyChild(MemConflictFolder.class));
            setObservations(tc.getChildren(MemObservation.class));
            setTemplateParameters(tc.getChildren(MemTemplateParameters.class));
            setObsComponents(tc.getChildren(MemObsComponent.class));
        } finally {
            returnProgramWriteLock();
        }
    }

    @Override
    public List<ISPNode> getChildren() {
        getProgramReadLock();
        try {
            final List<ISPNode> ret = new ArrayList<>();
            if (getConflictFolder() != null) ret.add(getConflictFolder());
            ret.addAll(getObsComponents());
            ret.addAll(getObservations());
            ret.addAll(getTemplateParameters());
            return ret;
        } finally {
            returnProgramReadLock();
        }
    }

    public List<ISPObservation> getObservations() {
        getProgramReadLock();
        try {
            return new ArrayList<>(obsList);
        } finally {
            returnProgramReadLock();
        }
    }

    public List<ISPObservation> getAllObservations() {
        return getObservations();
    }

    public void setObservations(List<? extends ISPObservation> newObsList) throws SPNodeNotLocalException, SPTreeStateException {
        final List<ISPObservation> newCopy = new ArrayList<>(newObsList);
        getProgramWriteLock();
        try {
            SPAssert.setsNoDuplicateObs(this, newCopy);
            final List<ISPObservation> oldCopy = new ArrayList<>(obsList);
            updateChildren(obsList, newCopy);
            firePropertyChange(TEMPLATE_OBSERVATIONS_PROP, oldCopy, newCopy);
            fireStructureChange(TEMPLATE_OBSERVATIONS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addObservation(ISPObservation obs) throws SPNodeNotLocalException, SPTreeStateException {
        addObservation(-1, obs);
    }

    public void addObservation(int index, ISPObservation obs) throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException {
        // Get the local component (throwing an SPNodeNotLocalException if not
        // local).
        final MemObservation node = (MemObservation) obs;
        getProgramWriteLock();
        try {
            SPAssert.addsNoDuplicateObs(this, obs);
            List<ISPObservation> oldCopy = new ArrayList<>(obsList);
            node.attachTo(this);
            if (index >= 0) obsList.add(index, node); else obsList.add(node);
            List<ISPObservation> newCopy = new ArrayList<>(obsList);
            firePropertyChange(TEMPLATE_OBSERVATIONS_PROP, oldCopy, newCopy);
            fireStructureChange(TEMPLATE_OBSERVATIONS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void removeObservation(ISPObservation obs) {
        MemObservation node = (MemObservation) obs;
        getProgramWriteLock();
        try {
            int index = obsList.indexOf(node);
            if (index == -1) {
                LOG.warning("Component was not located and can't be removed.");
                return;
            }
            List<ISPObservation> oldCopy = new ArrayList<>(obsList);
            node.detachFrom(this);
            obsList.remove(index);
            List<ISPObservation> newCopy = new ArrayList<>(obsList);
            firePropertyChange(TEMPLATE_OBSERVATIONS_PROP, oldCopy, newCopy);
            fireStructureChange(TEMPLATE_OBSERVATIONS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public List<ISPTemplateParameters> getTemplateParameters() {
        getProgramReadLock();
        try {
            return new ArrayList<>(paramsList);
        } finally {
            returnProgramReadLock();
        }
    }

    public void setTemplateParameters(List<? extends ISPTemplateParameters> newParamsList) throws SPNodeNotLocalException, SPTreeStateException {
        List<ISPTemplateParameters> newCopy = new ArrayList<>(newParamsList);
        getProgramWriteLock();
        try {
            List<ISPTemplateParameters> oldCopy = new ArrayList<>(paramsList);
            updateChildren(paramsList, newCopy);
            firePropertyChange(TEMPLATE_PARAMETERS_PROP, oldCopy, newCopy);
            fireStructureChange(TEMPLATE_PARAMETERS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addTemplateParameters(ISPTemplateParameters params) throws SPNodeNotLocalException, SPTreeStateException {
        addTemplateParameters(-1, params);
    }

    public void addTemplateParameters(int index, ISPTemplateParameters params) throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException {
        // Get the local component (throwing an SPNodeNotLocalException if not
        // local).
        MemTemplateParameters node = (MemTemplateParameters) params;
        getProgramWriteLock();
        try {
            List<ISPTemplateParameters> oldCopy = new ArrayList<>(paramsList);
            node.attachTo(this);
            if (index >= 0) paramsList.add(index, node); else paramsList.add(node);
            List<ISPTemplateParameters> newCopy = new ArrayList<>(paramsList);
            firePropertyChange(TEMPLATE_PARAMETERS_PROP, oldCopy, newCopy);
            fireStructureChange(TEMPLATE_PARAMETERS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void removeTemplateParameters(ISPTemplateParameters params) {
        MemTemplateParameters node = (MemTemplateParameters) params;
        getProgramWriteLock();
        try {
            int index = paramsList.indexOf(node);
            if (index == -1) {
                LOG.warning("Component was not located and can't be removed.");
                return;
            }
            List<ISPTemplateParameters> oldCopy = new ArrayList<>(paramsList);
            node.detachFrom(this);
            paramsList.remove(index);
            List<ISPTemplateParameters> newCopy = new ArrayList<>(paramsList);
            firePropertyChange(TEMPLATE_PARAMETERS_PROP, oldCopy, newCopy);
            fireStructureChange(TEMPLATE_PARAMETERS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public List<ISPObsComponent> getObsComponents() {
        getProgramReadLock();
        try {
            return new ArrayList<>(obsCompList);
        } finally {
            returnProgramReadLock();
        }
    }

    public void setObsComponents(List<? extends ISPObsComponent> newObsCompList) throws SPNodeNotLocalException, SPTreeStateException {
        List<ISPObsComponent> newCopy = new ArrayList<>(newObsCompList);
        getProgramWriteLock();
        try {
            List<ISPObsComponent> oldCopy = new ArrayList<>(obsCompList);
            updateChildren(obsCompList, newCopy);
            firePropertyChange(OBS_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    public void addObsComponent(ISPObsComponent obsComp) throws SPNodeNotLocalException, SPTreeStateException {
        addObsComponent(-1, obsComp);
    }

    public void addObsComponent(int index, ISPObsComponent obsComp) throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException {
        // Get the local component (throwing an SPNodeNotLocalException if not
        // local).
        MemObsComponent node = (MemObsComponent) obsComp;
        getProgramWriteLock();
        try {
            List<ISPObsComponent> oldCopy = new ArrayList<>(obsCompList);
            node.attachTo(this);
            if (index >= 0) obsCompList.add(index, node); else obsCompList.add(node);
            List<ISPObsComponent> newCopy = new ArrayList<>(obsCompList);
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
            int index = obsCompList.indexOf(node);
            if (index == -1) {
                LOG.warning("Component was not located and can't be removed.");
                return;
            }
            List<ISPObsComponent> oldCopy = new ArrayList<>(obsCompList);
            node.detachFrom(this);
            obsCompList.remove(index);
            List<ISPObsComponent> newCopy = new ArrayList<>(obsCompList);
            firePropertyChange(OBS_COMPONENTS_PROP, oldCopy, newCopy);
            fireStructureChange(OBS_COMPONENTS_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    @Override
    public void accept(ISPProgramVisitor visitor)  {
        visitor.visitTemplateGroup(this);
    }

    @Override public MemProgram getProgram() { return program; }
}

