package jsky.app.ot.gemini.obscat;

import jsky.app.ot.shared.gemini.obscat.ObsCatalogInfo;
import jsky.catalog.*;
import jsky.catalog.gui.CatalogQueryPanel;
import jsky.util.NameValue;
import jsky.util.gui.GridBagUtil;
import jsky.util.gui.MultiSelectComboBox;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.Serializable;

/**
 * Defines the main panel for querying an ObsCatalog. This replaces the default
 * {@link jsky.catalog.gui.CatalogQueryPanel} with one specialized for the ObsCatalog class.
 *
 * @author Allan Brighton
 */
public class ObsCatalogQueryPanel extends CatalogQueryPanel {

    // For choosing instrument specific options
    private JTabbedPane _tabbedPane;

    // Instrument specific option panels
    private JPanel[] _panels;

    /** Layout utility classes for instrument panels */
    private GridBagUtil[] _layouts;

    /** Array(InstIndex, row) of instrument specific labels displayed */
    private JLabel[][] _panelLabels;

    /** Array(InstIndex, row) of components displayed next to the labels */
    protected JComponent[][] _panelComponents;

    /** Instrument combo box */
    private MultiSelectComboBox<String> _instComboBox;

    /**
     * Initialize a query panel for the given catalog.
     *
     * @param catalog the catalog, for which a user interface component is being generated
     * @param numCols the number of columns to use for the display (should be an even number)
     */
    public ObsCatalogQueryPanel(Catalog catalog, int numCols) {
        super(catalog, numCols);
    }

    /**
     * Make the display panel items.
     * (Redefined from the parent class version to add a tabbed pane for the
     * instrument specific items).
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void makePanelItems() {
        super.makePanelItems();

        _tabbedPane = new JTabbedPane();
        final String[] instruments = ObsCatalogInfo.INSTRUMENTS;
        final int nInst = instruments.length;
        final int nPanels = nInst + 1;
        _panelLabels = new JLabel[nPanels][];
        _panelComponents = new JComponent[nPanels][];
        _panels = new JPanel[nPanels];
        _layouts = new GridBagUtil[nPanels];
        for (int i = 0; i < nPanels; i++) {
            _panels[i] = new JPanel();
            _layouts[i] = new GridBagUtil(_panels[i]);
            if (i == 0) {
                // Non-instrument specific items
                _panels[i].setName("General");
            } else {
                // Instrument specific items
                _panels[i].setName(instruments[i - 1]);
                final FieldDescAdapter[] params = ObsCatalog.getInstrumentParamDesc(instruments[i - 1]);
                if (params != null) {
                    final int n = params.length;
                    _panelLabels[i] = new JLabel[n];
                    _panelComponents[i] = new JComponent[n];

                    for (int j = 0; j < n; j++) {
                        _panelLabels[i][j] = makeLabel(params[j].getName());
                        _panelComponents[i][j] = makeComponent(params[j]);
                    }
                }
            }
        }

        // link the instrument combo box with the tabbed pane containing the instrument
        // specific options
        _instComboBox = (MultiSelectComboBox<String>) getComponentForLabel(ObsCatalogInfo.INSTRUMENT);
        _instComboBox.addActionListener(e -> {
            final int n = _instComboBox.getModel().getSize();
            for (int i = 0; i < n; i++) {
                _tabbedPane.setEnabledAt(i + 1, false);
            }
            final int[] indexes = _instComboBox.getSelectedIndexes();
            for (int indexe : indexes) {
                _tabbedPane.setEnabledAt(indexe + 1, true);
            }
        });
    }

    /**
     * Make and return a combo box with the values that the given field may have.
     */
    @Override
    protected JComponent makeComboBox(FieldDesc p) {
        final int n = p.getNumOptions();

        final NameValue[] ar = new NameValue[n];
        for (int i = 0; i < n; i++) {
            ar[i] = new NameValue(p.getOptionName(i), p.getOptionValue(i));
        }

        final MultiSelectComboBox<NameValue> cb = new MultiSelectComboBox<>(ar);

        final String s = p.getDescription();
        if (s != null) cb.setToolTipText(s);

        cb.addActionListener(e -> fireChange(cb));

        return cb;
    }

    /**
     * Remove the panel items.
     */
    @Override
    protected void removePanelItems() {
        super.removePanelItems();
        remove(_tabbedPane);

        for (int i = 0; i < _panels.length; i++) {
            _tabbedPane.remove(_panels[i]);
            if (i > 0) {
                for (int j = 0; j < _panelLabels[i].length; j++) {
                    if (_panelLabels[i][j] != null)
                        _panels[i].remove(_panelLabels[i][j]);
                    if (_panelComponents[i][j] != null)
                        _panels[i].remove(_panelComponents[i][j]);
                }
            }
        }
    }

    /**
     * Combine the panel items in a tabbed pane layout, with one
     * "General" pane and instrument specific panes.
     *
     * @param layout utility object used for the layout
     * @return the number of rows in the layout
     */
    @Override
    protected int doGridBagLayout(GridBagUtil layout) {

        // put the non-instrument specific items in the first pane
        super.doGridBagLayout(_layouts[0]);

        layout.add(_tabbedPane, 0, 0, 1, 1, 1.0, 0.0,
                   GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST,
                   new Insets(0, 0, 0, 0));

        final int numCols = getNumCols();
        for (int i = 0; i < _panels.length; i++) {
            _tabbedPane.add(_panels[i]);
            if (i > 0) {
                _tabbedPane.setEnabledAt(i, false);
                int row = 0;
                int col = 0;
                for (int j = 0; j < _panelLabels[i].length; j++) {
                    if (_panelLabels[i][j] != null)
                        _layouts[i].add(_panelLabels[i][j], col, row, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.NONE, GridBagConstraints.EAST, LABEL_INSETS);
                    col++;
                    if (_panelComponents[i][j] != null)
                        _layouts[i].add(_panelComponents[i][j], col, row, 1, 1, 1.0, 0.0,
                                        GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, VALUE_INSETS);
                    col++;
                    if (col >= numCols) {
                        col = 0;
                        row++;
                    }
                }
                // add space to fill up at bottom, so widgets will be displayed at the top and not centered vertically
                _layouts[i].add(Box.createVerticalStrut(8), 0, row+1, numCols, 1, 0.0, 1.0,
                                GridBagConstraints.VERTICAL, GridBagConstraints.NORTHWEST, new Insets(0, 0, 0, 0));
            }
        }
        return 1;
    }

    /**
     * Return the display component corresponding to the given instrument name and
     * label text, or null if not found.
     */
    protected JComponent getInstComponentForLabel(String inst, String s) {
        final int i = ObsCatalogInfo.getInstIndex(inst) + 1;
        if (i != 0) {
            for (int j = 0; j < _panelLabels[i].length; j++) {
                if (_panelLabels[i][j].getText().equals(s))
                    return _panelComponents[i][j];
            }
        }
        return null;
    }

    /**
     * Return a QueryArgs object based on the current panel settings
     * that can be passed to the Catalog.query() method.
     * (Redefined from the parent class to handle the extra instrument panels.)
     *
     * @return the QueryArgs object to use for a catalog query.
     */
    @Override
    public ObsCatalogQueryArgs getQueryArgs() {
        final ObsCatalog catalog = (ObsCatalog) getCatalog();
        final ObsCatalogQueryArgs queryArgs = new ObsCatalogQueryArgs(catalog);
        initQueryArgs(queryArgs);

        final String[] instruments = queryArgs.getInstruments();
        if (instruments != null) {
            for (String instrument : instruments) {
                final int instIndex = ObsCatalogInfo.getInstIndex(instrument) + 1;
                final FieldDesc[] params = ObsCatalog.getInstrumentParamDesc(instrument);
                if (params != null) {
                    for (int j = 0; j < _panelLabels[instIndex].length; j++) {
                        final Object value = getValue(params[j], _panelComponents[instIndex][j]);
                        if (value != null)
                            queryArgs.setInstParamValue(instrument, j, value);
                    }
                }
            }
        }
        return queryArgs;
    }

    /**
     * Return the value in the given component, or null if there is no value there.
     * Overrides parent version to allow value ranges (with ">", "<", "<=", ">=") in numerical
     * text fields and to use a MultiSelectComboBox instead of JComboBox for choices.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Serializable getValue(FieldDesc p, JComponent c) {
        if (p.getNumOptions() > 0) {
            // must be a combo box
            final MultiSelectComboBox<NameValue> cb = (MultiSelectComboBox<NameValue>) c;

            // TODO: here we could convert from the "name" to the value
            // maybe a better idea though is to have the combo box return
            // namevalue pairs

            return cb.getSelectedObjects();
        } else {
            // must be a text field
            final String s = ((JTextField) c).getText();
            if (s == null || s.length() == 0)
                return null;

            // Look for a range of values, possibly mixed with symbols, such as ">", "<", "<=", ">=".
            return FieldFormat.getValueRange(p, s);
        }
    }

    /** Set the value in the given component. */
    @SuppressWarnings("unchecked")
    protected void setValue(JComponent c, Object value) {
        if (c instanceof MultiSelectComboBox) {
            if (value instanceof Object[]) {
                ((MultiSelectComboBox<Object>) c).setSelectedObjects((Object[]) value);
            } else {
                ((MultiSelectComboBox<Object>) c).setSelectedObjects(new Object[]{value});
            }
        } else if (c instanceof JTextField) {
            final String s;
            if (value instanceof Double)
                s = nf.format(((Double) value).doubleValue());
            else
                s = value.toString();
            ((JTextField) c).setText(s);
        }
    }

    /**
     * Initialize a QueryArgs object based on the current panel settings
     * so that can be passed to the Catalog.query() method.
     */
    @Override
    public void initQueryArgs(QueryArgs queryArgs) {
        super.initQueryArgs(queryArgs);

        final ObsCatalogQueryArgs qArgs = (ObsCatalogQueryArgs) queryArgs;
        final String[] instruments = qArgs.getParamValueAsStringArray(ObsCatalogInfo.INSTRUMENT);
        if (instruments != null)
            qArgs.setInstruments(instruments);
    }

    // Return the index of the selected instruments, or null if none are selected
    protected int[] _getInstIndexes() {
        if (_instComboBox == null || _instComboBox.getSelectionCount() == 0)
            return null;

        return _instComboBox.getSelectedIndexes();
    }

    // Return an array with the names of the selected instruments, or null if none are selected
    protected String[] _getInstruments() {
        if (_instComboBox == null || _instComboBox.getSelectionCount() == 0)
            return null;
        return _instComboBox.getSelected();
    }

}
