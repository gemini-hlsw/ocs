package edu.gemini.qpt.ui.view.candidate;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import edu.gemini.qpt.shared.sp.Obs;


enum CandidateObsAttribute {
    Observation(w -> Comparator.naturalOrder()),
    SB(w -> Comparator.<Obs>comparingInt(o -> o.getProg().getBand()).thenComparing(Observation.getComparator(w))),
	P(w -> Comparator.comparing(Obs::getPriority).thenComparing(Observation.getComparator(w))),
    Score(w -> ((o1,o2) -> 0)), // HACK: THIS IS NEVER CALLED! SEE CandidateObsComparator.compare()
    Target(w -> Comparator.comparing(Obs::getTargetName)),
	RA(w -> ((o1,o2) -> (int) (100 * o1.getRa(w) - 100 * o2.getRa(w)))),
    Inst(w -> Comparator.comparing(Obs::getInstrumentString)),
	Dur(w -> Comparator.comparingLong(o -> TimeUnit.MINUTES.toMillis(o.getRemainingTime()))),
	;

	private final Function<Long, Comparator<Obs>> comp;

	CandidateObsAttribute(Function<Long, Comparator<Obs>> comp) {
		this.comp = comp;
	}

	public Comparator<Obs> getComparator(Long when) {
		return comp.apply(when);
	}

}
