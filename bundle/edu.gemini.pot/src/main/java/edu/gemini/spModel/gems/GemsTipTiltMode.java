package edu.gemini.spModel.gems;

/**
 * The user can constrain the search for tip tilt correction (mascot asterisms) to
 * use only Canopus CWFS, GSAOI, or both.
 *
 * See OT-21
 */
public enum GemsTipTiltMode {
    canopus, instrument, both
}
