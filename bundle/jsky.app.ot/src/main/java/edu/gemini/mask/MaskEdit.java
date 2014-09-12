/**
 * $Id: MaskEdit.java 6526 2005-08-03 21:27:13Z brighton $
 */

package edu.gemini.mask;

import jsky.util.gui.Theme;
import jsky.util.gui.DialogUtil;
import jsky.navigator.NavigatorImageDisplayFrame;
import jsky.navigator.NavigatorImageDisplay;
import jsky.navigator.Navigator;
import jsky.navigator.NavigatorManager;
import jsky.navigator.NavigatorFrame;
import jsky.navigator.NavigatorMenuBar;
import jsky.navigator.NavigatorQueryTool;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.catalog.Catalog;
import jsky.catalog.gui.CatalogNavigatorMenuBar;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Main class for the mask editing software
 */
public class MaskEdit extends JFrame {

    // image display
    private NavigatorImageDisplayFrame _imageDisplayFrame = new NavigatorImageDisplayFrame();

    private NavigatorImageDisplay _imageDisplay =
            (NavigatorImageDisplay)_imageDisplayFrame.getImageDisplayControl()
            .getImageDisplay();

    // -- menu actions --
    private AbstractAction _loadFitsOtAction
            = new AbstractAction("Load FITS Object Table...") {
                public void actionPerformed(ActionEvent evt) {
                    _loadFitsOt();
                }
            };
    private AbstractAction _loadFitsOdfAction
            = new AbstractAction("Load FITS Minimal Object Definition File...") {
                public void actionPerformed(ActionEvent evt) {
                    _loadFitsOdf();
                }
            };

    // -- File chooser --
    private JFileChooser _fileChooser = new JFileChooser(new File("."));

    // Filter to select *OT.fits files
    private FileFilter _fitsOtFilter = new FileFilter() {
        public boolean accept(File f) {
            return f.getName().endsWith("OT.fits");
        }
        public String getDescription() {
            return "FITS Object Tables (OT)";
        }
    };

    // Filter to select *ODFn.fits files
    private FileFilter _fitsOdfFilter = new FileFilter() {
        public boolean accept(File f) {
            return f.getName().matches(".*ODF[0-9]+\\.fits");
        }
        public String getDescription() {
            return "FITS Object Definition Files (ODF)";
        }
    };

    private boolean _firstTime = true;


    /**
     * Initialize the GUI
     */
    public MaskEdit(String imageFile, String objectTable) {
        _imageDisplayFrame.getImageDisplayControl().getImageDisplay().setTitle(_getTitle());
        _addMaskMenu(_imageDisplayFrame.getJMenuBar());

        _fileChooser.addChoosableFileFilter(_fitsOtFilter);
        _fileChooser.addChoosableFileFilter(_fitsOdfFilter);

        _imageDisplayFrame.setVisible(true);

        if (imageFile != null) {
            _imageDisplay.setFilename(imageFile);
        }
        if (objectTable != null && objectTable.endsWith(".fits")) {
             _loadFitsTable(objectTable);
        }
    }

    private void _addMaskMenu(JMenuBar menuBar) {
        menuBar.add(createMaskMenu());
    }

    private JMenu createMaskMenu() {
        JMenu menu = new JMenu("Mask");
        menu.add(_loadFitsOtAction);
        menu.add(_loadFitsOdfAction);

        return menu;
    }

    private String _getTitle() {
        return MaskVersion.MASK_VERSION;
    }


    private void _loadFitsOt() {
        _fileChooser.setFileFilter(_fitsOtFilter);
        int option = _fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION &&
                _fileChooser.getSelectedFile() != null) {
            _loadFitsTable(_fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void _loadFitsOdf() {
        _fileChooser.setFileFilter(_fitsOdfFilter);
        int option = _fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION &&
                _fileChooser.getSelectedFile() != null) {
            _loadFitsTable(_fileChooser.getSelectedFile().getAbsolutePath());
        }
    }


    // Load a FITS object table file, display the table, and plot the
    // objects on the image
    private void _loadFitsTable(String filename) {
        SkycatCatalog catalog = null;
        try {
            catalog = ObjectTable.makeCatalog(filename);
        } catch (Exception e) {
            DialogUtil.error(this, e);
            return;
        }
        if (catalog != null) {
            if (_firstTime) {
                _firstTime = false;
                _initNavigator();
            }

            // This makes sure the ObjectTableDisplay component is reused for query results
            ((ObjectTable)catalog.getTable()).makeComponent(NavigatorManager.get());

            _imageDisplay.openCatalogWindow(catalog);
        }
    }

    // Add the Mask menubutton to the Navigator menu bar and make any changes in the default
    // appearance
    private void _initNavigator() {
        _imageDisplay.openCatalogWindow((Catalog)null);
        Navigator navigator = NavigatorManager.get();
        NavigatorFrame f = (NavigatorFrame)navigator.getParentFrame();
        f.setTitle("MaskEdit: Navigator");
        NavigatorMenuBar menuBar = (NavigatorMenuBar)f.getJMenuBar();
        _addMaskMenu(menuBar);

        // don't display the catalog tree
        CatalogNavigatorMenuBar.setCatalogTreeIsVisible(NavigatorQueryTool.class, false);

        // Make the navigator Open action handle FITS ObjectTables
        navigator.setOpenAction(new AbstractAction("Open") {
            public void actionPerformed(ActionEvent evt) {
                _loadFitsOt();
            }
        });
    }

    /**
     * MaskEdit main.
     *
     * @param args optional two args: first is FITS image, second is the object table file
     */
    public static void main(final String args[]) {
        // image display tile cache initialization
        int tilecache = 64;
        TileCache cache = JAI.getDefaultInstance().getTileCache();
        cache.setMemoryCapacity(tilecache * 1024 * 1024);

        Theme.installGreenTheme();

        // Set the default catalog config file URL
        SkycatConfigFile.setConfigFile(Resources.getResource("conf/skycat.cfg"));

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String imageFile = (args.length > 0 ? args[0] : null);
                String objectTable = (args.length > 1 ? args[1] : null);
                new MaskEdit(imageFile, objectTable);
            }
        });
    }
}
