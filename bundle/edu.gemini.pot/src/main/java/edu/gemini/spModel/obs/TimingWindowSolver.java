package edu.gemini.spModel.obs;

import java.util.List;
import java.util.logging.Logger;

import edu.gemini.skycalc.Solver;
import edu.gemini.skycalc.TimeUtils;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow;

public class TimingWindowSolver extends Solver {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(TimingWindowSolver.class.getName());

	private final List<TimingWindow> windows;
	private final boolean value;

	public TimingWindowSolver(List<TimingWindow> windows, boolean value) {
		super(TimeUtils.MS_PER_HOUR / 4, TimeUtils.MS_PER_MINUTE);
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






//	<paramset name="timing-window">
//	  <param name="start" value="1194466791889"/>
//	  <param name="duration" value="86400000"/>
//	  <param name="repeat" value="-1"/>
//	  <param name="period" value="445500000"/>
//	</paramset>
//	<paramset name="timing-window">
//	  <param name="start" value="1194466941508"/>
//	  <param name="duration" value="-1"/>
//	  <param name="repeat" value="0"/>
//	  <param name="period" value="0"/>
//	</paramset>
//	<paramset name="timing-window">
//	  <param name="start" value="1194466953531"/>
//	  <param name="duration" value="86400000"/>
//	  <param name="repeat" value="1000"/>
//	  <param name="period" value="445500000"/>
//	</paramset>
//	</paramset>
//
//class Test {
//	
//	public static void main(String[] args) {
//		
//		TimingWindow[] windows = {
//				new TimingWindow(1194466791889L, 86400000L, -1, 445500000L),
//				new TimingWindow(1194466941508L, -1L, 0, 0),
//				new TimingWindow(1194466953531L, 86400000, 1000, 445500000),
//			};
//
//		long when = 1194544843256L;
//		
//		TwilightBoundedNight tbn = 
//			TwilightBoundedNight.forObservingNight(TwilightBoundType.NAUTICAL, new ObservingNight(SiteDesc.CERRO_PACHON, when));
//		
//		TimingWindowSolver tws = new TimingWindowSolver(Arrays.asList(windows));
//		System.out.println(tws.solve(tbn.getStartTime(), tbn.getEndTime()));
//		
//	}
//	
//}
//



