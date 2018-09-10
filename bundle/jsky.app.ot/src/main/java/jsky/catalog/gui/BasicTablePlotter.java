package jsky.catalog.gui;

import diva.canvas.CanvasLayer;
import diva.canvas.DamageRegion;
import diva.canvas.TransformContext;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.LayerListener;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.SiderealTarget;
import jsky.catalog.*;
import jsky.coords.*;
import jsky.graphics.CanvasGraphics;
import jsky.image.graphics.DivaImageGraphics;
import jsky.image.graphics.ShapeUtil;
import jsky.image.gui.GraphicsImageDisplay;
import jsky.image.gui.ImageCoordinateConverter;
import jsky.navigator.NavigatorPane;
import jsky.util.java2d.ShapeUtilities;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements basic plotting of catalog tables on in an image window.
 * For each row in a table query result, one or more symbols may be displayed
 * in an image window at a position given by the row's coordinate columns.
 * <p>
 * Note: This class was previously implemented using Diva figures for catalog symbols,
 * however this turned out to be slow for large numbers of figures, so this version
 * handles the drawing and selection of catalog symbols directly.
 *
 * @version $Revision: 38445 $
 * @author Allan Brighton
 */
public class BasicTablePlotter
        implements TablePlotter, LayerListener, ChangeListener {

    /** The Diva layer to use to draw catalog symbols */
    private CanvasLayer _layer;

    /** The object used to convert to screen coordinates for drawing */
    private final ImageCoordinateConverter _coordinateConverter;

    /** The equinox of the image (assumed by the ImageCoordinateConverter methods) */
    private double _imageEquinox = 2000.;

    /** List of (table, symbolList) pairs, used to keep track of symbols for tables. */
    private List<TableListItem> _tableList = new LinkedList<>();

    /** Array of (symbol, figureList) pairs (for each table, which may have multiple plot symbols) */
    private SymbolListItem[] _symbolAr;

    /** List of (shape, rowNum) pairs (for each table/symbol entry). */
    private List<FigureListItem> _figureList;

    /** If true, catalog symbols are visible, otherwise hidden */
    private boolean _visible = true;

    /** list of listeners for selection events on symbols */
    private EventListenerList _listenerList = new EventListenerList();

    /** list of listeners for selection events on symbols */
    private EventListenerList _tableListenerList = new EventListenerList();

    /** Hash table for caching parsed plot symbol info. */
    private final Map<Object, Object> _plotSymbolMap = new HashMap<>();

    /** Used to draw selected symbols */
    private Stroke _selectedStroke = new BasicStroke(3.0F);


    /**
     * Default Constructor
     * (Note: you need to call setCanvasGraphics() and setCoordinateConverter()
     * before using this object).
     *
     * @param canvasGraphics Set the object to use to draw catalog symbols
     * @param cc             Return the object used to convert to screen coordinates for drawing
     */
    public BasicTablePlotter(final CanvasGraphics canvasGraphics, final CoordinateConverter cc) {
        /* The object to use to draw catalog symbols */
        // TODO Make the castings safer
        DivaImageGraphics _imageGraphics = (DivaImageGraphics) canvasGraphics;
        final NavigatorPane pane = (NavigatorPane) _imageGraphics.getGraphicsPane();
        _layer = pane.getSymbolLayer();
        pane.getBackgroundEventLayer().addLayerListener(this);
        _coordinateConverter = (ImageCoordinateConverter) cc;
    }

    /** Return the object used to convert to screen coordinates for drwing */
    @Override
    public CoordinateConverter getCoordinateConverter() {
        return _coordinateConverter;
    }

    /**
     * Check if there is an image loaded and if so, if it supports world coordinates.
     * If no image is loaded, try to generate a blank image for plotting.
     *
     * @param table describes the table data
     * @return false if no suitable image could be used or generated, otherwise true
     */
    public boolean check(final TableQueryResult table) {
        // If no image is being displayed, try to generate a blank WCS image
        final GraphicsImageDisplay imageDisplay = (GraphicsImageDisplay) _coordinateConverter.getImageDisplay();
        if (imageDisplay.isClear()) {
            final WorldCoordinates pos = table.getWCSCenter();
            if (pos != null) {
                imageDisplay.blankImage(pos.getRaDeg(), pos.getDecDeg());
            }
        }

        return _coordinateConverter.isWCS();
    }


    /**
     * Plot the given table data.
     *
     * @param table describes the table data
     */
    @Override
    public void plot(final TableQueryResult table) {
        if (_layer == null || _coordinateConverter == null) {
            return;
        }

        if (!check(table)) {
            return;
        }

        final TablePlotSymbol[] symbols = getPlotSymbolInfo(table);
        if (symbols == null) {
            return;
        }

        // The symbol objects need a reference to the table being plotted
        // to evaluate expressions based on column values
        for (TablePlotSymbol symbol : symbols) symbol.setTable(table);

        // holds the plot symbols for this table
        _symbolAr = new SymbolListItem[symbols.length];
        for (int i = 0; i < symbols.length; i++) {
            // holds the figure info for this table/symbol entry
            _symbolAr[i] = new SymbolListItem(symbols[i]);
        }

        // plot the symbols
        plotSymbols(table, symbols);

        // add this table to the list of plotted tables
        // (but remove any previous table from the same source)
        final ListIterator<TableListItem> it = _tableList.listIterator(0);
        while (it.hasNext()) {
            final TableListItem item = it.next();
            if (item.table.equals(table) || item.table.getName().equals(table.getName())) {
                if (item.table.getCatalog().equals(table.getCatalog())) {
                    it.remove();
                    break;
                }
            }
        }
        _tableList.add(new TableListItem(table, _symbolAr));

        // track changes in the WCS coords
        _coordinateConverter.removeChangeListener(this);
        _coordinateConverter.addChangeListener(this);

        _layer.repaint();
    }


    /** Called when the WCS info changes */
    @Override
    public void stateChanged(final ChangeEvent e) {
        replotAll();
    }


    /**
     * Return the plot symbol info for the given table.
     *
     * @param table object representing the catalog table
     * @return an array of TablePlotSymbol objects, one for each plot symbol defined.
     */
    @Override
    public TablePlotSymbol[] getPlotSymbolInfo(final TableQueryResult table) {
        // first see if we have the plot information cached
        Object o = _plotSymbolMap.get(table);
        final Catalog catalog = table.getCatalog();
        if (o == null) {
            // also check the catalog where the query originated
            if (catalog != null)
                o = _plotSymbolMap.get(catalog);
        }
        if (o instanceof TablePlotSymbol[])
            return (TablePlotSymbol[]) o;

        if (catalog instanceof PlotableCatalog) {
            TablePlotSymbol[] symbols = ((PlotableCatalog) catalog).getSymbols();
            if (symbols != null) {
                _plotSymbolMap.put(table, symbols);
                _plotSymbolMap.put(catalog, symbols);
            }
            return symbols;
        }
        return null;
    }

    /** Set the plot symbol info for the given table */
    @Override
    public void setPlotSymbolInfo(final TableQueryResult table, final TablePlotSymbol[] symbols) {
        final Catalog catalog = table.getCatalog();
        _plotSymbolMap.put(table, symbols);
        if (catalog != null)
            _plotSymbolMap.put(catalog, symbols);
    }

    /**
     * Plot the table data using the given symbol descriptions.
     *
     * @param table describes the table data
     * @param symbols an array of objects describing the symbols to plot
     */
    private void plotSymbols(final TableQueryResult table, final TablePlotSymbol[] symbols) {
        // for each row in the catalog, evaluate the expressions and plot the symbols
        final int nrows = table.getRowCount();
        final RowCoordinates rowCoords = table.getRowCoordinates();

        final Vector<Vector<Object>> dataVec = table.getDataVector();

        final boolean isWCS = rowCoords.isWCS();
        final boolean isPix = rowCoords.isPix();
        final int cooSys;
        if (isPix) {
            cooSys = CoordinateConverter.IMAGE;
        } else if (isWCS) {
            cooSys = CoordinateConverter.WORLD;
            _imageEquinox = _coordinateConverter.getEquinox();
        } else
            throw new RuntimeException("no wcs or image coordinates to plot");

        for (int row = 0; row < nrows; row++) {
            final Vector<Object> rowVec = dataVec.get(row);
            final Coordinates pos = rowCoords.getCoordinates(rowVec);
            if (pos == null)
                continue;   // coordinates might be missing - just ignore

            double x, y;
            if (isPix) {
                x = pos.getX();
                y = pos.getY();
            } else {
                // need to keep table values in the image equinox, since the WCS conversion
                // methods all assume the image equinox
                double[] radec = ((WorldCoords) pos).getRaDec(_imageEquinox);

                x = radec[0];
                y = radec[1];
            }

            for (int i = 0; i < symbols.length; i++) {
                _figureList = _symbolAr[i].figureList;
                try {
                    plotRow(row, rowVec, x, y, cooSys, symbols[i]);
                } catch (Exception e) {
                    //e.printStackTrace();
                    // ignore: may be WCS out of range...
                }
            }
        }
    }


    /**
     * Plot the symbol for the given row.
     * The row data is taken from the given row vector.
     *
     * @param row the row number (first row is 0)
     * @param rowVec a vector containing the row elements
     * @param x the X position coordinate
     * @param y the Y position coordinate
     * @param cooSys the coordinate system of X and Y (CoordinateConverter constant)
     * @param symbol an object describing the symbol
     */
    private void plotRow(final int row, final Vector<Object> rowVec, final double x, final double y,
                           final int cooSys, final TablePlotSymbol symbol) {
        // eval expr to get condition

        final boolean cond = symbol.getCond(rowVec);
        if (!cond)
            return;

        // eval expr to get radius
        final double radius = symbol.getSize(rowVec);
        if (radius <= 0. || Double.isNaN(radius)) {
            // don't want a neg or 0 radius
            return;
        }

        // ratio may be an expression with column name variables
        final double ratio = symbol.getRatio(rowVec);

        // angle may be an expression with column name variables
        final double angle = symbol.getAngle(rowVec);

        // label may also contain col name vars, but might not be numeric
        final String label = symbol.getLabel(rowVec);

        plotSymbol(row, symbol, x, y, cooSys, radius, ratio, angle, label);
    }


    /**
     * Plot the given symbol.
     *
     * @param row the row number (starting with 0)
     * @param symbol an object describing the symbol
     * @param x the X position coordinate
     * @param y the Y position coordinate
     * @param cooSys the coordinate system of X and Y (CoordinateConverter constant)
     * @param radius the radius (size) of the symbol (the symbol object contains the size units)
     * @param ratio the x/y ratio (ellipticity ratio) of the symbol
     * @param angle the rotation angle
     * @param label the label to display next to the symbol
     */
    private void plotSymbol(final int row, final TablePlotSymbol symbol, final double x, final double y,
                              final int cooSys, final double radius, final double ratio, final double angle, final String label) {

        // convert to screen coordinates
        final Point2D.Double pos = new Point2D.Double(x, y);
        _coordinateConverter.convertCoords(pos, cooSys, CoordinateConverter.SCREEN, false);

        final Point2D.Double size = new Point2D.Double(radius, radius);
        final int sizeType = getCoordType(symbol.getUnits());
        _coordinateConverter.convertCoords(size, sizeType, CoordinateConverter.SCREEN, true);

        // get the Shape object for the symbol
        final Shape shape = makeShape(symbol, pos.x, pos.y, Math.max(size.x, size.y), ratio, angle);

        // Add an item for this symbol to the figure list, and store it as client data also
        final FigureListItem item = new FigureListItem(shape, label, row);
        _figureList.add(item);
    }

    /**
     * Return the CoordinateConverter type code for the given name.
     */
    private int getCoordType(final String name) {
        if (name != null && name.length() != 0) {
            if (name.startsWith("deg"))
                return CoordinateConverter.WORLD;
            if (name.equals("image"))
                return CoordinateConverter.IMAGE;
            if (name.equals("screen"))
                return CoordinateConverter.SCREEN;
            if (name.equals("canvas"))
                return CoordinateConverter.CANVAS;
            if (name.equals("user"))
                return CoordinateConverter.USER;
        }
        return CoordinateConverter.IMAGE;
    }


    /**
     * Return the Shape object for the given symbol.
     *
     * @param symbol an object describing the symbol
     * @param x the X position screen coordinate
     * @param y the Y position screen coordinate
     * @param size the radius of the symbol in screen coordinates
     * @param ratio the x/y ratio (ellipticity ratio) of the symbol
     * @param angle the rotation angle
     */
    private Shape makeShape(final TablePlotSymbol symbol, final double x, final double y, final double size,
                              final double ratio, final double angle) {

        final int shape = symbol.getShape();

        // do the simple ones first
        switch (shape) {
            case TablePlotSymbol.CIRCLE:
                return new Ellipse2D.Double(x - size, y - size, size * 2, size * 2);

            case TablePlotSymbol.SQUARE:
                return new Rectangle2D.Double(x - size, y - size, size * 2, size * 2);

            case TablePlotSymbol.CROSS:
                return ShapeUtil.makeCross(x, y, size);

            case TablePlotSymbol.TRIANGLE:
                return ShapeUtil.makeTriangle(x, y, size);

            case TablePlotSymbol.DIAMOND:
                return ShapeUtil.makeDiamond(x, y, size);
        }

        // get center, north and east in screen coords
        final Point2D.Double center = new Point2D.Double(x, y);
        final Point2D.Double north = new Point2D.Double(x, y - size);
        final Point2D.Double east = new Point2D.Double(x - size, y);

        // Get WCS NORTH and EAST, converted to screen coords
        getNorthAndEast(center, size, ratio, angle, north, east);

        switch (shape) {
            case TablePlotSymbol.COMPASS:
                return ShapeUtil.makeCompass(center, north, east);

            case TablePlotSymbol.LINE:
                return ShapeUtil.makeLine(center, north, east);

            case TablePlotSymbol.ARROW:
                return ShapeUtil.makeArrow(center, north);

            case TablePlotSymbol.ELLIPSE:
                return ShapeUtil.makeEllipse(center, north, east);

            case TablePlotSymbol.PLUS:
                return ShapeUtil.makePlus(center, north, east);
        }

        throw new RuntimeException("Unknown plot symbol shape: " + symbol.getShapeName());
    }


    /**
     * Set x and y in the north and east parameters in screen
     * coordinates, given the center point and radius in screen
     * coordinates, an optional rotation angle, and an x/y ellipticity
     * ratio.  If the image supports world coordinates, that is taken
     * into account (the calculations are done in RA,DEC before
     * converting to screen coords).  The conversion to screen coords
     * automatically takes the current zoom and rotate settings into
     * account.
     *
     * @param center the center position screen coordinate
     * @param size the radius of the symbol in screen coordinates
     * @param ratio the x/y ratio (ellipticity ratio) of the symbol
     * @param angle the rotation angle
     * @param north on return, contains the screen coordinates of WCS north
     * @param east on return, contains the screen coordinates of WCS east
     */
    private void getNorthAndEast(final Point2D.Double center,
                                   final double size,
                                   final double ratio,
                                   final double angle,
                                   final Point2D.Double north,
                                   final Point2D.Double east) {

        if (_coordinateConverter.isWCS()) {
            // get center and radius in deg 2000
            final Point2D.Double wcsCenter = new Point2D.Double(center.x, center.y);
            _coordinateConverter.screenToWorldCoords(wcsCenter, false);
            final Point2D.Double wcsRadius = new Point2D.Double(size, size);
            _coordinateConverter.screenToWorldCoords(wcsRadius, true);

            // adjust the radius by the ratio
            if (ratio < 1.)
                wcsRadius.y *= 1.0 / ratio;
            else if (ratio > 1.)
                wcsRadius.x *= ratio;

            // set east
            east.x = Math.IEEEremainder(wcsCenter.x + Math.abs(wcsRadius.x) / Math.cos((wcsCenter.y / 180.) * Math.PI), 360.);
            if (east.x < 0.)
                east.x += 360.;

            east.y = wcsCenter.y;

            // set north
            north.x = wcsCenter.x;

            north.y = wcsCenter.y + Math.abs(wcsRadius.y);
            if (north.y >= 90.)
                north.y = 180. - north.y;
            else if (north.y <= -90.)
                north.y = -180. - north.y;

            // convert back to screen coords
            _coordinateConverter.worldToScreenCoords(north, false);
            _coordinateConverter.worldToScreenCoords(east, false);
        } else {
            // not using world coords: adjust the radius by the ratio
            double rx = size, ry = size;
            if (ratio < 1.)
                ry *= 1.0 / ratio;
            else if (ratio > 1.)
                rx *= ratio;

            east.x = center.x - rx;
            east.y = center.y;

            north.x = center.x;
            north.y = center.y - ry;
        }

        // rotate by angle
        if (angle != 0.) {
            rotatePoint(north, center, angle);
            rotatePoint(east, center, angle);
        }
    }


    /**
     * Rotate the point p around the center point by the given
     * angle in deg.
     */
    private void rotatePoint(final Point2D.Double p, final Point2D.Double center, final double angle) {
        p.x -= center.x;
        p.y -= center.y;
        final double tmp = p.x;
        final double rad = angle * Math.PI / 180.;
        final double cosa = Math.cos(rad);
        final double sina = Math.sin(rad);
        p.x = p.x * cosa + p.y * sina + center.x;
        p.y = -tmp * sina + p.y * cosa + center.y;
    }


    /** Erase the plot of the given table data */
    @Override
    public void unplot(TableQueryResult table) {
        final ListIterator<TableListItem> it = _tableList.listIterator(0);
        while (it.hasNext()) {
            TableListItem item = it.next();
            if (item.table.equals(table)) {
                it.remove();
                _layer.repaint();
                break;
            }
        }
    }

    /** Erase all plot symbols */
    @Override
    public void unplotAll() {
        _tableList = new LinkedList<>();
        _layer.repaint();
    }


    /** Recalculate the coordinates and replot all symbols after a change in the coordinate system. */
    @SuppressWarnings("unchecked")
    @Override
    public void replotAll() {
        final LinkedList<TableListItem> list = new LinkedList<>(_tableList);
        _tableList = new LinkedList<>();
        for (TableListItem tli: list) {
            tli.inRange = tableInRange(tli.table);
            if (tli.inRange)
                plot(tli.table);
            else
                _tableList.add(tli);
        }
        _layer.repaint();
    }

    /** Return an array containing the tables managed by this object. */
    @Override
    public TableQueryResult[] getTables() {
        if (!_tableList.isEmpty()) {
            final List<TableQueryResult> tableVec = _tableList.stream().filter(item -> item.inRange).map(item -> item.table).collect(Collectors.toList());
            if (!tableVec.isEmpty()) {
                final TableQueryResult[] tables = new TableQueryResult[tableVec.size()];
                tableVec.toArray(tables);
                return tables;
            }
        }
        return null;
    }

    /** Schedule a repaint of the area given by the given shape */
    private void repaint(final Shape shape) {
        _layer.repaint(DamageRegion.createDamageRegion(new TransformContext(_layer), shape.getBounds2D()));
    }

    /** Set the selection state of the symbol corresponding to the given table row */
    private void selectSymbol(final TableQueryResult table, final int tableRow, final boolean selected) {
        // Find the plot symbol for the given row in the given table
        _tableList.stream().filter(tli -> tli.table.equals(table)).forEach(tli -> {
            for (SymbolListItem sli : tli.symbolAr) {
                sli.figureList.stream().filter(fli -> fli.row == tableRow).filter(fli -> fli.selected != selected).forEach(fli -> {
                    fli.selected = selected;
                    repaint(fli.shape);
                });
            }
        });
        fireTableSelectionEvent(table, tableRow, selected);
    }

    /** Select the symbol corresponding to the given table row */
    @Override
    public void selectSymbol(final TableQueryResult table, final int tableRow) {
        selectSymbol(table, tableRow, true);
    }

    /** Deselect the symbol corresponding to the given table row */
    @Override
    public void deselectAll(final TableQueryResult table) {
        _tableList.stream().filter(tli -> tli.table.equals(table)).forEach(tli -> {
            for (SymbolListItem sli : tli.symbolAr) {
                sli.figureList.stream().filter(fli -> fli.selected).forEach(fli -> {
                    fli.selected = false;
                    repaint(fli.shape);
                });
            }
        });
        // TODO Should it fire table selection events?
    }

    /** Deselect the symbol corresponding to the given table row */
    @Override
    public void deselectSymbol(final TableQueryResult table, final int tableRow) {
        selectSymbol(table, tableRow, false);
    }

    /** Add a listener for selection events on symbols */
    @Override
    public void addSymbolSelectionListener(final SymbolSelectionListener listener) {
        _listenerList.remove(SymbolSelectionListener.class, listener);
        _listenerList.add(SymbolSelectionListener.class, listener);
    }

    /** Remove a listener for selection events on symbols */
    @Override
    public void removeSymbolSelectionListener(final SymbolSelectionListener listener) {
        _listenerList.remove(SymbolSelectionListener.class, listener);
    }

    /**
     * Notify any listeners that a symbol was selected or deselected.
     *
     * @param table the table containing the symbol
     * @param row the row index of the selected symbol
     * @param isSelected set to true if the symbol was selected, false if deselected
     */
    private void fireSymbolSelectionEvent(final TableQueryResult table, final int row, final boolean isSelected) {
        final SymbolSelectionEvent event = new SymbolSelectionEvent(row, table);
        final Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SymbolSelectionListener.class) {
                final SymbolSelectionListener listener = (SymbolSelectionListener) listeners[i + 1];
                if (isSelected)
                    listener.symbolSelected(event);
                else
                    listener.symbolDeselected(event);
            }
        }
    }

    /** Add a listener for selection events on tables */
    @Override
    public void addTableSelectionListener(final TableSelectionListener listener) {
        _tableListenerList.add(TableSelectionListener.class, listener);
    }

    /** Remove a listener for selection events on tables */
    @Override
    public void removeTableSelectionListener(final TableSelectionListener listener) {
        _tableListenerList.remove(TableSelectionListener.class, listener);
    }

    /**
     * Notify any listeners that a table row was selected
     *
     * @param table the table containing the selected row
     * @param row the selected row index
     * @param selected set to true if the row was selected, false if deselected
     */
    private void fireTableSelectionEvent(final TableQueryResult table, final int row, final boolean selected) {
        final TableSelectionEvent event = new TableSelectionEvent(row, table);
        final Object[] listeners = _tableListenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TableSelectionListener.class) {
                final TableSelectionListener listener = (TableSelectionListener) listeners[i + 1];
                if (selected) {
                    listener.tableSelected(event);
                }
            }
        }
    }


    /**
     * If the given argument is false, hide all plot symbols managed by this object,
     * otherwise show them again.
     */
    @Override
    public void setVisible(final boolean isVisible) {
        _visible = isVisible;
    }

    /**
     * Return a panel to use to configure the plot symbols for the given table.
     *
     * @param table the result of a query
     */
    @Override
    public JPanel getConfigPanel(final TableQueryResult table) {
        return new TableSymbolConfig(this, table);
    }

    /**
     * Paint the catalog symbols using the given graphics object.
     *
     * @param g2d the graphics context
     * @param region if not null, the region to paint
     */
    @Override
    public void paintSymbols(final Graphics2D g2d, final Rectangle2D region) {
        if (!_visible)
            return;

        // plot each table
        g2d.setPaintMode();
        for (TableListItem tli: _tableList) {
            if (!tli.inRange) // ignore tables not in image range
                continue;
            // plot each symbol type in the table
            for (SymbolListItem sli: tli.symbolAr) {
                g2d.setColor(sli.symbol.getFg());
                // plot each figure
                // draw selected symbols with a thicker stroke
                // If there is a label for the symbol, draw it too
                sli.figureList.stream().filter(fli -> region == null || fli.shape.intersects(region)).forEach(fli -> {
                    if (fli.selected) {
                        // draw selected symbols with a thicker stroke
                        final Stroke stroke = g2d.getStroke();
                        g2d.setStroke(_selectedStroke);
                        g2d.draw(fli.shape);
                        g2d.setStroke(stroke);
                    } else {
                        g2d.draw(fli.shape);
                    }
                    // If there is a label for the symbol, draw it too
                    if (fli.label != null) {
                        final Rectangle2D r = fli.shape.getBounds();
                        g2d.drawString(fli.label, (float) r.getCenterX(), (float) r.getCenterY());
                    }
                });
            }
        }
    }


    /**
     * Transform the plot symbols using the given AffineTransform
     * (called when the image is transformed, to keep the plot symbols up to date).
     */
    @Override
    public void transformGraphics(AffineTransform trans) {
        for (TableListItem tli: _tableList) {
            for (SymbolListItem sli: tli.symbolAr) {
                for (FigureListItem fli: sli.figureList) {
                    fli.shape = ShapeUtilities.transformModify(fli.shape, trans);
                }
            }
        }
        _layer.repaint();
    }


    /**
     * Return true if the coordinates of the objects in the given table may be in a
     * range where they can be plotted in the current image.
     */
    private boolean tableInRange(final TableQueryResult table) {
        // get the coordinates of the region that the table covers from the query arguments, if known
        QueryArgs queryArgs = table.getQueryArgs();
        CoordinateRadius region;
        if (queryArgs == null) {
            // scan table here to get the range that it covers
            region = getTableRegion(table);
            if (region != null) {
                queryArgs = new BasicQueryArgs(table);
                queryArgs.setRegion(region);
                table.setQueryArgs(queryArgs);
            }
        } else {
            region = queryArgs.getRegion();
        }
        if (region == null)
            return false;
        final Coordinates centerPosition = region.getCenterPosition();
        if (!(centerPosition instanceof WorldCoords)) {
            return true;
        }
        if (!_coordinateConverter.isWCS())
            return false;

        WorldCoords pos = (WorldCoords) centerPosition;
        double ra = pos.getRaDeg();
        double dec = pos.getDecDeg();
        double w = region.getWidth();     // in arcmin
        double h = region.getHeight();
        final Rectangle2D.Double tableRect = new Rectangle2D.Double(ra, dec, w, h);

        // get the image coords
        final Point2D.Double p = _coordinateConverter.getWCSCenter();
        pos = new WorldCoords(p.x, p.y, _imageEquinox);
        ra = pos.getRaDeg();
        dec = pos.getDecDeg();
        w = _coordinateConverter.getWidthInDeg() * 60;  // in arcmin
        h = _coordinateConverter.getHeightInDeg() * 60;
        final Rectangle2D.Double imageRect = new Rectangle2D.Double(ra, dec, w, h);

        return tableRect.intersects(imageRect);
    }

    /**
     * Scan the given table and return an object describing the area of the sky that
     * it covers, or null if not known.
     */
    private CoordinateRadius getTableRegion(final TableQueryResult table) {
        final int nrows = table.getRowCount();
        if (nrows == 0)
            return null;

        final RowCoordinates rowCoords = table.getRowCoordinates();
        final double tableEquinox = rowCoords.getEquinox();
        final Vector<Vector<Object>> dataVec = table.getDataVector();

        if (rowCoords.isPix()) {
            // no WCS, just use image center and size
            final Point2D.Double p = _coordinateConverter.getImageCenter();
            final ImageCoords pos = new ImageCoords(p.x, p.y);
            final double w = _coordinateConverter.getWidth();
            final double h = _coordinateConverter.getHeight();
            final double r = Math.sqrt(w * w + h * h);
            return new CoordinateRadius(pos, r, w, h);
        }

        if (!rowCoords.isWCS())
            return null;

        // we have world coordinates: find the bounding box of the objects in the table
        double ra0 = 0., ra1 = 0., dec0 = 0., dec1 = 0.;
        boolean firstTime = true;
        for (int row = 1; row < nrows; row++) {
            final Vector<Object> rowVec = dataVec.get(row);
            final Coordinates pos = rowCoords.getCoordinates(rowVec);
            if (pos == null)
                continue;
            if (firstTime) {
                firstTime = false;
                ra0 = pos.getX();
                ra1 = ra0;
                dec0 = pos.getY();
                dec1 = dec0;
            } else {
                double ra = pos.getX(), dec = pos.getY();
                ra0 = Math.min(ra0, ra);
                ra1 = Math.max(ra1, ra);
                dec0 = Math.min(dec0, dec);
                dec1 = Math.max(dec1, dec);
            }
        }

        // get the center point and radius
        final WorldCoords centerPos = new WorldCoords((ra0 + ra1) / 2., (dec0 + dec1) / 2., tableEquinox);
        final double d = WorldCoords.dist(ra0, dec0, ra1, dec1);
        return new CoordinateRadius(centerPos, d / 2.);
    }


    // -- Implement the LayerListener interface --

    /** Invoked when the mouse moves while the button is still held down. */
    @Override
    public void mouseDragged(LayerEvent e) { }

    /** Invoked when the mouse is pressed on a layer or figure. */
    @Override
    public void mousePressed(LayerEvent e) { }

    /** Invoked when the mouse is released on a layer or figure. */
    @Override
    public void mouseReleased(LayerEvent e) { }

    /**
     * Invoked when the mouse is clicked on a layer or figure.
     * <p>
     * Note that if a catalog symbol is selected, the event is modified,
     * so that any other listeners will get the modified location, which is
     * set to the center of the selected catalog symbol. This implements
     * a kind of "snap to catalog symbol" feature for any layer listeners that
     * are added after this instance.
     */
    @Override
    public void mouseClicked(final LayerEvent e) {
        if (!_visible)
            return;

        final double x = e.getLayerX(), y = e.getLayerY();
        final boolean toggleSel = (e.isShiftDown() || e.isControlDown());

        // Find the plot symbol under the mouse pointer
        for (TableListItem tli: _tableList) {
            if (!tli.inRange)
                continue;
            for (SymbolListItem sli: tli.symbolAr) {
                for (FigureListItem fli: sli.figureList) {
                    if (sli.symbol.getBoundingShape(fli.shape).contains(x, y)) {
                        if (toggleSel) {
                            fli.selected = !fli.selected;
                            repaint(fli.shape);
                            fireSymbolSelectionEvent(tli.table, fli.row, fli.selected);
                        } else {
                            if (!fli.selected) {
                                fli.selected = true;
                                repaint(fli.shape);
                                fireSymbolSelectionEvent(tli.table, fli.row, fli.selected);
                            }
                        }
                    } else if (!toggleSel) {
                        if (fli.selected) {
                            fli.selected = false;
                            repaint(fli.shape);
                            fireSymbolSelectionEvent(tli.table, fli.row, fli.selected);
                        }
                    }
                }
            }
        }
    }

    /**
     * If the given screen coordinates point is within a displayed catalog symbol, set it to
     * point to the center of the symbol and return the name and coordinates (and brightness,
     * if known) from the catalog table row. Otherwise, return null and do nothing.
     */
    @Override
    public Option<SiderealTarget> getCatalogObjectAt(final Point2D.Double p) {
        // Find the plot symbol under the mouse pointer
        for (TableListItem tli: _tableList) {
            if (!tli.inRange)
                continue;
            for (SymbolListItem sli: tli.symbolAr) {
                for (FigureListItem fli: sli.figureList) {
                    // assume symbol has already been selected
                    if (fli.selected && sli.symbol.getBoundingShape(fli.shape).contains(p)) {
                        Option<SiderealTarget> skyObject = tli.table.getSiderealTarget(fli.row);
                        skyObject.forEach(s -> {
                            // This is a bit strange, we convert the incoming parameter to the position of the
                            // object and it also side-effects setting the equinox
                            _imageEquinox = _coordinateConverter.getEquinox();
                            p.x = s.coordinates().ra().toDegrees();
                            p.y = s.coordinates().dec().toDegrees();
                            _coordinateConverter.convertCoords(p, CoordinateConverter.WORLD, CoordinateConverter.SCREEN, false);

                        });
                        return skyObject;
                    }
                }
            }
        }
        return None.instance();
    }


    /**
     * Local class used for tableList elements (one for each table).
     */
    private class TableListItem {

        public final TableQueryResult table;      // a reference to the table to plot
        public final SymbolListItem[] symbolAr;   // array mapping symbol desc to figure list
        public boolean inRange = true;      // set to true if table coords are in image range

        public TableListItem(final TableQueryResult t, final SymbolListItem[] ar) {
            table = t;
            symbolAr = ar;
        }
    }

    /**
     * Local class used for TableListItem.symbolList elements (one for each plot
     * symbol entry, for each table)
     */
    private class SymbolListItem {

        public final TablePlotSymbol symbol;                  // plot symbol description
        public final LinkedList<FigureListItem> figureList = new LinkedList<>();   // list of figures to draw using the above symbol

        public SymbolListItem(TablePlotSymbol s) {
            symbol = s;
        }
    }

    /**
     * Local class used for SymbolListItem.figureList elements (one for each plot symbol).
     */
    private class FigureListItem {

        public Shape shape;     // shape of the symbol
        public final String label;    // optional label
        public final int row;         // row index in table
        public boolean selected = false;  // true if selected

        public FigureListItem(final Shape s, final String lab, final int r) {
            shape = s;
            label = lab;
            row = r;
        }
    }
}
