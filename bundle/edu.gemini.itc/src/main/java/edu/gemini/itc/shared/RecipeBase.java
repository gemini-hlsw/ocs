package edu.gemini.itc.shared;

import edu.gemini.itc.operation.*;

import java.awt.image.BufferedImage;
import java.io.PrintWriter;

public abstract class RecipeBase implements Recipe {

    // Intermediate alculation results
    public interface IntermediateResult {}
    public static final class ImagingResult implements IntermediateResult {
        public final SourceFraction SFcalc;
        public final double peak_pixel_count;
        public final ImagingS2NCalculatable IS2Ncalc;
        public final ImageQualityCalculatable IQcalc;
        public ImagingResult(final ImageQualityCalculatable IQcalc, final SourceFraction SFcalc, final double peak_pixel_count, final ImagingS2NCalculatable IS2Ncalc) {
            this.IQcalc             = IQcalc;
            this.SFcalc             = SFcalc;
            this.peak_pixel_count   = peak_pixel_count;
            this.IS2Ncalc           = IS2Ncalc;
        }

    }
    public static final class SpectroscopyResult implements IntermediateResult {
        public final SourceFraction SFcalc;
        public final SpecS2NLargeSlitVisitor[] specS2N;
        public final SlitThroughput st;
        public final ImageQualityCalculatable IQcalc;
        public SpectroscopyResult(final SourceFraction SFcalc, final ImageQualityCalculatable IQcalc, final SpecS2NLargeSlitVisitor[] specS2N, final SlitThroughput st) {
            this.SFcalc             = SFcalc;
            this.IQcalc             = IQcalc;
            this.specS2N            = specS2N;
            this.st                 = st;
        }
    }

    // Results will be written to this PrintWriter if it is set.
    protected PrintWriter _out = null; // set from servlet request

    protected RecipeBase() {
    }

    protected RecipeBase(PrintWriter pr) {
        _out = pr;
    }

    // Prints string to implied destination. If _out is null, prints to
    // System.out otherwise prints to _out PrintWriter with html line breaks.

    // Prints string to implied destination. If _out is null, prints to
    // System.out otherwise prints to _out PrintWriter.
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

    protected void _println(BufferedImage image, String imageName) {
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

    protected void _println(String s) {
        _print(s);
        if (_out == null)
            System.out.println();
        else
            _out.println();
    }

    // Display an error text
    protected void _error(String s) {
        _println("<span style=\"color:red; font-style:italic;\">" + s + "</span>");
    }

    protected String _printSpecTag(String spectrumName) {
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

    protected void _println(VisitableSampledSpectrum sed, String header, String spectrumName) {
        // this will print out the VisitableSampled Spectrum as a text file to
        // be taken by the user

        try {
            ITCImageFileIO.saveSedtoDisk(header, sed, spectrumName);
        } catch (Exception ex) {
            System.out.println("Unable to save file");
            ex.printStackTrace();
        }
    }

    protected void _println(VisitableSampledSpectrum sed, String header, String spectrumName, int firstIndex, int lastIndex) {
        // this will print out the VisitableSampled Spectrum as a text file to
        // be taken by the user

        try {
            ITCImageFileIO.saveSedtoDisk(header, sed, spectrumName, firstIndex, lastIndex);
        } catch (Exception ex) {
            System.out.println("Unable to save file");
            ex.printStackTrace();
        }
    }

    protected void checkSourceFraction(double nExp, double fracSource) {
        double epsilon = 0.2;
        double number_source_exposures = nExp * fracSource;
        int iNumExposures = (int) (number_source_exposures + 0.5);
        double diff = number_source_exposures - iNumExposures;
        if (Math.abs(diff) > epsilon) {
            _println("nExp= " + nExp + " frac= " + fracSource);
            throw new IllegalArgumentException(
                    "Fraction with source value produces non-integral number of source exposures with source (" +
                            number_source_exposures + " vs. " + iNumExposures + ").");
        }
    }
}
