package jsky.app.ot.tpe;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import edu.gemini.spModel.core.Angle$;
import edu.gemini.spModel.inst.FeatureGeometry;
import jsky.coords.WorldCoords;
import edu.gemini.spModel.util.Angle;

public final class TpeImageInfo {

    // Screen coordinates of the base position.
    private Point2D.Double _baseScreenPos;

    // World coordinates of the base position.
    private WorldCoords _basePos = new WorldCoords();

    // Scale of the image in pixels per arcsec.
    private double _pixelsPerArcsec = 1.0;

    // Due north in the sky relative to up in the image.
    private double _theta = 0.0;

    // The current position angle (in degrees).
    private double _posAngleDegrees = 0.0;

    // Set to -1.0 if the RA axis is flipped about the base position, otherwise 1.0.
    // The position angle should be multiplied by this value before using.
    private double _flipRA = 1.0;


    /** Default constructor: initialize all fields to null */
    public TpeImageInfo() {
    }

    /**
     * Returns an AffineTransform that will map to screen coordinates.
     */
    public AffineTransform toScreen() {
        return FeatureGeometry.screenTransform(
                getBaseScreenPos(),
                getPixelsPerArcsec(),
                Angle$.MODULE$.fromRadians(getTheta()),
                getFlipRA());
    }

    /**
     * Return the screen coordinates of the base position.
     */
    public Point2D.Double getBaseScreenPos() {
        return _baseScreenPos;
    }

    /**
     * Set the screen coordinates of the base position.
     */
    void setBaseScreenPos(Point2D.Double p) {
        _baseScreenPos = p;
    }


    /**
     * Return the world coordinates of the base position.
     */
    public WorldCoords getBasePos() {
        return _basePos;
    }

    /**
     * Set the world coordinates of the base position.
     */
    void setBasePos(WorldCoords pos) {
        _basePos = pos;
    }


    /**
     * Return the scale of the image in pixels per arcsec.
     */
    public double getPixelsPerArcsec() {
        return _pixelsPerArcsec;
    }

    /**
     * Set the scale of the image in pixels per arcsec.
     */
    void setPixelsPerArcsec(double d) {
        _pixelsPerArcsec = d;
    }


    /**
     * Return due north in the sky relative to up in the image.
     */
    public double getTheta() {
        return _theta;
    }

    /**
     * Set due north in the sky relative to up in the image.
     */
    void setTheta(double d) {
        _theta = d;
    }


    /**
     * Return -1 if the image RA axis is flipped about the base position, otherwise 1.
     */
    public double flipRA() {
        return _flipRA;
    }

    /**
     * Return true if the image RA axis is flipped about the base position. In this
     * case the position angle increases in the clockwise direction (because WCS East is in
     * that direction). Otherwise the position angle increases counterclockwise.
     */
    public boolean getFlipRA() {
        return (_flipRA == -1);
    }

    /**
     * Set to true if the RA axis is flipped about the base position. In this case the
     * position angle increases in the clockwise direction (because WCS East is in
     * that direction). Otherwise the position angle will increase counterclockwise.
     */
    void setFlipRA(boolean flipRA) {
        _flipRA = (flipRA ? -1. : 1.);
    }


    /**
     * Return the current position angle (in degrees).
     */
    public double getPosAngleDegrees() {
        return _posAngleDegrees;
    }

    /**
     * Set the current position angle (in degrees).
     */
    void setPosAngleDegrees(double d) {
        _posAngleDegrees = d;
    }


    /**
     * Return the current position angle (in radians).
     */
    private double getPosAngleRadians() {
        return Angle.degreesToRadians(_posAngleDegrees);
    }


    /**
     * Return the corrected position angle in radians.
     * This is actually sign * posAngle + theta.
     */
    public double getCorrectedPosAngleRadians() {
        return _flipRA * getPosAngleRadians() + _theta;
    }

    /**
     * Calculates the position angle (east of north) formed by the line that
     * passes through the mouse location and the base position.
     */
    public edu.gemini.spModel.core.Angle positionAngle(TpeMouseEvent evt) {
        // All the points are in screen coordinates, which means y increases down
        // This makes x and y relative to the origin in a right side up frame.
        final double xp = evt.xWidget - _baseScreenPos.x;
        final double yp = _baseScreenPos.y - evt.yWidget;

        final double xa = Math.abs(xp);
        final double ya = Math.abs(yp);

        final double angle;
        if (xa == 0) {
            angle = (yp >= 0) ? 0.0 : Math.PI;
        } else {
            final double a = edu.gemini.spModel.util.Angle.atanRadians(xa/ya);
            if ((xp > 0) && (yp >= 0)) {
                angle = Math.PI * 2.0 - a;
            } else if ((xp < 0) && (yp >= 0)) {
                angle = a;
            } else if ((xp < 0) && (yp < 0)) {
                angle = Math.PI - a;
            } else {
                angle = Math.PI + a;
            }
        }
        return Angle$.MODULE$.fromRadians(angle * flipRA() - getTheta());
    }

    /**
     * Standard debugging method.
     */
    public String toString() {
        return getClass().getName() +
                "[baseScreenPos=" + _baseScreenPos +
                ", basePos=" + _basePos +
                ", pixelsPerArcsec=" + _pixelsPerArcsec +
                ", theta=" + _theta +
                ", posAngleDegrees=" + _posAngleDegrees + "]";
    }
}

