package jsky.image.gui;

import jsky.coords.WorldCoords;
import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.LinkedList;

/**
 * Used to store information about previously viewed images.
 * For downloaded files, the filename is the name of a temp file that can
 * be accessed only in this session. If the application is restarted, the
 * URL will have to be used instead.
 */
public final class ImageHistoryItem extends AbstractAction implements ChangeListener {

    // immutable serializable core separate from the Action part
    public static final class Data implements Serializable {
        /** The title for this item */
        final String title;

        /** The RA coordinate of the image center (or Double.NaN if not known) */
        final double raDeg;

        /** The Dec coordinate of the image center (or Double.NaN if not known) */
        final double decDeg;

            /** The image width in deg (or Double.NaN if not known) */
        final double widthDeg;

        /** The image height in deg (or Double.NaN if not known) */
        final double heightDeg;

        /** The original image URL */
        final URL url;

        /** Filename (may be a temp download file) */
        final String filename;

        /** Color map used */
        final String cmap;

        /** Intensity table used */
        final String itt;

        /** Low cut */
        final double hcut;

        /** High cut */
        final double lcut;

        /** True if user set the cut levels */
        final boolean userSetCutLevels;

        /** Name of the image lookup scale algorithm */
        final int scaleAlg;

        /** magnification factor */
        final float scale;

        /**
         * Create an image history item data based on the given arguments.
         *
         * @param imageDisplay the image display widget
         * @param raDeg the image center RA coordinate
         * @param decDeg the image center Dec coordinate
         * @param widthDeg the image width in deg
         * @param heightDeg the image height in deg
         * @param title the title for the history menu
         * @param url the URL for the original image
         * @param filename the local filename, if downloaded
         */
        public Data(MainImageDisplay imageDisplay, double raDeg, double decDeg,
                                double widthDeg, double heightDeg, String title,
                                URL url, String filename) {
            this.raDeg = raDeg;
            this.decDeg = decDeg;
            this.widthDeg = widthDeg;
            this.heightDeg = heightDeg;
            this.title = title;
            this.filename = new File(filename).getAbsolutePath();
            this.url = url;
            this.scale = imageDisplay.getScale();

            ImageProcessor imageProcessor = imageDisplay.getImageProcessor();
            this.cmap = imageProcessor.getColorLookupTableName();
            this.itt = imageProcessor.getIntensityLookupTableName();
            this.hcut = imageProcessor.getHighCut();
            this.lcut = imageProcessor.getLowCut();
            this.userSetCutLevels = imageProcessor.isUserSetCutLevels();
            this.scaleAlg = imageProcessor.getScaleAlgorithm();
        }

        /**
         * Returns the distance from the history item image center to the given ra,dec position
         * in arcmin, if the position is on the image, otherwise null.
         */
        public Double match(double raDeg, double decDeg) {
            if (Double.isNaN(this.raDeg) || Double.isNaN(this.decDeg)
                    || Double.isNaN(widthDeg) || Double.isNaN(heightDeg))
                return null;

            double diff = Math.abs(raDeg - this.raDeg);
            if (diff >= widthDeg/2.0) {
                // Make sure that if both RAs are around 0, they aren't close enough.
                if (raDeg > (360.0 - widthDeg/2.0)) {
                    raDeg = raDeg - 360;
                } else if (raDeg < widthDeg/2.0) {
                    raDeg = raDeg + 360;
                } else {
                    return null;
                }

                diff = Math.abs(raDeg - this.raDeg);
                if (diff > widthDeg/2.0) {
                    return null;
                }
            }

            diff = Math.abs(decDeg - this.decDeg);
            if (diff > heightDeg/2.0) {
                return null;
            }
            return WorldCoords.dist(raDeg, decDeg, this.raDeg, this.decDeg);
        }
    }

    final Data data;

    /**
     * Create an image history item based on the given arguments.
     *
     * @param imageDisplay the image display widget
     * @param raDeg the image center RA coordinate
     * @param decDeg the image center Dec coordinate
     * @param widthDeg the image width in deg
     * @param heightDeg the image height in deg
     * @param title the title for the history menu
     * @param url the URL for the original image
     * @param filename the local filename, if downloaded
     */
    public ImageHistoryItem(MainImageDisplay imageDisplay, double raDeg, double decDeg,
                            double widthDeg, double heightDeg, String title,
                            URL url, String filename) {
        super(title);
        data = new Data(imageDisplay, raDeg, decDeg, widthDeg, heightDeg, title, url, filename);
    }

    public ImageHistoryItem(Data d) {
        super(d.title);
        data = d;
    }

    /**
     * Returns the distance from the history item image center to the given ra,dec position
     * in arcmin, if the position is on the image, otherwise null.
     */
    public Double match(double raDeg, double decDeg) {
        return data.match(raDeg, decDeg);
    }

    /**
     * Load the file if it exists, otherwise the URL, and arrange to restore the history
     * settings once the image is loaded.
     */
    public void actionPerformed(ActionEvent evt) {
        MainImageDisplay imageDisplay = ImageDisplayMenuBar.getCurrentImageDisplay();
        if (data.filename != null && new File(data.filename).exists()) {
            imageDisplay.addChangeListener(this);
            imageDisplay.setFilename(data.filename, data.url);
        } else if (data.url != null) {
            imageDisplay.addChangeListener(this);
            imageDisplay.setURL(data.url);
        } else {
            System.out.println("XXX ImageHistoryItem.actionPerformed: no file and no URL");
        }
    }

    /** Called when the image is actually loaded, so we can restore the settings */
    public void stateChanged(ChangeEvent ce) {
        ImageChangeEvent e = (ImageChangeEvent) ce;
        if (e.isNewImage() && !e.isBefore()) {
            DivaMainImageDisplay imageDisplay = (DivaMainImageDisplay) e.getSource();
            ImageProcessor imageProcessor = imageDisplay.getImageProcessor();
            imageDisplay.removeChangeListener(this);

            // restore image processor settings
            imageProcessor.setColorLookupTable(data.cmap);
            imageProcessor.setIntensityLookupTable(data.itt);
            imageProcessor.setScaleAlgorithm(data.scaleAlg);
            imageProcessor.setCutLevels(data.lcut, data.hcut, data.userSetCutLevels);
            imageDisplay.setScale(data.scale);

            imageProcessor.update();
        }
    }

    public static LinkedList<Data> map(LinkedList<ImageHistoryItem> history) {
        final LinkedList<Data> res = new LinkedList<>();
        for (ImageHistoryItem ihi : history) {
            res.add(ihi.data);
        }
        return res;
    }

    public static LinkedList<ImageHistoryItem> apply(LinkedList<Data> data) {
        final LinkedList<ImageHistoryItem> res = new LinkedList<>();
        for (Data d : data) {
            res.add(new ImageHistoryItem(d));
        }
        return res;
    }
}


