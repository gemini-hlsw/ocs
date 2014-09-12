//
// $
//

package edu.gemini.spModel.guide;

import java.util.List;

/**
 * A collection of {@link GuideOption} offered by a {@link GuideProbe}.
 */
public interface GuideOptions {

    /**
     * Get all of the available individual options.
     */
    List<GuideOption> getAll();

    /**
     * Gets the default option to use when a GuideOption value needs to be
     * initialized.
     */
    GuideOption getDefault();

    /**
     * Gets the default guide option for which
     * {@link edu.gemini.spModel.guide.GuideOption#isActive()}
     * returns <code>true</code>.
     */
    GuideOption getDefaultActive();

    /**
     * Gets the default guide option for which
     * {@link edu.gemini.spModel.guide.GuideOption#isActive()}
     * returns <code>false</code>.
     */
    GuideOption getDefaultInactive();

    /**
     * Gets the default guide option for a completely unused guider.
     */
    GuideOption getDefaultOff();

    /**
     * Parses the GuideOption string
     * @param optionString
     * @return
     */
    GuideOption parse(String optionString);

    GuideOption fromDefaultGuideOption(DefaultGuideOptions.Value opt);
}
