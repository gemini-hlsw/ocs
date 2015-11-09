package jsky.app.ot.gemini.editor.horizons;

import edu.gemini.horizons.api.EphemerisEntry;
import jsky.app.ot.tpe.TpeImageFeature;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeImageFeatureCategory;
import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoords;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class TpeHorizonsFeature extends TpeImageFeature {

    private static final Logger LOG = Logger.getLogger(TpeHorizonsFeature.class.getName());

    private List<EphemerisEntry> _ephemerisData;
    private static final DateFormat dateFormatter = new SimpleDateFormat("dd-MMM HH:mm");
    static {
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    private static final double SIZE = 5;
    /**
     * Create with a short name and a longer description.
     */
    public TpeHorizonsFeature(String name, String descr) {
        super(name, descr);
    }

    public void draw(Graphics g, TpeImageInfo tii) {
        _drawEphemeris(g);
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.target;
    }

    private List<EphemerisEntry> getEphemeris() {
        if (_ephemerisData == null) {
            return Collections.emptyList();
        }
        return _ephemerisData;
    }

    private CoordinateConverter getCoordinateConverter() {
        return _iw.plotter().getCoordinateConverter();
    }

    public void setEphemeris(List<EphemerisEntry> data) {
        _ephemerisData = data;
    }


    private void _drawEphemeris(Graphics g) {

        Color origColor = g.getColor();
        Font origFont = g.getFont();

        g.setColor(Color.RED);
        g.setFont(FONT);
        double angle = getAngle(getEphemeris());

        for (EphemerisEntry entry : getEphemeris()) {
            WorldCoords pos = entry.getCoordinates();

            CoordinateConverter converter = getCoordinateConverter();

            double equinox = converter.getEquinox();
            double[] radec = pos.getRaDec(equinox);

            double x = radec[0];
            double y = radec[1];

            Point2D.Double point = new Point2D.Double(x, y);
            try {
                converter.convertCoords(point, CoordinateConverter.WORLD, CoordinateConverter.USER, false);
                // clip to image bounds
                double w = converter.getWidth();
                double h = converter.getHeight();
                if (point.x < 0. || point.y < 0. || point.x >= w || point.y >= h)
                    continue;
                converter.convertCoords(point, CoordinateConverter.USER, CoordinateConverter.SCREEN, false);

                Shape shape = createCenteredSymbol(point.x, point.y, SIZE);

                Graphics2D g2d = (Graphics2D) g;
                g2d.draw(shape);

                TextLayout layout = new TextLayout(
                        dateFormatter.format(entry.getDate()),
                        g2d.getFont(),
                        g2d.getFontRenderContext());
                double offsetX = SIZE * Math.cos(angle);
                double offsetY = SIZE * Math.sin(angle);

                g2d.translate(point.x + offsetX,  point.y + offsetY);
                g2d.rotate(angle);
                layout.draw(g2d, 0, 0);
                g2d.rotate(- angle);
                g2d.translate(-point.x - offsetX, -point.y - offsetY);

                //g.drawString(dateFormatter.format(entry.getDate()), (int) (point.x + SIZE), (int) (point.y + SIZE));
            } catch (RuntimeException ex) {
                //this can happen if the conversion fails with an out of range coordinate
                //just continue with the next one
                LOG.log(Level.INFO, "OUT OF RANGE: " + ex.getMessage());
            }
        }
        g.setColor(origColor);
        g.setFont(origFont);
    }


    private static Shape createCenteredSymbol(double x, double y, double size) {
        //return new Ellipse2D.Double(x - side, y - side, side * 2, side * 2);
        size = size / 2;
        Point2D.Double sw = new Point2D.Double(x - size, y + size);
        Point2D.Double se = new Point2D.Double(x + size, y + size);
        Point2D.Double nw = new Point2D.Double(x - size, y - size);
        Point2D.Double ne = new Point2D.Double(x + size, y - size);

        GeneralPath path = new GeneralPath();
        path.append(new Line2D.Double(se, nw), false);
        path.append(new Line2D.Double(sw, ne), false);
        return path;
    }


    private double getAngle(List<EphemerisEntry> entry) {

        if (entry == null || entry.size() <= 1) return 0;


        EphemerisEntry[] borders = {entry.get(0), entry.get(1)};

        CoordinateConverter converter = getCoordinateConverter();

        double equinox = converter.getEquinox();
        double[] radec1 = borders[0].getCoordinates().getRaDec(equinox);
        double[] radec2 = borders[1].getCoordinates().getRaDec(equinox);

        Point2D.Double point1 = new Point2D.Double(radec1[0], radec1[1]);
        Point2D.Double point2 = new Point2D.Double(radec2[0], radec2[1]);

        try {
            converter.convertCoords(point1, CoordinateConverter.WORLD, CoordinateConverter.SCREEN, false);

            converter.convertCoords(point2, CoordinateConverter.WORLD, CoordinateConverter.SCREEN, false);

        } catch (Exception e) {
            return 0;
        }

        //if the slope is close to infinitum, then the angle to use is zero.
        if (Math.abs(point1.getX() - point2.getX()) < 0.1 ) return 0;


        double slope = (point2.getY() - point1.getY())/(point2.getX() - point1.getX());

        double angle = Math.atan(slope) - Math.PI/2;


        // If the slope is negative, rotate the angle in 180 degrees, so
        // text won't be flipped
        if (slope < 0) {
            angle = angle - Math.PI;
        }
        return angle;
    }


}
