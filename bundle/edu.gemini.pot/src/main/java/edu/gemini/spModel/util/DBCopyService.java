/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: DBCopyFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
 */
package edu.gemini.spModel.util;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.init.ObservationNI;
import edu.gemini.spModel.template.SplitFunctor;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * A class that can be used by clients to efficiently copy science program tree nodes
 * from from program to another.
 * Nodes are copied recursively and it is possible to copy an entire program, one or
 * more observations, obs components or sequence nodes. If a program node is copied,
 * it overwrites the target program, deleting any existing nodes there. Otherwise the
 * copied nodes are inserted in the program at the specified position.
 *
 * @author Allan Brighton
 */
public class DBCopyService {

    // result of a copy operation
    private ISPNode _result;

    // The science program being copied to
    private ISPProgram _targetProg;

    // Used to create nodes
    private ISPFactory _factory;


    /**
     * This constructor is used for node copy operations
     *
     * @param prog the target program for the copy
     */
    private DBCopyService(ISPProgram prog, ISPFactory factory) {
        _targetProg = prog;
        _factory = factory;
    }

    /**
     * Return the result of a copy operation
     */
    private ISPNode getResult() {
        return _result;
    }


    private void execute(ISPNode node) throws SPException {
        if (node instanceof ISPProgram) {
            _result = _copyProg((ISPProgram) node);
        } else if (!_targetProg.equals(node)) {
            _result = _copyNode(node);
        }
    }


    /**
     * Copy the contents of the given source program to the
     * given target program.
     *
     * @param sourceProg the source of the copy
     */
    private ISPProgram _copyProg(ISPProgram sourceProg) throws SPException {

        // copy observations from source prog
        List<ISPNode> sourceChildList = sourceProg.getChildren();
        List<ISPNode> targetChildList = new ArrayList<ISPNode>(sourceChildList.size());
        for (ISPNode o : sourceChildList) {
            targetChildList.add(_copyNode(o));
        }
        _targetProg.setChildren(targetChildList);

        // remove the client data from the target
        Set<String> keys = _targetProg.getClientDataKeys();
        for (String key : keys) {
            _targetProg.removeClientData(key);
        }

        // copy the client data to the program
        keys = sourceProg.getClientDataKeys();
        for (String key : keys) {
            Object o = sourceProg.getClientData(key);
            _targetProg.putClientData(key, o);
        }

        return _targetProg;
    }

    // Returns true if the node is from the given program
    // See REL-373: On hold observations must revert to phase 2 when copied to another program
    private boolean sameProgram(ISPNode node, ISPProgram prog) {
        SPNodeKey key = node.getProgramKey();
        if (key != null) {
            return key.equals(prog.getProgramKey());
        }
        return false;
    }

    /**
     * Return a deep copy of the given science program node, with the progID field
     * set for the given program node. This should be used when copying a node from
     * one SP tree to another.
     *
     * @param node the science program tree node to copy
     */
    private ISPNode _copyNode(ISPNode node) throws SPException {

        if (node instanceof ISPObservation) {
            ISPObservation existingObs = (ISPObservation) node;
            ISPObservation obs = _factory.createObservationCopy(_targetProg, existingObs, false);
            ObservationNI.reset(obs, sameProgram(existingObs, _targetProg)); // reset status after copy
            return obs;
        }
        if (node instanceof ISPObsComponent) {
            ISPObsComponent existingObsComp = (ISPObsComponent) node;
            return _factory.createObsComponentCopy(_targetProg, existingObsComp, false);
        }
        if (node instanceof ISPGroup) {
            ISPGroup existingGroup = (ISPGroup) node;
            ISPGroup group = _factory.createGroupCopy(_targetProg, existingGroup, false);
            // need to reset the observations in the group
            for (ISPObservation o : group.getObservations()) {
                ObservationNI.reset(o, sameProgram(existingGroup, _targetProg));
            }
            return group;
        }
        if (node instanceof ISPSeqComponent) {
            ISPSeqComponent existingSeqComp = (ISPSeqComponent) node;
            return _factory.createSeqComponentCopy(_targetProg, existingSeqComp, false);
        }

        if (node instanceof ISPTemplateGroup) {
            return (new SplitFunctor((ISPTemplateGroup) node)).split(_factory);
        }
        return node;
    }

    /**
     * Return a deep copy of the given science program node, with the progID field
     * set for the given program node. This should be used when copying a node from
     * one SP tree to another. The result will be available via the getResult() method.
     * <p/>
     * If <code>node</code> is also an ISPProgram, the target program is overwritten
     * with the contents of the source program.
     *
     * @param targetProg the target science program tree root
     * @param node       the science program tree node to copy
     */
    public static ISPNode copy(ISPFactory factory, ISPProgram targetProg, ISPNode node) throws SPException {
        DBCopyService f = new DBCopyService(targetProg, factory);
        f.execute(node);
        return f.getResult();
    }
}

