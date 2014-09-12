//
// $
//

package edu.gemini.spModel.guide;

import java.util.List;
import java.util.Arrays;

/**
 * Guide options for standard guide probes.  A typical guide probe can be
 * set to providing guiding corrections, can be frozen in that it is not moved
 * but not attempting to guide, or parked in that it is put away out of the
 * field of view.
 */
public enum StandardGuideOptions implements GuideOptions {
    instance;

    /**
     * Individual standard guide option values.
     */
    public enum Value implements GuideOption {
        guide() {
            public boolean isActive() { return true;  }
        },

        park() {
            public boolean isActive() { return false; }
        },

        freeze() {
            public boolean isActive() { return false; }
        },
    }

    public List<GuideOption> getAll() {
        return Arrays.asList((GuideOption[]) Value.values());
    }

    public GuideOption getDefault()         { return Value.park;   }
    public GuideOption getDefaultActive()   { return Value.guide;  }
    public GuideOption getDefaultInactive() { return Value.freeze; }
    public GuideOption getDefaultOff()      { return Value.park;   }

    public GuideOption parse(String optString) {
        try {
            return Value.valueOf(optString);
        } catch (RuntimeException ex) {
            // Handle the case where an OnDetectorGuideOptions probe gets
            // converted into a StandardGuideOptions probe.  Bizarre.  They
            // decided that GSAOI ODGW can in fact be frozen and parked.
            switch (OnDetectorGuideOptions.Value.valueOf(optString)) {
                case on:  return Value.guide;
                case off: return Value.freeze;
            }
            throw ex;
        }
    }

    @Override
    public GuideOption fromDefaultGuideOption(DefaultGuideOptions.Value opt) {
        return opt == DefaultGuideOptions.Value.on ? Value.guide : Value.freeze;
    }
}
