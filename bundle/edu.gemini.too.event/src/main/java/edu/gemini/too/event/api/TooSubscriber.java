package edu.gemini.too.event.api;


/**
 * An interface for clients of ToO event information.
 */
public interface TooSubscriber {
    void tooObservationReady(TooEvent event);
}
