package jsky.image.gui;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.ScalaConverters;
import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Used to store information about previously viewed images.
 * For downloaded files, the filename is the name of a temp file that can
 * be accessed only in this session. If the application is restarted, the
 * URL will have to be used instead.
 */
final class ImageHistoryItem extends AbstractAction implements ChangeListener {

    final ImageItemDescriptor data;

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
    ImageHistoryItem(MainImageDisplay imageDisplay, double raDeg, double decDeg,
                     double widthDeg, double heightDeg, String title,
                     URL url, String filename) {
        super(title);
        data = ImageItemDescriptor.apply(imageDisplay, raDeg, decDeg, widthDeg, heightDeg, title, url, filename);
    }

    private ImageHistoryItem(ImageItemDescriptor d) {
        super(d.imageId().title());
        data = d;
    }

    /**
     * Returns the distance from the history item image center to the given ra,dec position
     * in arcmin, if the position is on the image, otherwise null.
     */
    public Option<Double> match(double raDeg, double decDeg) {
        return new ScalaConverters.ScalaOptionOps<>(data.matches4Java(raDeg, decDeg)).asGeminiOpt();
    }

    /**
     * Load the file if it exists, otherwise the URL, and arrange to restore the history
     * settings once the image is loaded.
     */
    public void actionPerformed(ActionEvent evt) {
        MainImageDisplay imageDisplay = ImageDisplayMenuBar.getCurrentImageDisplay();
        if (data.imageId().filename() != null && new File(data.imageId().filename()).exists()) {
            imageDisplay.addChangeListener(this);
            imageDisplay.setFilename(data.imageId().filename(), data.imageId().url());
        } else if (data.imageId().url() != null) {
            imageDisplay.addChangeListener(this);
            imageDisplay.setURL(data.imageId().url());
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
            imageProcessor.setColorLookupTable(data.imageDisplayProperties().cmap());
            imageProcessor.setIntensityLookupTable(data.imageDisplayProperties().itt());
            imageProcessor.setScaleAlgorithm(data.imageDisplayProperties().scaleAlg());
            imageProcessor.setCutLevels(data.imageDisplayProperties().lcut(), data.imageDisplayProperties().hcut(), data.imageDisplayProperties().userSetCutLevels());
            imageDisplay.setScale(data.imageDisplayProperties().scale());

            imageProcessor.update();
        }
    }

    public static LinkedList<ImageItemDescriptor> map(LinkedList<ImageHistoryItem> history) {
        return history.stream().map(ihi -> ihi.data).collect(Collectors.toCollection(LinkedList::new));
    }

    public static LinkedList<ImageHistoryItem> apply(LinkedList<ImageItemDescriptor> data) {
        return data.stream().map(ImageHistoryItem::new).collect(Collectors.toCollection(LinkedList::new));
    }
}


