// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: MemFactory.java 47005 2012-07-26 22:35:47Z swalker $
//

package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.core.SPProgramID;


import java.util.*;

//
// TODO: This class has gotten out of control.  Creation of objects needs to
// TODO: be rethought
//

/**
 * This class is a concrete extension of the <code>SPAbstractFactory</code>
 * and is used to create the "Mem" family of program nodes.
 */
public class MemFactory extends SPAbstractFactory {

    private List<SPComponentType> _creatableObsComponents;
    private List<SPComponentType> _creatableSeqComponents;

    private final UUID uuid;

    public MemFactory(UUID uuid)  {
        if (uuid == null) throw new IllegalArgumentException("uuid is null");
        this.uuid = uuid;
    }

    /**
     * Returns the type of the factory.
     *
     * @return a String indicating the type of nodes created by the factory
     */
    public String getType() {
        return "Memory Program";
    }

    protected ISPProgram doCreateProgram(SPNodeKey key, SPProgramID progID) {
        return MemProgram.create(key, progID, uuid);
    }


    public ISPProgram copyWithNewKeys(ISPProgram program, SPProgramID newProgID) {
        final MemProgram copy = MemProgram.copyWithNewKeys(program, newProgID, uuid, this);
        return copy;
    }

    public ISPProgram copyWithSameKeys(ISPProgram in) {
        final MemProgram dup = MemProgram.copyWithSameKeys(in, uuid, this);
        dup.setVersions(in.getVersions());
        return dup;
    }

    public ISPProgram copyWithNewLifespanId(ISPProgram in) {
        final MemProgram dup = MemProgram.copyWithNewLifespanId(in, uuid, this);
        dup.setVersions(in.getVersions());
        return dup;
    }

    protected ISPNightlyRecord doCreateNightlyPlan(SPNodeKey key, SPProgramID planID) {
        return MemNightlyRecord.create(key, planID, uuid);
    }

    /**
     * Creates a deep copy of the given <code>nightly record</code>
     */
    public ISPNightlyRecord renameNightlyRecord(ISPNightlyRecord record, SPNodeKey newKey, SPProgramID recordId) {
        MemNightlyRecord spPlan = MemNightlyRecord.rename(record, newKey, recordId, uuid);
        if (_nightlyPlanInit != null) _nightlyPlanInit.updateNode(spPlan);
        return spPlan;
    }

    @Override
    public ISPConflictFolder createConflictFolder(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        MemConflictFolder f = new MemConflictFolder((MemProgram) prog, key);
        if (_conflictFolderInit != null) _conflictFolderInit.initNode(this, f);
        return f;
    }

    @Override
    public ISPConflictFolder createConflictFolderCopy(ISPProgram prog, ISPConflictFolder that, boolean preserveKeys) throws SPUnknownIDException {
        MemConflictFolder f = new MemConflictFolder((MemProgram) prog, this, that, preserveKeys);
        if (_conflictFolderInit != null) _conflictFolderInit.initNode(this, f);
        return f;
    }


    public ISPTemplateFolder createTemplateFolder(ISPProgram prog, SPNodeKey key)
        throws SPUnknownIDException {
        MemTemplateFolder f = new MemTemplateFolder((MemProgram) prog, key);
        if (_templateFolderInit != null) _templateFolderInit.initNode(this, f);
        return f;
    }

    public ISPTemplateFolder createTemplateFolderCopy(ISPProgram prog, ISPTemplateFolder that, boolean preserveKeys)
        throws SPUnknownIDException {
        MemTemplateFolder folder = new MemTemplateFolder((MemProgram) prog, this, that, preserveKeys);
        if (_templateFolderInit != null) _templateFolderInit.updateNode(folder);
        return folder;
    }

    public ISPTemplateGroup createTemplateGroup(ISPProgram prog, SPNodeKey key)
        throws SPUnknownIDException {
        MemTemplateGroup tg = new MemTemplateGroup((MemProgram) prog, key);
        if (_templateGroupInit != null) _templateGroupInit.initNode(this, tg);
        return tg;
    }

    public ISPTemplateGroup createTemplateGroupCopy(ISPProgram prog, ISPTemplateGroup that, boolean preserveKeys)
        throws SPUnknownIDException {

        MemTemplateGroup tg = new MemTemplateGroup((MemProgram) prog, this, that, preserveKeys);
        if (_templateGroupInit != null) _templateGroupInit.updateNode(tg);
        return tg;
    }

    public ISPTemplateParameters createTemplateParameters(ISPProgram prog, SPNodeKey key)
            throws SPUnknownIDException {
        MemTemplateParameters tp = new MemTemplateParameters((MemProgram) prog, key);
        if (_templateParametersInit != null) _templateParametersInit.initNode(this, tp);
        return tp;
    }

    public ISPTemplateParameters createTemplateParametersCopy(ISPProgram prog, ISPTemplateParameters that, boolean preserveKeys)
            throws SPUnknownIDException {
        MemTemplateParameters tp = new MemTemplateParameters((MemProgram) prog, that, preserveKeys);
        if (_templateParametersInit != null) _templateParametersInit.updateNode(tp);
        return tp;
    }

    /**
     * Creates an <code>ISPObservation</code>.
     */
    @Override protected ISPObservation doCreateObservation(ISPProgram prog, int index, SPNodeKey key) throws SPException {
        final MemProgram  mp = (MemProgram) prog;
        final ProgramData pd = (ProgramData) mp.getDocumentData();
        if (index < 0) {
            index = pd.incrAndGetMaxObsNumber();
        } else {
            pd.ensureMaxEqualToOrGreaterThan(index);
        }
        return new MemObservation(mp, index, key);
    }

    /**
     * Creates a deep copy of the given <code>observation</code>
     */
    public ISPObservation createObservationCopy(ISPProgram prog,
                                                ISPObservation observation,
                                                boolean preserveKeys)
            throws SPUnknownIDException {
        MemProgram mprog = (MemProgram) prog;
        ProgramData progData = (ProgramData) mprog.getDocumentData();
        int index;
        if (preserveKeys) {
            // Updated to avoid renumbering observations on fetch/store.
            //
            index = observation.getObservationNumber();
            progData.ensureMaxEqualToOrGreaterThan(index);
        } else {
            index = progData.incrAndGetMaxObsNumber();
        }
        MemObservation obs = new MemObservation(mprog, index, this, observation,
                                                preserveKeys);
        if (_obsInit != null) _obsInit.updateNode(obs);
        return obs;
    }

    /**
     * Creates an <code>ISPGroup</code>.
     */
    public ISPGroup doCreateGroup(ISPProgram prog, SPNodeKey key)
            throws SPUnknownIDException {
        return new MemGroup((MemProgram) prog, key);
    }

    /**
     * Creates a deep copy of the given <code>group</code>
     */
    public ISPGroup createGroupCopy(ISPProgram prog,
                                    ISPGroup ispGroup,
                                    boolean preserveKeys)
            throws SPUnknownIDException {
        MemGroup group = new MemGroup((MemProgram) prog, this, ispGroup, preserveKeys);
        if (_groupInit != null) _groupInit.updateNode(group);
        return group;
    }


    /**
     * Creates an <code>ISPObsComponent</code>.
     */
    @Override
    protected ISPObsComponent doCreateObsComponent(ISPProgram prog,
                                                SPComponentType type,
                                                SPNodeKey key)
            throws SPUnknownIDException {
        return new MemObsComponent((MemProgram) prog, type, key);
    }

    /**
     * Creates a deep copy of the given <code>component</code>
     */
    public ISPObsComponent createObsComponentCopy(ISPProgram prog,
                                                  ISPObsComponent component,
                                                  boolean preserveKeys)
            throws SPUnknownIDException {
        MemObsComponent obsComp = new MemObsComponent((MemProgram) prog, component, preserveKeys);
        if (_obsCompInitMap != null) {
            ISPNodeInitializer init = _obsCompInitMap.get(obsComp.getType());
            if (init != null) init.updateNode(obsComp);
        }
        return obsComp;
    }

    public ISPObsQaLog doCreateObsQaLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        return new MemObsQaLog((MemProgram) prog, key);
    }

    public ISPObsQaLog createObsQaLogCopy(ISPProgram prog, ISPObsQaLog log, boolean preserveKeys) throws SPUnknownIDException {
        final MemObsQaLog mol = new MemObsQaLog((MemProgram) prog, log, preserveKeys);
        if (_obsQaLogInit != null) _obsQaLogInit.updateNode(mol);
        return mol;
    }

    public ISPObsExecLog doCreateObsExecLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        return new MemObsExecLog((MemProgram) prog, key);
    }

    public ISPObsExecLog createObsExecLogCopy(ISPProgram prog, ISPObsExecLog log, boolean preserveKeys) throws SPUnknownIDException {
        final MemObsExecLog mol = new MemObsExecLog((MemProgram) prog, log, preserveKeys);
        if (_obsExecLogInit != null) _obsExecLogInit.updateNode(mol);
        return mol;
    }

    /**
     * Creates an <code>ISPSeqComponent</code>.
     */
    public ISPSeqComponent doCreateSeqComponent(ISPProgram prog,
                                                SPComponentType type,
                                                SPNodeKey key)
            throws SPUnknownIDException {
        return new MemSeqComponent((MemProgram) prog, type, key);
    }

    /**
     * Creates a deep copy of the given <code>component</code>
     */
    public ISPSeqComponent createSeqComponentCopy(ISPProgram prog,
                                                  ISPSeqComponent component,
                                                  boolean preserveKeys)
            throws SPUnknownIDException {
        MemSeqComponent seqComp = new MemSeqComponent((MemProgram) prog, this, component, preserveKeys);
        if (_sequenceCompInitMap != null) {
            ISPNodeInitializer init = _sequenceCompInitMap.get(seqComp.getType());
            if (init != null) init.updateNode(seqComp);
        }
        return seqComp;
    }

    /**
     * Returns a List of <code>{@link SPComponentType}</code> objects, one
     * for each type of obs component that can be created by the factory
     * of this database.
     * In order to discover the types that can be
     * created by the factory, a client can use this method.
     */
    public List<SPComponentType> getCreatableObsComponents()  {
        if (_creatableObsComponents == null) return new ArrayList<SPComponentType>();
        return new ArrayList<SPComponentType>(_creatableObsComponents);
    }

    /**
     * Returns a List of <code>{@link SPComponentType}</code> objects, one
     * for each type of seq component that can be created by this factory.
     * In order to discover the types that can be
     * created by the factory, a client can use this method.
     */
    public List<SPComponentType> getCreatableSeqComponents()  {
        if (_creatableSeqComponents == null) return new ArrayList<SPComponentType>();
        return new ArrayList<SPComponentType>(_creatableSeqComponents);
    }

    /**
     * Public method to allow the DatabaseManager to set the list
     * of obs component createables.  Used for internal implementation only.
     */
    public void setCreatableObsComponents(List<SPComponentType> ocl) {
        _creatableObsComponents = ocl;
    }

    /**
     * Package only method to allow the DatabaseManager to set the list
     * of seq component createables.  Used for internal implmentation only.
     */
    public void setCreatableSeqComponents(List<SPComponentType> scl) {
        _creatableSeqComponents = scl;
    }
}

