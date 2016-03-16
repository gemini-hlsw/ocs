package edu.gemini.spModel.target.system;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.spModel.target.WatchablePos;

import java.util.Set;

// transitional; will go away
public abstract class TransitionalSPTarget extends WatchablePos {

    protected abstract ITarget getTarget();



    public Option<Double> getRaHours(Option<Long> time) {
        return getTarget().getRaHours(time);
    }

    public Option<String> getRaString(Option<Long> time) {
        return getTarget().getRaString(time);
    }

    public Option<Double> getRaDegrees(Option<Long> time) {
        return getTarget().getRaDegrees(time);
    }


    public Option<Double> getDecDegrees(Option<Long> time) {
        return getTarget().getDecDegrees(time);
    }

    public Option<String> getDecString(Option<Long> time) {
        return getTarget().getDecString(time);
    }

    public void setRaString(String value) {
        getTarget().setRaString(value);
        _notifyOfUpdate();
    }

    public void setDecString(String value) {
        getTarget().setDecString(value);
        _notifyOfUpdate();
    }

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


    @Deprecated
    public void notifyOfGenericUpdate() {
        super._notifyOfUpdate();
    }

    public scala.Option<HmsDegTarget> getHmsDegTarget() {
        ITarget t = getTarget();
        return (t instanceof HmsDegTarget) ? new scala.Some<>((HmsDegTarget) t) : scala.Option.empty();
    }

    public scala.Option<NonSiderealTarget> getNonSiderealTarget() {
        ITarget t = getTarget();
        return (t instanceof NonSiderealTarget) ? new scala.Some<>((NonSiderealTarget) t) : scala.Option.empty();
    }

    public scala.Option<ConicTarget> getConicTarget() {
        ITarget t = getTarget();
        return (t instanceof ConicTarget) ? new scala.Some<>((ConicTarget) t) : scala.Option.empty();
    }

    public scala.Option<NamedTarget> getNamedTarget() {
        ITarget t = getTarget();
        return (t instanceof NamedTarget) ? new scala.Some<>((NamedTarget) t) : scala.Option.empty();
    }

    public synchronized Option<Coordinates> getSkycalcCoordinates(Option<Long> when) {
        return getTarget().getSkycalcCoordinates(when);
    }

    public ImList<Magnitude> getMagnitudes() {
        return getTarget().getMagnitudes();
    }

    public boolean isTooTarget() {
        final ITarget t = getTarget();
        if (t instanceof HmsDegTarget) {
            final HmsDegTarget hmsDeg = (HmsDegTarget) t;
            return hmsDeg.getRa().getValue() == 0.0 &&
                    hmsDeg.getDec().getValue() == 0.0; ///
        }
        return false;
    }

    public Set<Magnitude.Band> getMagnitudeBands() {
        return getTarget().getMagnitudeBands();
    }

}