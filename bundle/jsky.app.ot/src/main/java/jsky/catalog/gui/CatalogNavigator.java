package jsky.catalog.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.HTMLQueryResultHandler;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.URLQueryResult;
import jsky.html.HTMLViewerFrame;
import jsky.html.HTMLViewerInternalFrame;
import jsky.util.*;
import jsky.util.gui.*;

/**
 * Used to navigate the catalog hierarchy. This class displays a tree of catalogs in one
 * panel and the interface for searching the catalog, or the query results in the other panel.
 * <p/>
 * The tree display is based on a top level catalog directory. The details must be defined
 * in a derived class.
 */
@Deprecated
public abstract class CatalogNavigator extends JPanel
        implements QueryResultDisplay, GenericToolBarTarget, HTMLQueryResultHandler {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(CatalogNavigator.class);

    // The top level parent frame (or internal frame) used to close the window
    private Component _parent;

    // The root catalog directory to use
    private static CatalogDirectory _catDir;

    // Set this to the JDesktopPane, if using internal frames.
    private JDesktopPane _desktop = null;

    // Displays the catalog tree and the catalog query widgets
    private JPanel _queryPanel;

    // Displays query results, such as tabular data.
    private JPanel _resultPanel;

    // Query panel currently being displayed
    private JComponent _queryComponent;

    // Result panel currently being displayed
    private JComponent _resultComponent;

    // The original URL for the display component's data (for history list)
    private URL _origURL;

    // reuse file chooser widget for open
    private static JFileChooser _fileChooser;

    // reuse file chooser widget for saveAs
    private static JFileChooser _saveFileChooser;

    // Panel used to display download progress information
    private ProgressPanel _progressPanel;

    // list of listeners for change events
    private EventListenerList _listenerList = new EventListenerList();

    // Saved query result (set in background thread)
    private QueryResult _queryResult;

    // Optional object to use to plot table data
    private TablePlotter _plotter;

    // Utility object used to control background thread
    private SwingWorker _worker;

    // Top level window (or internal frame) for viewing an HTML page
    private Component _htmlViewerFrame;

    // Manages a list of settings for stored querries, so that you can repeat the query later on
    private CatalogQueryList _queryList;

    // Maps query components to their corresponding result components
    private Hashtable<JComponent, JComponent> _queryResultComponentMap = new Hashtable<>();

    // The pane dividing the query and the results panel
    private JSplitPane _resultSplitPane;

    // Action to use for the "Open..." menu and toolbar items
    private AbstractAction _openAction = new AbstractAction(_I18N.getString("open")) {
        public void actionPerformed(ActionEvent evt) {
            open();
        }
    };

    // Action to use for the "Save as..." menu and toolbar items
    private AbstractAction _saveAsAction = new AbstractAction(_I18N.getString("saveAs")) {
        public void actionPerformed(ActionEvent evt) {
            saveAs();
        }
    };

    // Action to use for the "Save With Image..." menu and toolbar items
    private AbstractAction _saveWithImageAction = new AbstractAction(_I18N.getString("saveCatalogWithImage")) {
        public void actionPerformed(ActionEvent evt) {
            saveWithImage();
        }
    };

    // Action to use for the "Save as HTML..." menu and toolbar items
    private AbstractAction _saveAsHTMLAction = new AbstractAction(_I18N.getString("saveAsHTML")) {
        public void actionPerformed(ActionEvent evt) {
            saveAsHTML();
        }
    };

    // Action to use for the "Print..." menu and toolbar items
    private AbstractAction _printAction = new AbstractAction(_I18N.getString("print") + "...") {
        public void actionPerformed(ActionEvent evt) {
            print();
        }
    };

    // Action to use for the "Add Row" menu item
    private AbstractAction _addRowAction = new AbstractAction(_I18N.getString("addRow")) {
        public void actionPerformed(ActionEvent evt) {
            addRow();
        }
    };

    // Action to use for the "Delete Rows..." menu item
    private AbstractAction _deleteSelectedRowsAction = new AbstractAction(_I18N.getString("deleteSelectedRows")) {
        public void actionPerformed(ActionEvent evt) {
            deleteSelectedRows();
        }
    };

    // Action to use for the "Query => Store => New Query..." menu item
    private AbstractAction _storeNewQueryAction = new AbstractAction(_I18N.getString("new")) {
        public void actionPerformed(ActionEvent evt) {
            storeNewQuery();
        }
    };

    // Action to use for the "Query => Delete => All" menu item
    private AbstractAction _deleteAllQueryAction = new AbstractAction(_I18N.getString("all")) {
        public void actionPerformed(ActionEvent evt) {
            clearQueryList();
        }
    };

    /**
     * Construct a CatalogNavigator using the given CatalogTree widget
     * (Call setQueryResult to set the root catalog to display).
     *
     * @param parent      the parent component
     */
    public CatalogNavigator(Component parent) {
        _parent = parent;
        setLayout(new BorderLayout());

        _queryPanel = new JPanel();
        _queryPanel.setLayout(new BorderLayout());

        _resultPanel = new JPanel();
        _resultPanel.setLayout(new BorderLayout());

        _resultSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _queryPanel, _resultPanel);
        _resultSplitPane.setOneTouchExpandable(true);
        _resultSplitPane.setDividerLocation(270);
        add(_resultSplitPane, BorderLayout.CENTER);

        _queryList = new CatalogQueryList();
    }

    /**
     * Construct a CatalogNavigator using the given CatalogTree widget
     * and TablePlotter
     * (Call setQueryResult to set the root catalog to display).
     *
     * @param parent      the parent component
     * @param plotter     the object to use to plot catalog table data
     *                    (when the plot button is pressed)
     */
    public CatalogNavigator(Component parent, TablePlotter plotter) {
        this(parent);
        _plotter = plotter;
    }

    /**
     * Return the object used to plot table data, or null if none was defined.
     */
    public TablePlotter getPlotter() {
        return _plotter;
    }

    /**
     * Set the object used to plot table data.
     */
    public void setPlotter(TablePlotter tp) {
        _plotter = tp;
    }

    /**
     * Set the query or result component to display. The choice is made based on
     * which interfaces the component implements. If the component implements
     * QueryResultDisplay, it is considered a result component.
     */
    public void setComponent(JComponent component) {
        if (component instanceof QueryResultDisplay) {
            setResultComponent(component);
        } else {
            setQueryComponent(component);

            if ((component instanceof CatalogQueryTool) && (((CatalogQueryTool) component).getCatalog().isLocal())) {
                ((CatalogQueryTool) component).search();
            }
        }
    }

    /**
     * Set the query component to display
     */
    public void setQueryComponent(JComponent component) {
        if (component == null || component == _queryComponent)
            return;
        if (_queryComponent != null) {
            _queryPanel.remove(_queryComponent);
            _queryComponent = null;
        }
        _queryComponent = component;
        /*
        Catalog cat = _catalogTree.getSelectedNode();
        if (cat != null)
            _panelTreeNodeTable.put(_queryComponent, cat);*/
        _queryPanel.add(_queryComponent, BorderLayout.CENTER);

        // restore the query result corresponding to this catalog, if known
        Object resultComp = _queryResultComponentMap.get(_queryComponent);
        if (resultComp == null)
            setResultComponent(new EmptyPanel());
        else
            setResultComponent((JComponent) resultComp);
        update();
    }

    /**
     * Return the panel currently being displayed
     */
    public JComponent getQueryComponent() {
        return _queryComponent;
    }

    /**
     * Set the result component to display
     */
    public void setResultComponent(JComponent component) {
        if (component == null || component == _resultComponent)
            return;
        if (_resultComponent != null) {
//            if (_resultComponent instanceof TableDisplayTool) {
            // if we're not reusing the current table window, tell it to hide any related popup
            // windows before replacing it (It might be needed again later though, if the user
            // goes back to it).
            //((TableDisplayTool)_resultComponent).hidePopups();
//            }
            _resultPanel.remove(_resultComponent);
            _resultComponent = null;
        }
        _resultComponent = component;
        if (_queryComponent != null)
            _queryResultComponentMap.put(_queryComponent, _resultComponent);
        _resultPanel.add(_resultComponent, BorderLayout.CENTER);
        update();
        _resultComponentChanged();

        // try to display the right amount of the query window
        SwingUtilities.invokeLater(_resultSplitPane::resetToPreferredSizes);
    }

    /**
     * Return the panel currently being displayed
     */
    public JComponent getResultComponent() {
        return _resultComponent;
    }

    /**
     * Called whenever the display component is changed
     */
    protected void _resultComponentChanged() {
        // set the state of the "Save As..." menu item
        _saveAsAction.setEnabled(_resultComponent instanceof Saveable);
        _printAction.setEnabled(_resultComponent instanceof PrintableWithDialog);
        boolean isTable = (_resultComponent instanceof TableDisplayTool);
        _saveWithImageAction.setEnabled(isTable);
        _deleteSelectedRowsAction.setEnabled(isTable);
        _addRowAction.setEnabled(isTable);
        fireChange(new ChangeEvent(this));
    }

    /**
     * Register to receive change events from this object whenever a new
     * query result is displayed.
     */
    public void addChangeListener(ChangeListener l) {
        _listenerList.add(ChangeListener.class, l);
    }

    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        _listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Notify any listeners that a new query result is being displayed.
     */
    protected void fireChange(ChangeEvent e) {
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(e);
            }
        }
    }

    /**
     * Set the original URL for the current catalog or table.
     *
     * @param url the URL of the catalog, table or FITS file
     */
    public void setOrigURL(URL url) {
        _origURL = url;
    }

    /**
     * Update the layout after a new component has been inserted
     */
    protected void update() {
        //updateTreeSelection();
        _queryPanel.revalidate();
        _resultPanel.revalidate();
        _parent.repaint();
    }

    /**
     * Display the given query result.
     */
    public void setQueryResult(QueryResult queryResult) {
        setQueryResult(queryResult, true);

    }

    /**
     * Display the given query result.
     *
     * @param runInBg if true, run in a background thread if the resource is not local
     */
    public void setQueryResult(QueryResult queryResult, boolean runInBg) {
        if (queryResult == null) {
            return;
        }
        if (_worker != null) {
            // shouldn't happen if user interface disables it
            DialogUtil.error(_I18N.getString("queryInProgress"));
            return;
        }

        // result is a URL, get the data in a background thread
        _queryResult = queryResult;

        // Use a background thread for remote catalog access only
        boolean isLocal = true;
        if (queryResult instanceof URLQueryResult) {
            URLQueryResult uqr = (URLQueryResult) queryResult;
            URL url = uqr.getURL();
            isLocal = (url.getProtocol().equals("file"));
        } else if (queryResult instanceof Catalog) {
            isLocal = ((Catalog) queryResult).isLocal();
        }
        if (isLocal || !runInBg) {
            // Its not a URL, so do it in the foreground
            setComponent(makeQueryResultComponent(queryResult));
        } else {
            // remote catalog: run in a separate thread, so the user can monitor progress
            makeProgressPanel();
            _worker = new SwingWorker() {
                JComponent component;

                public Object construct() {
                    component = makeQueryResultComponent();
                    return component;
                }

                public void finished() {
                    _worker = null;
                    _progressPanel.stop();
                    setComponent(component);
                }
            };
            _worker.start();
        }
    }

    /**
     * If it does not already exist, make the panel used to display
     * the progress of network access.
     */
    protected void makeProgressPanel() {
        if (_progressPanel == null) {
            _progressPanel = ProgressPanel.makeProgressPanel(_I18N.getString("accessingCatalogServer"));
            _progressPanel.addActionListener(e -> {
                if (_worker != null) {
                    _worker.interrupt();
                    _worker = null;
                }
            });
        }
    }

    /**
     * Create and return a component displaying the given query result
     */
    protected JComponent makeQueryResultComponent() {
        return makeQueryResultComponent(_queryResult);
    }

    /**
     * Create and return a JComponent displaying the given query result.
     */
    protected JComponent makeQueryResultComponent(QueryResult queryResult) {
        _origURL = null;
        try {
            // See if there is a user interface handler for the query result
            if (queryResult instanceof CatalogUIHandler) {
                JComponent c = ((CatalogUIHandler) queryResult).makeComponent(this);
                if (c != null)
                    return c;
            }

            // No UI handler, return the default component for the query result
            if (queryResult instanceof CatalogDirectory) {
                return makeCatalogDirectoryComponent((CatalogDirectory) queryResult);
            }
            if (queryResult instanceof TableQueryResult) {
                return makeTableQueryResultComponent((TableQueryResult) queryResult);
            }
            if (queryResult instanceof Catalog) {
                return makeCatalogComponent((Catalog) queryResult);
            }
            if (queryResult instanceof URLQueryResult) {
                URL url = ((URLQueryResult) queryResult).getURL();
                return makeURLComponent(url);
            }
        } catch (Exception e) {
            if (_progressPanel != null)
                _progressPanel.stop();
            DialogUtil.error(e);
        }
        return new EmptyPanel();
    }

    /**
     * Return a new JComponent displaying the contents of the given catalog directory
     */
    protected JComponent makeCatalogDirectoryComponent(CatalogDirectory catalogDirectory) {
        // get the number of catalogs in the directory
        int numCatalogs = catalogDirectory.getNumCatalogs();
        if (numCatalogs == 0)
            return makeCatalogComponent(catalogDirectory);
        if (numCatalogs == 1)
            return makeCatalogComponent(catalogDirectory.getCatalog(0));
        return new EmptyPanel();
    }

    /**
     * Return a new JComponent displaying the contents of the given table query result.
     */
    protected JComponent makeTableQueryResultComponent(TableQueryResult tableQueryResult) {
        if (_resultComponent instanceof TableDisplayTool) {
            TableDisplayTool tdt = (TableDisplayTool) _resultComponent;
            if (tdt.getTable().getName().equals(tableQueryResult.getName())) {
                tdt.setQueryResult(tableQueryResult);
                return tdt;
            }
        }
        TableDisplayTool t = new TableDisplayTool(tableQueryResult, this, _plotter);

        // add a popup menu to the table
        makeTablePopupMenu(t);
        return t;
    }

    /**
     * Add a popup menu to the given TableDisplayTool
     */
    protected void makeTablePopupMenu(TableDisplayTool t) {
        final JPopupMenu m = new JPopupMenu();
        m.add(_addRowAction);
        m.add(_deleteSelectedRowsAction);
        t.getTableDisplay().getTable().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    m.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    m.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * Return a new JComponent displaying the contents of (or the interface for searching)
     * the given catalog
     */
    protected JComponent makeCatalogComponent(Catalog catalog) {
        // catalog may contain multiple tables and implement the CatalogDirectory interface
        if (catalog instanceof CatalogDirectory) {
            CatalogDirectory catalogDirectory = (CatalogDirectory) catalog;
            int numCatalogs = catalogDirectory.getNumCatalogs();
            if (numCatalogs == 1) {
                Catalog c = catalogDirectory.getCatalog(0);
                if (c instanceof TableQueryResult) {
                    return makeTableQueryResultComponent((TableQueryResult) c);
                } else {
                    DialogUtil.error(_I18N.getString("subCatalogError") + ": " + c);
                    return new EmptyPanel();
                }
            } else if (numCatalogs > 1) {
                return makeTableQueryResultComponent(catalogDirectory.getCatalogList());
            }
        }
        if (catalog instanceof TableQueryResult)
            return makeTableQueryResultComponent((TableQueryResult) catalog);

        // Default to normal catalog query component
        return makeCatalogQueryTool(catalog);
    }

    /**
     * Make a panel for querying a catalog
     */
    protected CatalogQueryTool makeCatalogQueryTool(Catalog catalog) {
        return new CatalogQueryTool(catalog, this);
    }

    /**
     * Return a new JComponent displaying the contents of the given URL.
     */
    protected JComponent makeURLComponent(URL url) throws IOException {
        try {
            URLConnection connection;
            if (url.getProtocol().equals("file")) {
                connection = url.openConnection();
            } else {
//                // XXX REL-201: This prevents a bug related to showing the modal progress bar dialog
//                // when its parent frame is invisible
//                if (!_parent.isShowing()) {
//                    if (!SwingUtilities.isEventDispatchThread()) {
//                        SwingUtilities.invokeAndWait(new Runnable() {
//                            @Override
//                            public void run() {
//                                SwingUtil.showFrame(_parent);
//                            }
//                        });
//                    }
//                }

                connection = _progressPanel.openConnection(url);
            }
            if (connection == null)
                return _queryComponent;
            String contentType = connection.getContentType();
            if (contentType == null)
                contentType = "unknown";
            return makeURLComponent(url, contentType);
        } catch (ProgressException e) {
            // ignore: user pressed the stop button in the progress panel
        } catch (Exception e) {
            DialogUtil.error(e);
        }
        if (_resultComponent != null) {
            return _resultComponent;
        }
        return new EmptyPanel();
    }

    /**
     * Return a new JComponent displaying the contents of the given URL.
     */
    protected JComponent makeURLComponent(URL url, String contentType) throws IOException {
        String filename = url.getFile();
        if ((contentType.indexOf("text/html") == 0) || filename.endsWith(".html")) {
            displayHTMLPage(url);
            return _resultComponent;
        }
        if (contentType.indexOf("text/plain") == 0) {
            displayPlainText(url);
            return _resultComponent;
        }

        // If it is not one of the known content types, call a method that may be
        // redefined in a derived class to handle that type
        return makeUnknownURLComponent();
    }


    /* XXX Attempt to show a URL in the default web browser and return true if successful.
    protected boolean displayHTMLPageWithDefaultBrowser(URL url) {
	//  XXX (a) JNLP only works when in a Java WebStart client...
	try {
	    // Lookup the javax.jnlp.BasicService object
	    BasicService bs = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
	    // Invoke the showDocument method
	    return bs.showDocument(url);
	} catch(UnavailableServiceException e) {
	    e.printStackTrace();
	    // Service is not supported
	    return false;
	}

	// XXX (b) works only if netscape is installed and in the exec path (not likely under Windows)

	// for convenience, try to load the HTML page in netscape first, and if
	// that fails, use a Java based HTML viewer
	try {
	    String[] cmd = new String[] {
		"netscape", "-remote", "openURL(" + url.toString() + ",new-window)"
	    };
	    Process process = Runtime.getRuntime().exec(cmd);
	    process.waitFor();
	    InputStream stderr = process.getErrorStream();
	    if (stderr.available() > 3)
		throw new RuntimeException("netscape not running");
	}
	catch(Exception e) {
	    return new HTMLResultViewer(_parent, url);
	}

	return false;
    }
    XXX */


    /**
     * Display the given HTML URL in a popup window containing a JEditorPane.
     */
    public void displayHTMLPage(URL url) {
        //if (displayHTMLPageWithDefaultBrowser(url))
        //    return;

        if (_htmlViewerFrame != null) {
            if (_htmlViewerFrame instanceof HTMLViewerFrame) {
                ((HTMLViewerFrame) _htmlViewerFrame).getHTMLViewer().setPage(url);
                ((HTMLViewerFrame) _htmlViewerFrame).setState(Frame.NORMAL);
                _htmlViewerFrame.setVisible(true);
            } else if (_htmlViewerFrame instanceof HTMLViewerInternalFrame) {
                ((HTMLViewerInternalFrame) _htmlViewerFrame).getHTMLViewer().setPage(url);
                _htmlViewerFrame.setVisible(true);
            }
            return;
        }
        if (_desktop != null) {
            _htmlViewerFrame = new HTMLViewerInternalFrame();
            ((HTMLViewerInternalFrame) _htmlViewerFrame).getHTMLViewer().setPage(url);
            _desktop.add(_htmlViewerFrame, JLayeredPane.DEFAULT_LAYER);
            _desktop.moveToFront(_htmlViewerFrame);
        } else {
            _htmlViewerFrame = new HTMLViewerFrame();
            ((HTMLViewerFrame) _htmlViewerFrame).getHTMLViewer().setPage(url);
        }
    }

    /**
     * Display the text pointed to by the given URL.
     */
    public void displayPlainText(URL url) {
        try {
            String msg = FileUtil.getURL(url);
            if (_progressPanel != null)
                _progressPanel.stop();
            if (msg.length() < 256)
                DialogUtil.error(msg);
            else
                displayHTMLPage(url);
        } catch (IOException e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Return a new JComponent displaying the contents of the given URL.
     * A null return value causes an empty panel to be displayed.
     * Returning the current component (_resultComponent) will cause no change.
     * This should be done if the URL is displayed in a separate window.
     */
    protected JComponent makeUnknownURLComponent() {
        if (_resultComponent != null)
            return _resultComponent;
        return new EmptyPanel();
    }

    /**
     * Display a file chooser to select a local catalog file to open
     */
    public void open() {
        if (_fileChooser == null) {
            _fileChooser = makeFileChooser();
        }
        int option = _fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION && _fileChooser.getSelectedFile() != null) {
            open(_fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * Create and return a new file chooser to be used to select a local file
     * to open.
     */
    protected JFileChooser makeFileChooser() {
        return new JFileChooser(new File("."));
    }

    /**
     * Create and return a new file chooser to be used for saving to a file.
     */
    protected JFileChooser makeSaveFileChooser() {
        return new JFileChooser(new File("."));
    }

    /**
     * Open the given file or URL
     */
    public void open(String fileOrUrl) {
        try {
            setQueryComponent(new EmptyPanel());
            URL url = FileUtil.makeURL(null, fileOrUrl);
            URLQueryResult _queryResult = new URLQueryResult(url);
            setQueryResult(_queryResult);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Exit the application with the given status.
     */
    public void exit() {
        System.exit(0);
    }

    /**
     * Close the window
     */
    public void close() {
        if (_parent != null)
            _parent.setVisible(false);
    }

    // These are for the GenericToolBarTarget interface
    public AbstractAction getOpenAction() {
        return _openAction;
    }

    public AbstractAction getSaveAsAction() {
        return _saveAsAction;
    }

    public AbstractAction getSaveAsHTMLAction() {
        return _saveAsHTMLAction;
    }

    public AbstractAction getSaveWithImageAction() {
        return _saveWithImageAction;
    }

    public AbstractAction getPrintAction() {
        return _printAction;
    }

    public AbstractAction getAddRowAction() {
        return _addRowAction;
    }

    public AbstractAction getDeleteSelectedRowsAction() {
        return _deleteSelectedRowsAction;
    }

    public AbstractAction getStoreNewQueryAction() {
        return _storeNewQueryAction;
    }

    public AbstractAction getDeleteAllQueryAction() {
        return _deleteAllQueryAction;
    }

    /**
     * Display a dialog to enter a URL to display
     */
    public void openURL() {
        String urlStr = DialogUtil.input(_I18N.getString("enterURLDisplay") + ":");
        if (urlStr != null) {
            URL url;
            try {
                url = new URL(urlStr);
            } catch (Exception e) {
                DialogUtil.error(e);
                return;
            }
            setQueryResult(new URLQueryResult(url));
        }
    }

    /**
     * Clear the display.
     */
    public void clear() {
        setQueryComponent(new EmptyPanel());
        _origURL = null;
    }

    /**
     * Pop up a dialog to ask the user for a file name, and then save the current query result
     * to the selected file.
     */
    public void saveAs() {
        if (_resultComponent instanceof SaveableWithDialog) {
            ((SaveableWithDialog) _resultComponent).saveAs();
        } else {
            DialogUtil.error(_I18N.getString("saveNotSupportedForObjType"));
        }
    }

    /**
     * Save the current table as a FITS table in the current FITS image
     * (Should be defined in a derived class).
     */
    public void saveWithImage() {
    }

    /**
     * Pop up a dialog to ask the user for a file name, and then save the current query result
     * to the selected file in HTML format.
     */
    public void saveAsHTML() {
        if (_resultComponent instanceof SaveableAsHTML) {
            if (_saveFileChooser == null) {
                _saveFileChooser = makeSaveFileChooser();
            }
            int option = _saveFileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION && _saveFileChooser.getSelectedFile() != null) {
                saveAsHTML(_saveFileChooser.getSelectedFile().getAbsolutePath());
            }
        } else {
            DialogUtil.error(_I18N.getString("htmlOutputNotSupportedForObjType"));
        }
    }

    /**
     * Save the current query result to the selected file in HTML format.
     */
    public void saveAsHTML(String filename) {
        if (_resultComponent instanceof SaveableAsHTML) {
            try {
                ((SaveableAsHTML) _resultComponent).saveAsHTML(filename);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        } else {
            DialogUtil.error(_I18N.getString("htmlOutputNotSupportedForObjType"));
        }
    }

    /**
     * Pop up a dialog for printing the query results.
     */
    public void print() {
        if (_resultComponent instanceof PrintableWithDialog) {
            try {
                ((PrintableWithDialog) _resultComponent).print();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        } else {
            DialogUtil.error(_I18N.getString("printingNotSupportedForObjType"));
        }
    }

    /**
     * If a table is being displayed, add an empty row in the table.
     */
    public void addRow() {
        if (_resultComponent instanceof TableDisplayTool) {
            ((TableDisplayTool) _resultComponent).addRow();
        }
    }

    /**
     * If a table is being displayed, delete the selected rows.
     */
    public void deleteSelectedRows() {
        if (_resultComponent instanceof TableDisplayTool) {
            ((TableDisplayTool) _resultComponent).deleteSelectedRows();
        }
    }

    /**
     * Set the editable state of the cells in the displayed table.
     */
    public void setTableCellsEditable(boolean b) {
        if (_resultComponent instanceof TableDisplayTool) {
            ((TableDisplayTool) _resultComponent).setTableCellsEditable(b);
        }
    }

    /**
     * Used to identify an empty query or result panel
     */
    public class EmptyPanel extends JPanel implements QueryResultDisplay {

        public void setQueryResult(QueryResult queryResult) {
            throw new RuntimeException(_I18N.getString("queryResultDisplayError"));
        }
    }

    /**
     * Ask the user for a name, and then store the current query and display settings
     * under that name for later use.
     */
    public void storeNewQuery() {
        String name = DialogUtil.input(this, _I18N.getString("enterNameForQuery"));
        if (name == null || name.length() == 0)
            return;
        storeQuery(name);
    }

    /**
     * Store the current query and display settings under the given name for later use.
     */
    public void storeQuery(String name) {
        if (_queryComponent instanceof Storeable) {
            try {
                Object queryInfo = ((Storeable) _queryComponent).storeSettings();
                Object resultInfo = null;
                if (_resultComponent instanceof Storeable)
                    resultInfo = ((Storeable) _resultComponent).storeSettings();
                CatalogQueryItem item = new CatalogQueryItem(name, queryInfo, resultInfo);
                _queryList.add(item);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    }

    /**
     * Delete the named query and display settings.
     */
    public void deleteQuery(String name) {
        _queryList.remove(name);
    }


    /**
     * Remove all items from the query list.
     */
    public void clearQueryList() {
        _queryList.clear();
    }

    /**
     * Add Query items (for previously stored queries) to the given menu,
     * using the given listener, if supplied, otherwise the default (restore the query).
     */
    public void addQueryMenuItems(JMenu menu, ActionListener l) {
        Iterator it = _queryList.iterator();
        if (l == null) {
            while (it.hasNext()) {
                menu.add((CatalogQueryItem) it.next());
            }
        } else {
            while (it.hasNext()) {
                CatalogQueryItem item = (CatalogQueryItem) it.next();
                JMenuItem menuItem = new JMenuItem(item.getName());
                menuItem.addActionListener(l);
                menu.add(menuItem);
            }
        }
    }

}

