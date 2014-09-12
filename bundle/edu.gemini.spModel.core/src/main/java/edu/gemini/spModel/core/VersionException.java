package edu.gemini.spModel.core;

public class VersionException extends Exception {

    // N.B. we're not storing the version objects themselves because they might not be
    // serializable-compatible :-\

    static final long serialVersionUID = 42L;

    public VersionException(Version expected, Version.Compatibility compatability) {
        super(String.format("Incompatible versions (%s): expected %s, actual was (unknown).", compatability, expected));
    }

    public VersionException(Version expected, Version actual, Version.Compatibility compatability) {
        super(String.format("Incompatible versions (%s): expected %s, actual was %s", compatability, expected, actual));
    }

    /** Returns a multi-line message suitable for showing to the user. */
    public String getLongMessage() {
        return getLongMessage("the remote server."); // :-\
    }
        /** Returns a multi-line message suitable for showing to the user. */
    public String getLongMessage(String peerName) {
        return String.format(
            "Your software is incompatible with the service provided by %s.\n" +
            "You probably need to upgrade your software to a more recent version.\n\n" +
            "Details:\n%s", peerName, getMessage());
    }

}
