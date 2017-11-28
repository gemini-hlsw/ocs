package edu.gemini.spModel.obs;

import java.util.List;
import java.util.logging.Logger;

import edu.gemini.skycalc.Solver;
import edu.gemini.shared.util.DateTimeUtils;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow;

public class TimingWindowSolver extends Solver {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(TimingWindowSolver.class.getName());

	private final List<TimingWindow> windows;
	private final boolean value;

	public TimingWindowSolver(List<TimingWindow> windows, boolean value) {
		super(DateTimeUtils.MillisecondsPerHour() / 4, DateTimeUtils.MillisecondsPerMinute());
		this.windows = windows;
		this.value   = value;
	}

	public TimingWindowSolver(List<TimingWindow> windows) {
		this(windows, true);
	}

	@Override
	protected boolean f(final long t) {
		if (windows.isEmpty()) return true;
		for (TimingWindow w: windows) {
			if (w.getDuration() == TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) {
				if (t > w.getStart()) return value;
			} else {
				long end = w.getStart() + w.getDuration();
				long repeat = w.getRepeat();
				if (repeat == -1) repeat = Long.MAX_VALUE;
				long steps = 0;
				long p = w.getPeriod();
				long t2 = t;
				while (t2 > end && steps++ < repeat)
					t2 -= p;
				if (t2 > w.getStart() && t2 < end)
					return value;
			}
		}
		return !value;
	}

	public boolean includes(long t) {
		return f(t);
	}

}




