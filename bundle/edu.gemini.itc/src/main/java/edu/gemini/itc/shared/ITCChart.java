package edu.gemini.itc.shared;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import scala.collection.JavaConversions;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public final class ITCChart {
    private final XYSeriesCollection seriesData = new XYSeriesCollection();
    private final JFreeChart chart;

    public ITCChart(final SpcDataSet s, final PlottingDetails plotParams) {

        chart = ChartFactory.createXYLineChart(s.title(), s.xAxisLabel(), s.yAxisLabel(), this.seriesData, PlotOrientation.VERTICAL, true, false, false);
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.setBackgroundPaint(Color.white);

        if (plotParams.getPlotLimits().equals(PlottingDetails.PlotLimits.USER)) {
            setDomainMinMax(plotParams.getPlotWaveL(), plotParams.getPlotWaveU());
        } else {
            autoscale();
        }

        for (final SpcData d : JavaConversions.seqAsJavaList(s.series())) {
            addArray(d.data(), d.label(), d.color());
        }
    }

    public BufferedImage getBufferedImage()  {
        return chart.createBufferedImage(675, 500);
    }

    private void addArray(final double data[][], final String seriesName) {
        final XYSeries newSeries = new XYSeries(seriesName);
        for (int i = 0; i < data[0].length; i++) {
            if (data[0][i] > 0)   ///!!!!keeps negative x values from being added to a chart!!!!
                newSeries.add(data[0][i], data[1][i]);
        }

        this.seriesData.addSeries(newSeries);
    }

    private void addArray(final double data[][], final String seriesName, final Color color) {
        this.addArray(data, seriesName);
        if (color != null) {
            ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(this.seriesData.getSeriesCount() - 1, color);
        }
    }

    private void autoscale() {
        chart.getXYPlot().getDomainAxis().setAutoRange(true);
        chart.getXYPlot().getRangeAxis().setAutoRange(true);
    }

    private void setDomainMinMax(final double lower, final double upper) {
        chart.getXYPlot().getDomainAxis().setRange(lower, upper);
    }

    // =============

    // GENERIC CHART CREATION
    // Utility functions that create generic signal and signal to noise charts for several instruments.

    public static ITCChart forSpcDataSet(final SpcDataSet s, final PlottingDetails plotParams) {
        return new ITCChart(s, plotParams);
    }

    public static SpcDataSet createSignalChart(final SpectroscopyResult result) {
        return createSignalChart(result, 0);
    }

    public static SpcDataSet createSignalChart(final SpectroscopyResult result, final int index) {
        return createSignalChart(result, "Signal and Background ", index);
    }

    public static SpcDataSet createSigSwAppChart(final SpectroscopyResult result, final int index) {
        return createSignalChart(result, "Signal and SQRT(Background) in software aperture of " + result.specS2N()[index].getSpecNpix() + " pixels", index);
    }

    public static SpcDataSet createSignalChart(final SpectroscopyResult result, final String title, final int index) {
        final List<SpcData> data = new ArrayList<>();
        data.add(new SpcData("Signal ",             Color.RED,   result.specS2N()[index].getSignalSpectrum().getData()));
        data.add(new SpcData("SQRT(Background)  ",  Color.BLUE,  result.specS2N()[index].getBackgroundSpectrum().getData()));
        return new SpcDataSet("Signal", title, "Wavelength (nm)", "e- per exposure per spectral pixel", JavaConversions.asScalaBuffer(data));
    }

    public static SpcDataSet createS2NChart(final SpectroscopyResult result) {
        return createS2NChart(result, 0);
    }

    public static SpcDataSet createS2NChart(final SpectroscopyResult result, final int index) {
        return createS2NChart(result, "Intermediate Single Exp and Final S/N", index);
    }

    public static SpcDataSet createS2NChart(final SpectroscopyResult result, final String title, final int index) {
        final List<SpcData> data = new ArrayList<>();
        data.add(new SpcData("Single Exp S/N",      Color.RED,   result.specS2N()[index].getExpS2NSpectrum().getData()));
        data.add(new SpcData("Final S/N  ",         Color.BLUE,  result.specS2N()[index].getFinalS2NSpectrum().getData()));
        return new SpcDataSet("S2N", title, "Wavelength (nm)", "Signal / Noise per spectral pixel", JavaConversions.asScalaBuffer(data));
    }


}
