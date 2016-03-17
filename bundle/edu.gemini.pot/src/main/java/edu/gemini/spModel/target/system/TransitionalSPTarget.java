package edu.gemini.spModel.target.system;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.target.WatchablePos;

import java.util.Set;

// transitional; will go away
public abstract class TransitionalSPTarget extends WatchablePos {

    protected abstract ITarget getTarget();


    public void putMagnitude(final Magnitude mag) {
        getTarget().putMagnitude(mag);
        _notifyOfUpdate();
    }

    public Option<Magnitude> getMagnitude(final Magnitude.Band band) {
        return getTarget().getMagnitude(band);
    }

    public void setMagnitudes(final ImList<Magnitude> magnitudes) {
        getTarget().setMagnitudes(magnitudes);
        _notifyOfUpdate();
    }

    public ImList<Magnitude> getMagnitudes() {
        return getTarget().getMagnitudes();
    }

    public Set<Magnitude.Band> getMagnitudeBands() {
        return getTarget().getMagnitudeBands();
    }


    public scala.Option<HmsDegTarget> getHmsDegTarget() {
        ITarget t = getTarget();
        return (t instanceof HmsDegTarget) ? new scala.Some<>((HmsDegTarget) t) : scala.Option.empty();
    }

    public scala.Option<NonSiderealTarget> getNonSiderealTarget() {
        ITarget t = getTarget();
        return (t instanceof NonSiderealTarget) ? new scala.Some<>((NonSiderealTarget) t) : scala.Option.empty();
    }




}