//
// $
//

package edu.gemini.dataman.gsa;

/**
 * Embodies all the exceptions that can happen when trying to determine the
 * GsaFileStatus.
 */
public final class GsaFileStatusException extends Exception {
    public GsaFileStatusException(Exception wrapped) {
        super(wrapped);
    }
}
