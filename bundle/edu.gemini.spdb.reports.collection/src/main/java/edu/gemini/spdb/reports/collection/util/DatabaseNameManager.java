package edu.gemini.spdb.reports.collection.util;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.Site;

// RCN: this should be removed entirely, but since it's used in VM templates there's no easy way to do this safely.
// So for now we just leave it as-is and return the static site info.
@SuppressWarnings("serial")
public final class DatabaseNameManager  {

	private static final DatabaseNameManager INSTANCE = new DatabaseNameManager();

	public static DatabaseNameManager getInstance() {
		return INSTANCE;
	}

    // Called from Velocity
	public String getSiteAbbreviation(Object ignored) {
		return Site.currentSiteOrNull != null ? Site.currentSiteOrNull.abbreviation : "??";
	}

    // Called from Velocity
	public String getSiteName(Object ignored) {
        return Site.currentSiteOrNull != null ? Site.currentSiteOrNull.displayName : "Unknown Site";
	}

}

