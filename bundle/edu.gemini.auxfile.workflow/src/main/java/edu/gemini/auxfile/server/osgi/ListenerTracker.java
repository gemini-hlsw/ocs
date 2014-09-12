//
// $Id: ListenerTracker.java 511 2006-08-02 20:11:22Z shane $
//

package edu.gemini.auxfile.server.osgi;

import edu.gemini.auxfile.api.AuxFileListener;
import edu.gemini.auxfile.server.notify.NotifyingBackend;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Logger;

public final class ListenerTracker extends ServiceTracker<AuxFileListener, AuxFileListener> {
    private static final Logger LOG = Logger.getLogger(ListenerTracker.class.getName());

    private final NotifyingBackend _notifier;

    public ListenerTracker(BundleContext context, NotifyingBackend notifier) {
        super(context, AuxFileListener.class.getName(), null);
        _notifier = notifier;
    }

    @Override
    public AuxFileListener addingService(ServiceReference<AuxFileListener> ref) {
        ListenerTracker.LOG.info("Adding AuxFileListener");
        final AuxFileListener listener = context.getService(ref);
        _notifier.addListener(listener);
        return listener;
    }

    @Override
    public void removedService(ServiceReference<AuxFileListener> ref, AuxFileListener listener) {
        ListenerTracker.LOG.info("Removing AuxFileListener");
        _notifier.removeListener(listener);
        context.ungetService(ref);
    }
}
