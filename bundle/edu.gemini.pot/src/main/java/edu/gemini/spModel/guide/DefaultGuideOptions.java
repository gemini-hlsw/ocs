//
// $
//

package edu.gemini.spModel.guide;

import java.util.Arrays;
import java.util.List;

/**
 * Default guide options that can be initialized on an offset position ahead of
 * time before knowing what the specific guider will be.
 */
public enum DefaultGuideOptions implements GuideOptions {
    instance;

    /**
     * Individual standard guide option values.
     */
    public enum Value implements GuideOption {
        on() {
            @Override public boolean isActive() { return true;  }
            @Override public String toString() { return "on"; }
        },

        off() {
            @Override public boolean isActive() { return false; }
            @Override public String toString() { return "off"; }
        },
        ;

        public boolean isActive() { return true; }
    }

    public List<GuideOption> getAll() {
        return Arrays.asList((GuideOption[]) Value.values());
    }

    public GuideOption getDefault() {
        return Value.on;
    }

    public GuideOption getDefaultActive() {
        return Value.on;
    }

    public GuideOption getDefaultInactive() {
        return Value.off;
    }

    public GuideOption getDefaultOff() { return getDefaultInactive(); }

    public GuideOption parse(String optString) {
        return Value.valueOf(optString);
    }

    @Override public GuideOption fromDefaultGuideOption(Value opt) { return opt; }
}
