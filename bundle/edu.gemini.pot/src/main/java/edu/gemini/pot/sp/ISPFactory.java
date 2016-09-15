package edu.gemini.pot.sp;

import edu.gemini.spModel.core.SPProgramID;

import java.util.List;


/**
 * This is the interface used for the creation of science programs
 * nodes.  The factory creates concrete products of a single kind.
 */
public interface ISPFactory {
    /**
     * Returns a string hint for the type of concrete products
     * produced by the factory.
     */
    String getType();

    /**
     * Creates an ISPProgram using the default initializer for programs
     * provided one has been registered (see
     * <code>{@link #registerProgramInit}</code>).
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     * @param progID the program ID to use
     */
    ISPProgram createProgram(SPNodeKey key, SPProgramID progID);

    /**
     * Creates an ISPProgram using the provided initializer (overriding
     * the default initializer for programs).
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * the newly created node; will be used instead of any registered
     * default initializer
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     * @param progID the program ID to use
     */
    ISPProgram createProgram(ISPNodeInitializer init, SPNodeKey key, SPProgramID progID);

    /**
     * Creates an ISPProgram that is a deep copy of the given <code>program</code>.
     * All nodes have identical keys and the program shares the same LifespanId.
     * This copy is intended for transactional updates.  That is, a duplicate
     * is made and updated and if successful swapped in for the existing
     * version of the program.
     */
    ISPProgram copyWithSameKeys(ISPProgram program);

    /**
     * Creates a duplicate program with the same node keys as for
     * {@link #copyWithSameKeys}, but assigns a new LifecycleId.  This version
     * of copy is intended to be used when a program is transported to a new
     * database.
     */
    ISPProgram copyWithNewLifespanId(ISPProgram program);

    /**
     * Creates a copy of the program that is identical in structure and data to
     * the given <code>program</code> but with all new node keys and a new
     * program id.
     */
    ISPProgram copyWithNewKeys(ISPProgram program, SPProgramID newProgID);

    /**
     * Sets the default initializer to use when creating a new program.
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * a newly created program
     */
    void registerProgramInit(ISPNodeInitializer init);

    ISPConflictFolder createConflictFolder(ISPProgram prog, SPNodeKey key)
        throws SPUnknownIDException;

    ISPConflictFolder createConflictFolderCopy(ISPProgram prog,
                                               ISPConflictFolder folder,
                                               boolean preserveKeys)
        throws SPUnknownIDException;

    void registerConflictFolderInit(ISPNodeInitializer init);

    /**
     * Creates an ISPTemplateFolder using the default initializer for
     * template folders provided one has been registered (see
     * <code>{@link #registerTemplateFolderInit}</code>).
     *
     * @param prog the program with which this folder should be associated
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPTemplateFolder createTemplateFolder(ISPProgram prog, SPNodeKey key)
        throws SPUnknownIDException;


    /**
     * Creates an ISPTemplateFolder that is a deep copy of the given
     * <code>folder</code>.
     *
     * @param prog the program with which this folder should be associated
     *
     * @param folder the folder to copy
     *
     * @param preserveKeys whether the copied folder will have unique keys
     * or will copy the keys of the source <code>folder</code>
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPTemplateFolder createTemplateFolderCopy(ISPProgram prog,
                                         ISPTemplateFolder folder,
                                         boolean preserveKeys)
        throws SPUnknownIDException;

    /**
     * Sets the default initializer to use when creating a new template folder.
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * a newly created folder
     */
    void registerTemplateFolderInit(ISPNodeInitializer init);

    /**
     * Creates an ISPTemplateGroup using the default initializer for
     * template groups provided one has been registered (see
     * <code>{@link #registerTemplateGroupInit}</code>).
     *
     * @param prog the program with which this template group should be associated
     *
     * @param key key to use for this node (if <code>null</code> a new key will
     *            be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPTemplateGroup createTemplateGroup(ISPProgram prog, SPNodeKey key)
        throws SPUnknownIDException;


    /**
     * Creates an ISPTemplateGroup that is a deep copy of the given
     * <code>group</code>.
     *
     * @param prog the program with which this group should be associated
     *
     * @param group the group to copy
     *
     * @param preserveKeys whether the copied group will have unique keys
     * or will copy the keys of the source <code>group</code>
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPTemplateGroup createTemplateGroupCopy(ISPProgram prog,
                                         ISPTemplateGroup group,
                                         boolean preserveKeys)
        throws SPUnknownIDException;

    /**
     * Sets the default initializer to use when creating a new template group.
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * a newly created group
     */
    void registerTemplateGroupInit(ISPNodeInitializer init);


    /**
     * Creates an ISPTemplateParameters using the default initializer for
     * template parameterss provided one has been registered (see
     * <code>{@link #registerTemplateParametersInit}</code>).
     *
     * @param prog the program with which this template parameters should be
     *             associated
     *
     * @param key key to use for this node (if <code>null</code> a new key will
     *            be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPTemplateParameters createTemplateParameters(ISPProgram prog, SPNodeKey key)
            throws SPUnknownIDException;


    /**
     * Creates an ISPTemplateParameters that is a deep copy of the given
     * <code>parameters</code>.
     *
     * @param prog the program with which this parameters should be associated
     *
     * @param parameters the parameters to copy
     *
     * @param preserveKeys whether the copied parameters will have unique keys
     * or will copy the keys of the source <code>parameters</code>
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPTemplateParameters createTemplateParametersCopy(ISPProgram prog,
                                                       ISPTemplateParameters parameters,
                                                       boolean preserveKeys)
            throws SPUnknownIDException;

    /**
     * Sets the default initializer to use when creating a new template parameters.
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * a newly created parameters
     */
    void registerTemplateParametersInit(ISPNodeInitializer init);

    /**
     * Creates an ISPNightlyRecord using the default initializer for nightly plans
     * provided one has been registered (see <code>{@link #registerNightlyRecordInit}</code>).
     *
     * @param planID the plan ID to use
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     */
    ISPNightlyRecord createNightlyRecord(SPNodeKey key, SPProgramID planID);

    /**
     * Creates an ISPNightlyRecord using the provided initializer (overriding
     * the default initializer for nightly plans).
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * the newly created node; will be used instead of any registered
     * default initializer
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     */
    ISPNightlyRecord createNightlyRecord(ISPNodeInitializer init, SPNodeKey key, SPProgramID planID);

    /**
     * Creates an ISPNightlyRecord that is a deep copy of the given <code>nightly plan</code>.
     */
    ISPNightlyRecord renameNightlyRecord(ISPNightlyRecord plan, SPNodeKey key, SPProgramID planID);

    /**
     * Sets the default initializer to use when creating a new nightly plan.
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * a newly created nightly plan
     */
    void registerNightlyRecordInit(ISPNodeInitializer init);

    /**
     * Creates an ISPObservation using the default initializer for
     * observations provided one has been registered (see
     * <code>{@link #registerObservationInit}</code>).
     *
     * @param prog the program with which this observation should be associated
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPObservation createObservation(ISPProgram prog, SPNodeKey key) throws SPException;

    /**
     * Creates an ISPObservation using the default initializer for
     * observations provided one has been registered (see
     * <code>{@link #registerObservationInit}</code>).
     *
     * @param prog the program with which this observation should be associated
     * @param index the index of the observation inside of the program
     * (or -1 to automatically generate a new index)
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPObservation createObservation(ISPProgram prog, int index, SPNodeKey key) throws SPException;

    /**
     * Creates an ISPObservation using the provided initializer (overriding
     * the default initializer for observations).
     *
     * @param prog the program with which this observation should be associated
     *
     * @param index the index of the observation inside of the program
     * (or -1 to automatically generate a new index)
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * the newly created node; will be used instead of any registered
     * default initializer
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPObservation createObservation(ISPProgram prog, int index, ISPNodeInitializer init, SPNodeKey key) throws SPException;

    /**
     * Creates an ISPObservation that is a deep copy of the given
     * <code>observation</code>.
     *
     * @param prog the program with which this observation should be associated
     *
     * @param observation the observation to copy
     *
     * @param preserveKeys whether the copied observation will have unique keys
     * or will copy the keys of the source <code>observation</code>
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPObservation createObservationCopy(ISPProgram prog, ISPObservation observation, boolean preserveKeys) throws SPException;

    /**
     * Sets the default initializer to use when creating a new observation.
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * a newly created observation
     */
    void registerObservationInit(ISPNodeInitializer init);

    ISPObsQaLog createObsQaLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException;
    ISPObsQaLog createObsQaLogCopy(ISPProgram prog, ISPObsQaLog log, boolean preserveKeys) throws SPUnknownIDException;
    void registerObsQaLogInit(ISPNodeInitializer init);

    ISPObsExecLog createObsExecLog(ISPProgram prog, SPNodeKey key) throws SPUnknownIDException;
    ISPObsExecLog createObsExecLogCopy(ISPProgram prog, ISPObsExecLog log, boolean preserveKeys) throws SPUnknownIDException;
    void registerObsExecLogInit(ISPNodeInitializer init);

    /**
     * Creates an ISPObsComponent using the default initializer for the
     * given component <code>type</code>, if one has been registered
     * (see <code>{@link #registerObsComponentInit}</code>).
     *
     * @param prog the program with which this component should be associated
     *
     * @param type the type that distinguishes this observation component
     * from others (for example, "target environment" or "niri")
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPObsComponent createObsComponent(ISPProgram prog,
                                       SPComponentType type, SPNodeKey key)
            throws SPUnknownIDException;

    /**
     * Creates an ISPObsComponent using the given initializer.
     *
     * @param prog the program with which this component should be associated
     *
     * @param type the type that distinguishes this observation component from
     * others (for example, "target environment" or "niri")
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * the newly created component; will be used instead of any registered
     * default initializer
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPObsComponent createObsComponent(ISPProgram prog, SPComponentType type,
                                       ISPNodeInitializer init, SPNodeKey key)
            throws SPUnknownIDException;

    /**
     * Creates an ISPObsComponent that is a deep copy of the given
     * <code>component</code>.
     *
     * @param prog the program with which this component should be associated
     *
     * @param component the component to copy
     *
     * @param preserveKeys whether the copied component will have unique keys
     * or will copy the keys of the source <code>component</code>
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPObsComponent createObsComponentCopy(ISPProgram prog,
                                           ISPObsComponent component,
                                           boolean preserveKeys)
            throws SPUnknownIDException;

    /**
     * Sets the default initializer to use when creating an observation
     * component of the given <code>type</code>.
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * a newly created component
     */
    void registerObsComponentInit(SPComponentType type, ISPNodeInitializer init);

    /**
     * Creates an ISPSeqComponent using the default initializer for the
     * given component <code>type</code>, if one has been registered
     * (see <code>{@link #registerSeqComponentInit}</code>).
     *
     * @param prog the program with which this component should be associated
     *
     * @param type the type that distinguies this sequence component
     * from others (for example, "offset" or "niri")
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPSeqComponent createSeqComponent(ISPProgram prog,
                                       SPComponentType type, SPNodeKey key)
            throws SPUnknownIDException;

    /**
     * Creates an ISPSeqComponent using the given initializer.
     *
     * @param prog the program with which this component should be associated
     *
     * @param type the type that distinguies this sequence component from
     * others (for example, "offset" or "niri")
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * the newly created component; will be used instead of any registered
     * default initializer
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPSeqComponent createSeqComponent(ISPProgram prog,
                                       SPComponentType type,
                                       ISPNodeInitializer init,
                                       SPNodeKey key)
            throws SPUnknownIDException;

    /**
     * Creates an ISPSeqComponent that is a deep copy of the given
     * <code>component</code>.
     *
     * @param prog the key of the program with which this component should
     * be associated
     *
     * @param component the component to copy
     *
     * @param preserveKeys whether the copied component will have unique keys
     * or will copy the keys of the source <code>component</code>
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPSeqComponent createSeqComponentCopy(ISPProgram prog,
                                           ISPSeqComponent component,
                                           boolean preserveKeys)
            throws SPUnknownIDException;

    /**
     * Sets the default initializer to use when creating a sequence
     * component of the given <code>type</code>.
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * a newly created component
     */
    void registerSeqComponentInit(SPComponentType type, ISPNodeInitializer init);

    /**
     * Returns a List of <code>{@link SPComponentType}</code> objects, one
     * for each type of obs component that can be created by the factory
     * of this database.
     * In order to discover the types that can be
     * created by the factory, a client can use this method.
     */
    List<SPComponentType> getCreatableObsComponents();

    /**
     * Returns a List of <code>{@link SPComponentType}</code> objects, one
     * for each type of seq component that can be created by this factory.
     * In order to discover the types that can be
     * created by the factory, a client can use this method.
     */
    List<SPComponentType> getCreatableSeqComponents();

    /**
     * Sets the default initializer to use when creating a new group.
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * a newly created group
     */
    void registerGroupInit(ISPNodeInitializer init);

    /**
     * Creates an ISPGroup using the provided node key.
     *
     * @param prog the program with which this group should be associated
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPGroup createGroup(ISPProgram prog, SPNodeKey key)
            throws SPUnknownIDException;

    /**
     * Creates an ISPGroup using the provided initializer (overriding
     * the default initializer for groups).
     *
     * @param prog the program with which this group should be associated
     *
     * @param init the <code>ISPNodeInitializer</code> to use when initializing
     * the newly created node; will be used instead of any registered
     * default initializer
     *
     * @param key key to use for this node (if <code>null</code> a
     * new key will be assigned)
     *
     * @throws SPUnknownIDException if the given <code>progKey</code> refers
     * to a program that is not known by this factory
     */
    ISPGroup createGroup(ISPProgram prog,
                                ISPNodeInitializer init,
                                SPNodeKey key)
            throws SPUnknownIDException;

    /**
     * Creates a deep copy of the given <code>group</code>
     */
    ISPGroup createGroupCopy(ISPProgram prog,
                                    ISPGroup ispGroup,
                                    boolean preserveKeys)
            throws SPUnknownIDException;

}

