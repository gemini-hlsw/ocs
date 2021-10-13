package edu.gemini.spModel.seqcomp;

import edu.gemini.spModel.gemini.ghost.GhostExposureTimeProvider;

/**
 * A Ghost sequence component that supports red and blue exposure time and counts.
 */
public interface GhostExpSeqComponent extends GhostExposureTimeProvider, IObserveSeqComponent {
}
