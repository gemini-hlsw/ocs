package edu.gemini.wdba.session;

import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.session.dbup.DBUpdateService;
import edu.gemini.wdba.session.dbup.AllEventsLoggingService;

//
// Gemini Observatory/AURA
// $Id: ProductionSessionConfiguration.java 838 2007-05-06 05:10:20Z gillies $
//

public final class ProductionSessionConfiguration implements ISessionConfiguration {

    private final WdbaContext ctx;

    public ProductionSessionConfiguration(WdbaContext ctx) {
        this.ctx = ctx;
    }

    public void initialize(ISessionEventProducer producer) {

        // Start the service that logs events to the database
        new DBUpdateService(producer, ctx);

        // Start a service that logs all events to LOG4j
        new AllEventsLoggingService(producer);
    }

}
