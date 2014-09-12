package edu.gemini.wdba.session;

import edu.gemini.wdba.session.dbup.AllEventsLoggingService;

//
// Gemini Observatory/AURA
// $Id: TestSessionConfiguration.java 756 2007-01-08 18:01:24Z gillies $
//

public class TestSessionConfiguration implements ISessionConfiguration {

    public void initialize(ISessionEventProducer producer) {

        // Start a service that logs all events to LOG4j
        new AllEventsLoggingService(producer);
    }

}
