package jsky.plot;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.ui.RectangleEdge;
import org.jfree.data.category.IntervalCategoryDataset;

import java.awt.geom.Rectangle2D;
import java.awt.print.PrinterException;
import java.util.*;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import jsky.util.I18N;
import jsky.util.PrintableWithDialog;
import jsky.util.SaveableWithDialog;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.PrintUtil;

import java.awt.*;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;

/**
 * A panel for displaying an observation chart for given target positions.
 *
 * @version $Revision: 42349 $
 * @author Allan Brighton
 */
public class ObservationPanel extends JPanel implements LegendTitleUser, PrintableWithDialog, SaveableWithDialog {

    // Used to access internationalized strings (see i18n/gui*.properties)
    private static final I18N _I18N = I18N.getInstance(ObservationPanel.class);

    // Tabbed pane containing the charts for the different categories
    private JTabbedPane _tabbedPane;

    // Saved array of target categories (there is one tab for each)
    private String[] _categories;

    // Displays the observation charts
    private JFreeChart[] _chart;
    private ChartPanel[] _chartPanel;

    private static Font _labelFont = new Font("Monospaced", Font.PLAIN, 10);

    // Alpha value used to draw dark area on graph
    private static final float DARKNESS_ALPHA = 0.1F;
    private static final Composite DARKNESS_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DARKNESS_ALPHA);

    // Alpha value used to draw twilight area on graph
    private static final float TWILIGHT_ALPHA = 0.05F;
    private static final Composite TWILIGHT_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, TWILIGHT_ALPHA);

    // This is a required parameter, but we don't want to see the outline
    private static final Stroke DARKNESS_STROKE = new BasicStroke();


    // Available priority strings
    private static String[] _availablePriorities = {
        "High",
        "Medium",
        "Low"
    };

    // Colors to use for the above available priorities
    private static Paint[] _availablePriorityColors = {
        Color.red,
        Color.blue,
        Color.green
    };

    // Used to configure the paint/color to use for the different target priorities
    private Map<String, Paint> _priorityPaintMap;

    // Provides the model data for the graph and tables
    private ElevationPlotModel _model;

    // Used to display a legend with the colors used to indicate priority
    private LegendItemCollection _priorityLegendItemCollection;

    // Controls the visibility of the graph legend
    private boolean _showLegend = true;

//    // This is used for the tooltips on the bars
//    private IntervalCategoryToolTipGenerator _tooltipGenerator = new IntervalCategoryToolTipGenerator() {
//            public String generateToolTip(CategoryDataset data, int series, int category) {
//                TargetDesc[] targets = _model.getTargets();
//                if (category < targets.length) {
//                    return targets[category].getDescription();
//                }
//                return "";
//            }
//        };

    // Use a custom chart renderer that uses the priority to determine the bar color
    // and marks the dark time as a darker grey area
    private GanttRenderer _renderer = new GanttRenderer() {
        public Paint getItemPaint(int row, int column) {
            TargetDesc[] targets = _model.getTargets();
            if (column < targets.length) {
                String prio = targets[column].getPriority();
                Paint paint = _priorityPaintMap.get(prio);
                if (paint != null)
                    return paint;
            }
            return super.getItemPaint(row, column);
        }
        // Draw a darker grey area for the dark time
        public void drawRangeMarker(Graphics2D g2, CategoryPlot plot, ValueAxis rangeAxis,
                                    Marker marker, Rectangle2D dataArea) {
            if (_model.getNumTargets() == 0)
                return;
            IntervalMarker m = (IntervalMarker) marker;
            double x1 = rangeAxis.valueToJava2D(m.getStartValue(), dataArea, RectangleEdge.BOTTOM);
            double x2 = rangeAxis.valueToJava2D(m.getEndValue(), dataArea, RectangleEdge.BOTTOM);
            Rectangle2D rect = new Rectangle2D.Double(x1, dataArea.getMinY(), x2 - x1, dataArea.getHeight());
            g2.setPaint(m.getOutlinePaint());
            double alpha = m.getAlpha();
            if (alpha == TWILIGHT_ALPHA)
                g2.setComposite(TWILIGHT_COMPOSITE);
            else if (alpha == DARKNESS_ALPHA)
                g2.setComposite(DARKNESS_COMPOSITE);
            g2.fill(rect);
            g2.setPaintMode();
        }
    };



    /**
     * Create an observation chart panel.
     */
    public ObservationPanel() {
        setLayout(new BorderLayout());
        _tabbedPane = new JTabbedPane();
        add(_tabbedPane, BorderLayout.CENTER);

        // initialize the default priority colors
        _priorityPaintMap = new HashMap<>();
        _priorityLegendItemCollection = new LegendItemCollection();
        for (int i = 0; i < _availablePriorities.length; i++) {
            _priorityPaintMap.put(_availablePriorities[i], _availablePriorityColors[i]);
            String s = _availablePriorities[i] + " Priority";
            _priorityLegendItemCollection.add(new LegendItem(s, _availablePriorityColors[i]));
        }

    }

    /**
     * Set the model containing the elevation plot data and update the display.
     */
    public void setModel(ElevationPlotModel model) {
        _model = model;
        _update();
        _model.addChangeListener(e -> _update());
    }

    /**
     * Return the model containing the elevation plot data.
     */
    public ElevationPlotModel getModel() {
        return _model;
    }


    /** Update the GUI to reflect what is in the model */
    private void _update() {
        String[] categories = _model.getCategories();
        if (_categories == null || !Arrays.equals(_categories, categories)) {
            _categories = categories;
            _tabbedPane.removeAll();
            _chart = new JFreeChart[_categories.length];
            _chartPanel = new ChartPanel[_categories.length];
        }

        Dimension dim = new Dimension();
        for (int i = 0; i < _categories.length; i++) {
            IntervalCategoryDataset dataset = _model.getIntervalCategoryDataset(_categories[i]);
            if (_chart[i] == null) {
                _chartPanel[i] = _makeObservationChart(dataset, i);
                _tabbedPane.add(new JScrollPane(_chartPanel[i]), _categories[i]);
            } else {
                // Can't change the date axis timezone, so create a new one
                DateAxis dateAxis = _makeDateAxis();
                CategoryPlot plot = _chart[i].getCategoryPlot();
                plot.setRangeAxis(0, dateAxis);

                plot.setDataset(dataset);
            }
            _chart[i].setTitle(_model.getTitle() + " (" + _categories[i] + ")");

            // Force the full range on the X axis
            CategoryPlot plot = (CategoryPlot) _chart[i].getPlot();
            DateAxis axis = (DateAxis) plot.getRangeAxis();
            axis.setRange(_model.getStartDate(), _model.getEndDate());
            axis.setLabel(_getXAxisLabel());

            // mark the ranges of twilight and darkness
            plot.clearRangeMarkers();
            _addDarknessMarker(plot, new Date(_model.getSunSet()),
                    new Date(_model.getSunRise()), TWILIGHT_ALPHA, Color.gray);
            _addDarknessMarker(plot, new Date(_model.getNauticalTwilightStart()),
                    new Date(_model.getNauticalTwilightEnd()), DARKNESS_ALPHA, Color.black);

            // Make sure labels are readable, use scrollbar if needed
            int numTargets = dataset.getColumnCount();
            if (numTargets <= 20) {
                _chartPanel[i].setPreferredSize(null);
            } else {
                _chartPanel[i].getSize(dim);
                dim.height = 50 + numTargets * 24;
                _chartPanel[i].setPreferredSize(dim);
            }

        }
    }

    // Make the X axis displaying the dates
    private DateAxis _makeDateAxis() {
        DateAxis timeAxis = new DateAxis(_getXAxisLabel(), _model.getTimeZone());

        // Override date format to handle LST dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm") {
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

    // Add a marker to the given plot, showing the area of darkness
    private void _addDarknessMarker(CategoryPlot plot, Date startDate, Date endDate, float alpha, Color color) {
        double start = startDate.getTime();
        double end = endDate.getTime();

        if (start < end) {
            IntervalMarker m = new IntervalMarker(start, end, color, DARKNESS_STROKE,
                    color, DARKNESS_STROKE, alpha);
            plot.addRangeMarker(m);
        } else {
            double first = _model.getDateForHour(0.).getTime();
            double last = _model.getDateForHour(24.).getTime();
            IntervalMarker m1 = new IntervalMarker(start, last, color, DARKNESS_STROKE,
                    color, DARKNESS_STROKE, alpha);
            plot.addRangeMarker(m1);
            IntervalMarker m2 = new IntervalMarker(first, end, color, DARKNESS_STROKE,
                    color, DARKNESS_STROKE, alpha);
            plot.addRangeMarker(m2);
        }
    }


    // Return the label for the time axis
    private String _getXAxisLabel() {
        if (_model.getTimeZoneId().equals(ElevationPlotModel.SITE_TIME)) {
            return _I18N.getString("time") + " (" + _model.getTimeZone().getDisplayName() + ")";
        } else {
            return _I18N.getString("time") + " (" + _model.getTimeZoneId() + ")";
        }
    }

    // Make and return the observation chart panel
    private ChartPanel _makeObservationChart(IntervalCategoryDataset dataset, int categoryIndex) {
        String title = _model.getTitle();

        String categoryAxisLabel = _I18N.getString("Observation");
        _chart[categoryIndex] = _createGanttChart(title, categoryAxisLabel, dataset);
        _chart[categoryIndex].setBackgroundPaint(getBackground());
        CategoryPlot plot = (CategoryPlot) _chart[categoryIndex].getPlot();
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);

        TextTitle textTitle = _chart[categoryIndex].getTitle();
        textTitle.setFont(textTitle.getFont().deriveFont(12.0F));
        ChartPanel chartPanel = new ChartPanel(
                _chart[categoryIndex],
                ChartPanel.DEFAULT_WIDTH,
                ChartPanel.DEFAULT_HEIGHT,
                ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH,
                ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT,
                ChartPanel.DEFAULT_MAXIMUM_DRAW_WIDTH,
                ChartPanel.DEFAULT_MAXIMUM_DRAW_HEIGHT,
                true,  // use buffer
                false, // properties
                false, // save
                false, // print
                false, // zoom
                true); // tooltips

        // disable scaling
        chartPanel.setMaximumDrawWidth(100000);
        chartPanel.setMaximumDrawHeight(100000);

        return chartPanel;
    }

    // Modified version of ChartFactory.createGanttChart() that uses a custom renderer and legend
    private JFreeChart _createGanttChart(String title,
                                         String categoryAxisLabel,
                                         IntervalCategoryDataset data) {

        CategoryAxis categoryAxis = new CategoryAxis(categoryAxisLabel);
        categoryAxis.setTickLabelFont(_labelFont);
        DateAxis dateAxis = _makeDateAxis();

        CategoryPlot plot = new CategoryPlot(data, categoryAxis, dateAxis, _renderer) {
            // use a custom legend
            public LegendItemCollection getLegendItems() {
                return _priorityLegendItemCollection;
            }
        };
        _renderer.setShadowVisible(false);
        _renderer.setMaximumBarWidth(0.25);

        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        plot.setOrientation(PlotOrientation.HORIZONTAL);
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        if (_showLegend) {
            useLegendTitle(chart);
        }

        return chart;
    }

    /** Display a dialog for printing the chart */
    public void print() throws PrinterException {
        int index = _tabbedPane.getSelectedIndex();
        if (index != -1) {
            PrintUtil printUtil = new PrintUtil(_chartPanel[index]);
            printUtil.setUseBgThread(false); // otherwise get ConcurrentModificationException
            printUtil.print();
        }
    }

    /**
     * Display a dialog for saving the chart in PNG format.
     */
    public void saveAs() {
        int index = _tabbedPane.getSelectedIndex();
        if (index != -1) {
            try {
                _chartPanel[index].doSaveAs();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    }


    /** Set the visibility of the legend */
    public void setShowLegend(boolean show) {
        _showLegend = show;
        if (_chart != null) {
            if (show) {
                for (JFreeChart a_chart : _chart) {
                    a_chart.addSubtitle(new LegendTitle(a_chart.getPlot()));
                }
            } else {
                for (JFreeChart a_chart : _chart) {
                    a_chart.removeLegend();
                }
            }
        }
    }


}
