package jsky.app.ot.tpe;

import javax.swing.*;
import jsky.navigator.NavigatorImageDisplayToolBar;

/**
 * A tool bar for the image display window.
 */
public class TpeImageDisplayToolBar extends NavigatorImageDisplayToolBar {

    // toolbar buttons
    private JButton skyImageButton;
    private JButton manualGuideStarButton;

    /**
     * Create the toolbar for the given window
     */
    public TpeImageDisplayToolBar(TpeImageWidget imageDisplay) {
        super(imageDisplay);
    }

    /**
     * Add the items to the tool bar.
     */
    protected void addToolBarItems() {
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
    protected JButton makeManualGuideStarButton() {
        if (manualGuideStarButton == null) {
            final AbstractAction a = ((TpeImageWidget)imageDisplay).getManualGuideStarAction();
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
            final AbstractAction a = ((TpeImageWidget)imageDisplay).getSkyImageAction();
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
