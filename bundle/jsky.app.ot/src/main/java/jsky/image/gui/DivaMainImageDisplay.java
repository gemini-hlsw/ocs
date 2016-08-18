package jsky.image.gui;

import com.sun.media.jai.codec.*;
import diva.canvas.GraphicsPane;
import jsky.coords.WorldCoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;
import jsky.image.fits.codec.FITSImage;
import jsky.image.fits.gui.*;
import jsky.image.graphics.MeasureBand;
import jsky.image.graphics.gui.CanvasDraw;
import jsky.image.graphics.gui.FITSGraphics;
import jsky.util.*;
import jsky.util.Resources;
import jsky.util.gui.*;
import jsky.util.gui.SwingWorker;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.BufferedFile;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.io.*;
import java.net.URL;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements the main image display window and provides methods
 * to implement each of the menu items in the ImageDisplayMenuBar class.
 *
 * @author Allan Brighton
 * @version $Revision: 47126 $
 */
public class DivaMainImageDisplay extends DivaGraphicsImageDisplay implements MainImageDisplay {
    private static final Logger LOG = Logger.getLogger(DivaMainImageDisplay.class.getName());

    // Used to access internationalized strings (see i18n/gui*.properties)
    private static final I18N _I18N = I18N.getInstance(DivaMainImageDisplay.class);

    /**
     * The top level parent frame (or internal frame) used to close the window.
     */
    private Component _parent;

    /**
     * Panel used to display download progress information
     */
    private ProgressPanel _progressPanel;

    /**
     * file chooser dialog
     */
    private JFileChooser _fileChooser;

    /**
     * Save dialog window
     */
    private ImageSaveDialog _saveDialog;

    /**
     * Print dialog window
     */
    private ImagePrintDialog _printDialog;

    /**
     * Name of image file, if any
     */
    private String _filename;

    /**
     * Set to true if the image has been modified and needs saving
     */
    private boolean saveNeeded = false;

    /**
     * The URL for the image, if one was specified (after downloading, if possible)
     */
    private URL _url;

    /**
     * The original image URL (before downloading, for history)
     */
    private URL _origURL;

    /**
     * The base title string for the image frame
     */
    private String _title = _I18N.getString("imageDisplay");

    /**
     * Manages interactive drawing on the image canvas
     */
    private CanvasDraw _canvasDraw;

    /**
     * Top level windows (or internal frame) for setting cut levels
     */
    private Component _imageCutLevelsFrame;

    /**
     * Top level window (or internal frame) for viewing the FITS keywords.
     */
    private Component _fitsKeywordsFrame;

    /**
     * Top level window (or internal frame) for viewing image properties
     */
    private Component _imagePropertiesFrame;

    /**
     * Top level window (or internal frame) for manipulating image colormaps
     */
    private Component _imageColorsFrame;

    /**
     * Top level window (or internal frame) for selecting image objects (stars, galaxies)
     */
    private Component _pickObjectFrame;

    /**
     * Pick Object panel, if initialized
     */
    private PickObject _pickObjectPanel;

    /**
     * Top level window (or internal frame) for manipulating FITS extensions
     */
    private Component _fitsHDUChooserFrame;

    /**
     * Panel for manipulating FITS extensions
     */
    private FITSHDUChooser _fitsHDUChooser;

    /**
     * Used to save graphics to a FITS table in the image and reload it again later.
     */
    private FITSGraphics _fitsGraphics;

    /**
     * Event passed to change listeners
     */
    private ImageChangeEvent _imageChangeEvent = new ImageChangeEvent(this);

    /**
     * Utility object used to control background thread
     */
    private jsky.util.gui.SwingWorker _worker;

    /**
     * Stack of ImageHistoryItem, used to go back to a previous image
     */
    private Stack<ImageHistoryItem> _backStack = new Stack<>();

    /**
     * Stack of ImageHistoryItem, used to go forward to the next image
     */
    private Stack<ImageHistoryItem> _forwStack = new Stack<>();

    /**
     * Set when the back or forward actions are active to avoid the normal history stack handling
     */
    private boolean _noStack = false;

    /**
     * List of ImageHistoryItem, for previously viewed images (shared by all instances of this class)
     */
    private LinkedList<ImageHistoryItem> _historyList;

    /**
     * Base filename for serialization of the history list
     */
    private static final String HISTORY_LIST_DATA_NAME = "imageHistoryListData";


    /**
     * Action to use for the "Open" menu and toolbar items
     */
    private AbstractAction _openAction = new AbstractAction(
            _I18N.getString("open"),
            Resources.getIcon("Open24.gif")) {

        public void actionPerformed(ActionEvent evt) {
            try {
                open();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Action to use for the "Back" menu and toolbar items
     */
    private AbstractAction _backAction = new AbstractAction(
            _I18N.getString("back"),
            Resources.getIcon("Back24.gif")) {

        public void actionPerformed(ActionEvent evt) {
            try {
                back();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Action to use for the "Forward" menu and toolbar items
     */
    private AbstractAction _forwAction = new AbstractAction(
            _I18N.getString("forward"),
            Resources.getIcon("Forward24.gif")) {

        public void actionPerformed(ActionEvent evt) {
            try {
                forward();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Action to use for the "Save" menu and toolbar items
     */
    private AbstractAction _saveAction = new AbstractAction(_I18N.getString("save")) {

        public void actionPerformed(ActionEvent evt) {
            try {
                save();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Action to use for the "Save as..." menu and toolbar items
     */
    private AbstractAction _saveAsAction = new AbstractAction(_I18N.getString("saveAs")) {

        public void actionPerformed(ActionEvent evt) {
            try {
                saveAs();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Action to use for the "Print Preview..." menu and toolbar items
     */
    private AbstractAction _printPreviewAction = new AbstractAction(_I18N.getString("printPreview")) {

        public void actionPerformed(ActionEvent evt) {
            try {
                printPreview();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Action to use for the "Print..." menu and toolbar items
     */
    private AbstractAction _printAction = new AbstractAction(_I18N.getString("print") + "...") {

        public void actionPerformed(ActionEvent evt) {
            try {
                print();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Action to use for the "Cut Levels..." menu and toolbar items
     */
    private AbstractAction _cutLevelsAction = new AbstractAction(
            _I18N.getString("cutLevels"),
            Resources.getIcon("CutLevels24.gif")) {

        public void actionPerformed(ActionEvent evt) {
            try {
                editCutLevels();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Action to use for the "Colors..." menu and toolbar items
     */
    private AbstractAction _colorsAction = new AbstractAction(_I18N.getString("colors")) {

        public void actionPerformed(ActionEvent evt) {
            try {
                editColors();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Initialize with the given Diva GraphicsPane (contains the layers used to draw the image
     * and graphics. Note that you need to call setParentFrame() if you use this version.
     *
     * @param pane the Diva GraphicsPane to use (contains the layers used to display the
     *             image and graphics)
     */
    public DivaMainImageDisplay(GraphicsPane pane) {
        super(pane, new ImageProcessor(), "Main Image");

        setDownloadState(false);
        updateEnabledStates();

        new MeasureBand(this);
        _canvasDraw = new CanvasDraw(this);
        _fitsGraphics = new FITSGraphics(this);

        _historyList = new LinkedList<>();

        cleanupImageCache();
    }

    /**
     * Default constructor. Note that you need to call setParentFrame() if you use this version).
     */
    public DivaMainImageDisplay() {
        this(new GraphicsPane());
    }


    /**
     * Construct a DivaMainImageDisplay widget.
     *
     * @param pane   the Diva GraphicsPane to use (contains the layers used to display the
     *               image and graphics)
     * @param parent the top level parent frame (or internal frame) used to close the window
     */
    public DivaMainImageDisplay(GraphicsPane pane, Component parent) {
        this(pane);
        _parent = parent;
    }

    /**
     * Construct a DivaMainImageDisplay widget.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     */
    public DivaMainImageDisplay(Component parent) {
        this(new GraphicsPane());
        _parent = parent;
    }

    /**
     * Return the object that manages interactive drawing on the image
     */
    public CanvasDraw getCanvasDraw() {
        return _canvasDraw;
    }


    /**
     * Set the image file to display.
     */
    public void setFilename(String fileOrUrl) {
        if (fileOrUrl.startsWith("http:")) {
            setURL(FileUtil.makeURL(null, fileOrUrl));
            return;
        }
        if (!checkSave())
            return;


        addToHistory();
        _filename = fileOrUrl;
        _url = _origURL = FileUtil.makeURL(null, fileOrUrl);

        // free up any previously opened FITS images
        FITSImage fitsImage = getFitsImage();
        if (fitsImage != null) {
            fitsImage.close();
            fitsImage.clearTileCache();
        }

        // load non FITS images with JAI, but try to load FITS files using the
        // the FITS I/O library, which is more efficient when using its own RandomAccess I/O
        if (isJAIImageType(_filename)) {
            try {
                setImage(JAI.create("fileload", _filename));
            } catch (Exception e) {
                DialogUtil.error(e);
                _filename = null;
                _url = _origURL = null;
                clear();
            }
        } else {
            try {
                fitsImage = new FITSImage(_filename);
                initFITSImage(fitsImage);
                setImage(fitsImage);
            } catch (Exception e) {
                // fall back to JAI method
                try {
                    setImage(JAI.create("fileload", _filename));
                } catch (Exception ex) {
                    DialogUtil.error(e);
                    _filename = null;
                    _url = _origURL = null;
                    clear();
                }
            }
            updateTitle();
        }
    }

    /**
     * Set the image file to display and use the given URL for the image history
     * (the URL is used if the file is deleted).
     */
    public void setFilename(String fileOrUrl, URL url) {
        setFilename(fileOrUrl);
        _origURL = url;
    }

    /**
     * Return the image file name, if there is one.
     */
    public String getFilename() {
        return _filename;
    }

    /**
     * Return true if the given filename has a suffix that indicates that it is
     * not a FITS file and is one of the standard JAI supported image types.
     */
    public boolean isJAIImageType(String filename) {
        return filename.endsWith("jpg")
                || filename.endsWith("jpeg")
                || filename.endsWith("gif")
                || filename.endsWith("tif")
                || filename.endsWith("tiff")
                || filename.endsWith("ppm")
                || filename.endsWith("png")
                || filename.endsWith("pgm")
                || filename.endsWith("pnm")
                || filename.endsWith("bmp");
    }


    /**
     * Clear the image display.
     */
    public void clear() {
        super.clear();
        updateEnabledStates();
    }


    /**
     * Display the FITS table at the given HDU index (if supported).
     */
    public void displayFITSTable(int hdu) {
    }


    /**
     * Return the name of the object being displayed, if known, otherwise null.
     */
    public String getObjectName() {
        FITSImage fitsImage = getFitsImage();
        if (fitsImage != null) {
            Object o = fitsImage.getKeywordValue("OBJECT");
            if (o instanceof String)
                return o.toString();
        }
        return null;
    }


    /**
     * Set the URL for the image to display. This method first tries to download the
     * URL to a temporary file, since files can be displayed more efficiently. If
     * that does not work, it tries to load the URL directly.
     */
    public void setURL(URL url) {
        _origURL = url;
        String s = url.getProtocol();
        if (s.equals("file")) {
            setFilename(url.getFile());
        } else if (s.equals("http")) {
            downloadImageToTempFile(url);
        } else {
            DialogUtil.error("Unsupported URL syntax: " + s);
        }
    }

    /**
     * Return the image URL, if there is one.
     */
    public URL getURL() {
        return _url;
    }


    /**
     * Called after a new FITSImage object was created to do FITS specific initialization
     */
    protected void initFITSImage(FITSImage fitsImage) throws IOException, FitsException {
        int numHDUs = fitsImage.getNumHDUs();
        if (numHDUs >= 2 && fitsImage.isEmpty() && (fitsImage.getHDU(1) instanceof ImageHDU)) {
            fitsImage.setHDU(1);
        }

        // If the user previously set the image scale, restore it here to avoid doing it twice
        ImageHistoryItem hi = getImageHistoryItem(new File(_filename));
        float scale;
        if (hi != null) {
            scale = hi.data.scale;
            addChangeListener(hi); // This will restore the history settings for the image
        } else {
            scale = getScale();
        }
        if (scale != 1.0F)
            fitsImage.setScale(scale);
    }


    /**
     * Update the display to show the contents of the currently loaded image file.
     */
    public void updateImageData() {
        if (_filename != null)
            setFilename(_filename);
        else if (_url != null)
            setURL(_url);
    }

    /**
     * Add the current URL to the history list
     */
    protected void addToHistory() {
        if (_filename == null)
            return;

        ImageHistoryItem historyItem = makeImageHistoryItem();

        if (!_noStack) {
            // add to the "Back" stack
            _backStack.push(historyItem);
            _backAction.setEnabled(true);
            if (_forwStack.size() != 0) {
                _forwStack.clear();
                _forwAction.setEnabled(false);
            }
        }
        addToHistory(historyItem);
    }

    /**
     * Add the given item to the history list, removing duplicates and
     * keeping the list size to a maximum of 20.
     */
    @SuppressWarnings("unchecked")
    protected void addToHistory(ImageHistoryItem historyItem) {
        ListIterator<ImageHistoryItem> it = ((LinkedList<ImageHistoryItem>) _historyList.clone()).listIterator(0);
        for (int i = 0; it.hasNext(); i++) {
            ImageHistoryItem item = it.next();
            if (item.data.title.equals(historyItem.data.title)) {
                _historyList.remove(i);
            }
        }
        _historyList.addFirst(historyItem);
        /*
          Max number of items in the history list
        */
        int _maxHistoryItems = 20;
        if (_historyList.size() > _maxHistoryItems) {
            ImageHistoryItem item = _historyList.removeLast();
            // remove the file, if it is in cache
            String cacheDir = Preferences.getPreferences().getCacheDir().getPath();
            if (item.data.filename.startsWith(cacheDir))
                new File(item.data.filename).deleteOnExit();
        }
    }

    /**
     * Make and return an ImageHistoryItem for the current image
     */
    protected ImageHistoryItem makeImageHistoryItem() {
        // make the title
        double ra = Double.NaN, dec = Double.NaN;
        double widthDeg = Double.NaN, heightDeg = Double.NaN;
        String radecStr = "";

        if (isWCS()) {
            WorldCoordinateConverter wcs = getWCS();
            WorldCoords center = new WorldCoords(wcs.getWCSCenter(), wcs.getEquinox());
            radecStr += center.toString();
            ra = center.getRaDeg();
            dec = center.getDecDeg();
            widthDeg = wcs.getWidthInDeg();
            heightDeg = wcs.getHeightInDeg();
        }

        String name = "";
        if (_filename != null) {
            name = new File(_filename).getName();
        }

        String object = getObjectName();
        if (object == null || object.startsWith("dss"))
            object = "";
        else
            object = object + ": ";

        String title = name;
        if (object.length() != 0 || radecStr.length() != 0)
            title = title + " [" + object + radecStr + "]";
        return new ImageHistoryItem(this, ra, dec, widthDeg, heightDeg, title, _origURL, _filename);
    }

    /**
     * Merge the historyList with current serialized version (another instance
     * may have written it since we read it last).
     */
    protected LinkedList<ImageHistoryItem> mergeHistoryList() {
        Object[] items = _historyList.toArray();
        loadHistory();

        // Go through the list in reverse, since addToHistory inserts at the start of the list
        for (int i = items.length - 1; i >= 0; i--) {
            addToHistory((ImageHistoryItem) items[i]);
        }
        return _historyList;
    }


    /**
     * Add the current URL to the history list
     */
    protected void clearHistory() {
        // remove the cached image files
        String cacheDir = Preferences.getPreferences().getCacheDir().getPath();
        ListIterator<ImageHistoryItem> it = _historyList.listIterator(0);
        while (it.hasNext()) {
            ImageHistoryItem historyItem = it.next();
            if (historyItem.data.filename.startsWith(cacheDir)) {
                File file = new File(historyItem.data.filename);
                if (file.exists()) {
                    try {
                        if (!file.delete()) {
                            LOG.warning("Cannot delete history file " + file.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Exception deleting file " + file.getAbsolutePath(), e);
                    }
                }
            }
        }

        _historyList = new LinkedList<>();
        _backAction.setEnabled(false);
        _backStack.clear();
        _forwAction.setEnabled(false);
        _forwStack.clear();
        saveHistory(false);
    }


    /**
     * Save the current history list to a file.
     *
     * @param merge if true, merge the list with the existing list on disk.
     */
    protected void saveHistory(boolean merge) {
        try {
            LinkedList<ImageHistoryItem> l;
            if (merge)
                l = mergeHistoryList();
            else
                l = _historyList;
            Preferences.getPreferences().serialize(HISTORY_LIST_DATA_NAME, ImageHistoryItem.map(l));
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Try to load the history list from a file, and create an empty list if that fails.
     */
    @SuppressWarnings("unchecked")
    protected void loadHistory() {
        try {
            _historyList = ImageHistoryItem.apply((LinkedList<ImageHistoryItem.Data>) Preferences.getPreferences().deserialize(HISTORY_LIST_DATA_NAME));
        } catch (Exception e) {
            _historyList = new LinkedList<>();
        }
    }

    /**
     * Remove any files from the image cache that are no longer in the
     * history list.
     */
    protected void cleanupImageCache() {
        String cacheDir = Preferences.getPreferences().getCacheDir().getPath();
        File dir = new File(cacheDir);
        if (dir.isDirectory()) {
            // Just to be safe, use a filter to only remove files we created
            // (there shouldn't be any other files in the cache dir though)
            FilenameFilter filter = (dir1, name) -> name.startsWith("jsky");
            File[] files = dir.listFiles(filter);
            for (File file : files) {
                if (!fileInHistoryList(file)) {
                    if (!file.delete()) {
                        LOG.warning("Cannot delete file " + file.getAbsolutePath());
                    }
                }

            }
        }
    }

    /**
     * Return true if the given file is referenced in the history list.
     */
    public boolean fileInHistoryList(File file) {
        return getImageHistoryItem(file) != null;
    }

    /**
     * Return the ImageHistoryItem from the history list, or null if not found.
     */
    @SuppressWarnings("unchecked")
    protected ImageHistoryItem getImageHistoryItem(File file) {
        ListIterator<ImageHistoryItem> it = ((LinkedList<ImageHistoryItem>) _historyList.clone()).listIterator(0);
        while (it.hasNext()) {
            ImageHistoryItem historyItem = it.next();
            File f = new File(historyItem.data.filename);
            if (f.equals(file.getAbsoluteFile())) {
                return historyItem;
            }
        }
        return null;
    }


    /**
     * Make the download progress panel if needed, and display it
     */
    protected void initProgressPanel() {
        if (_progressPanel == null) {
            _progressPanel = ProgressPanel.makeProgressPanel("Downloading image data ...", _parent);
            _progressPanel.addActionListener(e -> {
                if (_worker != null) {
                    _worker.interrupt();
                    _worker = null;
                }
            });
        }
        _progressPanel.start();
    }

    /**
     * Download the given URL to a temporary file in a separate thread and then
     * display the image file when done.
     */
    protected void downloadImageToTempFile(final URL url) {    //System.out.println("XXX downloadImageToTempFile: " + url);
        initProgressPanel();
        _worker = new SwingWorker() {

            String filename;

            public Object construct() {
                setDownloadState(true);
                try {
                    String dir = Preferences.getPreferences().getCacheDir().getPath();
                    File file = File.createTempFile("jsky", ".tmp", new File(dir));
                    ProgressBarFilterInputStream in = _progressPanel.getLoggedInputStream(url);
                    FileOutputStream out = new FileOutputStream(file);
                    FileUtil.copy(in, out);
                    in.close();
                    out.close();
                    filename = file.toString();
                } catch (Exception e) {
                    return e;
                }
                return null;
            }

            public void finished() {
                _progressPanel.stop();
                setDownloadState(false);
                _worker = null;
                Object o = getValue();
                if ((o instanceof Exception) && !(o instanceof ProgressException)) {
                    DialogUtil.error((Exception) o);
                    return;
                }
                if (!_progressPanel.isInterrupted())
                    setFilename(filename);
            }

        };
        _worker.start();
    }


    /**
     * Set the state of the menubar/toolbar actions
     *
     * @param downloading set to true if an image is being downloaded
     */
    protected void setDownloadState(boolean downloading) {
        if (downloading) {
            _backAction.setEnabled(false);
            _forwAction.setEnabled(false);
            _openAction.setEnabled(false);
        } else {
            _backAction.setEnabled(_backStack.size() > 0);
            _forwAction.setEnabled(_forwStack.size() > 0);
            _openAction.setEnabled(true);
        }
    }


    /**
     * Update the enabled states of some menu/toolbar actions.
     */
    protected void updateEnabledStates() {
        boolean fileLoaded = (_filename != null);
        boolean imageLoaded = (fileLoaded || _url != null);
        _saveAction.setEnabled(fileLoaded && saveNeeded);
        _saveAsAction.setEnabled(imageLoaded);
        _cutLevelsAction.setEnabled(imageLoaded);
        _colorsAction.setEnabled(imageLoaded);
    }


    /**
     * Go back to the previous image in the history list
     */
    public void back() {
        if (_backStack.size() == 0)
            return;

        if (!checkSave())
            return;

        if (_filename != null) {
            _forwStack.push(makeImageHistoryItem());
            _forwAction.setEnabled(true);
        }

        ImageHistoryItem historyItem = _backStack.pop();
        if (_backStack.size() == 0)
            _backAction.setEnabled(false);

        ImageDisplayMenuBar.setCurrentImageDisplay(this);
        _noStack = true;
        try {
            historyItem.actionPerformed(null);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
        _noStack = false;
    }


    /**
     * Go forward to the next image in the history list
     */
    public void forward() {
        if (_forwStack.size() == 0)
            return;

        if (!checkSave())
            return;

        if (_filename != null) {
            _backStack.push(makeImageHistoryItem());
            _backAction.setEnabled(true);
        }

        ImageHistoryItem historyItem = _forwStack.pop();
        if (_forwStack.size() == 0)
            _forwAction.setEnabled(false);

        ImageDisplayMenuBar.setCurrentImageDisplay(this);
        _noStack = true;
        try {
            historyItem.actionPerformed(null);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
        _noStack = false;
    }


    /**
     * If an image was previously loaded in this session with a center near the
     * given ra,dec coordinates, reload it, otherwise generate a blank image
     * with the center at those coordinates (in J2000).
     *
     * @param ra  the RA coordinate for the image (J2000)
     * @param dec the Dec coordinate for the image (J2000)
     * @return true if an image was loaded
     */
    @SuppressWarnings("unchecked")
    public boolean loadCachedImage(double ra, double dec) {
        LinkedList<ImageHistoryItem> l = ((LinkedList<ImageHistoryItem>) _historyList.clone());
        if (_filename != null) {
            l.add(0, makeImageHistoryItem()); // check the current image first
        }
        ImageHistoryItem closestHistoryItem = null;
        Double closestDist = null;
        ListIterator<ImageHistoryItem> it = l.listIterator(0);
        while (it.hasNext()) {
            ImageHistoryItem historyItem = it.next();
            File file = new File(historyItem.data.filename);
            if (!file.exists()) {
                // remove dead item
                _historyList.remove(historyItem);
                continue;
            }
            Double dist = historyItem.match(ra, dec);
            if (dist != null) {
                if (closestDist == null || dist < closestDist) {
                    closestDist = dist;
                    closestHistoryItem = historyItem;
                }
            }
        }
        if (closestHistoryItem != null) {
            if (_filename != null && _filename.equals(closestHistoryItem.data.filename))
                return false; // already displaying the file
            ImageDisplayMenuBar.setCurrentImageDisplay(this);
            closestHistoryItem.actionPerformed(null);
            return true;
        }

        blankImage(ra, dec);
        return false;
    }


    /**
     * Add history items (for previously loaded images) to the given menus
     */
    public void addHistoryMenuItems(JMenu menu) {
        ListIterator<ImageHistoryItem> it = _historyList.listIterator(0);
        while (it.hasNext()) {
            ImageHistoryItem historyItem = it.next();
            File file = new File(historyItem.data.filename);
            if (!file.exists()) {
                // remove dead item
                it.remove();
                continue;
            }
            menu.add(historyItem);
        }
    }


    /**
     * register to receive change events from this object whenever the
     * image or cut levels are changed.
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }


    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }


    /**
     * Notify any listeners of a change in the image or cut levels.
     */
    protected void fireChange(ImageChangeEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(e);
            }
        }
        e.reset();
    }

    /**
     * Return true if this is the main application window (enables exit menu item)
     */
    public boolean isMainWindow() {
        return true;
    }

    /**
     * Exit the application (called from exit menu).
     */
    public void exit() {
        if (!checkSave())
            return;
        System.exit(0);
    }


    /**
     * This method is called before and after a new image is loaded, each time
     * with a different argument.
     *
     * @param before set to true before the image is loaded and false afterwards
     */
    protected void newImage(boolean before) {
        super.newImage(before);
        if (!before) {
//            // If there are multiple images and/or tables in the image file, pop up
//            // a dialog to select one. (Wait a bit, in case main image window is not
//            // up yet.)
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
//                    checkExtensions(false);
//                }
//            });

            // check for graphics items saved in a FITS binary table named ".GRAPHICS"
            // (also produced by skycat)
            _fitsGraphics.loadGraphicsFromImage(".GRAPHICS");

            updateEnabledStates();
        }

        // notify listeners that a new image was (or will be) loaded
        _imageChangeEvent.setNewImage(true);
        _imageChangeEvent.setBefore(before);
        fireChange(_imageChangeEvent);
    }

    /**
     * This method updates the source image for this window, which is
     * scaled to the correct magnification before displaying.
     */
    protected void updateImage(PlanarImage im) {
        super.updateImage(im);
        if (im == null) {
            fireChange(_imageChangeEvent);
            return;
        }

        fireChange(_imageChangeEvent);
    }


    /**
     * Set the origin of the image to display in canvas coordinates.
     */
    public void setOrigin(Point2D.Double origin) {
        super.setOrigin(origin);
        _imageChangeEvent.setNewOrigin(true);
    }


    /**
     * Set the scale (zoom factor) for the image.
     * This also adjusts the origin so that the center of the image remains about the same.
     */
    public void setScale(float scale) {
        super.setScale(scale);
        _imageChangeEvent.setNewScale(true);
    }


    /**
     * If there are multiple images and/or tables in the image file, pop up
     * a dialog to select the one to display.
     *
     * @param show if true, always show the window, if there are any extensions,
     *             otherwise, just update the window if it is already showing
     *             or show it if the primary extension is empty.
     */
    public void checkExtensions(boolean show) {
        FITSImage fitsImage = getFitsImage();
        int numHDUs;
        if (fitsImage == null || (numHDUs = fitsImage.getNumHDUs()) <= 1) {
            if (_fitsHDUChooser != null) {
                _fitsHDUChooser.clear();
                _fitsHDUChooser.setShow(false);
            }
            return;
        }

        // Must be more than one FITS extension: update and show the window if it was already open
        if (!show)
            show = (_fitsHDUChooserFrame != null && _fitsHDUChooserFrame.isVisible());

        // check the primary HDU to see if it is empty
        int currentHDU = fitsImage.getCurrentHDUIndex();
        boolean skipEmptyPrimary = (numHDUs >= 2)
                && currentHDU == 0
                && fitsImage.isEmpty()
                && (fitsImage.getHDU(1) instanceof ImageHDU);

        if (!show)
            show = skipEmptyPrimary && numHDUs > 2;

        if (!show && numHDUs <= 1)
            return;

        // pop up a window to choose the HDU to view
        if (_fitsHDUChooser != null) {
            _fitsHDUChooser.updateDisplay(fitsImage);
        } else {
            _fitsHDUChooserFrame = new FITSHDUChooserFrame(this, fitsImage);
            _fitsHDUChooser = ((FITSHDUChooserFrame) _fitsHDUChooserFrame).getFitsHDUChooser();
        }

        if (skipEmptyPrimary)
            _fitsHDUChooser.selectImage(1);

        _fitsHDUChooser.setShow(show);
    }


    /**
     * Save the current image graphics to a binary FITS table in the current image
     */
    public void saveGraphicsWithImage() {
        try {
            _fitsGraphics.saveGraphicsWithImage(".GRAPHICS");
            setSaveNeeded(true);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    /**
     * Close the window
     */
    public void close() {
        if (isMainWindow()) {
            if (!checkSave())
                return;
            dispose();
        } else {
            if (_parent != null)
                _parent.setVisible(false);
        }
    }

    /**
     * Cleanup when the window is no longer needed.
     */
    public void dispose() {
        if (_imageCutLevelsFrame != null) {
            if (_imageCutLevelsFrame instanceof JFrame)
                ((JFrame) _imageCutLevelsFrame).dispose();
            else
                ((JInternalFrame) _imageCutLevelsFrame).dispose();
        }

        if (_imagePropertiesFrame != null) {
            if (_imagePropertiesFrame instanceof JFrame)
                ((JFrame) _imagePropertiesFrame).dispose();
            else
                ((JInternalFrame) _imagePropertiesFrame).dispose();
        }

        if (_imageColorsFrame != null) {
            if (_imageColorsFrame instanceof JFrame)
                ((JFrame) _imageColorsFrame).dispose();
            else
                ((JInternalFrame) _imageColorsFrame).dispose();
        }

        if (_pickObjectFrame != null) {
            if (_pickObjectFrame instanceof JFrame)
                ((JFrame) _pickObjectFrame).dispose();
            else
                ((JInternalFrame) _pickObjectFrame).dispose();
        }

        if (_fitsHDUChooserFrame != null) {
            if (_fitsHDUChooserFrame instanceof JFrame)
                ((JFrame) _fitsHDUChooserFrame).dispose();
            else
                ((JInternalFrame) _fitsHDUChooserFrame).dispose();
        }

        if (_parent instanceof JFrame) {
            ((JFrame) _parent).dispose();
        } else if (_parent instanceof JInternalFrame) {
            ((JInternalFrame) _parent).dispose();
        }
    }


    /**
     * Pop up a dialog window for displaying and setting the image cut levls.
     */
    public void editCutLevels() {
        if (_imageCutLevelsFrame != null) {
            SwingUtil.showFrame(_imageCutLevelsFrame);
        } else {
            _imageCutLevelsFrame = new ImageCutLevelsFrame(this);
        }
    }


    /**
     * Pop up a dialog window for displaying and setting the image colors.
     */
    public void editColors() {
        if (_imageColorsFrame != null) {
            SwingUtil.showFrame(_imageColorsFrame);
        } else {
            _imageColorsFrame = new ImageColorsFrame(this);
        }
    }


    /**
     * Pop up a dialog window to select objects (stars, galaxies) in the image
     */
    public void pickObject() {
        if (_pickObjectFrame != null) {
            SwingUtil.showFrame(_pickObjectFrame);
        } else {
            // create new frame
            _pickObjectFrame = new PickObjectFrame(this);
            _pickObjectPanel = ((PickObjectFrame) _pickObjectFrame).getPickObject();
            _pickObjectPanel.addActionListener(e -> pickedObject());
        }

        SwingUtilities.invokeLater(_pickObjectPanel::pickObject);
    }

    /**
     * Called when an object is selected in the Pick Object window.
     */
    protected void pickedObject() {
        // Do Nothing
    }

    /**
     * Pop up a dialog window for displaying the FITS Keywords.
     */
    public void viewFitsKeywords() {
        if (_fitsKeywordsFrame != null) {
            SwingUtil.showFrame(_fitsKeywordsFrame);
            if (_fitsKeywordsFrame instanceof FITSKeywordsFrame) {
                ((FITSKeywordsFrame) _fitsKeywordsFrame).getFITSKeywords().updateDisplay();
            }
        } else {
            _fitsKeywordsFrame = new FITSKeywordsFrame(this);
        }
    }

    /**
     * Pop up a dialog window for displaying the image properties.
     */
    public void viewImageProperties() {
        if (_imagePropertiesFrame != null) {
            SwingUtil.showFrame(_imagePropertiesFrame);
            if (_imagePropertiesFrame instanceof ImagePropertiesFrame) {
                ((ImagePropertiesFrame) _imagePropertiesFrame).getImageProperties().updateDisplay();
            } else if (_imagePropertiesFrame instanceof ImagePropertiesInternalFrame) {
                ((ImagePropertiesInternalFrame) _imagePropertiesFrame).getImageProperties().updateDisplay();
            }
        } else {
                _imagePropertiesFrame = new ImagePropertiesFrame(this);
        }
    }

    /**
     * Display a file chooser to select a filename to display
     */
    public void open() {
        if (_fileChooser == null) {
            _fileChooser = makeImageFileChooser();
        }
        int option = _fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION && _fileChooser.getSelectedFile() != null) {
            setFilename(_fileChooser.getSelectedFile().getAbsolutePath());
        }
    }


    /**
     * Create and return a new file chooser to be used to select an image file
     * to display.
     */
    public static JFileChooser makeImageFileChooser() {
        JFileChooser fileChooser = new JFileChooser(new File("."));

        ExampleFileFilter fitsFilter = new ExampleFileFilter(new String[]{
                "fits", "fits.gz", "fits.Z", "hfits"}, "FITS Image Files");
        fileChooser.addChoosableFileFilter(fitsFilter);

        ExampleFileFilter jpgFilter = new ExampleFileFilter(new String[]{
                "jpg", "jpeg"}, "JPEG Compressed Image Files");
        fileChooser.addChoosableFileFilter(jpgFilter);

        ExampleFileFilter gifFilter = new ExampleFileFilter("gif", "GIF Image Files");
        fileChooser.addChoosableFileFilter(gifFilter);

        ExampleFileFilter tifFilter = new ExampleFileFilter(new String[]{"tif", "tiff"}, "TIFF Image Files");
        fileChooser.addChoosableFileFilter(tifFilter);

        ExampleFileFilter ppmFilter = new ExampleFileFilter(new String[]{"ppm", "png", "pgm"}, "PPM Image Files");
        fileChooser.addChoosableFileFilter(ppmFilter);

        fileChooser.setFileFilter(fitsFilter);

        return fileChooser;
    }


    /**
     * Display a dialog to enter a URL to display
     */
    public void openURL() {
        String urlStr = DialogUtil.input("Enter the World Wide Web location (URL) to display:");
        if (urlStr != null) {
            URL url;
            try {
                url = new URL(urlStr);
            } catch (Exception e) {
                DialogUtil.error(e);
                return;
            }
            setURL(url);
        }
    }

    /**
     * Display a blank image with the given center coordinates
     * (15' * 60 seconds/minutes).
     *
     * @param ra  RA center coordinate in deg J2000
     * @param dec Dec center coordinate in deg J2000
     */
    @Override
    public void blankImage(double ra, double dec) {
        if (!checkSave())
            return;

        addToHistory();
        super.blankImage(ra, dec);
        _filename = null;
        _url = null;
        _origURL = null;
        updateEnabledStates();
    }

    /**
     * Set to true if the image file has been modified and needs saving.
     */
    @Override
    public void setSaveNeeded(boolean b) {
        saveNeeded = b;

        // fire a change event for the edit state
        _imageChangeEvent.setEditStateChanged(true);
        fireChange(_imageChangeEvent);
    }

    /**
     * If the current image file has been modified (by adding or deleting a FITS extension,
     * for example), ask the user to confirm saving it.
     *
     * @return false if the user pressed Cancel when asked to save the file,
     *         otherwise true
     */
    private boolean checkSave() {
        if (saveNeeded) {
            String s = _filename;
            if (s != null) {
                s = new File(s).getName(); // don't display the full path
            } else {
                if (_url != null)
                    s = _url.toString();
                else
                    s = "unknown";
            }
            int ans = DialogUtil.confirm("Save changes to '" + s + "'?");
            if (ans == JOptionPane.YES_OPTION) {
                if (_filename != null)
                    save();
                else
                    saveAs();
            } else if (ans == JOptionPane.CANCEL_OPTION) {
                return false;
            }

            setSaveNeeded(false);
        }
        return true;
    }


    /**
     * Save any changes to the current image file.
     */
    public void save() {
        if (_filename != null)
            saveAs(_filename);
        else
            saveAs();
    }


    /**
     * Pop up a dialog to ask the user for a file name, and then save the image
     * to the selected file.
     */
    @Override
    public void saveAs() {
        if (_saveDialog == null)
            _saveDialog = new ImageSaveDialog(this);
        _saveDialog.save();
    }

    /**
     * Save the current image to the given file, using an image format
     * based on the file suffix, which should be one of ".fits", ".jpg",
     * ".png", or ".tif".
     */
    @Override
    public void saveAs(String filename) {
        String s = filename.toLowerCase();
        String tmpFile = filename + ".TMP";

        // under windows, an existing file must first be deleted?
        File tf = new File(tmpFile);
        if (tf.exists() && !tf.delete()) {
            DialogUtil.error("Can't delete temp file: " + tmpFile);
            return;
        }


        if (s.endsWith(".jpeg") || s.endsWith(".jpg")) {
            JAI.create("filestore", getDisplayImage(), tmpFile, "JPEG", new JPEGEncodeParam());
        } else if (s.endsWith(".png")) {
            JAI.create("filestore", getImage(), tmpFile, "PNG", new PNGEncodeParam.Gray());
        } else if (s.endsWith(".pnm")) {
            JAI.create("filestore", getImage(), tmpFile, "PNM", new PNMEncodeParam());
        } else if (s.endsWith(".tiff") || s.endsWith(".tif")) {
            JAI.create("filestore", getImage(), tmpFile, "TIFF", new TIFFEncodeParam());
        } else if (s.endsWith(".bmp")) {
            JAI.create("filestore", getDisplayImage(), tmpFile, "BMP", new BMPEncodeParam());
        } else {
            // assume FITS format
            FITSImage fitsImage = getFitsImage();
            if (fitsImage != null && _url != null) {
                try {
                    BufferedFile bf = new BufferedFile(tmpFile, "rw");
                    fitsImage.getFits().write(bf);
                    bf.close();

                    // under Windows, this is necessary to avoid an error
                    fitsImage.getFits().getStream().close();
                } catch (Exception e) {
                    DialogUtil.error(e);
                }
            } else {
                // XXX the FITS codec code for the call below is not implemented yet...
                // XXX wait for new Image I/O package
                //JAI.create("filestore", getImage(), tmpFile, "FITS", new FITSEncodeParam());

                DialogUtil.error("Can't determine image format for: " + filename);
            }
        }

        // backup file if it exists
        File file = new File(filename);
        if (file.exists()) {
            File backup = new File(filename + ".BAK");
            if (backup.exists() && !backup.delete()) {
                DialogUtil.error("Can't delete backup file: " + backup);
                return;
            }

            if (!file.renameTo(backup)) {
                DialogUtil.error("Rename " + file + " to " + backup + " failed");
                return;
            }
        }

        // Rename tmpFile to filename
        if (!new File(tmpFile).renameTo(file)) {
            DialogUtil.error("Rename " + tmpFile + " to " + file + " failed");
            return;
        }

        setSaveNeeded(false);

        // make sure we are viewing the new file
        _noStack = true;
        try {
            setFilename(filename);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
        _noStack = false;
    }

    /**
     * Paint the image and graphics to the given graphics object (for save and print features)
     */
    @Override
    public void paintImageAndGraphics(Graphics2D g2D) {
        getCanvasPane().paint(g2D);
    }

    /**
     * Display a preview of the what the printed image view will look like.
     */
    public void printPreview() {
        if (_printDialog == null)
            _printDialog = new ImagePrintDialog(this);
        _printDialog.preview();
    }

    /**
     * Pop up a dialog for printing the image.
     */
    @Override
    public void print() {
        try {
            if (_printDialog == null)
                _printDialog = new ImagePrintDialog(this);
            _printDialog.print();
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Set the frame's basic title string. (Other info, the file name, etc. may still be appended).
     */
    public void setTitle(String s) {
        _title = s;
        updateTitle();
    }

    /**
     * Return the frame's title.
     */
    public String getTitle() {
        return _title;
    }

    /**
     * Set the frame's title.
     */
    protected void updateTitle() {
        String s = _title;
        if (_filename != null) {
            s += " - " + new File(_filename).getName();
        }
        if (_parent != null) {
            if (_parent instanceof JFrame)
                ((JFrame) _parent).setTitle(s);
            else
                ((JInternalFrame) _parent).setTitle(s);
        }
    }

    /**
     * Return the top level parent frame (or internal frame) used to close the window
     */
    public Component getParentFrame() {
        return _parent;
    }

    // These are for the GenericToolBarTarget interface
    public AbstractAction getOpenAction() {
        return _openAction;
    }

    public AbstractAction getBackAction() {
        return _backAction;
    }

    public AbstractAction getForwAction() {
        return _forwAction;
    }

    // Other menu actions
    public AbstractAction getColorsAction() {
        return _colorsAction;
    }

    public AbstractAction getCutLevelsAction() {
        return _cutLevelsAction;
    }

    public AbstractAction getSaveAction() {
        return _saveAction;
    }

    public AbstractAction getSaveAsAction() {
        return _saveAsAction;
    }

    public AbstractAction getPrintPreviewAction() {
        return _printPreviewAction;
    }

    public AbstractAction getPrintAction() {
        return _printAction;
    }

    /**
     * Return the top level window (or internal frame) for viewing the FITS keywords.
     */
    public Component getFitsKeywordsFrame() {
        return _fitsKeywordsFrame;
    }

    /**
     * Return the Pick Object panel, if initialized
     */
    public PickObject getPickObjectPanel() {
        return _pickObjectPanel;
    }

    /**
     * Return the base or center position in world coordinates.
     * If there is no base position, this method returns the center point
     * of the image. If the image does not support WCS, this method returns (0,0).
     * The position returned here should be used as the base position
     * for any catalog or image server requests.
     */
    @Override
    public WorldCoords getBasePos() {
        if (isWCS()) {
            WorldCoordinateConverter wcs = getWCS();
            return new WorldCoords(wcs.getWCSCenter(), wcs.getEquinox());
        }
        return new WorldCoords();
    }

}
