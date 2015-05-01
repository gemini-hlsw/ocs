package edu.gemini.itc.shared;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import scala.collection.JavaConversions;

import java.awt.*;
import java.awt.image.BufferedImage;


public final class ITCChart {
    private final XYSeriesCollection seriesData = new XYSeriesCollection();
    private final JFreeChart chart;

    public static ITCChart forSpcDataSet(SpcDataSet s, PlottingDetails plotParams) {
        return new ITCChart(s, plotParams);
    }

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

    public BufferedImage getBufferedImage(int width, int height)  {
        return chart.createBufferedImage(width, height);
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

}
