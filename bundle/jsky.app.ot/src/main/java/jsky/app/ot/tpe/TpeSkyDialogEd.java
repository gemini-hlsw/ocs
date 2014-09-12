package jsky.app.ot.tpe;

import jsky.catalog.CatalogDirectory;
import jsky.catalog.Catalog;
import jsky.catalog.gui.CatalogNavigator;
import jsky.catalog.gui.StoreImageServerAction;
import jsky.util.gui.DialogUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for the SkyDialogForm
 */
public class TpeSkyDialogEd {

    private  TpeSkyDialogForm _dialog;

    private static TpeSkyDialogEd _instance;

    private TpeSkyDialogEd() {
        _dialog = new TpeSkyDialogForm();
        _dialog.setPreferredSize(new Dimension(400, 100));
        _initialize();
    }

    public static TpeSkyDialogEd getInstance() {
        if (_instance == null) {
            _instance = new TpeSkyDialogEd();
        }
        return _instance;
    }

    public void showDialog(Component parent) {
        int res = JOptionPane.showOptionDialog(parent,
                _dialog, "Select Image Server", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, null, null);

        if (res != JOptionPane.OK_OPTION) {
            return;
        }

        Catalog c = (Catalog)_dialog.catalogComboBox.getSelectedItem();
        // Store the catalog in the property file using the action... :)
        _dialog.catalogComboBox.setAction(StoreImageServerAction.getAction(c));
        _dialog.catalogComboBox.setSelectedItem(c);
        //perform the query
        TelescopePosEditor tpe = TpeManager.open();
        try {
            tpe.getSkyImage();
        } catch (Exception ex) {
            DialogUtil.error(parent, ex);
        }

    }

    private void _initialize() {
        CatalogDirectory dir = _getCatalogDirectory();
        if (dir == null) return;
        int nCat = dir.getNumCatalogs();
        for (int i = 0; i < nCat; i++) {
            Catalog c = dir.getCatalog(i);
            if (c != null && c.isImageServer()) {
                _dialog.catalogComboBox.addItem(c);
            }
        }
        //don't need to set a default one, since this dialog only
        //shows up when there is no default!
    }


     // return the catalog directory used by the catalog window
    private CatalogDirectory _getCatalogDirectory() {
        CatalogDirectory dir;
        try {
            dir = CatalogNavigator.getCatalogDirectory();
        } catch (Exception e) {
            DialogUtil.error(e);
            return null;
        }
        if (dir == null) {
            DialogUtil.error("No catalog config file was found");
        }
        return dir;
    }



}
