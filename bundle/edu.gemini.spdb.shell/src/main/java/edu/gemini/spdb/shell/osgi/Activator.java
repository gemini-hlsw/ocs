package edu.gemini.spdb.shell.osgi;

import edu.gemini.pot.spdb.IDBDatabaseService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.*;
import java.security.Principal;
import edu.gemini.util.security.principal.StaffPrincipal;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<IDBDatabaseService, IDBDatabaseService> {

    private static final String COMMAND_SCOPE    = "osgi.command.scope";
    private static final String COMMAND_FUNCTION = "osgi.command.function";

    private BundleContext context;
    private ServiceTracker<IDBDatabaseService, IDBDatabaseService> tracker;

    public void start(final BundleContext context) throws Exception {
        this.context = context;
        this.tracker = new ServiceTracker<>(context, IDBDatabaseService.class, this);

        tracker.open();

        final Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(COMMAND_SCOPE, "spdb");
        dict.put(COMMAND_FUNCTION, new String[]{
                "lsprogs",
                "lsplans",
                "rmprog",
                "importXml",
                "exportXml",
                "exportOcs3",
                "du",
                "purge",
                "migrateAltair",
                "purgeEphemeris"
        });
        final Set<Principal> user = Collections.singleton(StaffPrincipal.Gemini());
        context.registerService(Commands.class.getName(), new Commands(tracker, user), dict);
        System.out.println("edu.gemini.spdb.shell started.");
    }

    public void stop(BundleContext context) throws Exception {
        tracker.close();
        tracker = null;
        System.out.println("edu.gemini.spdb.shell stopped.");
    }

    public IDBDatabaseService addingService(final ServiceReference<IDBDatabaseService> serviceReference) {
        return context.getService(serviceReference);
    }

    public void modifiedService(final ServiceReference<IDBDatabaseService> serviceReference, final IDBDatabaseService o) {
        // nop
    }

    public void removedService(final ServiceReference<IDBDatabaseService> serviceReference, final IDBDatabaseService o) {
        context.ungetService(serviceReference);
    }
}



