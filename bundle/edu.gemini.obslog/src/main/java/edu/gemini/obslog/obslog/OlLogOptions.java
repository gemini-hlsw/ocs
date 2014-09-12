package edu.gemini.obslog.obslog;

import edu.gemini.skycalc.ObservingNight;
import edu.gemini.spModel.core.Site;

import java.io.Serializable;

//
// Gemini Observatory/AURA
// $Id: OlLogOptions.java,v 1.7 2005/12/11 15:54:15 gillies Exp $
//

/**
 * A class to allow actions to configure the production of the log.
 * <p/>
 * Options are defined in the action and passed to lower levels for use when constructing values.  An example is the
 * observing time which must be changed based upon whether the display is multi-night or not.
 */
public final class OlLogOptions implements Serializable {

    private static final boolean MULTINIGHT_DEFAULT = false;
    private boolean _isMultiNight;

    private static final boolean SHOWNODATAOBS_DEFAULT = false;
    private boolean _showNoData;
    /**
     * private static final boolean FULLFILENAMES_DEFAULT = false;
     * private boolean _fullFilenames;
     */

    private static final ObservingNight LIMITCONFIGDATESBYNIGHT_DEFAULT = null;
    private ObservingNight _limitConfigDatesByNight;


    /**
     * Public version of default options.
     * <ul>
     * <li>multiNight = false</li>
     * </ul>
     */
    public static final OlLogOptions DEFAULT_OPTIONS = new OlLogOptions();

    /**
     * Construct typical options.  Note that there is a single instance of this default options set.
     */
    public OlLogOptions() {
        _isMultiNight = MULTINIGHT_DEFAULT;
        _showNoData = SHOWNODATAOBS_DEFAULT;
        _limitConfigDatesByNight = LIMITCONFIGDATESBYNIGHT_DEFAULT;
    }

    public void setOptions(OlLogOptions options) {
        if (options == null) return;
        _isMultiNight = options._isMultiNight;
        _showNoData = options._showNoData;
        _limitConfigDatesByNight = options._limitConfigDatesByNight;
    }

    /**
     * Determine if multi-night option is set.
     *
     * @return true if multi-night option is set
     */
    public boolean isMultiNight() {
        return _isMultiNight;
    }

    /**
     * Set the multi-night option.
     *
     * @param state true if multi-night option should be used.   False to not use it.  Default value is false.
     */
    public void setMultiNight(boolean state) {
        _isMultiNight = state;
    }

    /**
     * Determine if options for showing empty unique configs is set.
     *
     * @return true if they should be shown.
     */
    public boolean isShowEmpties() {
        return _showNoData;
    }

    /**
     * Set the Show Empties option.
     *
     * @param state true if observations with no data should be shown in a request.  Default value is false.
     */
    public void setShowEmpties(boolean state) {
        _showNoData = state;
    }

    /**
     * Is the limit configs by night option set
     *
     * @return true if only confis appearing to start during the date should be included.  Value returned is the limits as
     *         an <code>ObservingNight</code> object
     */
    public ObservingNight getLimitConfigDatesByNight() {
        return _limitConfigDatesByNight;
    }

    /**
     * Set the limit configs by night option
     *
     * @param state true if the only configs appear on that date should be shown
     */
    public void setLimitConfigDatesByNight(ObservingNight state) {
        _limitConfigDatesByNight = state;
    }

    /**
     * Provides the site specified in the SITE config file.  Default value is "south" if the GEMINI_SITE
     * property is not set.
     *
     * @return a string that is either north or south (or whatever GEMINI_SITE) is set to.
     */
    public Site getGeminiSite() {
        final Site s = Site.currentSiteOrNull;
        return (s == null) ? Site.GS : s;  // seems somewhat dubious ...
    }

    /**
     * Convenience method to check if at south site
     */
    public boolean isSouth() {
        return getGeminiSite() == Site.GS;
    }

    /**
     * Convenience method to check if at north site
     */
    public boolean isNorth() {
        return getGeminiSite() == Site.GN;
    }

}
