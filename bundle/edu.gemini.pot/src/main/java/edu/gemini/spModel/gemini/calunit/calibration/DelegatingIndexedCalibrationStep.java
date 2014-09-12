package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.gemini.calunit.CalUnitParams;

import java.util.Set;

/**
 * An IndexedCalibrationStep that delegates everything except the (explicitly
 * provided) index to a CalibrationStep instance.
 */
public final class DelegatingIndexedCalibrationStep implements IndexedCalibrationStep {
    private final CalibrationStep delegate;
    private final Integer index;

    public DelegatingIndexedCalibrationStep(CalibrationStep delegate, Integer index) {
        this.delegate = delegate;
        this.index    = index;
    }

    @Override public Boolean isFlat() { return delegate.isFlat(); }

    @Override public Boolean isArc() { return delegate.isArc(); }

    @Override public Set<CalUnitParams.Lamp> getLamps() { return delegate.getLamps(); }

    @Override public CalUnitParams.Shutter getShutter() { return delegate.getShutter(); }

    @Override public CalUnitParams.Filter getFilter() { return delegate.getFilter(); }

    @Override public CalUnitParams.Diffuser getDiffuser() { return delegate.getDiffuser(); }

    @Override public Double getExposureTime() { return delegate.getExposureTime(); }

    @Override public Integer getCoadds() { return delegate.getCoadds(); }

    @Override public Boolean isBasecalDay() { return delegate.isBasecalDay(); }

    @Override public Boolean isBasecalNight() { return delegate.isBasecalNight(); }

    // note: the  delegation is backed by a calibration impl which does not supply this value, it will be calculated on the fly
    @Override public String getObsClass() { return ""; }

    @Override public Integer getIndex() { return index; }
}
