package edu.gemini.ui.workspace.osgi;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IShellAdvisor;
import edu.gemini.ui.workspace.impl.Shell;
import edu.gemini.ui.workspace.impl.Workspace;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<IShellAdvisor, Shell> {

    private static final Logger LOGGER = Logger.getLogger(Activator.class.getName());

    private Workspace workspace;
    private BundleContext context;
    private ServiceTracker<IShellAdvisor, Shell> tracker;

    public void start(BundleContext context) throws Exception {
        this.context = context;

        // The workspace
        workspace = new Workspace(context);
        workspace.open();

        // Track IShellAdvisor instances. We will create a Shell for each one
        // that we encounter.
        tracker = new ServiceTracker<>(context, IShellAdvisor.class.getName(), this);
        tracker.open();

    }

    public void stop(BundleContext context) throws Exception {

        // Close everything, in opposite order.
        tracker.close();
        workspace.close();

        // And let everything be GC'd
        workspace = null;
        tracker = null;
        this.context = null;

    }

    @Override
    public Shell addingService(ServiceReference<IShellAdvisor> ref) {

        // Open a shell for the advisor. Note that opening a shell causes its
        // advisor to be opened, and closing a shell closes the advisor. However
        // closing the shell's JFrame peer may also cause the advisor to be
        // closed (as an optimization). So the advisor's close event may happen
        // prior to the call to shell.close() below.
        IShellAdvisor advisor = context.getService(ref);
        Shell shell = workspace.createShell(advisor);
        shell.open();
        return shell;

    }

    @Override
    public void modifiedService(ServiceReference<IShellAdvisor> ref, Shell shell) {
        LOGGER.info("No action taken for modifiedService(" + ref + ", " + shell + ")");
    }

    @Override
    public void removedService(ServiceReference<IShellAdvisor> ref, Shell shell) {

        // See note in addingService() above; the advisor may have been closed
        // prior to this call to shell.close() due to the user closing the
        // shell's JFrame peer.
        shell.close();
        context.ungetService(ref);
    }

}
