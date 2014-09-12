//
//$Id: DBCheckerUtil.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.p2checker.util;

import edu.gemini.pot.sp.*;


import java.util.logging.Logger;


public class DBCheckerUtil {

    private static final Logger LOG = Logger.getLogger(DBCheckerUtil.class.getName());

    private DBCheckerUtil() {
    }

    /**
     * For any given node, it will return the top level node that
     * represent the program
     * @param n any node within a program
     * @return the program node that contains the given <code>ISPNode</code>. If
     * the argument is an <code>ISPProgram</code> it will return the same object.
     * <p/>
     * Return null if the argument is <code>null</code>.
     */
    public static ISPProgram getProgramNode(ISPNode n) {

        if (n == null) return null;
        if (n instanceof ISPProgram) return (ISPProgram) n;
//
//        try {
            return getProgramNode(n.getParent());
//        } catch (RemoteException e) {
//            LOG.log(Level.SEVERE, e.getMessage());
//            throw GeminiRuntimeException.newException(e);
//        }
    }

}
