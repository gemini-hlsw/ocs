// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ISPNodeInitializer.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.pot.sp;

import java.io.Serializable;




/**
 * Initializers provide a means whereby newly created Science Program nodes
 * may be initialized before being returned to the client.  For instance,
 * the <code>{@link ISPFactory}</code> provides a methods to register
 * initializers of various types that it then uses when creating nodes of
 * the matching type.  "Initialization" may refer to any setup work that
 * must be performed, including adding specific user objects, component
 * objects, or child nodes.
 */
public interface ISPNodeInitializer extends Serializable {
    /**
     * Initializes the given <code>node</code>.
     *
     * @param factory the factory that may be used to create any required
     * science program nodes
     *
     * @param node the science program node to be initialized
     */
    void initNode(ISPFactory factory, ISPNode node);

    /**
     * Updates the given <code>node</code>. This should be called on any new
     * nodes created by making a deep copy of another node, so that the user
     * objects are updated correctly.
     *
     * @param node the science program node to be updated
     */
    void updateNode(ISPNode node);
}

