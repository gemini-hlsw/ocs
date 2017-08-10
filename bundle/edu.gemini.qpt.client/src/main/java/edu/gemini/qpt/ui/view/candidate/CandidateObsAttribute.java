package edu.gemini.qpt.ui.view.candidate;

import java.util.Comparator;

import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.util.TimeUtils;

enum CandidateObsAttribute {

	SB(new Comparator<Obs>() {
		public int compare(Obs o1, Obs o2) {
			int ret = o1.getProg().getBand() - o2.getProg().getBand();
			return ret != 0 ? ret : Observation.getComparator().compare(o1, o2);
		}
	}),
	
	P(new Comparator<Obs>() {
		public int compare(Obs o1, Obs o2) {
			int ret = o1.getPriority().compareTo(o2.getPriority());
			return ret != 0 ? ret : Observation.getComparator().compare(o1, o2);
		}
	}),
	
	Score(new Comparator<Obs>() {
		public int compare(Obs o1, Obs o2) {
			return 0; // HACK: THIS IS NEVER CALLED! SEE CandidateObsComparator.compare()
		}
	}),
	
	Observation(new Comparator<Obs>() {
		public int compare(Obs o1, Obs o2) {
			return o1.compareTo(o2);
		}
	}), 
	
	Target(new Comparator<Obs>() {
		public int compare(Obs o1, Obs o2) {
			return o1.getTargetName().compareTo(o2.getTargetName());
		}
	}), 
	
	RA(new Comparator<Obs>() {
		public int compare(Long when, Obs o1, Obs o2) {
			return (int) (100 * o1.getRa(when) - 100 * o2.getRa(when));
		}
	}), 
	
	Inst(new Comparator<Obs>() {
		public int compare(Obs o1, Obs o2) {
			return o1.getInstrumentString().compareTo(o2.getInstrumentString());
		}
	}), 
	
	Dur(new Comparator<Obs>() {
		public int compare(Obs o1, Obs o2) {
			return (int) (o1.getRemainingTime() / TimeUtils.MS_PER_MINUTE - o2.getRemainingTime() / TimeUtils.MS_PER_MINUTE);
		}
	})
	
	;
	
	private final Comparator<Obs> comp;
	
	private CandidateObsAttribute(Comparator<Obs> comp) {
		this.comp = comp;
	}

	public Comparator<Obs> getComparator() {
		return comp;
	}
	
}
