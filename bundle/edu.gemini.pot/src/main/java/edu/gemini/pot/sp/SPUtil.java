// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPUtil.java 6017 2005-05-02 22:49:39Z shane $
//

package edu.gemini.pot.sp;

import edu.gemini.shared.util.immutable.MapOp;

import java.util.Collection;
import java.util.Iterator;


/**
 * Utility code useful for clients and implementors.
 */
public final class SPUtil {

// Don't allow instances
    private SPUtil() {
    }

    private static String getPropName(String prefix, String key) {
        final StringBuilder buf = new StringBuilder();
        buf.append(prefix).append(':').append(key);
        return buf.toString();
    }

    public static String getClientDataPropertyName(String clientDataKey) {
        return getPropName(ISPNode.CLIENT_DATA_PROP_PREFIX, clientDataKey);
    }

    public static String getTransientClientDataPropertyName(String clientDataKey) {
        return getPropName(ISPNode.TRANSIENT_CLIENT_DATA_PROP_PREFIX, clientDataKey);
    }

    private static final String TRANSIENT_PROP_NAME_PREFIX =
            getPropName(ISPNode.TRANSIENT_CLIENT_DATA_PROP_PREFIX, "");

    public static boolean isTransientClientDataPropertyName(String propertyName) {
        return (propertyName != null) && propertyName.startsWith(TRANSIENT_PROP_NAME_PREFIX);
    }

    public static String getDataObjectPropertyName() {
        return getClientDataPropertyName(ISPNode.DATA_OBJECT_KEY);
    }

    /**
     * Return true only if the two strings are different.  This method will work
     * with null arguments, which is its chief reason for existence since non-null
     * strings may not be compared with <code>String.equals()</code>.
     */
    public static boolean stringsDiffer(String s1, String s2) {
        boolean same = true;
        if (s1 != s2) {
            if ((s1 == null) || (s2 == null)) {
                same = false;
            } else {
                same = s1.equals(s2);
            }
        }
        return !same;
    }

    /**
     * Prints the given <code>Collection</code> to <code>stdout</code> for
     * debugging.
     */
    public void printCollection(Collection col, String title) {
        System.out.println("--- " + title + " ---");
        Iterator it = col.iterator();
        while (it.hasNext()) {
            System.out.println("\t" + it.next());
        }
        System.out.println("---------------------");
    }

    public static <N extends ISPNode,T> T readLocking(N node, MapOp<N,T> op) {
        node.getProgramReadLock();
        try {
            return op.apply(node);
        } finally {
            node.returnProgramReadLock();
        }
    }

    public static <N extends ISPNode,T> T writeLocking(N node, MapOp<N,T> op) {
        node.getProgramWriteLock();
        try {
            return op.apply(node);
        } finally {
            node.returnProgramWriteLock();
        }
    }
}
