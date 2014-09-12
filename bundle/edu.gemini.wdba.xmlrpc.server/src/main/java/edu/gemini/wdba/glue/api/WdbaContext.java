package edu.gemini.wdba.glue.api;

import edu.gemini.spModel.core.Site;

import java.security.Principal;
import java.util.Set;

/**
 * Gemini Observatory/AURA
 * $Id: TccContext.java 756 2007-01-08 18:01:24Z gillies $
 */
public final class WdbaContext {

    public final WdbaDatabaseAccessService db;
    public final Site site;
    public final Set<Principal> user;

    public WdbaContext(Site site, WdbaDatabaseAccessService db, Set<Principal> user) {
        this.site = site;
        this.db   = db;
        this.user = user;
    }

    public WdbaDatabaseAccessService getWdbaDatabaseAccessService() {
        return db;
    }

    public Site getSite() {
        return site;
    }
}
