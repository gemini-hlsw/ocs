package jsky.app.ot.gemini.editor;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.shared.gui.bean.PropertyCtrl;
import edu.gemini.spModel.data.ISPDataObject;
import jsky.app.ot.editor.OtItemEditor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A base class for component editors.
 */
public abstract class ComponentEditor<N extends ISPNode, D extends ISPDataObject> extends OtItemEditor<N, D> {

    private static final Logger LOG = Logger.getLogger(ComponentEditor.class.getName());

    /**
     * Size of the gap between rows of properties.
     */
    public static final int PROPERTY_ROW_GAP = 10;

    /**
     * Size of the row gap between a component and it's warning label.
     */
    public static final int WARNING_ROW_GAP = 5;

    /**
     * Size of the gap between columns of properties.
     */
    public static final int PROPERTY_COLUMN_GAP = 10;

    /**
     * Size of the gap between a label and the widget it names.
     */
    public static final int LABEL_WIDGET_GAP = 5;

    /**
     * Border to place around the panel.
     */
    public static final Border PANEL_BORDER = BorderFactory.createEmptyBorder(15, 15, 15, 15);

    /**
     * Border to place around a tab panel.
     */
    public static final Border TAB_PANEL_BORDER = BorderFactory.createEmptyBorder(10, 5, 5, 5);

    /**
     * Background color to use for a panel with read-only information.
     */
    public static final Color INFO_BG_COLOR = new Color(247, 243, 239);

    /**
     * Color to use for fatal problems.
     */
    public static final Color FATAL_FG_COLOR = new Color(204, 0, 0);

    /**
     * Color to use for warnings.
     */
    public static final Color WARNING_FG_COLOR = new Color(153, 153, 0);

    // A mouse listener that can be used to properly place the caret in the place the mouse is clicked
    // overriding the default behavior which will put the caret in position 0
    // This could be blanket enabled by updating the class TextFieldPropertyCtrl.
    protected static final MouseListener focusOnCaretPositionListener = new MouseAdapter() {
        public void mousePressed(final MouseEvent e) {
            SwingUtilities.invokeLater(() -> {
                if (e.getSource() instanceof JTextField) {
                    final JTextField tf = (JTextField) e.getSource();
                    final int offset = tf.viewToModel(e.getPoint());
                    tf.setCaretPosition(offset);
                }
            });
        }
    };

    /**
     * Creates GridBagConstraints appropriate for a property label to the
     * left of the widget it edits.  The label will be aligned to the east.
     *
     * @param x GridBagConstraints.gridx
     * @param y GridBagConstraints.gridy
     */
    public static GridBagConstraints propLabelGbc(final int x, final int y) {
        return propLabelGbc(x, y, 1, 1);
    }

    /**
     * Creates GridBagConstraints appropriate for a property label to the
     * left of the widget it edits.  The label will be aligned to the east.
     *
     * @param x GridBagConstraints.gridx
     * @param y GridBagConstraints.gridy
     * @param w GridBagConstraints.gridwidth
     * @param h GridBagConstraints.gridheight
     */
    public static GridBagConstraints propLabelGbc(final int x, final int y, final int w, final int h) {
        return new GridBagConstraints(x, y, w, h, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(PROPERTY_ROW_GAP, 0, 0, LABEL_WIDGET_GAP), 0, 0);
    }

    /**
     * Creates GridBagConstraints appropriate for a widget.
     *
     * @param x GridBagConstraints.gridx
     * @param y GridBagConstraints.gridy
     */
    public static GridBagConstraints propWidgetGbc(final int x, final int y) {
        return propWidgetGbc(x, y, 1, 1);
    }

    /**
     * Creates GridBagConstraints appropriate for a widget.
     *
     * @param x GridBagConstraints.gridx
     * @param y GridBagConstraints.gridy
     * @param w GridBagConstraints.gridwidth
     * @param h GridBagConstraints.gridheight
     */
    public static GridBagConstraints propWidgetGbc(final int x, final int y, final int w, final int h) {
        return new GridBagConstraints(x, y, w, h, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(PROPERTY_ROW_GAP, 0, 0, 0), 0, 0);
    }

    /**
     * Creates GridBagConstraints appropriate for a label showing the units
     * of a widget, placed in the same row after the widget.  The label will
     * be aligned to the west.
     *
     * @param x GridBagConstraints.gridx
     * @param y GridBagConstraints.gridy
     */
    public static GridBagConstraints propUnitsGbc(final int x, final int y) {
        return new GridBagConstraints(x, y, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(PROPERTY_ROW_GAP, LABEL_WIDGET_GAP, 0, 0), 0, 0);
    }

    /**
     * Creates GridBagConstraints appropriate for a gap between columns that
     * contain widgets.
     *
     * @param x GridBagConstraints.gridx
     * @param y GridBagConstraints.gridy
     */
    public static GridBagConstraints colGapGbc(final int x, final int y) {
        return new GridBagConstraints(x, y, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), PROPERTY_COLUMN_GAP, 0);
    }

    /**
     * Creates GridBagConstraints appropriate for a JSeparator component.
     *
     * @param x     GridBagConstraints.gridx
     * @param y     GridBagConstraints.gridy
     * @param width GridBagConstraints.gridwidth
     */
    public static GridBagConstraints separatorGbc(final int x, final int y, final int width) {
        return new GridBagConstraints(x, y, width, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(PROPERTY_ROW_GAP, 0, 0, 0), 0, 0);
    }

    /**
     * Creates GridBagConstraints appropriate for a panel used to push the
     * contents of the editor to the top left of the page.
     *
     * @param x GridBagConstraints.gridx
     * @param y GridBagConstraints.gridy
     */
    public static GridBagConstraints pushGbc(final int x, final int y) {
        return new GridBagConstraints(x, y, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0);
    }

    /**
     * Creates GridBagConstraints appropriate for a warning label displayed
     * below a component when there is a problem.
     *
     * @param x     GridBagConstraints.gridx
     * @param y     GridBagConstraints.gridy
     * @param width GridBagConstraints.gridwidth
     */
    public static GridBagConstraints warningLabelGbc(final int x, final int y, final int width) {
        return new GridBagConstraints(x, y, width, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, WARNING_ROW_GAP);
    }

    /**
     * Adds a {@link PropertyCtrl} item to the given panel at the given x/y
     * position.
     *
     * @param pan  panel in which the widgets will be added
     * @param x    GridBagConstraints.gridx position for the label widget
     * @param y    GridBagConstraints.gridy position for the label widget
     * @param ctrl {@link PropertyCtrl} element that contains the widget to
     *             place
     * @param <D>  bean type
     * @param <T>  property type
     */
    public static <D extends ISPDataObject, T> void addCtrl(final JPanel pan, final int x, final int y,
                                                            final PropertyCtrl<D, T> ctrl) {
        addCtrl(pan, x, y, ctrl, null);
    }

    /**
     * Adds a {@link PropertyCtrl} item to the given panel at the given x/y
     * position.
     *
     * @param pan   panel in which the widgets will be added
     * @param x     GridBagConstraints.gridx position for the label widget
     * @param y     GridBagConstraints.gridy position for the label widget
     * @param ctrl  {@link PropertyCtrl} element that contains the widget to
     *              place
     * @param units units label to display to the left of the widget (optional)
     * @param <D>   bean type
     * @param <T>   property type
     */
    public static <D extends ISPDataObject, T> void addCtrl(final JPanel pan, final int x, final int y,
                                                            final PropertyCtrl<D, T> ctrl, final String units) {
        addCtrl(pan, x, y, 1, 1, ctrl, units);
    }

    /**
     * Adds a {@link PropertyCtrl} item to the given panel at the given x/y
     * position.
     *
     * @param pan   panel in which the widgets will be added
     * @param x     GridBagConstraints.gridx position for the label widget
     * @param y     GridBagConstraints.gridy position for the label widget
     * @param w     GridBagConstraints.gridwidth
     * @param h     GridBagConstraints.gridheight
     * @param ctrl  {@link PropertyCtrl} element that contains the widget to
     *              place
     * @param units units label to display to the left of the widget (optional)
     * @param <D>   bean type
     * @param <T>   property type
     */
    public static <D extends ISPDataObject, T> void addCtrl(final JPanel pan,
                                                            final int x, final int y, final int w, final int h,
                                                            final PropertyCtrl<D, T> ctrl, final String units) {
        pan.add(new JLabel(ctrl.getDescriptor().getDisplayName()), propLabelGbc(x, y, 1, h));
        pan.add(ctrl.getComponent(), propWidgetGbc(x + 1, y, w, h));
        if (units != null) pan.add(new JLabel(units), propUnitsGbc(x + 2, y));
    }

    /**
     * Sets the value associated with the given property, on the given
     * instrument, with the given value.
     *
     * @param pd   property to be set
     * @param comp data object for the component
     * @param val  new value of the property
     * @param <D>  type of the instrument
     */
    public static <D extends ISPDataObject> void set(final PropertyDescriptor pd, final D comp, final Object val) {
        if (comp == null) return;
        final Method m = pd.getWriteMethod();
        try {
            m.invoke(comp, val);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not update instrument property " + pd.getName(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Gets the value associated with the given property, on the given
     * instrument.
     *
     * @param pd   property to be read
     * @param comp data object for the instrument
     * @param <D>  type of the component
     */
    public static <D extends ISPDataObject> Object get(final PropertyDescriptor pd, final D comp) {
        if (comp == null) return null;
        final Method m = pd.getReadMethod();
        try {
            return m.invoke(comp);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not read instrument property " + pd.getName(), ex);
            throw new RuntimeException(ex);
        }
    }


    /**
     * Handle any tasks required before resetting the instrument data object.
     * For example, removing property change listeners.
     */
    protected void handlePreDataObjectUpdate(final D dataObj) {
    }

    @Override
    protected void cleanup() {
        handlePreDataObjectUpdate(getDataObject());
    }

    @Override
    public void init() {
        handlePostDataObjectUpdate(getDataObject());
    }

    /**
     * Handle any tasks required after resetting the instrument data object.
     * For example, adding any property change listeners.
     */
    protected void handlePostDataObjectUpdate(final D dataObj) {
    }

}
