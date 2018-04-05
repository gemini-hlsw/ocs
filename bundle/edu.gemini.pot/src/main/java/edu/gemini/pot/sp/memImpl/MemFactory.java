// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: MemFactory.java 47005 2012-07-26 22:35:47Z swalker $
//

package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.init.NodeInitializers;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;

import java.util.*;

//
// TODO: This class has gotten out of control.  Creation of objects needs to
// TODO: be rethought
//

/**
 * This class is a concrete extension of the <code>SPAbstractFactory</code>
 * and is used to create the "Mem" family of program nodes.
 */
public final class MemFactory extends SPAbstractFactory {

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
        NodeInitializers.instance.record.updateNode(spPlan);
        return spPlan;
    }

    @Override
    public ISPConflictFolder createConflictFolder(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        MemConflictFolder f = new MemConflictFolder((MemProgram) prog, key);
        NodeInitializers.instance.conflict.initNode(this, f);
        return f;
    }

    @Override
    public ISPConflictFolder createConflictFolderCopy(ISPProgram prog, ISPConflictFolder that, boolean preserveKeys) throws SPUnknownIDException {
        MemConflictFolder f = new MemConflictFolder((MemProgram) prog, this, that, preserveKeys);
        NodeInitializers.instance.conflict.initNode(this, f);
        return f;
    }

    public ISPTemplateFolder createTemplateFolder(ISPProgram prog, SPNodeKey key)
        throws SPUnknownIDException {
        MemTemplateFolder f = new MemTemplateFolder((MemProgram) prog, key);
        NodeInitializers.instance.templateFolder.initNode(this, f);
        return f;
    }

    public ISPTemplateFolder createTemplateFolderCopy(ISPProgram prog, ISPTemplateFolder that, boolean preserveKeys)
        throws SPUnknownIDException {
        MemTemplateFolder f = new MemTemplateFolder((MemProgram) prog, this, that, preserveKeys);
        NodeInitializers.instance.templateFolder.updateNode(f);
        return f;
    }

    public ISPTemplateGroup createTemplateGroup(ISPProgram prog, SPNodeKey key)
        throws SPUnknownIDException {
        MemTemplateGroup tg = new MemTemplateGroup((MemProgram) prog, key);
        NodeInitializers.instance.templateGroup.initNode(this, tg);
        return tg;
    }

    public ISPTemplateGroup createTemplateGroupCopy(ISPProgram prog, ISPTemplateGroup that, boolean preserveKeys)
        throws SPUnknownIDException {

        MemTemplateGroup tg = new MemTemplateGroup((MemProgram) prog, this, that, preserveKeys);
        NodeInitializers.instance.templateGroup.updateNode(tg);
        return tg;
    }

    public ISPTemplateParameters createTemplateParameters(ISPProgram prog, SPNodeKey key)
            throws SPUnknownIDException {
        MemTemplateParameters tp = new MemTemplateParameters((MemProgram) prog, key);
        NodeInitializers.instance.templateParameters.initNode(this, tp);
        return tp;
    }

    public ISPTemplateParameters createTemplateParametersCopy(ISPProgram prog, ISPTemplateParameters that, boolean preserveKeys)
            throws SPUnknownIDException {
        MemTemplateParameters tp = new MemTemplateParameters((MemProgram) prog, that, preserveKeys);
        NodeInitializers.instance.templateParameters.updateNode(tp);
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

        // Find the node initializer to use.
        final Option<Instrument> inst = ImOption.fromOptional(
            observation.getObsComponents().stream()
                    .filter(c -> c.getType().broadType == SPComponentBroadType.INSTRUMENT)
                    .flatMap(c -> Instrument.fromComponentType(c.getType()).toStream())
                    .findFirst()
        );

        NodeInitializers.instance.obs(inst).updateNode(obs);
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
        NodeInitializers.instance.group.updateNode(group);
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

        final MemObsComponent obsComp = new MemObsComponent((MemProgram) prog, component, preserveKeys);
        final SPComponentType type    = obsComp.getType();

        final ISPNodeInitializer<ISPObsComponent, ? extends ISPDataObject> init =
                NodeInitializers.instance.obsComp.get(type);

        if (init == null) {
            throw new RuntimeException("Missing initializer for " + type);
        } else {
            init.updateNode(obsComp);
        }
        return obsComp;
    }

    public ISPObsQaLog doCreateObsQaLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        return new MemObsQaLog((MemProgram) prog, key);
    }

    public ISPObsQaLog createObsQaLogCopy(ISPProgram prog, ISPObsQaLog log, boolean preserveKeys) throws SPUnknownIDException {
        final MemObsQaLog mol = new MemObsQaLog((MemProgram) prog, log, preserveKeys);
        NodeInitializers.instance.obsQaLog.updateNode(mol);
        return mol;
    }

    public ISPObsExecLog doCreateObsExecLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        return new MemObsExecLog((MemProgram) prog, key);
    }

    public ISPObsExecLog createObsExecLogCopy(ISPProgram prog, ISPObsExecLog log, boolean preserveKeys) throws SPUnknownIDException {
        final MemObsExecLog mol = new MemObsExecLog((MemProgram) prog, log, preserveKeys);
        NodeInitializers.instance.obsExecLog.updateNode(mol);
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

        final MemSeqComponent seqComp = new MemSeqComponent((MemProgram) prog, this, component, preserveKeys);
        final SPComponentType type    = seqComp.getType();

        final ISPNodeInitializer<ISPSeqComponent, ? extends ISPSeqObject> init =
                NodeInitializers.instance.seqComp.get(type);

        if (init == null) {
            throw new RuntimeException("Missing initializer for " + type);
        } else {
            init.updateNode(seqComp);
        }
        return seqComp;
    }
}

