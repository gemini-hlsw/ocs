package edu.gemini.spModel.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionException extends Exception {

    // N.B. we're not storing the version objects themselves because they might not be
    // serializable-compatible :-\

    static final long serialVersionUID = 42L;

    private static final Pattern matcher = Pattern.compile(".*: expected ([\\w|\\p{Punct}]*), .*");

    public VersionException(Version expected, Version.Compatibility compatibility) {
        super(String.format("Incompatible versions (%s): expected %s, actual was (unknown).", compatibility, expected));
    }

    public VersionException(Version expected, Version actual, Version.Compatibility compatibility) {
        super(String.format("Incompatible versions (%s): expected %s, actual was %s", compatibility, expected, actual));
    }

    /** Returns a multi-line message suitable for showing to the user. */
    public String getLongMessage() {
        return getLongMessage("the remote server."); // :-\
    }

    /** Returns an html-based message for displaying to the user */
    public String getHtmlMessage(String peerName) {
        // We cannot transmit the actual version created on the server, the only hope to extract
        // that information is to parse the content of the error message
        Matcher match = matcher.matcher(getMessage());
        String remoteVersion = "Unknown";
        if (match.matches()) {
            remoteVersion = match.group(1);
        }
        return String.format(
            "Your software is incompatible with the service provided by %s.<br/>" +
            "To upgrade to the latest version please go to <br/>" +
            "<a href=\"http://www.gemini.edu/node/11172\">http://www.gemini.edu/node/11172</a><br/><br/>" +
            "Details:<br/>Local OT version: %s<br/>Server version: %s", peerName, CurrentVersion.get().toString(), remoteVersion);
    }

    /** Returns a multi-line message suitable for showing to the user. */
    public String getLongMessage(String peerName) {
        return String.format(
            "Your software is incompatible with the service provided by %s.\n" +
            "You probably need to upgrade your software to a more recent version.\n\n" +
            "Details:\n%s", peerName, getMessage());
    }

}
