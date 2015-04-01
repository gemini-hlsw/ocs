package edu.gemini.itc.web.html;

import edu.gemini.itc.shared.*;

import java.awt.image.BufferedImage;
import java.io.PrintWriter;

public abstract class PrinterBase {

    public abstract void writeOutput();

    private final PrintWriter _out;

    protected PrinterBase(final PrintWriter pr) {
        _out = pr;
    }

    /* TODO: this needs to be validated for spectroscopy for all instruments, find a better place to do this */
    protected void validatePlottingDetails(final PlottingDetails pdp, final Instrument instrument) {
        if (pdp != null && pdp.getPlotLimits().equals(PlottingDetails.PlotLimits.USER)) {
            if (pdp.getPlotWaveL() > instrument.getObservingEnd() || pdp.getPlotWaveU() < instrument.getObservingStart()) {
                throw new IllegalArgumentException("User limits for plotting do not overlap with filter.");
            }
        }
    }

    protected void _print(String s) {
        if (_out == null) {
            s = s.replaceAll("<br>", "\n");
            s = s.replaceAll("<BR>", "\n");
            s = s.replaceAll("<LI>", "-");
            System.out.print(s);
        } else {
            s = s.replaceAll("\n", "<br>");
            _out.print(s.replaceAll("\n", "<br>") + "<br>");
        }
    }

    protected void _println(final BufferedImage image, final String imageName) {
        try {
            final String fileName = ITCImageFileIO.saveCharttoDisk(image);
            _print("<IMG alt=\"" + fileName
                    + "\" height=500 src=\"" + ServerInfo.getServerURL()
                    + "itc/servlet/images?type=img&filename="
                    + fileName + "\" width=675 border=0>");
        } catch (Exception ex) {
            System.out.println("Unable to save file");
            _print("<br>Failed to save image " + imageName + "<br>");
            ex.printStackTrace();
        }
    }

    protected void _println(final String s) {
        _print(s);
        if (_out == null)
            System.out.println();
        else
            _out.println();
    }

    // Display an error text
    protected void _error(final String s) {
        _println("<span style=\"color:red; font-style:italic;\">" + s + "</span>");
    }

    protected String _printSpecTag(final String spectrumName) {
        String Filename = "";

        try {
            Filename = ITCImageFileIO.getRandomFileName(".dat");

            _println("<a href ="
                    +
                    "\"" + ServerInfo.getServerURL()
                    + "itc/servlet/images?type=txt&filename=" + Filename
                    + "\"> Click here for " + spectrumName + ". </a>");
        } catch (Exception ex) {
            System.out.println("Unable to get random file");
            ex.printStackTrace();
        }
        return Filename;
    }

    protected void _println(final VisitableSampledSpectrum sed, final String header, final String spectrumName) {
        // this will print out the VisitableSampled Spectrum as a text file to
        // be taken by the user

        try {
            ITCImageFileIO.saveSedtoDisk(header, sed, spectrumName);
        } catch (Exception ex) {
            System.out.println("Unable to save file");
            ex.printStackTrace();
        }
    }

    protected void _println(final VisitableSampledSpectrum sed, final String header, final String spectrumName, final int firstIndex, final int lastIndex) {
        // this will print out the VisitableSampled Spectrum as a text file to
        // be taken by the user

        try {
            ITCImageFileIO.saveSedtoDisk(header, sed, spectrumName, firstIndex, lastIndex);
        } catch (Exception ex) {
            System.out.println("Unable to save file");
            ex.printStackTrace();
        }
    }


}
