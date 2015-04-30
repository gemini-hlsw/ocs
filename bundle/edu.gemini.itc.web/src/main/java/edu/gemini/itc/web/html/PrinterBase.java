package edu.gemini.itc.web.html;

import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.servlets.ImageServlet;
import scala.Tuple2;

import java.io.PrintWriter;
import java.util.UUID;

public abstract class PrinterBase {

    protected UUID cache(final ItcSpectroscopyResult result) {
        return ImageServlet.cache(result);
    }

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

    // Adds a HTML link to a file.
    protected void _printFileLink(final UUID id, final String name, final String label) {
        _printFileLink(id, name, 0, label);
    }

    protected void _printFileLink(final UUID id, final String name, final int index, final String label) {
        _println("<a href =" +
                "\"" + ServerInfo.getServerURL()
                + "itc/servlet/images?type=txt"
                + "&filename=" + name
                + "&index=" + index
                + "&id=" + id
                //+ "&SessionID=0" // TODO: add fake "SessionID" in order to have URL ignored in science regression tests
                + "\"> Click here for " + label + ". </a>");
    }

    // Adds an HTML image link
    protected void _printImageLink(final UUID id, final String name) {
        _printImageLink(id, name, 0);
    }

    protected void _printImageLink(final UUID id, final String name, final int index) {
        _print("<IMG alt=\"SessionID123456.png\" height=500 src=\"" + ServerInfo.getServerURL() // TODO: get rid of SessionID when recreating baseline
                + "itc/servlet/images?type=img"
                + "&filename=" + name
                + "&index=" + index
                + "&id=" + id
                //+ "&SessionID=0" // TODO: add fake "SessionID" in order to have URL ignored in science regression tests
                + "\" width=675 border=0>");
    }

}
