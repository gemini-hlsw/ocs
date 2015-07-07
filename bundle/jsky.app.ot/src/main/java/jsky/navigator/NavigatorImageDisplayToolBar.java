package jsky.navigator;

import javax.swing.JButton;

import jsky.image.gui.ImageDisplayToolBar;
import jsky.util.I18N;
import jsky.util.Resources;

/**
 * A tool bar for the image display window.
 */
public class NavigatorImageDisplayToolBar extends ImageDisplayToolBar {

    // Used to access internationalized strings (see i18n/gui*.properties)
    private static final I18N _I18N = I18N.getInstance(NavigatorImageDisplayToolBar.class);

    // toolbar buttons
    protected JButton catalogButton;
    protected JButton newCatalogButton;

    /**
     * Create the toolbar for the given window
     */
    public NavigatorImageDisplayToolBar(NavigatorImageDisplay imageDisplay) {
        super(imageDisplay);
    }

    /**
     * Add the items to the tool bar.
     */
    protected void addToolBarItems() {
        super.addToolBarItems();
        addSeparator();
        add(makeCatalogButton());
        add(makeNewCatalogButton());
    }

    /**
     * Make the catalog button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the catalog button
     */
    protected JButton makeCatalogButton() {
        if (catalogButton == null)
            catalogButton = makeButton(_I18N.getString("showCatalogWindow"),
                                       ((NavigatorImageDisplay) imageDisplay).getCatalogBrowseAction());

        updateButton(catalogButton,
                _I18N.getString("catalogs"),
                Resources.getIcon("Catalog24.gif", this.getClass()));
        return catalogButton;
    }

    /**
     * Make the new catalog button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the catalog button
     */
    protected JButton makeNewCatalogButton() {
        if (newCatalogButton == null)
            newCatalogButton = makeButton(_I18N.getString("showCatalogWindow"),
                                       ((NavigatorImageDisplay) imageDisplay).getCatalogBrowseAction());

        updateButton(newCatalogButton,
                     "New Catalog",
                     Resources.getIcon("Catalog24.gif", this.getClass()));
        return newCatalogButton;
    }


    /**
     * Update the toolbar display using the current text/pictures options.
     * (redefined from the parent class).
     */
    public void update() {
        super.update();
        makeCatalogButton();
        makeNewCatalogButton();
    }
}

