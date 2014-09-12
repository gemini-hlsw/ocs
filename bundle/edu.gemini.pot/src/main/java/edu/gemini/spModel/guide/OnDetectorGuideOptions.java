//
// $
//

package edu.gemini.spModel.guide;

import java.util.List;
import java.util.Arrays;

/**
 * Guide options for on detector guiders.  They don't park and freeze like a
 * normal guide probe.  They are either on or off.
 */
public enum OnDetectorGuideOptions implements GuideOptions {
    instance;

    public enum Value implements GuideOption {
        on() {
            public boolean isActive() { return true;  }
        },

        off() {
            public boolean isActive() { return false; }
        },
    }

    public List<GuideOption> getAll() {
        return Arrays.asList((GuideOption[]) Value.values());
    }

    public GuideOption getDefault()         { return Value.off;            }
    public GuideOption getDefaultActive()   { return Value.on;             }
    public GuideOption getDefaultInactive() { return Value.off;            }
    public GuideOption getDefaultOff()      { return getDefaultInactive(); }

    public GuideOption parse(String optString) {
        return Value.valueOf(optString);
    }

    @Override
    public GuideOption fromDefaultGuideOption(DefaultGuideOptions.Value opt) {
        return opt == DefaultGuideOptions.Value.on ? Value.on : Value.off;
    }
}