package jsky.plot;

import jsky.util.I18N;
import jsky.util.PrintableWithDialog;
import jsky.util.SaveableWithDialog;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.PrintUtil;
import org.jfree.chart.*;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import javax.print.attribute.standard.OrientationRequested;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.text.*;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import static java.lang.Math.PI;

/**
 * A panel for displaying an elevation plot for given target positions.
 */
public class ElevationPanel extends JPanel implements LegendTitleUser, PrintableWithDialog, SaveableWithDialog {

    // Used to access internationalized strings (see i18n/gui*.properties)
    private static final I18N _I18N = I18N.getInstance(ElevationPanel.class);

    // Displays the elevation plot
    private JFreeChart _chart;
    private ChartPanel _chartPanel;

    // The secondary Y axis
    private NumberAxis _valueAxis2;

    // Alpha value used to draw dark area on graph
    private static final float DARKNESS_ALPHA = 0.1F;

    // Alpha value used to draw twilight area on graph
    private static final float TWILIGHT_ALPHA = 0.2F;

    // This is a required parameter, but we don't want to see the outline
    private static final Stroke DARKNESS_STROKE = new BasicStroke(0.0F);

    // Used for the parallactic angle plot
    private static final Stroke DASHED_LINE_STROKE
            = new BasicStroke(2.0F,
                              BasicStroke.CAP_BUTT,
                              BasicStroke.JOIN_BEVEL,
                              0.0F,
                              new float[]{4.0F, 4.0F},
                              0.0F);

    // Array of colors for color legend
    private static final Paint[] _COLORS = ChartColor.createDefaultPaintArray();

    // Color used to display elevation constraints
    private static final Color CONSTRAINT_COLOR = new Color(0.0F, 1.0F, 0.0F, 0.5F);

    private static final Paint TIMING_WINDOW_PAINT;

    static {
        final BufferedImage bi = new BufferedImage(4, 4,  BufferedImage.TYPE_4BYTE_ABGR);
        final int rgb = new Color(164, 160, 203).getRGB();
        bi.setRGB(2, 0, rgb);
        bi.setRGB(3, 0, rgb);
        bi.setRGB(1, 1, rgb);
        bi.setRGB(2, 1, rgb);
        bi.setRGB(0, 2, rgb);
        bi.setRGB(1, 2, rgb);
        bi.setRGB(0, 3, rgb);
        bi.setRGB(3, 3, rgb);

        TIMING_WINDOW_PAINT = new TexturePaint(bi, new Rectangle2D.Double(0, 0, bi.getWidth(), bi.getHeight()));
    }

    // Provides the model data for the graph and tables
    private ElevationPlotModel _model;

    // If true, display the trajectory label at the high point of each target
    private boolean _showTrajectoryLabels = true;

    // Controls the visibility of the graph legend
    private boolean _showLegend = true;

    // Optional custom legend items
    private LegendItemCollection _legendItems;

    // Optional custom colors, corresponding to the custom legend (indexed by series)
    private Paint[] _itemColors;

    // Controls the visibility of the parallactic angle plot
    private boolean _paPlotVisible = true;

    // Controls the visibility of the altitude plot
    private boolean _altitudePlotVisible = true;

    // Controls the visibility of the elevation constraints plot
    private boolean _elevationConstraintsMarkerVisible = true;

    // Controls the visibility of the timing windows plot
    private boolean _timingWindowsMarkerVisible = true;

    // Label for primary Y axis
    private String _yAxisLabel = _I18N.getString("AltitudeInDeg");

    // Options for secondary Y axis
    public static final String Y2_AXIS_AIRMASS = _I18N.getString("Airmass");
    public static final String Y2_AXIS_PA = _I18N.getString("ParallacticAngle");
    private String _y2AxisLabel = Y2_AXIS_AIRMASS;

    // Used to display the degrees values in the primary axis in the range 0..90
    private static final NumberFormat _yAxisNumberFormat = new DecimalFormat() {
        @Override
        public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
            if (number > 90.0) {
                return result; // used to leave blank space for labels above 90 deg mark
            }
            return super.format(number, result, fieldPosition);
        }
    };

    // Used to display the airmass values in the secondary axis (instead of elevation in deg)
    private static NumberFormat _y2AxisAirmassNumberFormat = new DecimalFormat() {
        @Override
        public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
            if (number > 90.0 || number < 5.0) {
                return result; // ignore huge results near horizon (<5)
            }
            double d = ElevationPlotUtil.getAirmass(number);
            return super.format(d, result, fieldPosition);
        }
    };

    // Used to display the PA values in the secondary axis (instead of airmass)
    private static NumberFormat _y2AxisPaNumberFormat = new DecimalFormat();

    static {
        _yAxisNumberFormat.setMinimumFractionDigits(0);
        _yAxisNumberFormat.setMaximumFractionDigits(0);
        _y2AxisAirmassNumberFormat.setMinimumFractionDigits(2);
        _y2AxisAirmassNumberFormat.setMaximumFractionDigits(2);
        _y2AxisPaNumberFormat.setMinimumFractionDigits(1);
        _y2AxisPaNumberFormat.setMaximumFractionDigits(1);
    }

    // Graph item renderer for elevation plot
    private final StandardXYItemRenderer _elevationItemRenderer =
            new StandardXYItemRenderer(StandardXYItemRenderer.LINES) {

                // add labels to the graph lines
                @Override
                public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot,
                                                      XYDataset dataset, PlotRenderingInfo info) {
                    XYItemRendererState result = super.initialise(g2, dataArea, plot, dataset, info);
                    if (_altitudePlotVisible && _showTrajectoryLabels) {
                        g2.setFont(LegendTitle.DEFAULT_ITEM_FONT);
                        g2.setColor(Color.black);
                        TimeSeriesCollection tsc = (TimeSeriesCollection) dataset;
                        int count = tsc.getSeriesCount();
                        for (int i = 0; i < count; i++) {
                            TimeSeries timeSeries = tsc.getSeries(i);
                            String label = timeSeries.getKey().toString();
                            double xDataValue = _model.getMaxElevationTime(i);
                            double yDataValue = _model.getMaxElevation(i);
                            double x = plot.getDomainAxis().valueToJava2D(xDataValue,
                                    dataArea, RectangleEdge.BOTTOM);
                            double y = plot.getRangeAxis().valueToJava2D(yDataValue,
                                    dataArea, RectangleEdge.LEFT);
                            x += 3; // padding
                            y -= 3;
                            g2.rotate(-PI / 2, x, y);
                            g2.drawString(label, (int) x, (int) y);
                            g2.rotate(PI / 2, x, y);
                        }
                    }
                    return result;
                }

                // Override to provide custom colors, matching the custom legend
                @Override
                public Paint getSeriesPaint(int series) {
                    if (_itemColors != null && _itemColors.length > series)
                        return _itemColors[series];
                    return super.getSeriesPaint(series);
                }

                // Overridden to hide the item if needed
                @Override
                public void drawItem(Graphics2D g2,
                                     XYItemRendererState state,
                                     Rectangle2D dataArea,
                                     PlotRenderingInfo info,
                                     XYPlot plot,
                                     ValueAxis domainAxis,
                                     ValueAxis rangeAxis,
                                     XYDataset dataset,
                                     int series,
                                     int item,
                                     CrosshairState crosshairState,
                                     int pass) {
                    if (_altitudePlotVisible) {
                        super.drawItem(g2,
                                state,
                                dataArea,
                                info,
                                plot,
                                domainAxis,
                                rangeAxis,
                                dataset,
                                series,
                                item,
                                crosshairState,
                                pass);
                    }
                }
            };

    // Item renderer for parallactic angle plot
    private final StandardXYItemRenderer _paItemRenderer =
            new StandardXYItemRenderer(StandardXYItemRenderer.LINES) {

                // Override to provide custom colors, matching the custom legend
                @Override
                public Paint getSeriesPaint(int series) {
                    if (_itemColors != null && _itemColors.length > series)
                        return _itemColors[series];
                    return super.getSeriesPaint(series);
                }

                // Override to provide custom colors, matching the custom legend
                @Override
                public Stroke getSeriesStroke(int series) {
                    return DASHED_LINE_STROKE;
                }
            };

    // Item renderer for airmass plot
    private final StandardXYItemRenderer _hiddenItemRenderer =
            new StandardXYItemRenderer(StandardXYItemRenderer.LINES) {
                // don't want to draw any items for the airmass, just change the
                // secondary Y axis display (see _y2AxisAirmassNumberFormat)
                @Override
                public void drawItem(Graphics2D g2,
                                     XYItemRendererState state,
                                     Rectangle2D dataArea,
                                     PlotRenderingInfo info,
                                     XYPlot plot,
                                     ValueAxis domainAxis,
                                     ValueAxis rangeAxis,
                                     XYDataset dataset,
                                     int series,
                                     int item,
                                     CrosshairState crosshairState,
                                     int pass) {
                }
            };


    /**
     * Create an elevation plot panel.
     */
    public ElevationPanel() {
        setLayout(new BorderLayout());
    }

    /**
     * Set the model containing the graph data and update the display.
     */
    public void setModel(ElevationPlotModel model) {
        _model = model;
        _update();
        _model.addChangeListener(e -> _update());
    }

    /**
     * Return the model containing the graph data.
     */
    public ElevationPlotModel getModel() {
        return _model;
    }

    // Update the y axis to leave space for labels at top if needed
    private void _updateYAxisSize() {
        if (_chart != null) {
            XYPlot xyPlot = _chart.getXYPlot();
            ValueAxis yAxis = xyPlot.getRangeAxis();
            ValueAxis yAxis2 = xyPlot.getRangeAxis(1);
            if (_showTrajectoryLabels) {
                yAxis.setRange(0.0, 114.0); // max 90 deg, but leave space for labels
                if (_y2AxisLabel.equals(Y2_AXIS_AIRMASS)) {
                    yAxis2.setRange(0.0, 114.0);
                } else {
                    yAxis2.setRange(-180.0, 180.0);
                }
            }
            else {
                yAxis.setRange(0.0, 90.0);
                if (_y2AxisLabel.equals(Y2_AXIS_AIRMASS)) {
                    yAxis2.setRange(0.0, 90.0);
                } else {
                    yAxis2.setRange(-180.0, 180.0);
                }
            }
        }
   }

    /** Controls the display of trajectory labels at the high point of each target */
    public void setShowTrajectoryLabels(boolean b) {
        _showTrajectoryLabels = b;
        _updateYAxisSize();
    }

    // Set the visibility of the parallactic angle plot
    public void setPaPlotVisible(boolean visible) {
        _paPlotVisible = visible;
        _update();
    }

    // Set the visibility of the altitude plot
    public void setAltitudePlotVisible(boolean visible) {
        _altitudePlotVisible = visible;
        _update();
    }

    public void setElevationConstraintsMarkerVisible(boolean visible) {
        _elevationConstraintsMarkerVisible = visible;
        _update();
    }

    public void setTimingWindowsMarkerVisible(boolean visible) {
        _timingWindowsMarkerVisible = visible;
        _update();
    }

    // Return the label for the time axis
    private String _getXAxisLabel() {
        if (_model.getTimeZoneId().equals(ElevationPlotModel.SITE_TIME)) {
            return _I18N.getString("time") + " (" + _model.getTimeZone().getDisplayName() + ")";
        } else {
            return _I18N.getString("time") + " (" + _model.getTimeZoneId() + ")";
        }
    }

    /**
     * Set the label and meaning of the secondary Y axis.
     *
     * @param label one of the constants Y2_AXIS_AIRMASS or Y2_AXIS_PA
     * for "Airmass" or "Parallactic Angle".
     */
    public void setY2AxisLabel(String label) {
        _y2AxisLabel = label;
        if (_valueAxis2 != null) {
            _valueAxis2.setLabel(label);
        }
        _update();
    }

    /** Update the GUI to reflect what is in the model */
    private void _update() {
        if (_model == null) {
            return;
        }
        // create or update the graph display
        XYDataset dataset = _model.getXYDataset();
        if (_chart == null) {
            _chartPanel = _makeElevationChart(dataset);
            add(_chartPanel, BorderLayout.CENTER);
        } else {
            XYPlot xyPlot = _chart.getXYPlot();
            // Can't change the timezone, so recreate the X axis
            xyPlot.setDomainAxis(_makeDateAxis());
            xyPlot.setDataset(dataset);
        }

        _chart.setTitle(_model.getTitle());
        _updateSecondaryYAxis(dataset);

        if (_legendItems == null) {
            _setDefaultLegendItems();
        }

        final XYPlot xyPlot = _chart.getXYPlot();
        DateAxis axis = (DateAxis) xyPlot.getDomainAxis();
        axis.setRange(_model.getStartDate(), _model.getEndDate());
        axis.setLabel(_getXAxisLabel());

        // mark the ranges of twilight and darkness
        xyPlot.clearDomainMarkers();
        xyPlot.clearRangeMarkers();
        xyPlot.clearAnnotations();
        addDomainMarker(xyPlot,
                new Date(_model.getSunSet()), "Sunset",
                new Date(_model.getSunRise()), "Sunrise",
                TWILIGHT_ALPHA, Color.gray);
        addDomainMarker(xyPlot,
                new Date(_model.getNauticalTwilightStart()), "Evening 12° Twilight",
                new Date(_model.getNauticalTwilightEnd()), "Morning 12° Twilight",
                DARKNESS_ALPHA, Color.black);

        xyPlot.addRangeMarker(new ValueMarker(ElevationPlotModel.getObsThreshold()));

        plotCons(xyPlot, _elevationConstraintsMarkerVisible,2, _model::getConstraintsDataset, CONSTRAINT_COLOR);
        plotCons(xyPlot, _timingWindowsMarkerVisible, 3, _model::getTimingWindowsDataset, TIMING_WINDOW_PAINT);
    }

    private void plotCons(XYPlot xyPlot, boolean visible, int index, Supplier<XYDataset> dataset, final Paint p) {
        if (visible && _altitudePlotVisible) {
            xyPlot.setDataset(index, dataset.get());
            xyPlot.setRenderer(index, new XYStepAreaRenderer() {
                @Override
                public Paint getSeriesPaint(int series) { return p; }
            });
        } else {
            xyPlot.setDataset(index, null);
        }
    }

    // Make the X axis displaying the dates
    private DateAxis _makeDateAxis() {
        DateAxis timeAxis = new DateAxis(_getXAxisLabel(), _model.getTimeZone());
        timeAxis.setLowerMargin(0.02);  // reduce the default margins on the time axis
        timeAxis.setUpperMargin(0.02);

        // Override date format to handle LST dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm") {
            @Override
            public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
                if (_model.getTimeZoneId().equals(ElevationPlotModel.LST)) {
                    date = _model.getLst(date);
                }
                return super.format(date, toAppendTo, pos);
            }
        };
        dateFormat.setTimeZone(_model.getTimeZone());
        timeAxis.setDateFormatOverride(dateFormat);

        return timeAxis;
    }

    // Make and return the elevation chart panel
    private ChartPanel _makeElevationChart(XYDataset dataset) {
        String title = _model.getTitle();

        _chart = _createTimeSeriesChart(title, _yAxisLabel, _y2AxisLabel,
                dataset);

        _chart.setBackgroundPaint(getBackground());
        _updateYAxisSize();

        TextTitle textTitle = _chart.getTitle();
        textTitle.setFont(textTitle.getFont().deriveFont(12.0F));

        // mark the elevation threshold
        ChartPanel chartPanel = new ChartPanel(_chart, false, false, false, false, false);

        // disable scaling
        chartPanel.setMaximumDrawWidth(100000);
        chartPanel.setMaximumDrawHeight(100000);

        return chartPanel;
    }


    // Modified version of ChartFactory.createTimeSeriesChart() that uses a custom renderer
    private JFreeChart _createTimeSeriesChart(String title,
                                              String valueAxisLabel,
                                              String valueAxisLabel2,
                                              XYDataset dataset) {

        ValueAxis timeAxis = _makeDateAxis();
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);
        valueAxis.setNumberFormatOverride(_yAxisNumberFormat);
        XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, _elevationItemRenderer) {
            // use a custom legend
            @Override
            public LegendItemCollection getLegendItems() {
                if (_legendItems == null)
                    return super.getLegendItems();
                return _legendItems;
            }
        };

        // add a secondary Y axis for airmass or parallactic angle
        _valueAxis2 = new NumberAxis(valueAxisLabel2);
        plot.setRangeAxis(1, _valueAxis2);
        plot.mapDatasetToRangeAxis(1, 1);

        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        if (_showLegend) {
            useLegendTitle(chart);
        }

        return chart;
    }

    // Update the secondary Y axis to display either airmass or parallactic angle
    private void _updateSecondaryYAxis(XYDataset dataset) {
        // update the secondary dataset for airmass or parallactic angle
        XYPlot xyPlot = _chart.getXYPlot();
        if (_y2AxisLabel.equals(Y2_AXIS_AIRMASS)) {
            xyPlot.setDataset(1, dataset);
            _valueAxis2.setAutoRangeIncludesZero(false);
            _valueAxis2.setNumberFormatOverride(_y2AxisAirmassNumberFormat);
            xyPlot.setRenderer(1, _hiddenItemRenderer);
        } else {
            // assume parallactic angle
            XYDataset dataset2 = _model.getSecondaryDataset();
            xyPlot.setDataset(1, dataset2);
            _valueAxis2.setAutoRangeIncludesZero(true);
            _valueAxis2.setNumberFormatOverride(_y2AxisPaNumberFormat);
            if (_paPlotVisible) {
                xyPlot.setRenderer(1, _paItemRenderer);
            } else {
                xyPlot.setRenderer(1, _hiddenItemRenderer);
            }
        }
        _updateYAxisSize();
    }


    /** Set up a custom legend */
    public void setLegendItems(LegendItemCollection l) {
        _legendItems = l;
    }

    // Set the default legend items for the plot
    private void _setDefaultLegendItems() {
        TargetDesc[] targets = _model.getTargets();
        int colorIndex = 0;
        Paint[] colors = new Paint[targets.length];
        Map<String, Paint> paintMap = new TreeMap<>();
        LegendItemCollection lic = new LegendItemCollection();

        for (int i = 0; i < targets.length; i++) {
            String name = targets[i].getName();
            if ((colors[i] = paintMap.get(name)) == null) {
                Paint color = _COLORS[colorIndex++ % _COLORS.length];
                paintMap.put(name, color);
                colors[i] = color;
                lic.add(new LegendItem(name,color));
            }
        }
        _itemColors = colors;
        _legendItems = lic;
    }

    /** Set the optional custom colors, corresponding to the custom legend (indexed by series) */
    public void setItemColors(Paint[] colors) {
        _itemColors = colors;
    }


    // Add a marker to the given plot, showing the area of darkness
    private void addDomainMarker(final XYPlot xyPlot,
                                 final Date startDate, final String startDateName,
                                 final Date endDate,   final String endDateName,
                                 float alpha, Color color) {
        final DateFormat df = ((DateAxis)xyPlot.getDomainAxis()).getDateFormatOverride();
        double start = startDate.getTime();
        double end = endDate.getTime();

        if (start < end) {
            IntervalMarker m = new IntervalMarker(start, end, color, DARKNESS_STROKE,
                    color, DARKNESS_STROKE, alpha);
            xyPlot.addDomainMarker(m, Layer.BACKGROUND);
            xyPlot.addAnnotation(createAnnotation(String.format("%s: %s", startDateName, df.format(startDate)), start));
            xyPlot.addAnnotation(createAnnotation(String.format("%s: %s", endDateName, df.format(endDate)), end));
        } else {
            double first = _model.getDateForHour(0.0).getTime();
            double last = _model.getDateForHour(24.0).getTime();
            IntervalMarker m1 = new IntervalMarker(start, last, color, DARKNESS_STROKE,
                    color, DARKNESS_STROKE, alpha);
            xyPlot.addDomainMarker(m1, Layer.BACKGROUND);
            xyPlot.addAnnotation(createAnnotation(String.format("%s: %s", startDateName, df.format(startDate)), start));
            IntervalMarker m2 = new IntervalMarker(first, end, color, DARKNESS_STROKE,
                    color, DARKNESS_STROKE, alpha);
            xyPlot.addDomainMarker(m2, Layer.BACKGROUND);
            xyPlot.addAnnotation(createAnnotation(String.format("%s: %s", endDateName, df.format(endDate)), first));
        }
    }

    private XYTextAnnotation createAnnotation(final String s, final double pos) {
        final XYTextAnnotation a = new XYTextAnnotation(s, pos, 60.0f);
        a.setFont(a.getFont().deriveFont(12.0f));
        a.setRotationAnchor(TextAnchor.BASELINE_CENTER);
        a.setTextAnchor(TextAnchor.BASELINE_CENTER);
        a.setRotationAngle(-PI / 2);
        a.setPaint(Color.black);
        return a;
    }


    /** Display a dialog for printing the graph */
    @Override
    public void print() throws PrinterException {
        PrintUtil printUtil = new PrintUtil(_chartPanel);
        printUtil.setUseBgThread(false); // otherwise get ConcurrentModificationException
        printUtil.setAttribute(OrientationRequested.LANDSCAPE);
        printUtil.print();
    }

    /**
     * Display a dialog for saving the graph in PNG format
     */
    @Override
    public void saveAs() {
        try {
            _chartPanel.doSaveAs();
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /** Set the visibility of the legend */
    public void setShowLegend(boolean show) {
        _showLegend = show;
        if (_chart != null) {
            if (show) {
                _chart.addSubtitle(new LegendTitle(_chart.getPlot()));
            } else {
                _chart.removeLegend();
            }
        }
    }
}