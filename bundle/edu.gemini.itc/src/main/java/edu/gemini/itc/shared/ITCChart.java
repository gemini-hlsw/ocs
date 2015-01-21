// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.


package edu.gemini.itc.shared;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import java.util.List;


public class ITCChart {
    private String chartTitle, xAxisLabel, yAxisLabel;
    private XYSeriesCollection seriesData = new XYSeriesCollection();
    private JFreeChart chart;

    public ITCChart() {
        this("DefaultTitle", "xAxis", "yAxis");
    }

    public ITCChart(java.lang.String chartTitle,
                    java.lang.String xAxisLabel,
                    java.lang.String yAxisLabel) {
        this.chartTitle = chartTitle;
        this.xAxisLabel = xAxisLabel;
        this.yAxisLabel = yAxisLabel;
        chart = ChartFactory.createXYLineChart(this.chartTitle, this.xAxisLabel, this.yAxisLabel, this.seriesData, PlotOrientation.VERTICAL, true, false, false);
        //chart = createXYLineChart(this.chartTitle,this.xAxisLabel,this.yAxisLabel, this.seriesData, PlotOrientation.VERTICAL, true);

        chart.getLegend().setPosition(RectangleEdge.TOP);
        //chart.getLegend().setAnchor(Legend.NORTH);
        //chart.getXYPlot().getDomainAxis().setGridLinesVisible(false);
        chart.setBackgroundPaint(java.awt.Color.white);
    }

    public void addArray(double data[][], java.lang.String seriesName) {
        XYSeries newSeries = new XYSeries(seriesName);
        for (int i = 0; i < data[0].length; i++) {
            if (data[0][i] > 0)   ///!!!!keeps negative x values from being added to a chart!!!!
                newSeries.add(data[0][i], data[1][i]);
        }

        this.seriesData.addSeries(newSeries);

        //if (this.seriesData.getSeriesCount()%2 == 0)
        //   ((XYPlot)chart.getPlot()).getRenderer().setSeriesPaint(this.seriesData.getSeriesCount(),java.awt.Color.RED);
        //else
        //  ((XYPlot)chart.getPlot()).getRenderer().setSeriesPaint(this.seriesData.getSeriesCount(),java.awt.Color.BLUE);
    }

    public void addArray(double data[][], java.lang.String seriesName, java.awt.Color color) {
        this.addArray(data, seriesName);

        if (color != null) {
            ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(this.seriesData.getSeriesCount() - 1, color);
        }
    }

//    public void addArray(double data[][], java.lang.String seriesName) {
//        int seriesPosition = findSeriesInCollection(seriesName);
//        if (seriesPosition != -1) {
//            for (int i = 0; i<data[0].length; i++) {
//                if(data[0][i] >0)   ///!!!!keeps negative x values from being added to a chart!!!!
//                    seriesData.getSeries(seriesPosition).add(data[0][i],data[1][i]);
//            }
//        } else {
//            XYSeries newSeries = new XYSeries(seriesName);
//            for(int i = 0; i<data[0].length; i++) {
//                if(data[0][i] >0)   ///!!!!keeps negative x values from being added to a chart!!!!
//                    newSeries.add(data[0][i],data[1][i]);
//            }
//           
//            this.seriesData.addSeries(newSeries);
//        }
//    }

    public int findSeriesInCollection(java.lang.String seriesName) {
        XYSeries tempSeries;
        List seriesValues = this.seriesData.getSeries();
        Iterator seriesNames = seriesValues.iterator();
        while (seriesNames.hasNext()) {
            tempSeries = (XYSeries) seriesNames.next();
            if (tempSeries.getKey().equals(seriesName)) {
                return seriesValues.indexOf(tempSeries);
            }
        }
        return -1;


    }

    public void addTitle(java.lang.String chartTitle) {
        this.chartTitle = chartTitle;

        //List titleList = new ArrayList();

        //titleList.add(new TextTitle(chartTitle));
        //chart.setSubtitles(titleList);
        chart.setTitle(this.chartTitle);
    }

    public void addxAxisLabel(java.lang.String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
        chart.getXYPlot().getDomainAxis().setLabel(xAxisLabel);
    }

    public void addyAxisLabel(java.lang.String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
        chart.getXYPlot().getRangeAxis().setLabel(yAxisLabel);
    }

    public void autoscale() {
        chart.getXYPlot().getDomainAxis().setAutoRange(true);
        chart.getXYPlot().getRangeAxis().setAutoRange(true);
    }

    public void setDomainMinMax(double lower, double upper) {
        chart.getXYPlot().getDomainAxis().setRange(lower, upper);
    }

    public void setRangeMinMax(double lower, double upper) {
        chart.getXYPlot().getRangeAxis().setRange(lower, upper);
    }

    public void addHorizontalLine(double value) {
        //chart.getXYPlot().addHorizontalLine(new Double(value));
        chart.getXYPlot().addDomainMarker(new ValueMarker(value));

    }

    public void flush() {
        seriesData.removeAllSeries();
        this.chartTitle = "DefaultTitle";
        this.xAxisLabel = "xAxis";
        this.yAxisLabel = "yAxis";
    }

    public java.awt.image.BufferedImage getBufferedImage() throws
            Exception {
        //JFreeChart chart =
        //ChartFactory.createXYChart(this.chartTitle,this.xAxisLabel,this.yAxisLabel,
        //this.seriesData, true);
        BufferedImage image = chart.createBufferedImage(675, 500);
        return image;
    }

    public void saveChart(java.lang.String chartName) throws Exception {
        //JFreeChart chart =
        //ChartFactory.createXYChart(this.chartTitle,this.xAxisLabel,this.yAxisLabel,
        //this.seriesData, true);
        ChartUtilities.saveChartAsPNG(new File(chartName), chart,
                675, 500);
    }
    /**
     private static JFreeChart createXYLineChart(String title,
     String xAxisLabel,
     String yAxisLabel,
     XYDataset dataset,
     PlotOrientation orientation,
     boolean legend) {

     NumberAxis xAxis = new NumberAxis(xAxisLabel);
     xAxis.setAutoRangeIncludesZero(false);
     NumberAxis yAxis = new NumberAxis(yAxisLabel);
     XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
     XYPlot plot = new ITCChartXYPlot(dataset, xAxis, yAxis, renderer);
     plot.setOrientation(orientation);


     JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);

     return chart;

     }**/

}
