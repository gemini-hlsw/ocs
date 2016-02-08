package edu.gemini.spModel.target.system;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.core.SpatialProfile;
import edu.gemini.spModel.core.SpectralDistribution;
import edu.gemini.spModel.target.WatchablePos;

// transitional; will go away
public abstract class TransitionalSPTarget extends WatchablePos {

    public abstract ITarget getTarget();

    public void setRaDecDegrees(double ra, double dec) {
        getTarget().setRaDecDegrees(ra, dec);
        _notifyOfUpdate();
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

    public void setDecDegrees(double value) {
        getTarget().setDecDegrees(value);
        _notifyOfUpdate();
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

    @Deprecated
    public void notifyOfGenericUpdate() {
        super._notifyOfUpdate();
    }

}
