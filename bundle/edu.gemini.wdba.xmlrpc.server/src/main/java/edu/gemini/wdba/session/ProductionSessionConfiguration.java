package edu.gemini.wdba.session;

import edu.gemini.wdba.glue.api.WdbaContext;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//
// Gemini Observatory/AURA
// $Id: ProductionSessionConfiguration.java 838 2007-05-06 05:10:20Z gillies $
//

public final class ProductionSessionConfiguration implements ISessionConfiguration {

    private final WdbaContext ctx;

    private final Executor exec =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "DBUpdateService"));

    public ProductionSessionConfiguration(WdbaContext ctx) {
        this.ctx = ctx;
    }

    public void initialize(ISessionEventProducer producer) {

        // Start the service that logs events to the database
        final DBUpdateService dbUpdateService = new DBUpdateService(ctx);
        producer.addSessionEventListener(dbUpdateService);

        exec.execute(dbUpdateService);
    }

}
