// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPAbstractFactory.java 46866 2012-07-20 19:35:51Z swalker $
//

package edu.gemini.pot.sp;

import edu.gemini.spModel.core.SPProgramID;

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
    protected ISPNodeInitializer _progInit;
    protected ISPNodeInitializer _nightlyPlanInit;
    protected ISPNodeInitializer _obsInit;
    protected ISPNodeInitializer _obsQaLogInit;
    protected ISPNodeInitializer _obsExecLogInit;
    protected ISPNodeInitializer _conflictFolderInit;
    protected ISPNodeInitializer _templateFolderInit;
    protected ISPNodeInitializer _templateGroupInit;
    protected ISPNodeInitializer _templateParametersInit;
    protected ISPNodeInitializer _groupInit;
    protected Map<SPComponentType, ISPNodeInitializer> _obsCompInitMap;
    protected Map<SPComponentType, ISPNodeInitializer> _sequenceCompInitMap;

    /**
     * Default constructor, explicitly declared since
     * <code>UnicastRemoteObject</code> constructor is declared to throw
     * <code>RemoteException</code>.
     */
    protected SPAbstractFactory() {
    }

    public ISPProgram createProgram(SPNodeKey key, SPProgramID progID) {
        return createProgram(_progInit, key, progID);
    }

    /**
     * Creates an <code>ISPProgram</code> using the given initializer.
     */
    public ISPProgram createProgram(ISPNodeInitializer init, SPNodeKey key, SPProgramID progID) {
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

    /**
     * Registers an initializer for program initialization.
     */
    public void registerProgramInit(ISPNodeInitializer init) {
        _progInit = init;
    }

    public ISPNightlyRecord createNightlyRecord(SPNodeKey key, SPProgramID planID) {
        return createNightlyRecord(_nightlyPlanInit, key, planID);
    }

    /**
     * Creates an <code>ISPNightlyRecord</code> using the given initializer.
     */
    public ISPNightlyRecord createNightlyRecord(ISPNodeInitializer init, SPNodeKey key, SPProgramID planID) {
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


    /**
     * Registers an initializer for nightly plan initialization.
     */
    public void registerNightlyRecordInit(ISPNodeInitializer init) {
        _nightlyPlanInit = init;
    }

    @Override
    public void registerConflictFolderInit(ISPNodeInitializer init) {
        _conflictFolderInit = init;
    }

    @Override
    public void registerTemplateFolderInit(ISPNodeInitializer init)  {
        _templateFolderInit = init;
    }

    @Override
    public void registerTemplateGroupInit(ISPNodeInitializer init)  {
        _templateGroupInit = init;
    }

    @Override
    public void registerTemplateParametersInit(ISPNodeInitializer init)  {
        _templateParametersInit = init;
    }

    public ISPObservation createObservation(ISPProgram prog, SPNodeKey key) throws SPException {
        return createObservation(prog, -1, _obsInit, key);
    }
    public ISPObservation createObservation(ISPProgram prog, int index, SPNodeKey key) throws SPException {
        return createObservation(prog, index, _obsInit, key);
    }

    // finally, the canonical factory method
    public ISPObservation createObservation(ISPProgram prog, int index, ISPNodeInitializer init, SPNodeKey key) throws SPException {
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
     * Registers an initializer for observation initialization.
     */
    public void registerObservationInit(ISPNodeInitializer init) {
        _obsInit = init;
    }

    /**
     * Creates an <code>ISPGroup</code> using the given key.
     */
    public ISPGroup createGroup(ISPProgram prog, SPNodeKey key)
            throws SPUnknownIDException {
        return createGroup(prog, _groupInit, key);
    }

    /**
     * Creates an <code>ISPGroup</code> using the given initializer and key.
     */
    public ISPGroup createGroup(ISPProgram prog,
                                ISPNodeInitializer init,
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
     * Registers an initializer for group initialization.
     */
    public void registerGroupInit(ISPNodeInitializer init) {
        _groupInit = init;
    }

    /**
     * Creates an <code>ISPObsComponent</code> using the registered initializer
     * for the given type of obs component (if any).
     */
    public ISPObsComponent createObsComponent(ISPProgram prog,
                                              SPComponentType type,
                                              SPNodeKey key)
            throws SPUnknownIDException {
        ISPNodeInitializer init = null;
        if (_obsCompInitMap != null) {
            init = _obsCompInitMap.get(type);
        }
        return createObsComponent(prog, type, init, key);
    }

    /**
     * Creates an <code>ISPObsComponent</code> using the given initializer.
     */
    public ISPObsComponent createObsComponent(ISPProgram prog,
                                              SPComponentType type, ISPNodeInitializer init,
                                              SPNodeKey key)
            throws SPUnknownIDException {
        ISPObsComponent oc = doCreateObsComponent(prog, type, key);
        if (init != null) init.initNode(this, oc);

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


    /**
     * Registers an initializer for creating observation components of the
     * given <code>type</code>.
     */
    public void registerObsComponentInit(SPComponentType type,
                                         ISPNodeInitializer init) {
        if (_obsCompInitMap == null) {
            _obsCompInitMap = new HashMap<SPComponentType, ISPNodeInitializer>();
        }
        _obsCompInitMap.put(type, init);
    }

    public ISPObsQaLog createObsQaLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        final ISPObsQaLog log = doCreateObsQaLog(prog, key);
        if (_obsQaLogInit != null) _obsQaLogInit.initNode(this, log);
        return log;
    }

    protected abstract ISPObsQaLog doCreateObsQaLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException;

    public void registerObsQaLogInit(ISPNodeInitializer init) {
        _obsQaLogInit = init;
    }

    public ISPObsExecLog createObsExecLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException {
        final ISPObsExecLog log = doCreateObsExecLog(prog, key);
        if (_obsExecLogInit != null) _obsExecLogInit.initNode(this, log);
        return log;
    }

    protected abstract ISPObsExecLog doCreateObsExecLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException;

    public void registerObsExecLogInit(ISPNodeInitializer init) {
        _obsExecLogInit = init;
    }

    /**
     * Creates an <code>ISPSeqComponent</code> using the registered initializer
     * for the given type of sequence component (if any).
     */
    public ISPSeqComponent createSeqComponent(ISPProgram prog,
                                              SPComponentType type,
                                              SPNodeKey key)
            throws SPUnknownIDException {
        ISPNodeInitializer init = null;
        if (_sequenceCompInitMap != null) {
            init = _sequenceCompInitMap.get(type);
        }
        return createSeqComponent(prog, type, init, key);
    }

    /**
     * Creates an <code>ISPSeqComponent</code> using the given initializer.
     */
    public ISPSeqComponent createSeqComponent(ISPProgram prog,
                                              SPComponentType type,
                                              ISPNodeInitializer init,
                                              SPNodeKey key)
            throws SPUnknownIDException {
        ISPSeqComponent sc = doCreateSeqComponent(prog, type, key);
        if (init != null) init.initNode(this, sc);

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


    /**
     * Registers an initializer for creating sequence components of the
     * given <code>type</code>.
     */
    public void registerSeqComponentInit(SPComponentType type,
                                         ISPNodeInitializer init) {
        if (_sequenceCompInitMap == null) {
            _sequenceCompInitMap = new HashMap<SPComponentType, ISPNodeInitializer>();
        }
        _sequenceCompInitMap.put(type, init);
    }

    /**
     * Returns a List of <code>{@link SPComponentType}</code> objects, one
     * for each type of obs component that can be created by this factory.
     * In order to discover the types that can be
     * created by the factory, a client can use this method.
     */
    public abstract List<SPComponentType> getCreatableObsComponents();

    /**
     * Returns a List of <code>{@link SPComponentType}</code> objects, one
     * for each type of seq component that can be created by this factory.
     * In order to discover the types that can be
     * created by the factory, a client can use this method.
     */
    public abstract List<SPComponentType> getCreatableSeqComponents();
}

