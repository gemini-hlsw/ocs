//
// $
//

package edu.gemini.spModel.prog;

import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.util.NightlyProgIdGenerator;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Gemini specific plan id.  Parses out the site and date.
 */
public final class GemPlanId implements Serializable, Comparable {
    private static final Logger LOG = Logger.getLogger(GemPlanId.class.getName());

    private static final Pattern PAT = Pattern.compile("G([NS])-PLAN(\\d\\d\\d\\d\\d\\d\\d\\d)");

    private static final TimeZone UTC = SimpleTimeZone.getTimeZone("UTC");

    private static DateFormat getFormat() {
        DateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        fmt.setTimeZone(UTC);
        return fmt;
    }

    /**
     * Creates a GemPlanId assuming the program ID is a valid plan id.
     *
     * @param progId program id to be parsed into a GemPlanId
     *
     * @throws SPBadIDException if the program id cannot be parsed into a
     * GemPlanId
     */
    public static Option<GemPlanId> parse(SPProgramID progId) {
        if (progId == null) throw new NullPointerException();

        String  str = progId.stringValue();
        Matcher mat = PAT.matcher(str);

        if (!mat.matches()) {
            String msg = String.format("Could not parse '%s' into a plan id.", str);
            LOG.log(Level.INFO, msg);
            return None.instance();
        }

        GemSite site = GemSite.parse(mat.group(1)).getValue();

        Date date;
        DateFormat format = getFormat();
        try {
            date = format.parse(mat.group(2));
        } catch (ParseException ex) {
            String msg = String.format("Could not parse '%s' into a plan id.", str);
            LOG.log(Level.INFO, msg);
            return None.instance();
        }

        return new Some<GemPlanId>(new GemPlanId(progId, site, date));
    }

    /**
     * Creates a GemPlanId assuming the program ID is a valid plan id.
     *
     * @param progIdStr program id to be parsed into a GemPlanId
     *
     * @throws SPBadIDException if the program id cannot be parsed into a
     * GemPlanId
     */
    public static Option<GemPlanId> parse(String progIdStr) {
        SPProgramID progId;
        try {
            progId = SPProgramID.toProgramID(progIdStr);
        } catch (SPBadIDException e) {
            return None.instance();
        }
        return parse(progId);
    }


    /**
     * Creates a GemPlanId for the given site for the current observing night.
     *
     * @pram site location of the plan
     */
    public static GemPlanId create(GemSite site) {
        return create(site, System.currentTimeMillis());
    }

    /**
     * Creates a GemPlanId for the given site for the current observing night.
     *
     * @param site location of the plan
     * @param time time at which the the plan id is relevant
     */
    public static GemPlanId create(GemSite site, long time) {
        String prefix = NightlyProgIdGenerator.PLAN_ID_PREFIX;
        SPProgramID progId = NightlyProgIdGenerator.getProgramID(prefix, site.getSiteDesc(), System.currentTimeMillis());
        return parse(progId).getValue();
    }

    private final SPProgramID id;
    private final GemSite site;
    private final Date date;

    private GemPlanId(SPProgramID progId, GemSite site, Date date) {
        if ((progId == null) || (site == null) || (date == null)) {
            throw new NullPointerException();
        }

        this.id   = progId;
        this.site = site;
        this.date = date;
    }

    /**
     * Gets a SPProgramID equivalent to this plan id.  The program id is used
     * throughout the API, so access to it has been provided here.
     */
    public SPProgramID getProgramId() {
        return id;
    }

    /**
     * Gets the site associated with the plan.
     */
    public GemSite getSite() {
        return site;
    }

    /**
     * Gets the plan date.
     */
    public Date getDate() {
        return date;
    }

    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GemPlanId gemPlanId = (GemPlanId) o;

        if (!date.equals(gemPlanId.date)) return false;
        if (!id.equals(gemPlanId.id)) return false;
        return site == gemPlanId.site;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + site.hashCode();
        result = 31 * result + date.hashCode();
        return result;
    }

    public int compareTo(Object o) {
        GemPlanId that = (GemPlanId) o;
        int res = site.compareTo(that.site);
        if (res != 0) return res;
        return date.compareTo(that.date);
    }

    private GemPlanId offset(int offset) {
        Calendar cal = new GregorianCalendar(UTC);
        cal.setTime(date);
        cal.add(Calendar.DATE, offset);

        DateFormat fmt = getFormat();
        String dateStr = fmt.format(cal.getTime());

        String idStr = String.format("%s-PLAN%s", site.getIdPrefix(), dateStr);
        SPProgramID progId;
        try {
            progId = SPProgramID.toProgramID(idStr);
        } catch (SPBadIDException e) {
            throw new RuntimeException(e);
        }
        return new GemPlanId(progId, site, cal.getTime());
    }

    /**
     * Gets the plan id from the day before this plan id.
     */
    public GemPlanId yesterday() {
        return offset(-1);
    }

    /**
     * Gets the plan id from the day after this plan id.
     */
    public GemPlanId tomorrow() {
        return offset(1);
    }
}
