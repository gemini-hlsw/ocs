/**
 * $Id: Flamingos2_OIWFS_Feature.java 45719 2012-06-01 16:35:09Z swalker $
 */

package jsky.app.ot.gemini.flamingos2;

import diva.util.java2d.Polygon2D;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.guide.PatrolField;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.telescope.IssPort;
import jsky.app.ot.gemini.inst.OIWFS_FeatureBase;
import jsky.app.ot.tpe.TpeImageInfo;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.Set;


/**
 * Draws the OIWFS overlay for Flamingos2.
 */
public class Flamingos2_OIWFS_Feature  extends OIWFS_FeatureBase  {

    // OT-540:
    // OIWFS FOV: a "lozenge" defined by two arcs with different radii and different centers.
    // The first center is the base position, and radius from this center is 139.7mm.
    // The second center is located in the +(?)p direction, at a distance of 170.25mm from the
    // base position, and has radius 198.125mm.
    // see file: OIWFS_Patrol_Area.jpg and Jeff's Sweep Path Model(1).jpg

    // The color to use to draw the OIWFS probe arm
    private static final Color PROBE_ARM_COLOR = Color.red;

    // Used to draw dashed lines
//    private static final Stroke DASHED_LINE_STROKE
//            = new BasicStroke(2.0F,
//                              BasicStroke.CAP_BUTT,
//                              BasicStroke.JOIN_BEVEL,
//                              0.0F,
//                              new float[]{12.0F, 12.0F},
//                              0.0F);

    // The size of the OIWFS probe arm pickoff mirror in mm
    private static final double PICKOFF_MIRROR_SIZE = 19.8;

    private static final double PROBE_PICKOFF_ARM_TOTAL_LENGTH = 203.40;

    private static final double PROBE_BASE_ARM_LENGTH = 109.63;
    private static final double PROBE_PICKOFF_ARM_LENGTH = PROBE_PICKOFF_ARM_TOTAL_LENGTH - PICKOFF_MIRROR_SIZE/2;
    private static final double PROBE_ARM_OFFSET = 256.87;


    // The width of the tapered end of the probe arm in arcsec
    private static final double PROBE_ARM_TAPERED_WIDTH = 15.;

    // The length of the tapered end of the probe arm in arcsec
    private static final double PROBE_ARM_TAPERED_LENGTH = 180.;


    // Composite used for drawing items that block the view
    private static final Composite BLOCKED = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5F);


    private Area _patrolField;

    // Saved Flamingos2 port setting (side-looking or up-looking)
    private IssPort _port;

    // Saved Flamingos2 LyotWheel setting
    private Flamingos2.LyotWheel _lyotWheel;

    // from config file
    private boolean _flip;
    private double _fovRotation;//in radians
    /**
      * Construct the feature with its name and description.
      */
     public Flamingos2_OIWFS_Feature() {
         super("Flamingos2 OIWFS", "Show the Flamingos2 OIWFS patrol field.");
     }


    /**
     * Add the OIWFS patrol field to the list of figures to display.
     *
     * @param xc the X screen coordinate for the base position to use
     * @param yc the Y screen coordinate for the base position to use
     * @param plateScale plate scale in arcsec/mm
     */
    protected void _addPatrolField(double xc, double yc, double plateScale) {
        for (ObsContext ctx : _iw.getMinimalObsContext()) {
            // get scaled and offset f2 oiwfs patrol field
            PatrolField patrolField = Flamingos2OiwfsGuideProbe.instance.getCorrectedPatrolField(ctx);
            // rotation, scaling and transformation to match screen coordinates
            Angle rotation = new Angle(-_posAngle, Angle.Unit.RADIANS);
            Point2D.Double translation = new Point2D.Double(xc, yc);
            setTransformationToScreen(rotation, _pixelsPerArcsec, translation);

            // set patrol field for in Range check (this should probably be done using the inRange check provided by the guide probe)
            _patrolField = patrolField.getArea();
            _patrolField = transformToScreen(_patrolField);

            // draw patrol field
            addPatrolField(patrolField);
        }
    }


    /**
     * Add the OIWFS probe arm (without the pickoff mirror) to the display list.
     *
     * The probe arm is made of two fixed length components rotating about two axles.
     *
     *
     * @param xc the X screen coordinate for the base position
     * @param yc the Y screen coordinate for the base position
     * @param xg the X screen coordinate position for the guide star
     * @param yg the Y screen coordinate position for the guide star
     * @param xt translate resulting figure by this amount of pixels in X
     * @param yt translate resulting figure by this amount of pixels in Y
     * @param plateScale plate scale in arcsec/mm
     */
    private void _addProbeArm(double xc, double yc, double xg, double yg, double xt, double yt, double plateScale) {
        int sign = _flip ?  -1 : 1;

        Point2D.Double tp = new Point2D.Double(xg, yg);
        Point2D.Double tpOff = new Point2D.Double(xg + xt, yg + yt);
        //Point2D.Double tpOff = new Point2D.Double(xc + xt, yc + yt);

        if (_patrolField.contains(tpOff)) {
            double scale = _pixelsPerArcsec * plateScale;
            double length_base = PROBE_BASE_ARM_LENGTH * scale;
            double length_pickoff = PROBE_PICKOFF_ARM_LENGTH * scale;
            double base_arm_axis = PROBE_ARM_OFFSET * scale;

            //We will compute the position of the rotation axis of probe arm. It's defined
            //by the intersection point of the 2 circles defined by the 2 fixed length components
            //The intersection of two circles can be calculated knowing the position of the
            //two centers and the 2 radius.  We used the definitions from
            //http://local.wasp.uwa.edu.au/~pbourke/geometry/2circle/

            // position of the base arm, translated based on the position angle
            double pa = -_posAngle - _fovRotation;
            double x0 = xc + base_arm_axis * _flipRA * sign * Math.cos(pa);
            double y0 = yc + base_arm_axis * _flipRA * sign * Math.sin(pa);

            //The position of the OIWFS. Rotation of the PA doesn't affect this.
            double x1 = xg + xt; // xt already takes _flipRA into account
            double y1 = yg + yt;

            //Distance between OIWFS and the position of the base arm (the center of the 2 circles)
            double distance = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));

            if (distance > length_base + length_pickoff) {
                distance = length_base + length_pickoff;
            }

            //a is the adjacent side of the rectangle triangle formed by the OIWFS position, the interesection of
            //the 2 circles and the imaginary line that connects the OIWFS and the position of the base arm
            double a = (length_base * length_base - length_pickoff * length_pickoff + distance * distance) / (2 * distance);
            //h is the distance from a to the intersection point
            //Note: OT-38: fix was to multiply by sign below to handle both orientations correctly.
            double h = sign * Math.sqrt(length_base * length_base - a * a);
            //(x2,y2) are the coordinates of the point located "a" from the OIWFS
            double x2 = x0 + a * (x1 - x0) / distance;
            double y2 = y0 + a * (y1 - y0) / distance;

            //decide what solution to choose. Since we always want a /\ form, we should pick the one
            //whose y is smaller than the other one
            //We take the dot product to decide
            double y3 = y2 - h * (x1 - x0) / distance;
            double x3 = x2 + h * (y1 - y0) / distance;
            if (_flipRA < 0) { // REL-299: Orientation bug: seems this is only needed in this case
                if ((x3 * xc * _flipRA + y3 * yc) < 0 || (x3 * xc + y3 * yc * _flipRA) < 0) {
                    y3 = y2 + h * (x1 - x0) / distance;
                    x3 = x2 - h * (y1 - y0) / distance;
                }
            }

//            // XXX debug
//            _figureList.add(new OIWFSFigure(new Ellipse2D.Double(x0-5, y0-5, 10, 10), Color.pink, BLOCKED, OIWFS_STROKE));
//            _figureList.add(new OIWFSFigure(new Ellipse2D.Double(x1-5, y1-5, 10, 10), Color.white, BLOCKED, OIWFS_STROKE));
//            _figureList.add(new OIWFSFigure(new Ellipse2D.Double(x2-5, y2-5, 10, 10), Color.blue, BLOCKED, OIWFS_STROKE));
//            _figureList.add(new OIWFSFigure(new Ellipse2D.Double(x3-5, y3-5, 10, 10), Color.green, BLOCKED, OIWFS_STROKE));

            //Draw the fixed arm. Disabled for now.
//            Polygon2D.Double arm = new Polygon2D.Double(x0, y0);
//            arm.lineTo(x3, y3);
//            _figureList.add(new OIWFSFigure(arm, PROBE_ARM_COLOR, BLOCKED, OIWFS_STROKE));


            //Get the rotation angle of the probe.
            double angle = Math.atan2(y3 - y1, x3 - x1);

            //and build the transformation to apply to the arm and pickoff mirror
            AffineTransform armTrans = new AffineTransform();
            armTrans.translate(xt, yt);
            armTrans.rotate(angle, x1, y1);

            _addPickoffMirror(tp, armTrans, plateScale);
            _addProbeArm(tp, armTrans, plateScale);
        }
    }

    /**
     * Add the OIWFS probe arm (without the pickoff mirror) to the display list.
     *
     * @param tp    the location of the OIWFS guide star in screen coords
     * @param trans the transformation to apply to the figure
     * @param plateScale plate scale in arcsec/mm
     */
    private void _addProbeArm(Point2D.Double tp, AffineTransform trans, double plateScale) {
        double mirror = PICKOFF_MIRROR_SIZE * plateScale * _pixelsPerArcsec;
        double length = PROBE_PICKOFF_ARM_LENGTH * plateScale * _pixelsPerArcsec;
        double taperedWidth = PROBE_ARM_TAPERED_WIDTH * _pixelsPerArcsec;
        double taperedLength = PROBE_ARM_TAPERED_LENGTH * _pixelsPerArcsec;
        double hm = mirror / 2.;
        double htw = taperedWidth / 2.;

        double x0 = tp.x + hm;
        double y0 = tp.y - htw;
        Polygon2D.Double arm = new Polygon2D.Double(x0, y0);

        double x1 = x0 + taperedLength;
        double y1 = tp.y - hm;
        arm.lineTo(x1, y1);

        double x2 = x0 + length;
        double y2 = y1;
        arm.lineTo(x2, y2);

        double x3 = x2;
        double y3 = tp.y + hm;
        arm.lineTo(x3, y3);

        double x4 = x1;
        double y4 = y3;
        arm.lineTo(x4, y4);

        double x5 = x0;
        double y5 = tp.y + htw;
        arm.lineTo(x5, y5);

        arm.transform(trans);
        _figureList.add(new Figure(arm, PROBE_ARM_COLOR, BLOCKED, OIWFS_STROKE));
    }




    /**
     * Add the OIWFS probe arm pickoff mirror to the display list.
     *
     * @param tp the location of the OIWFS guide star in screen coords
     * @param trans the transformation to apply to the figure
     * @param plateScale plate scale in arcsec/mm
     */
    private void _addPickoffMirror(Point2D.Double tp, AffineTransform trans, double plateScale) {
        double width = PICKOFF_MIRROR_SIZE * _pixelsPerArcsec * plateScale;
        double d = width / 2.;
        double x = tp.x - d;
        double y = tp.y - d;
        Polygon2D.Double pickoffMirror = new Polygon2D.Double(x, y);
        pickoffMirror.lineTo(x + width, y);
        pickoffMirror.lineTo(x + width, y + width);
        pickoffMirror.lineTo(x, y + width);

        // rotate by position angle
        pickoffMirror.transform(trans);

        _figureList.add(new Figure(pickoffMirror, PROBE_ARM_COLOR, BLOCKED, OIWFS_STROKE));
    }


    /**
     * Update the list of figures to draw.
     *
     * @param guidePosX the X screen coordinate position for the OIWFS guide star
     * @param guidePosY the Y screen coordinate position for the OIWFS guide star
     * @param offsetPosX the X screen coordinate for the selected offset
     * @param offsetPosY the X screen coordinate for the selected offset
     * @param translateX translate resulting figure by this amount of pixels in X
     * @param translateY translate resulting figure by this amount of pixels in Y
     * @param basePosX the X screen coordinate for the base position (IGNORED)
     * @param basePosY the Y screen coordinate for the base position (IGNORED)
     * @param oiwfsDefined set to true if an OIWFS position is defined (otherwise
     *                     the xg and yg parameters are ignored)
     */
    protected void _updateFigureList(double guidePosX, double guidePosY, double offsetPosX, double offsetPosY,
                                     double translateX, double translateY, double basePosX, double basePosY, boolean oiwfsDefined) {
        // need to flip the drawing about the X axis if the instrument is side-mounted
        Flamingos2 inst = (Flamingos2) _iw.getInstObsComp();
        _port = inst.getIssPort();
        _lyotWheel = inst.getLyotWheel();

        ObsContext ctx =  _iw.getObsContext().getOrNull();
        if(ctx!=null){
            AbstractDataObject aoComp = ctx.getAOComponent().getOrNull();
            if(aoComp!=null){
                _flip = inst.getFlipConfig(aoComp.getNarrowType().equals(Gems.SP_TYPE.narrowType));
                _fovRotation = inst.getRotationConfig(aoComp.getNarrowType().equals(Gems.SP_TYPE.narrowType)).toRadians().getMagnitude();
            }else{
                _flip = inst.getFlipConfig(false);
                _fovRotation = inst.getRotationConfig(false).toRadians().getMagnitude();
            }
        }else{
            _flip = inst.getFlipConfig(false);
            _fovRotation = inst.getRotationConfig(false).toRadians().getMagnitude();
        }

        double plateScale = inst.getLyotWheel().getPlateScale();

        _figureList.clear();

        _addPatrolField(offsetPosX + translateX, offsetPosY + translateY, plateScale);

        addOffsetConstrainedPatrolField(basePosX, basePosY);

        // rotate by position angle
        AffineTransform trans = new AffineTransform();
        trans.rotate(-_posAngle, offsetPosX, offsetPosY);

        // If the OIWFS is defined for this observation, draw the probe arm.
        if (oiwfsDefined)
            _addProbeArm(offsetPosX, offsetPosY, guidePosX, guidePosY, translateX, translateY, plateScale);
    }

    private void addOffsetConstrainedPatrolField(double basePosX, double basePosY){
        for (ObsContext ctx : _iw.getMinimalObsContext()) {
            // get flipped and offset and scaled f2 oiwfs patrol field
            PatrolField patrolField = Flamingos2OiwfsGuideProbe.instance.getCorrectedPatrolField(ctx);
            // rotation, scaling and transformation to match screen coordinates
            Angle rotation = new Angle(-_posAngle, Angle.Unit.RADIANS);
            Point2D.Double translation = new Point2D.Double(basePosX, basePosY);
            setTransformationToScreen(rotation, _pixelsPerArcsec, translation);

            Set<Offset> offsets = getContext().offsets().scienceOffsetsJava();
            addOffsetConstrainedPatrolField(patrolField, offsets);
        }
    }


    /** Return true if the display needs to be updated because values changed. */
    protected boolean _needsUpdate(SPInstObsComp inst, TpeImageInfo tii) {
        // This method in its original form DOES NOT WORK and always returns FALSE.
        // This is likely because we are just comparing references below, which do not
        // change. The result of this is that the OIWFS probe arm is never drawn unless
        // explicitly forced by toggling the OIWFS checkbox in the Field of View section.
        return true;

        /*
        System.err.println("*** In _needsUpdate");
        if (super._needsUpdate(inst, tii)) {
            System.err.println("\t TRUE");
            return true;
        }

        Flamingos2 instFlamingos2 = (Flamingos2) inst;
        if (_port != instFlamingos2.getIssPort()) {
            System.err.println("\t TRUE");
            return true;
        }
        if (_lyotWheel != instFlamingos2.getLyotWheel()) {
            System.err.println("\t TRUE");
            return true;
        }
        System.err.println("\t FALSE");
        return false;
        */
    }
}
