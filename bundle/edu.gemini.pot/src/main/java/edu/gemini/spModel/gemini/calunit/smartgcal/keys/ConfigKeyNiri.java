package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri;

/**
 * Representation of NIRI instrument configurations for calibration lookups.
 * Instances of this class will be used as lookup keys in the corresponding calibration map.
 */
public class ConfigKeyNiri implements ConfigurationKey {

    public static enum Values implements ConfigurationKey.Values {
        DISPERSER("Disperser"),
        FILTER("Filter"),
        MASK("Focal Plane Mask"),
        CAMERA("Camera"),
        BEAM_SPLITTER("Beam Splitter");

        private final String name;

        private Values(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Niri.Disperser disperser;
    private final Niri.Filter filter;
    private final Niri.Mask mask;
    private final Niri.Camera camera;
    private final Niri.BeamSplitter beamSplitter;
    private final int hashCode;

    public ConfigKeyNiri(
            Niri.Disperser disperser,
            Niri.Filter filter,
            Niri.Mask mask,
            Niri.Camera camera,
            Niri.BeamSplitter beamSplitter) {
        if (disperser == null)      throw new IllegalArgumentException(Values.DISPERSER.toString()+" must not be null");
        if (filter == null)         throw new IllegalArgumentException(Values.FILTER.toString()+" must not be null");
        if (mask == null)           throw new IllegalArgumentException(Values.MASK.toString()+" must not be null");
        if (camera == null)         throw new IllegalArgumentException(Values.CAMERA.toString()+" must not be null");
        if (beamSplitter == null)   throw new IllegalArgumentException(Values.BEAM_SPLITTER.toString()+" must not be null");
        this.disperser = disperser;
        this.filter = filter;
        this.mask = mask;
        this.camera = camera;
        this.beamSplitter = beamSplitter;

        this.hashCode = calculateHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigKeyNiri that = (ConfigKeyNiri) o;

        if (disperser != that.disperser) return false;
        if (filter != that.filter) return false;
        if (mask != that.mask) return false;
        if (camera != that.camera) return false;
        if (beamSplitter != that.beamSplitter) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int calculateHashCode() {
        // this is an immutable object therefore we calculate the hash code only once
        int hc = 0;
        hc = disperser.hashCode();
        hc = 31 * hc + filter.hashCode();
        hc = 31 * hc + mask.hashCode();
        hc = 31 * hc + camera.hashCode();
        hc = 31 * hc + beamSplitter.hashCode();
        return hc;
    }

    @Override
    public String getInstrumentName() {
        return InstNIRI.SP_TYPE.readableStr;
    }


    @Override
    public ImList<String> export() {
        return DefaultImList.create(
                disperser.sequenceValue(),
                filter.sequenceValue(),
                mask.sequenceValue(),
                camera.sequenceValue(),
                beamSplitter.sequenceValue());
    }
}
