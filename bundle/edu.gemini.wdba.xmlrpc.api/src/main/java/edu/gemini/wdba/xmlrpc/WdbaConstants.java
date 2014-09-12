package edu.gemini.wdba.xmlrpc;

/**
 * A class that contains shared constants between XML-RPC implementations
 * Gemini Observatory/AURA
 * $Id: WdbaConstants.java 756 2007-01-08 18:01:24Z gillies $
 */
public final class WdbaConstants {
    // To ensure that no instances are created
    private WdbaConstants() {}

    // The context in the shared XML-RPC servlet
    public static final String APP_CONTEXT = "/wdba";

}
