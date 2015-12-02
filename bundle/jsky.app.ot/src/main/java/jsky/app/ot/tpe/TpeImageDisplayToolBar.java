package jsky.app.ot.tpe;

import jsky.image.gui.ImageDisplayToolBar;

import javax.swing.*;

/**
 * A tool bar for the image display window.
 */
class TpeImageDisplayToolBar extends ImageDisplayToolBar {

    // toolbar buttons
    private JButton manualGuideStarButton;

    /**
     * Create the toolbar for the given window
     */
    TpeImageDisplayToolBar(TpeImageWidget imageDisplay) {
        super(imageDisplay);
    }

    /**
     * Add the items to the tool bar.
     */
    @Override
    public void addToolBarItems() {
        // This is very poor. This is called during the super constructor, not after the main constructor completes...
        super.addToolBarItems();

        addSeparator();

        add(makeImageButton());

        addSeparator();

        add(makeManualGuideStarButton());
    }

    /**
     * Make the guide star search button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the guide star button
     */
    private JButton makeManualGuideStarButton() {
        if (manualGuideStarButton == null) {
            final AbstractAction a = ((TpeImageWidget)getImageDisplay()).getManualGuideStarAction();
            manualGuideStarButton = makeButton((String)a.getValue(Action.SHORT_DESCRIPTION), a);
        }

        updateButton(manualGuideStarButton,
                "Manual GS",
                jsky.util.Resources.getIcon("gsmanual.png", this.getClass()));
        return manualGuideStarButton;
    }

    /**
     * Make the default Image search button, if it does not yet exists.
     *
     * @return the Image Search button
     */
    protected JButton makeImageButton() {
        if (skyImageButton == null) {
            final AbstractAction a = ((TpeImageWidget)getImageDisplay()).getSkyImageAction();
            skyImageButton = makeButton((String)a.getValue(Action.SHORT_DESCRIPTION), a);
        }

        updateButton(skyImageButton,
                     "Images",
                     jsky.util.Resources.getIcon("sky.gif", this.getClass()));
        return skyImageButton;
    }

    /**
     * Update the toolbar display using the current text/pictures options.
     * (redefined from the parent class).
     */
    public void update() {
        super.update();
        makeManualGuideStarButton();
        makeImageButton();
    }

}
