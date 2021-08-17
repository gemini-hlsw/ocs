package edu.gemini.wdba.glue.api;

import edu.gemini.spModel.core.Site;
import edu.gemini.wdba.fire.FireService;

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
    public final FireService fireService;

    public WdbaContext(
        Site                      site,
        WdbaDatabaseAccessService db,
        Set<Principal>            user,
        FireService               fireService
    ) {
        this.site        = site;
        this.db          = db;
        this.user        = user;
        this.fireService = fireService;
    }

    public WdbaDatabaseAccessService getWdbaDatabaseAccessService() {
        return db;
    }

    public Site getSite() {
        return site;
    }

    public FireService getFireService() {
        return fireService;
    }
}
