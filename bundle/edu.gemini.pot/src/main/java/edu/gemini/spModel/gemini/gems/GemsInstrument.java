package edu.gemini.spModel.gemini.gems;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe;
import edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.gems.GemsTipTiltMode;

import java.util.Arrays;
import java.util.List;

/**
 * Creates an enum that describes the two instruments available for use with
 * GeMS from the point of view of guide star searches.
 * <p/>
 * See OT-21
 */
public enum GemsInstrument {
    gsaoi() {
        public Option<Offset> getOffset() {
            Angle a = new Angle(4 + GsaoiDetectorArray.DETECTOR_GAP_ARCSEC / 2, Angle.Unit.ARCSECS);
            return new Some<>(new Offset(a, a));
        }

        public GemsGuideProbeGroup getGuiders() {
            return GsaoiOdgw.Group.instance;
        }
    },

    flamingos2() {
        public Option<Offset> getOffset() {
            return None.instance();
        }

        public GemsGuideProbeGroup getGuiders() {
            return Flamingos2OiwfsGuideProbe.Group.instance;
        }
    };

    // Offset that usage of the instrument introduces to guide star searches.
    public abstract Option<Offset> getOffset();

    public abstract GemsGuideProbeGroup getGuiders();
}
