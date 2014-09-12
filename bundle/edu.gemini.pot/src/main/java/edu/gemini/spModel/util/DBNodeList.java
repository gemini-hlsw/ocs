// Copyright 2004 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: DBNodeList.java 47000 2012-07-26 19:15:10Z swalker $
//

package edu.gemini.spModel.util;

import edu.gemini.pot.sp.*;


import java.util.ArrayList;
import java.util.List;


/**
 * A class that can be used by clients to obtain a listing
 * of all the nodes under a given container node.
 *
 * @author Allan Brighton
 */
public class DBNodeList {
    private static void getNodeList(ISPNode node, List<ISPNode> res) {
        res.add(node);
        if (node instanceof ISPContainerNode) {
            List children = ((ISPContainerNode) node).getChildren();
            for (Object child : children) {
                getNodeList((ISPNode) child, res);
            }
        }
    }

    /**
     * Returns a list of the nodes under the given container node.
     * @return a list of ISPNode objects
     */
    public static List<ISPNode> getNodeList(ISPNode node) {
        final List<ISPNode> res = new ArrayList<ISPNode>();
        getNodeList(node, res);
        return res;
    }
}

