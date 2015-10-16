package edu.gemini.spModel.target.offset;

/**
 * A data object that describes an offset position and includes methods
 * for extracting positions.
 */
public final class OffsetPos extends OffsetPosBase {

    public static final Factory<OffsetPos> FACTORY = new Factory<OffsetPos>() {
        public OffsetPos create(String tag) {
            return new OffsetPos(tag);
        }
        public OffsetPos create(String tag, double p, double q) {
            return new OffsetPos(tag, p, q);
        }
    };

    /**
     * Create an OffsetPos with the given tag and coordinates
     */
    public OffsetPos(String tag, double xaxis, double yaxis) {
        super(tag, xaxis, yaxis);
    }

    /**
     * Create an OffsetPos with the given tag and (0,0) coordinates.
     */
    public OffsetPos(String tag) {
        super(tag);
    }

}
