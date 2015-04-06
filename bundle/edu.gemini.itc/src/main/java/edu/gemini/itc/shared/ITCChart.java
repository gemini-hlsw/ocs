package edu.gemini.itc.shared;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.image.BufferedImage;


public final class ITCChart {
    private final XYSeriesCollection seriesData = new XYSeriesCollection();
    private final JFreeChart chart;

    public ITCChart(final String chartTitle, final String xAxisLabel, final String yAxisLabel, final PlottingDetails plotParams) {
        chart = ChartFactory.createXYLineChart(chartTitle, xAxisLabel, yAxisLabel, this.seriesData, PlotOrientation.VERTICAL, true, false, false);
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.setBackgroundPaint(java.awt.Color.white);
        if (plotParams.getPlotLimits().equals(PlottingDetails.PlotLimits.USER)) {
            setDomainMinMax(plotParams.getPlotWaveL(), plotParams.getPlotWaveU());
        } else {
            autoscale();
        }
    }

    public void addArray(final double data[][], final String seriesName) {
        final XYSeries newSeries = new XYSeries(seriesName);
        for (int i = 0; i < data[0].length; i++) {
            if (data[0][i] > 0)   ///!!!!keeps negative x values from being added to a chart!!!!
                newSeries.add(data[0][i], data[1][i]);
        }

        this.seriesData.addSeries(newSeries);
    }

    public void addArray(final double data[][], final String seriesName, final Color color) {
        this.addArray(data, seriesName);
        if (color != null) {
            ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(this.seriesData.getSeriesCount() - 1, color);
        }
    }

    public void autoscale() {
        chart.getXYPlot().getDomainAxis().setAutoRange(true);
        chart.getXYPlot().getRangeAxis().setAutoRange(true);
    }

    public void setDomainMinMax(final double lower, final double upper) {
        chart.getXYPlot().getDomainAxis().setRange(lower, upper);
    }

    public BufferedImage getBufferedImage()  {
        return chart.createBufferedImage(675, 500);
    }

}
