package edu.gemini.qpt.core.util;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow;

public class TimingWindowSolver extends Solver {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(TimingWindowSolver.class.getName());
	
	private final List<TimingWindow> windows;
	private final boolean value;
	
	public TimingWindowSolver(Obs obs, boolean value) {
        super(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(1));
        this.value = value;
        this.windows = obs.getTimingWindows();
	}

	public TimingWindowSolver(Obs obs) {
		this(obs, true);
	}
	
	@Override
	protected boolean f(final long t) {
        if (windows.isEmpty()) return value;
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
