package jsky.catalog.gui;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import jsky.catalog.TablePlotSymbol;
import jsky.catalog.TableQueryResult;
import jsky.coords.CoordinateConverter;
import jsky.graphics.CanvasGraphics;

/**
 * This defines the interface for plotting the contents of a catalog table.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public interface TablePlotter {

    /** Plot the given table data */
    void plot(TableQueryResult table);

    /** Erase the plot of the given table data */
    void unplot(TableQueryResult table);

    /** Erase all plot symbols */
    void unplotAll();

    /** Recalculate the coordinates and replot all symbols after a change in the coordinate system. */
    void replotAll();

    /** Return an array containing the tables managed by this object. */
    TableQueryResult[] getTables();

    /** Select the symbol corresponding to the given table row */
    void selectSymbol(TableQueryResult table, int tableRow);

    /** Deselect the symbol corresponding to the given table row */
    void deselectSymbol(TableQueryResult table, int tableRow);


    /** Set the plot symbol info for the given table */
    void setPlotSymbolInfo(TableQueryResult table, TablePlotSymbol[] symbols);

    /**
     * Return the plot symbol info for the given table.
     *
     * @param table object representing the catalog table
     * @return an array of PlotSymbol objects, one for each plot symbol defined.
     */
    TablePlotSymbol[] getPlotSymbolInfo(TableQueryResult table);

    /**
     * If the given argument is false, hide all plot symbols managed by this object,
     * otherwise show them again.
     */
    void setVisible(boolean isVisible);

    /**
     * If the given screen coordinates point is within a displayed catalog symbol, set it to
     * point to the center of the symbol and return the name and coordinates
     * from the catalog table row. Otherwise, return null and do nothing.
     */
    NamedCoordinates getCatalogPosition(Point2D.Double p);

    /** Return the object used to convert to screen coordinates for drawing */
    CoordinateConverter getCoordinateConverter();

    /** Add a listener for selection events on symbols */
    void addSymbolSelectionListener(SymbolSelectionListener listener);

    /** Remove a listener for selection events on symbols */
    void removeSymbolSelectionListener(SymbolSelectionListener listener);

    /** Add a listener for selection events on tables */
    void addTableSelectionListener(TableSelectionListener listener);

    /** Remove a listener for selection events on tables */
    void removeTableSelectionListener(TableSelectionListener listener);

    /**
     * Return a panel to use to configure the plot symbols for the given table.
     *
     * @param table the result of a query
     */
    JPanel getConfigPanel(TableQueryResult table);

    /**
     * Paint the catalog symbols using the given graphics object.
     *
     * @param g the graphics context
     * @param region if not null, the region to paint
     */
    void paintSymbols(Graphics2D g, Rectangle2D region);

    /**
     * Transform the plot symbols using the given AffineTransform
     * (called when the image is transformed, to keep the plot symbols up to date).
     */
    void transformGraphics(AffineTransform trans);
}
