package jsky.navigator;

import jsky.catalog.Catalog;
import jsky.catalog.FieldDesc;
import jsky.catalog.TableQueryResult;
import jsky.catalog.astrocat.AstroCatConfig;
import jsky.catalog.gui.*;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.catalog.skycat.SkycatTable;
import jsky.image.gui.MainImageDisplay;
import jsky.image.gui.PickObjectStatistics;
import jsky.util.Preferences;
import jsky.util.gui.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;

/**
 * Extends CatalogNavigator to include support for displaying images
 * and plotting catalog data in images.
 */
public class Navigator extends CatalogNavigator implements CatalogNavigatorOpener {

    // Used for message logging
    private static Logger LOG = Logger.getLogger(Navigator.class.getName());

    // An optional object that can be used to create the image display frame when needed.
    private NavigatorImageDisplayManager _imageDisplayMgr;

    // Top level image window (frame or internal frame version)
    private Component _imageDisplayControlFrame;

    // Used to display images
    private NavigatorImageDisplay _imageDisplay;

    // Action to use to show the image window.
    private AbstractAction _imageDisplayAction = new AbstractAction("Image") {
        public void actionPerformed(ActionEvent evt) {
            showImageDisplay();
        }
    };

    /**
     * Construct a Navigator using the given CatalogTree widget
     * (call setQueryResult to set the catalog or data to display).
     *
     * @param parent the parent component
     *
     * @param catalogTree a CatalogTree (normally a subclass of CatalogTree
     *                    that knows about certain types of catalogs)
     *
     * @param plotter the object to use to plot catalog table data
     *                (when the plot button is pressed)
     *
     * @param imageDisplay optional widget to use to display images (if not specified,
     *                     or null, a new window will be created)
     *
     */
    public Navigator(Component parent, CatalogTree catalogTree,
                     TablePlotter plotter, MainImageDisplay imageDisplay) {
        super(parent, catalogTree, plotter);

        if (imageDisplay != null) {
            _imageDisplay = (NavigatorImageDisplay) imageDisplay;
            _imageDisplayControlFrame = imageDisplay.getRootComponent();
            initSymbolPlotter();
        }
    }

    /**
     * Construct a Navigator using the given CatalogTree widget
     * (call setQueryResult to set the catalog or data to display).
     *
     * @param parent the parent component
     *
     * @param catalogTree a CatalogTree (normally a subclass of CatalogTree
     *                    that knows about certain types of catalogs)
     *
     * @param plotter the object to use to plot catalog table data
     *                (when the plot button is pressed)
     *
     */
    public Navigator(Component parent, CatalogTree catalogTree, TablePlotter plotter) {
        this(parent, catalogTree, plotter, null);
    }


    /** Specify an (optional) object that can be used to create the image display frame when needed. */
    public void setImageDisplayManager(NavigatorImageDisplayManager imageDisplayMgr) {
        _imageDisplayMgr = imageDisplayMgr;
    }


    /** Return the image display widget. */
    public MainImageDisplay getImageDisplay() {
        return _imageDisplay;
    }

    /** Set the image display to use for plotting catalog objects. */
    protected void setImageDisplay(NavigatorImageDisplay imageDisplay) {
        _imageDisplay = imageDisplay;
        setImageDisplayControlFrame(imageDisplay.getParentFrame());
        notifyNewImageDisplay();
        initSymbolPlotter();
    }


    /** Set the frame belonging to the image display widget. */
    protected void setImageDisplayControlFrame(Component imageDisplayControlFrame) {
        _imageDisplayControlFrame = imageDisplayControlFrame;
    }

    /** Return the action to use to show the image window. */
    public Action getImageDisplayAction() {
        return _imageDisplayAction;
    }


    /**
     * Make a panel for querying a catalog
     * (Redefined from the parent class to use a CatalogQueryTool subclass).
     */
    @Override
    protected CatalogQueryTool makeCatalogQueryTool(Catalog catalog) {
        return new NavigatorQueryTool(catalog, this, _imageDisplay);
    }

    /**
     * Make an ImageDisplayControlFrame or ...InternalFrame, depending
     * on what type of frames are being used.
     */
    protected void makeImageDisplayControlFrame() {
        if (_imageDisplayMgr != null) {
            _imageDisplay = _imageDisplayMgr.getImageDisplay();
            _imageDisplayControlFrame = _imageDisplayMgr.getImageDisplayControlFrame();
        } else {
            Component parent = getParentFrame();
            if (parent instanceof JFrame) {
                _imageDisplayControlFrame = new NavigatorImageDisplayFrame();
                _imageDisplayControlFrame.setVisible(true);
                _imageDisplay = (NavigatorImageDisplay)
                        ((NavigatorImageDisplayFrame) _imageDisplayControlFrame).getImageDisplayControl().getImageDisplay();
            } else if (parent instanceof JInternalFrame) {
                JDesktopPane desktop = getDesktop();
                _imageDisplayControlFrame = new NavigatorImageDisplayInternalFrame(desktop);
                _imageDisplayControlFrame.setVisible(true);
                _imageDisplay = (NavigatorImageDisplay)
                        ((NavigatorImageDisplayInternalFrame) _imageDisplayControlFrame).getImageDisplayControl().getImageDisplay();
                desktop.add(_imageDisplayControlFrame, JLayeredPane.DEFAULT_LAYER);
                desktop.moveToFront(_imageDisplayControlFrame);
            }
        }

        _imageDisplay.setNavigator(this);
    }

    /**
     * Load and display the given image file.
     * The URL is only needed for the image history, in case the file is deleted.
     */
    protected void loadImage(String filename, URL url) {
        showImageDisplay();
        _imageDisplay.setFilename(filename, url);
    }


    /**
     * Show the image display window.
     */
    public void showImageDisplay() {
        if (_imageDisplay == null) {
            // create the image display frame
            makeImageDisplayControlFrame();
            notifyNewImageDisplay();
            initSymbolPlotter();
        } else {
            SwingUtil.showFrame(_imageDisplayControlFrame);
        }
    }

    /**
     * Download the given image URL to a temporary file and then
     * display the image file when done.
     * This method is called in a background thread.
     */
    protected void loadImage(URL url, String contentType) throws IOException {
        if (url.getProtocol().equals("file")) {
            SwingUtilities.invokeLater(new NavigatorImageLoader(url.getPath(), url));
        } else {
            String dir = Preferences.getPreferences().getCacheDir().getPath();

            // Try to determine the correct suffix for the file, for later reference
            String suffix;
            if (contentType.endsWith("hfits"))
                suffix = ".hfits"; // H-compressed FITS
            else if (contentType.endsWith("zfits")
                    || contentType.equals("image/x-fits")) // XX hack: caltech/oasis returns this with gzipped FITS!
                suffix = ".fits.gz"; // gzipped FITS (other contentTypes?)
            else if (contentType.endsWith("fits"))
                suffix = ".fits"; // plain FITS
            else
                suffix = ".tmp";  // other image formats...

            File file = File.createTempFile("jsky", suffix, new File(dir));

            // System.out.println("XXX loadImage: file = " + file);

            ProgressPanel progressPanel = getProgressPanel();
            ProgressBarFilterInputStream in = progressPanel.getLoggedInputStream(url);
            FileOutputStream out = new FileOutputStream(file);

            // copy the data
            synchronized (in) {
                synchronized (out) {
                    byte[] buffer = new byte[8 * 1024];
                    while (true) {
                        int bytesRead = in.read(buffer);
                        if (bytesRead == -1) {
                            break;
                        }
                        if (progressPanel.isInterrupted()) {
                            throw new ProgressException("Interrupted");
                        }
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }

            in.close();
            out.flush();
            out.close();

            if (!progressPanel.isInterrupted())
                SwingUtilities.invokeLater(new NavigatorImageLoader(file.toString(), url));
        }
    }


    /**
     *  Notify any panels that need to know about the new image display window.
     */
    protected void notifyNewImageDisplay() {
        notifyNewImageDisplay(getBackStack());
        notifyNewImageDisplay(getForwStack());

        JComponent queryComponent = getQueryComponent();
        if (queryComponent instanceof NavigatorQueryTool) {
            ((NavigatorQueryTool) queryComponent).setImageDisplay(_imageDisplay);
        }
    }

    /**
     * Notify any panels in the given stack that need to know about
     * the new image display window.
     */
    protected void notifyNewImageDisplay(Stack stack) {
        for (Object comp : stack) {
            if (comp instanceof NavigatorQueryTool) {
                ((NavigatorQueryTool) comp).setImageDisplay(_imageDisplay);
            }
        }
    }


    /**
     * initialize the symbol plotter
     */
    protected void initSymbolPlotter() {
        // initialize the symbol plotter
        TablePlotter plotter = getPlotter();
        if (plotter != null) {
            plotter.setCanvasGraphics(_imageDisplay.getCanvasGraphics());
            plotter.setCoordinateConverter(_imageDisplay.getCoordinateConverter());
            _imageDisplay.getNavigatorPane().setPlotter(plotter);
        }
    }


    /**
     * Return a new JComponent displaying the contents of the given URL.
     */
    @Override
    protected JComponent makeURLComponent(URL url, String contentType) throws IOException {
        String filename = url.getFile();
        String protocol = url.getProtocol();

        LOG.log(Level.FINE, "Display URL: ContentType = " + contentType + ", URL = " + url + ", protocol = " + protocol + ", filename = " + filename);

        // Check for a Skycat style catalog config file?
        if (filename.endsWith(".cfg")) {
            // skycat config file?
            String basename = new File(filename).getName();
            SkycatConfigFile cf = new SkycatConfigFile(basename, url);
            cf.setHTMLQueryResultHandler(this);
            return makeCatalogDirectoryComponent(cf);
        }

        if (filename.endsWith(".table") || filename.endsWith(".scat") || filename.endsWith(".cat")) {
            // skycat local catalog file?
            SkycatTable table = new SkycatTable(filename);
            return makeCatalogComponent(table.getCatalog());
        }

        if (filename.endsWith(".xml")) {
            // AstroCat XML file?
            String basename = new File(filename).getName();
            AstroCatConfig cf = new AstroCatConfig(basename, url);
            cf.setHTMLQueryResultHandler(this);
            return makeCatalogDirectoryComponent(cf);
        }

        if (contentType.indexOf("text/html") == 0 ) // could be that content-type contains more info
            return super.makeURLComponent(url, contentType);

        if (contentType.indexOf("text/plain") == 0)
            return super.makeURLComponent(url, contentType);

        if (filename.endsWith(".fits")) {
            // FITS table file?
            try {
                NavigatorFITSTable table = NavigatorFITSTable.getFitsTable(filename);
                return makeCatalogComponent(table.getCatalog());
            } catch(Exception e) {
                // ignore: might be an image file, which is handled below
            }
        }

        // assume it is an image and display in a separate window
        loadImage(url, contentType);
        return getResultComponent();
    }

    /**
     * Open the catalog navigator window (in this case, it is already open).
     * @see CatalogNavigatorOpener
     */
    @Override
    public void openCatalogWindow() {
    }


    /**
     * Open the catalog navigator window and display the interface for the given catalog,
     * if not null (in this case, the window is already open).
     * @see CatalogNavigatorOpener
     */
    @Override
    public void openCatalogWindow(Catalog cat) {
        if (cat != null) {
            setAutoQuery(false);
            setQueryResult(cat);
        }
    }

    /**
     * Open a catalog window for the named catalog, if found.
     * @see CatalogNavigatorOpener
     */
    @Override
    public void openCatalogWindow(String name) {
        Catalog cat = getCatalogDirectory().getCatalog(name);
        if (cat != null)
            openCatalogWindow(cat);
    }


    /**
     * Pop up a file browser to select a local catalog file to open.
     * @see CatalogNavigatorOpener
     */
    @Override
    public void openLocalCatalog() {
        open();
    }

    /**
     * Save the current table as a FITS table in the current FITS image.
     */
    @Override
    public void saveWithImage() {
        JComponent resultComponent = getResultComponent();
        if (!(resultComponent instanceof TableDisplayTool)) {
            DialogUtil.error("This operation is only supported for tables");
        } else if (_imageDisplay == null) {
            DialogUtil.error("No current FITS image.");
        } else {
            TableQueryResult table = ((TableDisplayTool) resultComponent).getTable();
            _imageDisplay.saveFITSTable(table);
        }
    }

    /**
     * Create and return a new file chooser to be used to select a local catalog file
     * to open.
     */
    @Override
    public JFileChooser makeFileChooser() {
        JFileChooser fileChooser = new JFileChooser(new File("."));

        ExampleFileFilter configFileFilter = new ExampleFileFilter(new String[]{"cfg"},
                                                                   "Catalog config Files (Skycat style)");
        fileChooser.addChoosableFileFilter(configFileFilter);

        ExampleFileFilter fitsFilter = new ExampleFileFilter(new String[]{
            "fit", "fits", "fts"}, "FITS File with Table Extensions");
        fileChooser.addChoosableFileFilter(fitsFilter);

        ExampleFileFilter skycatLocalCatalogFilter = new ExampleFileFilter(new String[]{
            "table", "scat", "cat"}, "Local catalog Files (Skycat style)");
        fileChooser.addChoosableFileFilter(skycatLocalCatalogFilter);

        ExampleFileFilter htmlFilter = new ExampleFileFilter(new String[]{
            "html", "htm"}, "HTML File");
        fileChooser.addChoosableFileFilter(htmlFilter);

        fileChooser.setFileFilter(skycatLocalCatalogFilter);

        return fileChooser;
    }


    /**
     * This local class is used to load an image in the event dispatching thread.
     * Doing it in the calling thread could cause the window to hang, since
     * it needs to create and show a top level window and the calling thread
     * (from CatalogNavigator) is already a background thread.
     * The url is only needed for the image history, in case the file is deleted.
     */
    protected class NavigatorImageLoader implements Runnable {

        String filename;
        URL url;

        public NavigatorImageLoader(String filename, URL url) {
            this.filename = filename;
            this.url = url;
        }

        public void run() {
            loadImage(filename, url);
            showImageDisplay();
        }
    }

    /**
     * Add the object described by stats to the currently
     * displayed table, or create a new table if none is being displayed.
     *
     * @param stats describes the selected object
     * @param isUpdate set to true if this is just an update of the previously selected position
     */
    public void addPickedObjectToTable(PickObjectStatistics stats, boolean isUpdate) {
        TableQueryResult table = null;
        TableDisplayTool tableDisplayTool = null;

        // see if a table is already being displayed, and if so, use it

        JComponent resultComponent = getResultComponent();
        if (resultComponent instanceof TableDisplayTool) {
            tableDisplayTool = (TableDisplayTool) resultComponent;
            table = tableDisplayTool.getTableDisplay().getTableQueryResult();
        }

        if (table == null) {
            // no table being displayed: make a new table
            table = makePickObjectTable(stats);

            // Make a dummy catalog for the table, so we have something to display in the top window
            SkycatCatalog cat = new SkycatCatalog((SkycatTable) table);
            setQueryResult(cat);

            JComponent queryComponent = getQueryComponent();
            if (queryComponent instanceof CatalogQueryTool) {
                // This is like pressing the Go button to show the contents of the table
                ((CatalogQueryTool) queryComponent).search();
            }
            getParentFrame().setVisible(true);
        } else {
            // just add the row to the existing table
            addRowForPickedObject(table, tableDisplayTool, stats, isUpdate);
        }
    }


    /**
     * Make a catalog table to use to hold the objects picked by the user and
     * add the first row based on the given stats object.
     */
    protected TableQueryResult makePickObjectTable(PickObjectStatistics stats) {
        Properties properties = new Properties();
        properties.setProperty(SkycatConfigFile.SERV_TYPE, "local");
        properties.setProperty(SkycatConfigFile.LONG_NAME, "Picked Objects");
        properties.setProperty(SkycatConfigFile.SHORT_NAME, "PickedObjects");
        properties.setProperty(SkycatConfigFile.SYMBOL,
                               "{{FWHM_X} {FWHM_Y} {Angle}} "
                               + "{{plus} {green} {$FWHM_X/$FWHM_Y} {$Angle} {} {1}} "
                               + "{{($FWHM_X+$FWHM_Y)*0.5} {image}}");
        properties.setProperty(SkycatConfigFile.URL, "none");
        SkycatConfigEntry configEntry = new SkycatConfigEntry(properties);
        FieldDesc[] fields = PickObjectStatistics.getFields();

        Vector<Vector<Object>> dataRows = new Vector<>();
        dataRows.add(stats.getRow());
        SkycatTable table = new SkycatTable(configEntry, dataRows, fields);
        table.setProperties(properties);
        return table;
    }


    /**
     * Add a row to the given table with information from the given stats object.
     * The target table may or may not be compatible, so column names and types
     * are checked.
     */
    protected void addRowForPickedObject(TableQueryResult table, TableDisplayTool tableDisplayTool,
                                         PickObjectStatistics stats, boolean isUpdate) {
        if (!table.hasCoordinates()) {
            DialogUtil.error("The current table does not support coordinates");
            return;
        }

        int numCols = table.getColumnCount();
        Vector<Object> v = stats.getRow();
        Vector<Object> rowVec = new Vector<>(numCols);

        for (int col = 0; col < numCols; col++) {
            FieldDesc field = table.getColumnDesc(col);
            String name = field.getName();
            if (field.isId()) {
                rowVec.add(v.get(PickObjectStatistics.ID));
            } else if (field.isRA()) {
                rowVec.add(stats.getCenterPos().getRA().toString());
            } else if (field.isDec()) {
                rowVec.add(stats.getCenterPos().getDec().toString());
            } else {
                Object o = null;
                for (int i = 0; i < PickObjectStatistics.NUM_FIELDS; i++) {
                    if (name.equals(PickObjectStatistics.FIELD_NAMES[i])) {
                        o = v.get(i);
                        break;
                    }
                }
                rowVec.add(o);
            }
        }
        if (isUpdate) {
            // if the last row added is for the same point, update it
            int rowIndex = tableDisplayTool.getRowCount() - 1;
            if (rowIndex >= 0)
                tableDisplayTool.updateRow(rowIndex, rowVec);
            else
                tableDisplayTool.addRow(rowVec);
        } else {
            tableDisplayTool.addRow(rowVec);
        }
        tableDisplayTool.replot();
    }

}

