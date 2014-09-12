package edu.gemini.spModel.gemini.gpi;

import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.telescope.IssPort;

/**
 * Enforces the range limit (-1 to 1)
 */
public final class GpiOffsetPos extends OffsetPosBase {

    public static final Factory<GpiOffsetPos> FACTORY = new Factory<GpiOffsetPos>() {
        public GpiOffsetPos create(String tag) {
            return new GpiOffsetPos(tag);
        }
        public GpiOffsetPos create(String tag, double p, double q) {
            return new GpiOffsetPos(tag, p, q);
        }
    };

    private static double clip(double d) {
        if (d < -1) return -1;
        if (d > 1) return 1;
        return d;
    }

    public GpiOffsetPos(String tag, double xaxis, double yaxis) {
        super(tag, clip(xaxis), clip(yaxis));
    }

    public GpiOffsetPos(String tag) {
        super(tag);
    }

    /**
     * Override clone to make sure the position is correctly
     * initialized.
     */
    public Object clone() {
        return super.clone();
    }


    @Override
    public void setXAxis(double xAxis) {
        super.setXAxis(clip(xAxis));
    }

    @Override
    public void setYAxis(double yAxis) {
        super.setYAxis(clip(yAxis));
    }

    @Override
    public void setXY(double xaxis, double yaxis, IssPort port) {
        super.setXY(clip(xaxis), clip(yaxis), port);
    }

    @Override
    public void noNotifySetXY(double xaxis, double yaxis, IssPort port) {
        super.noNotifySetXY(clip(xaxis), clip(yaxis), port);
    }

}
