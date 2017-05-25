package edu.gemini.qpt.core;

import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.core.util.IntervalType;
import edu.gemini.qpt.shared.util.PioSerializable;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents a scheduling block, normally an observing night.
 * @author rnorris
 */
public final class Block implements Comparable<Block>, IntervalType<Block>, PioSerializable {

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MMM-dd HH:mm");
    private static final String PROP_START = "start";
    private static final String PROP_END = "end";

    private final Interval interval;
	
	public Block(final Long low, final Long high) {
	    interval = new Interval(low, high);
	}

    public Block(final Interval interval) {
        this.interval = interval;
    }
		
	public Block(final ParamSet params) {
		interval = new Interval(params);
	}

    //
    // Interval type contract
    //
    @Override
    public Interval getInterval() { return interval; }

    @Override
    public long getStart() { return interval.getStart(); }

    @Override
    public long getEnd() { return interval.getEnd(); }

    @Override
    public long getLength() { return interval.getLength(); }

    @Override
    public boolean contains(final long value) { return interval.contains(value); }

    @Override
    public boolean overlaps(final IntervalType<?> b, final Interval.Overlap overlap) {
        return interval.overlaps(b.getInterval(), overlap);
    }

    @Override
    public boolean abuts(final IntervalType<?> b) { return b != null && interval.abuts(b.getInterval()); }

    @Override
    public Block plus(final IntervalType<?> b) { return new Block(interval.plus(b.getInterval())); }

    @Override
    public Block minus(final IntervalType<?> b) { return new Block(interval.minus(b.getInterval())); }

    @Override
    public Block create(final long start, final long end) { return new Block(start, end); }
	
	public String getName() {
		return FORMAT.format(new Date(getStart()));
	}
	
	@Override
	public String toString() {
		return "Block " + getName();
	}

    ///
    /// PIO
    ///
    @Override
    public ParamSet getParamSet(final PioFactory factory, final String name) {
        final ParamSet params = factory.createParamSet(name);
        Pio.addLongParam(factory, params, PROP_START, interval.getStart());
        Pio.addLongParam(factory, params, PROP_END, interval.getEnd());
        return params;
    }

    //
    // OBJECT
    //

    @Override
    public int hashCode() {
        return interval.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Block) {
            final Block b = (Block) obj;
            return interval.equals(b.interval);
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(final Block b) {
        return interval.compareTo(b.interval);
    }


}
