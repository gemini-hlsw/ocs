package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.gemini.calunit.CalUnitParams.Diffuser;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Filter;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Lamp;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Shutter;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * A mutable implementation of IndexedCalibrationStep.

 */
public final class MutableIndexedCalibrationStep extends AbstractIndexedCalibrationStep {

    private final Set<Lamp> lamps = new TreeSet<Lamp>();
    private Shutter shutter;
    private Filter filter;
    private Diffuser diffuser;
    private Double exposureTime;
    private Integer coadds;
    private String obsClass;

    @Override public Set<Lamp> getLamps() { return new TreeSet<Lamp>(lamps); }
    public void setLamps(Collection<Lamp> lamps) { this.lamps.clear(); this.lamps.addAll(lamps); }

    @Override public Shutter getShutter() { return shutter; }
    public void setShutter(Shutter shutter) { this.shutter = shutter; }

    @Override public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }

    @Override public Diffuser getDiffuser() { return diffuser; }
    public void setDiffuser(Diffuser diffuser) { this.diffuser = diffuser; }

    @Override public Double getExposureTime() { return exposureTime; }
    public void setExposureTime(Double exposureTime) { this.exposureTime = exposureTime; }

    @Override public Integer getCoadds() { return coadds; }
    public void setCoadds(Integer coadds) { this.coadds = coadds;}

    @Override public String getObsClass() { return obsClass; }
    public void setObsClass(String obsClass) { this.obsClass = obsClass; }

    // these methods are solely here to implement the interface, they are not needed in the editor
    @Override public Boolean isBasecalDay() { return false; }
    @Override public Boolean isBasecalNight() { return false; }
    @Override public Integer getIndex() { return 0; }
}
