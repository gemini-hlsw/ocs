package edu.gemini.qpt.ui.view.candidate;

import java.util.Comparator;
import java.util.function.Function;

import edu.gemini.model.p1.immutable.Observation;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.util.TimeUtils;

enum CandidateObsAttribute {

    SB((Long when) -> new Comparator<Obs>() {
        public int compare(Obs o1, Obs o2) {
            int ret = o1.getProg().getBand() - o2.getProg().getBand();
            return ret != 0 ? ret : Observation.getComparator(when).compare(o1, o2);
        }
    }),
    
    P((Long when) -> new Comparator<Obs>() {
        public int compare(Obs o1, Obs o2) {
            int ret = o1.getPriority().compareTo(o2.getPriority());
            return ret != 0 ? ret : Observation.getComparator(when).compare(o1, o2);
        }
    }),
    
    Score((Long when) -> new Comparator<Obs>() {
        public int compare(Obs o1, Obs o2) {
            return 0; // HACK: THIS IS NEVER CALLED! SEE CandidateObsComparator.compare()
        }
    }),
    
    Observation((Long when) -> new Comparator<Obs>() {
        public int compare(Obs o1, Obs o2) {
            return o1.compareTo(o2);
        }
    }), 
    
    Target((Long when) -> new Comparator<Obs>() {
        public int compare(Obs o1, Obs o2) {
            return o1.getTargetName().compareTo(o2.getTargetName());
        }
    }), 
    
    RA((Long when) -> new Comparator<Obs>() {
        public int compare(Obs o1, Obs o2) {
            return (int) (100 * o1.getRa(when) - 100 * o2.getRa(when));
        }
    }),

    Inst((Long when) -> new Comparator<Obs>() {
        public int compare(Obs o1, Obs o2) {
            return o1.getInstrumentString().compareTo(o2.getInstrumentString());
        }
    }), 
    
    Dur((Long when) -> new Comparator<Obs>() {
        public int compare(Obs o1, Obs o2) {
            return (int) (o1.getRemainingTime() / TimeUtils.MS_PER_MINUTE - o2.getRemainingTime() / TimeUtils.MS_PER_MINUTE);
        }
    })
    
    ;
    
    private final Function<Long, Comparator<Obs>> comp;

    private CandidateObsAttribute(Function<Long, Comparator<Obs>> comp) {
        this.comp = comp;
    }

    public Comparator<Obs> getComparator(Long when) {
        return comp.apply(when);
    }
    
}
