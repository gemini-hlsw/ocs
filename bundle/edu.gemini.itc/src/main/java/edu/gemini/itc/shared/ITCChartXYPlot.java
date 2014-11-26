// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.


/*
 * ITCChartPlot.java
 *
 * Created on January 21, 2004, 12:19 PM
 */

package edu.gemini.itc.shared;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;


/**
 *
 * @author  bwalls
 */
public class ITCChartXYPlot extends XYPlot {
    
    /** Creates a new instance of ITCChartPlot */
    /** With this class you loose the ability to use secondary datasets.  */
    public ITCChartXYPlot() {
        super();
    }
    
    public ITCChartXYPlot(XYDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis, XYItemRenderer renderer) {
        super(dataset, domainAxis, rangeAxis, renderer);
    }
    
    public org.jfree.chart.LegendItemCollection getLegendItems() {
        LegendItemCollection result = new LegendItemCollection();
        
        // get the legend items for the main dataset...
        XYDataset dataset1 = getDataset();
        if (dataset1 != null) {
            XYItemRenderer renderer = getRenderer();
            if (renderer != null) {
                int seriesCount = dataset1.getSeriesCount();
                //for (int i = 0; i < seriesCount; i++) {
                //    LegendItem item = renderer.getLegendItem(0, i);
                //    result.add(item);
                //}
                LegendItem item = renderer.getLegendItem(0, 0);
                    result.add(item);
                LegendItem item2 = renderer.getLegendItem(0, 1);
                    result.add(item2);
            }
        }
        
        
        return result;
        
    }
    
    
    
}
