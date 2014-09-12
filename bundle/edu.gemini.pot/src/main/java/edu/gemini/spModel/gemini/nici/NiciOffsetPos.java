//
// $
//

package edu.gemini.spModel.gemini.nici;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.telescope.IssPort;

import java.awt.geom.Point2D;

//
// The OffsetPos class and associated OffsetPosList, etc. are fairly grim and
// need to be replaced.  Doing so is a major effort and must wait for a
// reworking of the model.  To satisfy the requirements of SCT-232, I'm going
// to continue working within the structure of the existing model so that the
// position editor, etc. will continue to work.
//

/**
 * NICI offset positions are unique, in that when the Focal Plane Mask Wheel
 * is tracking the offset, the offset range is limited.  See SCT-232.
 */
public final class NiciOffsetPos extends OffsetPosBase {
    public static final double minD = -10.0;
    public static final double maxD =  10.0;

    public static final Factory<NiciOffsetPos> FACTORY = new Factory<NiciOffsetPos>() {
        public NiciOffsetPos create(String tag) {
            return new NiciOffsetPos(tag);
        }
        public NiciOffsetPos create(String tag, double p, double q) {
            return new NiciOffsetPos(tag, p, q);
        }
    };

    // distance along the arc, in arcsecs.
    private double d;

    // Whether the focal plane mask wheel is tracking.
    private boolean fpmwTracking;

    /**
     * Creates a NiciOffsetPos with FPMW tracking turned off, at 0 degrees
     * along the arc.
     */
    public NiciOffsetPos(String tag) {
        this(tag, 0.0, IssPort.DEFAULT);
    }

    /**
     * Creates a NiciOffsetPos with FPMW tracking turned off, at the
     * specified p/q coordinates (in arcsec).
     */
    public NiciOffsetPos(String tag, double p, double q) {
        super(tag, p, q);
        this.d = 0;
        fpmwTracking = false;
    }

    /**
     * Creates a NiciOffsetPos with FPMW tracking turned on, at the
     * specified distance along the arc (in arcsec).
     */
    public NiciOffsetPos(String tag, double darcsecs, IssPort port) {
        super(tag);
        fpmwTracking = true;
        setOffsetDistance(darcsecs, port);
    }

    /**
     * Gets the offset distance along the arc in arcsecs.  This value is
     * only valid if {@link #isFpmwTracking()}.
     */
    public synchronized double getOffsetDistance() {
        return d;
    }

    /**
     * Returns <code>true</code> if the Focal Plane Mask Wheel should track
     * this offset position.
     */
    public synchronized boolean isFpmwTracking() {
        return fpmwTracking;
    }

    public synchronized void setFpmwTacking(boolean tracking, IssPort port) {
        if (tracking == fpmwTracking) return;

        fpmwTracking = tracking;
        if (tracking) {
            setXY(getXaxis(), getYaxis(), port);
        } else {
            d = 0;
        }
        super._notifyOfGenericUpdate();
    }

    private double getSign(IssPort port) {
        return (port == IssPort.UP_LOOKING) ? -1.0 : 1.0;
    }

    /**
     * Sets the distance along the arc (in arcseconds), turns on focal plane
     * mask wheel tracking, and updates the p,q values to match the offset
     * distance.
     *
     * @param dArcsec distance along the arc (in arcsecs)
     */
    public synchronized final void setOffsetDistance(double dArcsec, IssPort port) {
        if (!fpmwTracking) return;

        if (dArcsec > maxD) {
            dArcsec = maxD;
        } else if (dArcsec < minD) {
            dArcsec = minD;
        }
        d = dArcsec;

        // calculate the p,q from the distance along the arc
        NiciOffsetCoefficients co = NiciOffsetCoefficients.lookup(null);
        double p = getSign(port) * co.computeP(d);
        double q = co.computeQ(d);

        super.noNotifySetXY(p, q, port);
        super._notifyOfLocationUpdate();
    }

    /**
     * An implementation class that groups d, p, and q in a single object.
     */
    private static class DPQ {
        private final double d;
        private final double p;
        private final double q;

        DPQ(double d, double p, double q) {
            this.d = d;
            this.p = p;
            this.q = q;
        }

        public double getD() {
            return d;
        }
        public double getP() {
            return p;
        }
        public double getQ() {
            return q;
        }
    }

    /**
     * Overrides the one method in the super class where the p,q (xaxis, yaxis)
     * values are modified in order to turn off focal plane mask tracking.
     * With FPMW tracking turned on, random p,q values are not supported.
     */
    public synchronized void noNotifySetXY(double xaxis, double yaxis, IssPort port) {
        if (!fpmwTracking) {
            super.noNotifySetXY(xaxis, yaxis, port);
            return;
        }

        double sign = getSign(port);
        DPQ res = computeDQP(sign * xaxis, yaxis);
        d = res.getD();
        super.noNotifySetXY(sign * res.getP(), res.getQ(), port);
    }

    private synchronized DPQ computeDQP(double p, double q) {
        // If tracking with FPMW, then compute the valid p,q from the
        // given offset.
        double d = Double.NaN;
        if (fpmwTracking) {
            NiciOffsetCoefficients co;
            co = NiciOffsetCoefficients.lookup(null);
            d =  NiciOffsetCoefficients.round(co.computeD(p, q));
            if (d < minD) {
                d = minD;
            } else if (d > maxD) {
                d = maxD;
            }
            p = co.computeP(d);
            q = co.computeQ(d);
        }
        return new DPQ(d, p, q);
    }

    public Point2D.Double computeXY(double xaxis, double yaxis, IssPort port) {
        double sign = 1.0;
        if (fpmwTracking) sign = getSign(port);
        DPQ dpq = computeDQP(sign* xaxis, yaxis);
        return new Point2D.Double(sign * dpq.getP(), dpq.getQ());
    }

    private static final String D = "d";
    private static final String FPMW_TRACKING = "fpmw";

    public synchronized ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet res = super.getParamSet(factory, name);
        Pio.addBooleanParam(factory, res, FPMW_TRACKING, fpmwTracking);
        if (fpmwTracking) {
            Pio.addDoubleParam(factory, res, D, d);
        }
        return res;
    }

    public synchronized void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        fpmwTracking = Pio.getBooleanValue(paramSet, FPMW_TRACKING, false);
        if (fpmwTracking) {
            d = Pio.getDoubleValue(paramSet, D, 0.0);
        }
    }

}
