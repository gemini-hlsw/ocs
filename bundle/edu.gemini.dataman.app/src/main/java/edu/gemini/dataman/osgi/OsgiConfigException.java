//
// $Id: OsgiConfigException.java 104 2005-09-08 14:21:07Z shane $
//

package edu.gemini.dataman.osgi;

/**
 *
 */
final class OsgiConfigException extends Exception {
    public static OsgiConfigException newException(Exception wrapped) {
        if (wrapped instanceof OsgiConfigException) {
            return (OsgiConfigException) wrapped;
        }
        return new OsgiConfigException(wrapped);
    }

    public OsgiConfigException(String msg) {
        super(msg);
    }

    private OsgiConfigException(Exception wrapped) {
        super(wrapped);
    }
}
