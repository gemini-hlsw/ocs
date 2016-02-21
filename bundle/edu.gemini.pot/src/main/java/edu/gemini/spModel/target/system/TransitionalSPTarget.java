package edu.gemini.spModel.target.system;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.spModel.core.SpatialProfile;
import edu.gemini.spModel.core.SpectralDistribution;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.target.WatchablePos;

// transitional; will go away
public abstract class TransitionalSPTarget extends WatchablePos {

    public abstract ITarget getTarget();

    public void setRaDecDegrees(double ra, double dec) {
        getTarget().setRaDecDegrees(ra, dec);
        _notifyOfUpdate();
    }

    public String getName() {
        return getTarget().getName();
    }

    public void setName(String name) {
        getTarget().setName(name);
        _notifyOfUpdate();
    }

    public void setRaDegrees(double value) {
        getTarget().setRaDegrees(value);
        _notifyOfUpdate();
    }


    public void setRaHours(double value) {
        getTarget().setRaHours(value);
        _notifyOfUpdate();
    }

    public Option<Double> getRaHours(Option<Long> time) {
        return getTarget().getRaHours(time);
    }

    public Option<String> getRaString(Option<Long> time) {
        return getTarget().getRaString(time);
    }

    public Option<Double> getRaDegrees(Option<Long> time) {
        return getTarget().getRaDegrees(time);
    }

    public void setDecDegrees(double value) {
        getTarget().setDecDegrees(value);
        _notifyOfUpdate();
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

    public void setSpectralDistribution(scala.Option<SpectralDistribution> sd) {
        getTarget().setSpectralDistribution(sd);
        _notifyOfUpdate();
    }

    public void setSpatialProfile(scala.Option<SpatialProfile> sp) {
        getTarget().setSpatialProfile(sp);
        _notifyOfUpdate();
    }

    public scala.Option<SpectralDistribution> getSpectralDistribution() {
        return getTarget().getSpectralDistribution();
    }

    public scala.Option<SpatialProfile> getSpatialProfile() {
        return getTarget().getSpatialProfile();
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

    public boolean isSidereal() {
        return getTarget() instanceof HmsDegTarget;
    }

    public boolean isNonSidereal() {
        return !isSidereal();
    }


    public synchronized Option<Coordinates> getSkycalcCoordinates(Option<Long> when) {
        return getTarget().getSkycalcCoordinates(when);
    }

}