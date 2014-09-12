// Copyright 2004 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: DBSequenceNodeService.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.util;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;


import java.util.ArrayList;
import java.util.List;


/**
 * A class that can be used by clients to find nodes of a given type
 * in a sequence.
 *
 * @author Allan Brighton
 */
public class DBSequenceNodeService {

    // holds the result (list of ISPNode)
    private List<ISPSeqComponent> _list = new ArrayList<ISPSeqComponent>();

    // if true, return only the first node of the given type found
    private boolean _firstOnly;

    // if true, search only as far as the first observe node (for narrowType search)
    private boolean _noObserve;

    // dummy exception class used to break recursion
    private static class MyException extends Exception {}

    // zero or one of these will be defined (0 for instrument node search)
    private SPComponentType _type;
    private String _broadType;
    private String _narrowType;

    /**
     * Create a functor that returns a list of sequence nodes with the given type
     *
     * @param type the type of sequence node to search for
     * @param firstOnly if true, return only the first node of the given type found
     */
    private DBSequenceNodeService(SPComponentType type, boolean firstOnly) {
        _type = type;
        _firstOnly = firstOnly;
    }

    /**
     * Create a functor that returns a list of sequence nodes with the given broad
     * or narrow type.
     *
     * @param type the broad or narrow type string
     * @param narrow if true, type is the narrow type, otherwise the broad type
     * @param firstOnly if true, return only the first node of the given type found
     */
    private DBSequenceNodeService(String type, boolean narrow, boolean firstOnly) {
        if (narrow) {
            _narrowType = type;
        } else {
            _broadType = type;
        }
        _firstOnly = firstOnly;
    }

    /**
     * Create a functor that returns a list of sequence nodes with the given broad
     * or narrow type.
     *
     * @param type the broad or narrow type string
     * @param narrow if true, type is the narrow type, otherwise the broad type
     * @param firstOnly if true, return only the first node of the given type found
     * @param noObserve if true, search only as far as the first observe node (for narrowType search)
     */
    private DBSequenceNodeService(String type, boolean narrow, boolean firstOnly, boolean noObserve) {
        if (narrow) {
            _narrowType = type;
        } else {
            _broadType = type;
        }
        _firstOnly = firstOnly;
        _noObserve = noObserve;
    }

    /**
     * Create a functor that returns a list of instrument sequence nodes.
     *
     * @param firstOnly if true, return only the first node of the given type found
     * @param noObserve if true, search only as far as the first observe node (for narrowType search)
     */
    private DBSequenceNodeService(boolean firstOnly, boolean noObserve) {
        _firstOnly = firstOnly;
        _noObserve = noObserve;
    }

    /**
     * Gets the resulting list of sequence nodes.
     * @return a list of ISPNode
     */
    private List<ISPSeqComponent> getList() {
        return _list;
    }

    // Add a node to the result list
    private void _addToResult(ISPSeqComponent sc) throws MyException {
        _list.add(sc);
        if (_firstOnly) {
            throw new MyException();
        }
    }

    /**
     * Entry point. Adds any matching nodes to the list.
     */
    private void execute(ISPNode node) {
        if (node instanceof ISPSeqComponent) {
            ISPSeqComponent sc = (ISPSeqComponent)node;
            try {
                if (_type != null) {
                    _findSeqComponentsByType(sc);
                } else if (_narrowType != null) {
                    _findSeqComponentsByNarrowType(sc);
                } else if (_broadType != null) {
                    _findSeqComponentsByBroadType(sc);
                } else {
                    _findInstSeqComponents(sc);
                }
            } catch (MyException e1) {
                // okay: thrown to break recursion
            }
        }
    }

     // Add all of the sequence components with the given type to the result list,
     // starting the search at the given sequence component.
    private void _findSeqComponentsByType(ISPSeqComponent sc)
            throws MyException {

        if (_type.equals(sc.getType())) _addToResult(sc);
        for (ISPSeqComponent ispSeqComponent : sc.getSeqComponents()) {
            _findSeqComponentsByType(ispSeqComponent);
        }
    }

     // Add all of the sequence components with the given type to the result list,
     // starting the search at the given sequence component.
    private void _findSeqComponentsByBroadType(ISPSeqComponent sc)
            throws MyException {

        SPComponentType t = sc.getType();
        if (t.broadType.equals(_broadType)) {
            _addToResult(sc);
        }

         for (ISPSeqComponent ispSeqComponent : sc.getSeqComponents()) {
             _findSeqComponentsByBroadType(ispSeqComponent);
         }
     }

     // Add all of the sequence components with the given type to the result list,
     // starting the search at the given sequence component.
    private void _findSeqComponentsByNarrowType(ISPSeqComponent sc)
            throws MyException {

        SPComponentType t = sc.getType();
        if (_noObserve && t.broadType.equals(SeqRepeatObserve.SP_TYPE.broadType)) {
             throw new MyException();
         }
        if (t.narrowType.equals(_narrowType)) {
            _addToResult(sc);
        }

         for (ISPSeqComponent ispSeqComponent : sc.getSeqComponents()) {
             _findSeqComponentsByNarrowType(ispSeqComponent);
         }
     }


     // Add all of the instrument sequence components to the result list,
     // starting the search at the given sequence component.
    private void _findInstSeqComponents(ISPSeqComponent sc)
            throws MyException {

         SPComponentType t = sc.getType();
        if (_noObserve && t.broadType.equals(SeqRepeatObserve.SP_TYPE.broadType)) {
             throw new MyException();
         }
         if (t.broadType.equals(SPComponentBroadType.INSTRUMENT)) {
             _addToResult(sc);
         } else {
             // If the program was imported from XML, the node won't use SPInstComponentType,
             // so we need to check the data object's version
             Object o = sc.getDataObject();
             if (o instanceof AbstractDataObject) {
                 SPComponentType type = ((AbstractDataObject) o).getType();
                 if (type.broadType.equals(SPComponentBroadType.INSTRUMENT)) {
                     _addToResult(sc);
                 }
             }
         }

         for (ISPSeqComponent ispSeqComponent : sc.getSeqComponents()) {
             _findInstSeqComponents(ispSeqComponent);
         }
     }


    /**
     * Return a list of all the sequence components with the given type,
     * starting the search at the given sequence component.
     *
     * @param sc the starting sequence component for the search
     * @param type the type of node to search for
     * @param firstOnly if true, return only the first node of the given type found
     */
    public static List<ISPSeqComponent> findSeqComponentsByType(ISPSeqComponent sc,
                                               SPComponentType type,
                                               boolean firstOnly) {
        DBSequenceNodeService f = new DBSequenceNodeService(type, firstOnly);
        f.execute(sc);
        return f.getList();
    }

    /**
     * Return a list of all the sequence components with the given narrow type,
     * starting the search at the given sequence component.
     *
     * @param sc the starting sequence component for the search
     * @param type the narrow type string
     * @param firstOnly if true, return only the first node of the given type found
     * @param noObserve if true, search only as far as the first observe node
     */
    public static List<ISPSeqComponent> findSeqComponentsByNarrowType(ISPSeqComponent sc,
                                                     String type,
                                                     boolean firstOnly,
                                                     boolean noObserve) {
        DBSequenceNodeService f = new DBSequenceNodeService(type, true, firstOnly, noObserve);
        f.execute(sc);
        return f.getList();
    }

    /**
     * Return a list of all the sequence components with the given broad type,
     * starting the search at the given sequence component.
     *
     * @param sc the starting sequence component for the search
     * @param type the broad type string
     * @param firstOnly if true, return only the first node of the given type found
     */
    public static List<ISPSeqComponent> findSeqComponentsByBroadType(ISPSeqComponent sc,
                                                    String type,
                                                    boolean firstOnly) {
        DBSequenceNodeService f = new DBSequenceNodeService(type, false, firstOnly);
        f.execute(sc);
        return f.getList();
    }

    /**
     * Return a list of all the instrument sequence components,
     * starting the search at the given sequence component.
     *
     * @param sc the starting sequence component for the search
     * @param firstOnly if true, return only the first node of the given type found
     * @param noObserve if true, search only as far as the first observe node
     */
    public static List<ISPSeqComponent> findInstSeqComponents(ISPSeqComponent sc, boolean firstOnly, boolean noObserve) {
        DBSequenceNodeService f = new DBSequenceNodeService(firstOnly, noObserve);
        f.execute(sc);
        return f.getList();
    }
}

