package edu.gemini.qpt.core;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.gemini.qpt.core.util.Interval;
import edu.gemini.spModel.pio.ParamSet;

/**
 * Represents a scheduling block, normally an observing night.
 * @author rnorris
 */
public final class Block extends Interval {

	private static final DateFormat FORMAT = new SimpleDateFormat("dd-MMM HH:mm");
	
	public Block(Long low, Long high) {
		super(low, high);
	}
		
	public Block(ParamSet params) {
		super(params);
	}

	@Override
	protected Block create(long start, long end) {
		return new Block(start, end);
	}
	
	public String getName() {
		String a = FORMAT.format(new Date(getStart()));
		return a;
	}
	
	@Override
	public String toString() {
		return "Block " + getName();
	}
		
}
