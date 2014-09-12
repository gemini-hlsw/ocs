package edu.gemini.obslog.obslog;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: IObservingLog.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public interface IObservingLog {

    /**
     * Get a list of Log Segments
     *
     * @return The <code>List</code> of <code>IObservingLogSegment</code> objects.
     */
    List<IObservingLogSegment> getLogSegments();

    /**
     * Add an observing log segment to the observing log.
     *
     * @param segment an instance implementing the <tt>IObservingLogSegment</tt> interface.
     */
    void addLogSegment(IObservingLogSegment segment);

    /**
     * Returns the number of segments in the <tt>IObservingLog</tt>
     *
     * @return number of segments
     */
    int getLogSegmentCount();

    /**
     * The parameters that define the users during the night.
     *
     * @return log information
     */
    OlLogInformation getLogInformation();

    /**
     * A list of weather information for this observing log.
     *
     * @return the list of weather information or the empty list if no weather information is available
     */
    IObservingLogSegment getWeatherSegment();

    /**
     * Diagnostic dump of all segments.
     */
    void dump();
}


