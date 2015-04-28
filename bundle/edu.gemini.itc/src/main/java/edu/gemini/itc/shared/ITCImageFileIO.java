package edu.gemini.itc.shared;


import org.jfree.chart.ChartUtilities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public final class ITCImageFileIO {

    //Try to use the /tmp dir.  Every UNIX machine has one.
    private static final String _directory = "/tmp";
    private static final String _subDir = "/ITCTempImages";
    private static final File _tempDir = new File(File.separator + _directory + File.separator + _subDir);

    static {
        //Include code here to check Directory structure and Exit
        // if it cannot complete its task.
        if (!(_tempDir.exists())) {
            _tempDir.mkdir();
        }
    }

    /**
     * Copies a file from the temp file storage to the given output stream.
     * @param filename
     * @param out
     * @throws IOException
     */
    public static void sendFiletoServOut(final String filename, final OutputStream out) throws IOException {
        final FileInputStream in = new FileInputStream(getImagePath() + File.separator + filename);
        try (final BufferedInputStream bis = new BufferedInputStream(in)) {
            int data;
            while ((data = bis.read()) != -1) {
                out.write(data);
            }
        }
    }

    public static String saveCharttoDisk(final Image tmpChart) throws IOException {
        final File randomFileName = File.createTempFile("SessionID", ".png", _tempDir);
        final FileOutputStream outfile = new FileOutputStream(randomFileName);
        try (final OutputStream out = new BufferedOutputStream(outfile)) {
            ChartUtilities.writeBufferedImageAsPNG(out, (BufferedImage) tmpChart);
        }
        return randomFileName.getName();
    }

    public static String getImagePath() {
        return _tempDir.getPath();
    }

}
