package edu.gemini.spModel.target.system;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.target.WatchablePos;

import java.util.Collections;
import java.util.Set;

// transitional; will go away
public abstract class TransitionalSPTarget extends WatchablePos {

    public Option<Magnitude> getMagnitude(final MagnitudeBand band) {
        return None.instance();
    }

    public void setMagnitudes(final ImList<Magnitude> magnitudes) {
    }

    public ImList<Magnitude> getMagnitudes() {
        return DefaultImList.create();
    }

    public Set<MagnitudeBand> getMagnitudeBands() {
        return Collections.emptySet();
    }

}