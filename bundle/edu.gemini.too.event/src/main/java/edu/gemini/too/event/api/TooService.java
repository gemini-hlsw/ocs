package edu.gemini.too.event.api;

import java.util.List;

/**
 * The ToO Service public interface.
 */
public interface TooService {

    /**
     * Gets the time of the last ToO event according to the server's clock.
     * Clients should use this method to get the initial timestamp with which
     * to poll.  If they use their own clock to produce a timestamp and the two
     * clocks are not exactly in sync, then the client could miss events.
     *
     * @return server's timestamp when last ToO event happened
     */
    TooTimestamp lastEventTimestamp();

    /**
     * Minimum amount of time that the service will retain ToO events in
     * milliseconds.  If the client polls for events less frequently than this,
     * it could miss ToO events.
     *
     * @return minimum time that the ToO service will remember a past ToO
     * event
     */
    long eventRetentionTime();

    /**
     * Gets all the TooEvents kept by the service and visible to the caller
     * since the given time, if any.
     * @param since  events before and on this timestamp are filtered from
     *               the results
     * @return all TooEvents known to the service that have happened since
     * the given time
     */
    List<TooEvent> events(TooTimestamp since);
}
