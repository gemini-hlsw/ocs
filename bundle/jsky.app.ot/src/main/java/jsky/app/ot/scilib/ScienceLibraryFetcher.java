package jsky.app.ot.scilib;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.SPProgramID;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.ProgressPanel;
import jsky.util.gui.SwingWorker;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Frame;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides static mechanisms to retrieve science libraries from the remote database and
 * insert them into the local database (fetch process).
 * </p>
 * UI and non-UI mechanisms are included
 */
final public class ScienceLibraryFetcher {

    private static final Logger LOG = Logger.getLogger(ScienceLibraryFetcher.class.getName());

    /**
     * Presents a dialog to the user to select the instruments for which
     * libraries will be fetched. The {@code listener} object will be invoked
     * with the libraries fetched.
     *
     * @param update   {@code true} if the request will update existing libraries in
     *                 case they are found
     * @param listener ScienceLibraryFetchListener invoked when the libraries
     *                 are fetched.
     * @param owner    The owner of the dialog to be shown
     */
    public static void fetchLibraries(final boolean update, final ScienceLibraryFetchListener listener, final Frame owner) {

        final ScienceLibraryFetcherPanel mainPanel = new ScienceLibraryFetcherPanel();

        final JDialog dialog = mainPanel.createDialog(owner);
        dialog.setResizable(false);
        dialog.setVisible(true);

        if (mainPanel.wasConfirmed()) {
            fetchLibraries(mainPanel.getSelectedItems(), update, listener, owner);
        }
    }

    /**
     * Fetch the libraries associated to the given SPComponentType instruments.
     * The {@code listener} object will be invoked
     * with the libraries fetched. The libraries to be fetched are defined in the
     * {@link jsky.app.ot.scilib.ScienceLibraryInfo} class.
     *
     * @param instruments List of instruments that are queried for libraries
     * @param update      {@code true} if the request will update existing libraries in
     *                    case they are found
     * @param listener    ScienceLibraryFetchListener invoked when the libraries
     *                    are fetched. Can be null.
     * @param parent      parent window to locate dialogs and other UI components. Can be null.
     */

    public static void fetchLibraries(final Collection<SPComponentType> instruments, final boolean update, final ScienceLibraryFetchListener listener, final Component parent) {


        if (instruments == null || instruments.size() <= 0) {
            DialogUtil.error(parent, "No instruments were selected");
            return;
        }

        final Map<SPComponentType, ScienceLibraryInfo> libInfoMap = ScienceLibraryInfo.getLibraryInfoMap();

        // Figure out the current state of all the libraries for the given instruments
        final Map<SPComponentType, List<ScienceLibraryState>> stateMap = _libraryState(instruments);

        //Decide which ones need to be fetched
        final Collection<ScienceLibraryState> toFetch = new ArrayList<>();
        final List<ISPProgram> fetchedLibraries = new ArrayList<>();
        final Collection<SPComponentType> unavailableLibraries = new ArrayList<>();
        final IDBDatabaseService odb = SPDB.get();

        for (final SPComponentType instrument : instruments) {
            final ScienceLibraryInfo info = libInfoMap.get(instrument);
            if (info == null) {
                unavailableLibraries.add(instrument);
                continue;
            }

            final List<ScienceLibraryState> states = stateMap.get(instrument);

            // toFetch ++ if (states.exists(_.checkedOut) && !shouldUpdate) states.filterNot(_.checkedOut) else states

            boolean someCheckedOut = false;
            for (ScienceLibraryState s : states) {
                if (s.checkedOut) {
                    someCheckedOut = true;
                    break;
                }
            }

            if (update && someCheckedOut && DialogUtil.confirm("Libraries for " + instrument.readableStr + " already exist. Do you want to update them?") == JOptionPane.OK_OPTION) {
                toFetch.addAll(states);
            } else {
                for (ScienceLibraryState s : states) {
                    if (s.checkedOut) {
                        fetchedLibraries.add(odb.lookupProgramByID(s.pid));
                    } else {
                        toFetch.add(s);
                    }
                }
            }
        }

        //inform the user that some libraries are not available
        if (unavailableLibraries.size() > 0) {
            final StringBuilder sb = new StringBuilder("No libraries are available for the following instrument");
            if (unavailableLibraries.size() > 1) {
                sb.append("s");
            }
            sb.append(":");

            for (final SPComponentType inst : unavailableLibraries) {
                sb.append("\n\t").append(inst.readableStr);
            }

            DialogUtil.message(sb.toString());
        }

        //Notify the listener with the fetched libraries right away if nothing else is pending
        if (fetchedLibraries.size() > 0 && toFetch.size() == 0 && listener != null) {
            listener.fetchedLibrary(fetchedLibraries);
        }

        if (toFetch.size() == 0) return;// nothing pending, return.

        new SwingWorker() {
            final ProgressPanel pp = ProgressPanel.makeProgressPanel("Fetching Libraries...", parent);

            public Object construct() {
                // in background thread
                pp.start();
                pp.setProgress(0);
                return doFetch(toFetch, pp);
            }

            public void finished() {
                // in event thread
                final Object o = getValue(); // return value from construct()
                if (o instanceof Exception) {
                    pp.stop();
                    LOG.log(Level.SEVERE, "Unexpected Exception", (Throwable) o);
                    final Exception e = (Exception) o;
                    DialogUtil.error(parent, "An unexpected error has occurred. Please try again later. \nError message: " + e.getMessage());
                } else {
                    //lets reconstruct the list
                    final List<ISPProgram> libraries = new ArrayList<>();
                    if (o instanceof List) {
                        final Iterable lib = (Iterable) o;
                        for (final Object obj : lib) {
                            if (obj instanceof ISPProgram) {
                                libraries.add((ISPProgram) obj);
                            }
                        }
                    }
                    //if everything went ok (we have at least one library fetched, and we are updating),
                    //let the user know how to access the libraries
                    if (update && libraries.size() > 0) {
                        pp.stopWithMessage("Use the Libraries button from the " +
                                "toolbar to access the fetched libraries", "Done.", "Done");
                    } else {
                        pp.stop();
                    }
                    if (listener != null) {
                        //add to the libraries the ones already present locally
                        libraries.addAll(fetchedLibraries);
                        //and perform the operation on them
                        listener.fetchedLibrary(libraries);
                    }
                }
            }
        }.start();
    }

    //build a status message for the progress status bar indicating how many libraries are being fetched
    private static String _buildStatusMessage(final ScienceLibraryState state) {
        final StringBuilder sb = new StringBuilder("Fetching ");
        sb.append(state.inst.readableStr).append(" library: ").append(state.pid);
        return sb.toString();

    }

    //Preform the actual fetching of the libraries for the list of instruments,
    //updating the progress panel in the process. The total is used just to compute the
    //percentage of completion that we are getting at.
    private static Object doFetch(final Collection<ScienceLibraryState> states, final ProgressPanel pp) {
        final int total = states.size();

        final Collection<ISPProgram> libraries = new ArrayList<>();
        try {
            int count = 0;
            for (ScienceLibraryState state : states) {
                pp.setText(_buildStatusMessage(state));

                final Peer peer = ScienceLibraryHelper.peerForSite(state.site);
                if (peer != null) {
                    final ISPProgram lib = ScienceLibraryHelper.checkout(peer, state.pid, state.checkedOut);
                    if (lib != null) libraries.add(lib);
                }
                ++count;
                pp.setProgress((int) (count / total * 100.0));

                if (pp.isInterrupted()) return null;
            }
        } catch (Exception e) {
            return e;
        }
        return libraries;
    }


    //Retrieves a map associating instruments to locally found libraries. Used to check what libraries are
    //already present in the local database
    private static Map<SPComponentType, List<ScienceLibraryState>> _libraryState(final Iterable<SPComponentType> instruments) {
        final Map<SPComponentType, ScienceLibraryInfo> map = ScienceLibraryInfo.getLibraryInfoMap();
        final Map<SPComponentType, List<ScienceLibraryState>> stateMap = new HashMap<>();

        final IDBDatabaseService db = SPDB.get();
        for (final SPComponentType instrument : instruments) {
            final ScienceLibraryInfo info = map.get(instrument);
            final List<ScienceLibraryState> instLibraries = new ArrayList<>();

            final Collection<SPProgramID> ps = (info == null) ? Collections.emptyList() : info.getLibraries();
            for (final SPProgramID p : ps) {
                final ISPProgram prog = db.lookupProgramByID(p);
                //noinspection ConstantConditions
                instLibraries.add(new ScienceLibraryState(info.getSite(), instrument, p, prog != null));
            }

            stateMap.put(instrument, instLibraries);
        }

        return stateMap;
    }
}

