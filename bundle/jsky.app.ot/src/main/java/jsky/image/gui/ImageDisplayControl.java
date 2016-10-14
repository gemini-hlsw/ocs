package jsky.image.gui;

import java.awt.*;
import java.net.URL;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;

import jsky.app.ot.tpe.TpeImageWidget;
import jsky.util.I18N;

/**
 * Combines an ImageDisplay with a control panel, zoom, and pan windows.
 *
 * @version $Revision: 5923 $
 * @author Allan Brighton
 */
public class ImageDisplayControl extends JPanel {

    // Used to access internationalized strings (see i18n/gui*.properties)
    private static final I18N _I18N = I18N.getInstance(ImageDisplayControl.class);

    /** The top level parent frame (or internal frame) used to close the window */
    protected Component parent;

    /** Pan window */
    private ImagePanner imagePanner;

    /** Panel containing the pan and zoom windows */
    private JPanel panZoomPanel;

    /** Zoom window */
    private ImageZoom imageZoom;

    /** Main image display */
    protected TpeImageWidget imageDisplay;

    /** Color bar */
    private ImageColorbar colorbar;

    /** Panel displaying information about the current mouse position */
    private ImageDisplayStatusPanel imageDisplayStatusPanel;

    /** Optional filename to load image from. */
    protected String filename; // name of image file, if known

    /** Used to toggle the visibility of the pan/zoom windows */
    private BasicArrowButton panZoomToggleButton;

    /**
     * Construct an ImageDisplayControl widget.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public ImageDisplayControl(Component parent, int size) {
        super();
        this.parent = parent;

        imageDisplay = makeImageDisplay();
        imagePanner = makePanWindow(size);
        imageZoom = makeZoomWindow(size);
        panZoomToggleButton = makePanZoomToggleButton();

        // This is just the panel to hold the pan and zoom windows and
        // the toggle button. We need to redefine paintComponent to avoid
        // having the toggle button disappear when the pan image is painted.
        panZoomPanel = new JPanel() {

            @Override public synchronized void paintComponent(Graphics g) {
                super.paintComponent(g);
                panZoomToggleButton.repaint();
            }
        };

        colorbar = makeColorbar();
        imageDisplayStatusPanel = makeStatusPanel();
        imageDisplayStatusPanel.setImageDisplay(imageDisplay);

        makeLayout(size);
        filename = "";
    }

    /** Make and return the image display window */
    protected TpeImageWidget makeImageDisplay() {
        return (TpeImageWidget)new DivaMainImageDisplay(parent);
    }

    /**
     * Make and return the pan window.
     *
     * @param size the size (width, height) to use for the pan window.
     */
    private ImagePanner makePanWindow(int size) {
        return new ImagePanner(imageDisplay, size, size);
    }

    /**
     * Make and return the zoom window.
     *
     * @param size the size (width, height) to use for the zoom window.
     */
    private ImageZoom makeZoomWindow(int size) {
        return new ImageZoom(imageDisplay, size, size, 4.0F);
    }


    /**
     * Make and return a button for showing and hiding the pan/zoom panel
     */
    private BasicArrowButton makePanZoomToggleButton() {
        final BasicArrowButton b = new BasicArrowButton(SwingConstants.NORTH);
        b.setToolTipText(_I18N.getString("panZoomToggleTip"));
        b.addActionListener(e -> {
            if (panZoomPanel.isVisible()) {
                panZoomPanel.setVisible(false);
                imageZoom.setActive(false);
                b.setDirection(SwingConstants.SOUTH);
            } else {
                panZoomPanel.setVisible(true);
                imageZoom.setActive(true);
                b.setDirection(SwingConstants.NORTH);
            }
        });

        return b;
    }

    /**
     * Make and return the colorbar window.
     */
    private ImageColorbar makeColorbar() {
        return new ImageColorbar(imageDisplay);
    }

    /** Make and return the status panel */
    protected ImageDisplayStatusPanel makeStatusPanel() {
        return new ImageDisplayStatusPanel();
    }

    /**
     * This method is responsible for the window layout for this widget.
     *
     * @param size the initial size (width, height) to use for the pan and zoom windows.
     */
    private void makeLayout(int size) {
        setLayout(new BorderLayout());
        colorbar.setBorder(BorderFactory.createEtchedBorder());
        colorbar.setPreferredSize(new Dimension(0, 20));
        JPanel imagePanel = new JPanel();

        // The layout is a bit tricky here, since we want to have the pan and zoom windows
        // appear inside the image frame, overlapping the image, to save space.
        GridBagLayout layout = new GridBagLayout();
        imagePanel.setLayout(layout);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.weightx = gbc.weighty = 1;
        layout.setConstraints(imageDisplay, gbc);

        // Put the pan and zoom windows in a separate panel
        panZoomPanel.setLayout(new BoxLayout(panZoomPanel, BoxLayout.Y_AXIS));
        imagePanner.setBorder(new LineBorder(getBackground(), 1));
        panZoomPanel.add(imagePanner);
        imageZoom.setBorder(new LineBorder(getBackground(), 1));
        panZoomPanel.add(imageZoom);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.weightx = gbc.weighty = 0;
        layout.setConstraints(panZoomPanel, gbc);
        layout.setConstraints(panZoomToggleButton, gbc);

        // Note that the order is important below, so that the panZoomPanel is on top
        imagePanel.add(panZoomToggleButton);
        imagePanel.add(panZoomPanel);
        imagePanel.add(imageDisplay);

        add(imagePanel, BorderLayout.CENTER);

        JPanel bot = new JPanel();
        bot.setLayout(new BorderLayout());
        bot.add(colorbar, BorderLayout.NORTH);
        bot.add(imageDisplayStatusPanel, BorderLayout.SOUTH);
        add(bot, BorderLayout.SOUTH);
    }


    /** Return the main image display widget */
    public TpeImageWidget getImageDisplay() {
        return imageDisplay;
    }

    /**
     * Set the background color of the image display, pan and zoom windows.
     */
    public void setImageBackground(Color bg) {
        if (imageDisplay != null) {
            imageDisplay.setBackground(bg);
        }
        if (imagePanner != null) {
            ((JComponent)imagePanner.getImageDisplay()).setBackground(bg);
        }
        if (imageZoom != null) {
            ((JComponent)imageZoom.getImageDisplay()).setBackground(bg);
        }
    }
}
