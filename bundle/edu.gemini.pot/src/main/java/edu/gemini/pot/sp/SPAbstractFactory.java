// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPAbstractFactory.java 46866 2012-07-20 19:35:51Z swalker $
//

package edu.gemini.pot.sp;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.init.NodeInitializers;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obscomp.SPGroup;

import edu.gemini.shared.util.immutable.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class provides an abstract implementation of the
 * <code>ISPFactory</code> interface.   Most methods of the interface have
 * been implemented, but in terms of more primitive abstract template methods.
 * Concrete factory subclasses can extend this implementation and provide
 * implementations of the template methods without worrying about
 * reimplementing some of the more redundant, broadly applicable code
 * like handling initializers, and initialization.
 *
 * <p>Please see the <code>{@link ISPFactory}</code> API for more
 * documentation on many of the methods in this class.
 */
public abstract class SPAbstractFactory implements ISPFactory {

    /**
     * Default constructor, explicitly declared since
     * <code>UnicastRemoteObject</code> constructor is declared to throw
     * <code>RemoteException</code>.
     */
    protected SPAbstractFactory() {
    }

    public ISPProgram createProgram(SPNodeKey key, SPProgramID progID) {
        return createProgram(NodeInitializers.instance.program, key, progID);
    }

    /**
     * Creates an <code>ISPProgram</code> using the given initializer.
     */
    public ISPProgram createProgram(ISPNodeInitializer<ISPProgram, SPProgram> init, SPNodeKey key, SPProgramID progID) {
        final ISPProgram prog = doCreateProgram(key, progID);
        if (init != null) init.initNode(this, prog);
        return prog;
    }

    /**
     * Handles the actual creation of a program.   Called by the
     * <code>createProgram</code> methods.  Once the program is
     * created and returned by this method, it is then initialized, provided
     * an initializer is available.
     */
    protected abstract ISPProgram doCreateProgram(SPNodeKey key, SPProgramID progID);

    public ISPNightlyRecord createNightlyRecord(SPNodeKey key, SPProgramID planID) {
        return createNightlyRecord(NodeInitializers.instance.record, key, planID);
    }

    /**
     * Creates an <code>ISPNightlyRecord</code> using the given initializer.
     */
    public ISPNightlyRecord createNightlyRecord(ISPNodeInitializer<ISPNightlyRecord, NightlyRecord> init, SPNodeKey key, SPProgramID planID) {
        final ISPNightlyRecord record = doCreateNightlyPlan(key, planID);
        if (init != null) init.initNode(this, record);
        return record;
    }

    /**
     * Handles the actual creation of a nightly plan.   Called by the
     * <code>createNightlyRecord</code> methods.  Once the nightly plan is
     * created and returned by this method, it is then initialized, provided
     * an initializer is available.
     */
    protected abstract ISPNightlyRecord doCreateNightlyPlan(SPNodeKey key, SPProgramID planID);


    public ISPObservation createObservation(ISPProgram prog, Option<Instrument> inst, SPNodeKey key) throws SPException {
        return createObservation(prog, -1, NodeInitializers.instance.obs(inst), key);
    }
    public ISPObservation createObservation(ISPProgram prog, int index, Option<Instrument> inst, SPNodeKey key) throws SPException {
        return createObservation(prog, index, NodeInitializers.instance.obs(inst), key);
    }

    // finally, the canonical factory method
    public ISPObservation createObservation(ISPProgram prog, int index, ISPNodeInitializer<ISPObservation, SPObservation> init, SPNodeKey key) throws SPException {
        final ISPObservation obs = doCreateObservation(prog, index, key);
        if ((obs != null) && (init != null)) init.initNode(this, obs);
        return obs;
    }

    /**
     * Handles the actual creation of an observation.   Called by the
     * <code>createObservation</code> methods.  Once the observation is
     * created and returned by this method, it is then initialized, provided
     * an initializer is available.
     */
    protected abstract ISPObservation doCreateObservation(ISPProgram prog,
                                                          int index,
                                                          SPNodeKey key)
            throws SPException;

    /**
     * Creates an <code>ISPGroup</code> using the given key.
     */
    public ISPGroup createGroup(ISPProgram prog, SPNodeKey key)
            throws SPUnknownIDException {
        return createGroup(prog, NodeInitializers.instance.group, key);
    }

    /**
     * Creates an <code>ISPGroup</code> using the given initializer and key.
     */
    public ISPGroup createGroup(ISPProgram prog,
                                ISPNodeInitializer<ISPGroup, SPGroup> init,
                                SPNodeKey key)
            throws SPUnknownIDException {

        ISPGroup group = doCreateGroup(prog, key);
        if ((group != null) && (init != null)) {
            init.initNode(this, group);
        }
        return group;
    }

    /**
     * Handles the actual creation of an Group.   Called by the
     * <code>createGroup</code> methods.  Once the Group is
     * created and returned by this method, it is then initialized, provided
     * an initializer is available.
     */
    protected abstract ISPGroup doCreateGroup(ISPProgram prog, SPNodeKey key)
            throws SPUnknownIDException;


    /**
     * Creates an <code>ISPObsComponent</code> using the default initializer
     * for the given type of obs component.
     */
    public ISPObsComponent createObsComponent(ISPProgram prog,
                                              SPComponentType type,
                                              SPNodeKey key)
            throws SPUnknownIDException {

        return createObsComponent(prog, type, null, key);
    }

    /**
     * Creates an <code>ISPObsComponent</code> using the given initializer.
     */
    public ISPObsComponent createObsComponent(ISPProgram prog,
                                              SPComponentType type,
                                              ISPNodeInitializer<ISPObsComponent, ? extends ISPDataObject> init,
                                              SPNodeKey key)
            throws SPUnknownIDException {

        final ISPNodeInitializer<ISPObsComponent, ? extends ISPDataObject> init0 =
                (init == null) ? NodeInitializers.instance.obsComp.get(type) : init;

        final ISPObsComponent oc = doCreateObsComponent(prog, type, key);
        if (init0 == null) {
            throw new RuntimeException("Missing initializer for " + type);
        } else {
            init0.initNode(this, oc);
        }
        return oc;
    }

    /**
     * Handles the actual creation of an observation component.   Called by the
     * <code>createObsComponent</code> methods.  Once the component is created
     * and returned by this method, it is then initialized, provided an
     * initializer is available.
     */
    protected abstract ISPObsComponent doCreateObsComponent(
            ISPProgram prog,
            SPComponentType type,
            SPNodeKey key)
            throws SPUnknownIDException;


    public ISPObsQaLog createObsQaLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        final ISPObsQaLog log = doCreateObsQaLog(prog, key);
        NodeInitializers.instance.obsQaLog.initNode(this, log);
        return log;
    }

    protected abstract ISPObsQaLog doCreateObsQaLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException;

    public ISPObsExecLog createObsExecLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        final ISPObsExecLog log = doCreateObsExecLog(prog, key);
        NodeInitializers.instance.obsExecLog.initNode(this, log);
        return log;
    }

    protected abstract ISPObsExecLog doCreateObsExecLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException;

    /**
     * Creates an <code>ISPSeqComponent</code> using the default initializer
     * for the given type of sequence component..
     */
    public ISPSeqComponent createSeqComponent(ISPProgram prog,
                                              SPComponentType type,
                                              SPNodeKey key)
            throws SPUnknownIDException {
        return createSeqComponent(prog, type, NodeInitializers.instance.seqComp.get(type), key);
    }

    /**
     * Creates an <code>ISPSeqComponent</code> using the given initializer.
     */
    public ISPSeqComponent createSeqComponent(ISPProgram prog,
                                              SPComponentType type,
                                              ISPNodeInitializer<ISPSeqComponent, ? extends ISPSeqObject> init,
                                              SPNodeKey key)
            throws SPUnknownIDException {

        final ISPSeqComponent sc = doCreateSeqComponent(prog, type, key);
        if (init == null) {
            throw new RuntimeException("Missing initializer for " + type);
        } else {
            init.initNode(this, sc);
        }
        return sc;
    }

    /**
     * Handles the actual creation of a sequence component.   Called by the
     * <code>createSeqComponent</code> methods.  Once the component is created
     * and returned by this method, it is then initialized, provided an
     * initializer is available.
     */
    protected abstract ISPSeqComponent doCreateSeqComponent(ISPProgram prog,
                                                            SPComponentType type,
                                                            SPNodeKey key)
            throws SPUnknownIDException;

}

