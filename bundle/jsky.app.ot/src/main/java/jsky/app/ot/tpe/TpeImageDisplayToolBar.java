/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TpeImageDisplayToolBar.java 39256 2011-11-22 17:42:49Z swalker $
 */

package jsky.app.ot.tpe;

import javax.swing.*;

import jsky.navigator.NavigatorImageDisplayToolBar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * A tool bar for the image display window.
 */
public class TpeImageDisplayToolBar extends NavigatorImageDisplayToolBar {

    // toolbar buttons
    private JButton skyImageButton;
    private JButton manualGuideStarButton;
    private JButton autoGuideStarButton;

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
        add(makeAutoGuideStarButton());
    }

    protected JButton makeAutoGuideStarButton(){
        if (autoGuideStarButton == null) {
            final AbstractAction a = ((TpeImageWidget)imageDisplay).getAutoGuideStarAction();
            a.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    // OT-36: Update text to display "Cancel Search" while in progress...
                    autoGuideStarButton.setText((String)a.getValue(Action.NAME));
                    autoGuideStarButton.setVisible(a.isEnabled());
                }
            });
            autoGuideStarButton = makeButton((String)a.getValue(Action.SHORT_DESCRIPTION), a, false);
        }

        updateButton(autoGuideStarButton,
                     "Auto GS",
                     jsky.util.Resources.getIcon("gsauto.png", this.getClass()));
        return autoGuideStarButton;
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
            manualGuideStarButton = makeButton((String)a.getValue(Action.SHORT_DESCRIPTION), a, false);
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
            skyImageButton = makeButton((String)a.getValue(Action.SHORT_DESCRIPTION), a, false);
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
        makeAutoGuideStarButton();
        makeImageButton();
    }

    public JButton getManualGuideStarButton() {
        return manualGuideStarButton;
    }

    public JButton getSkyImageButton() {
        return skyImageButton;
    }
}

