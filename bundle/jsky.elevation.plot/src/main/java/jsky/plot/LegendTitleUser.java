package jsky.plot;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.LegendTitle;
import org.jfree.ui.RectangleInsets;

public interface LegendTitleUser {
    // Used to properly space out legend titles in panels used in this class.
    RectangleInsets TITLE_INSETS = new RectangleInsets(2, 2, 2, 15);
    default void useLegendTitle(final JFreeChart chart) {
        final LegendTitle title = new LegendTitle(chart.getPlot());
        title.setItemLabelPadding(TITLE_INSETS);
        chart.addSubtitle(title);
    }
}
