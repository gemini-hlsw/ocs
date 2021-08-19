package edu.gemini.wdba.glue.api;

import edu.gemini.spModel.core.Site;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Gemini Observatory/AURA
 * $Id: TccContext.java 756 2007-01-08 18:01:24Z gillies $
 */
public final class WdbaContext {

    private final WdbaDatabaseAccessService db;
    private final Site site;
    private final Set<Principal> user;

    public WdbaContext(
        Site                      site,
        WdbaDatabaseAccessService db,
        Set<Principal>            user
    ) {
        this.site  = site;
        this.db    = db;
        this.user  = Collections.unmodifiableSet(new HashSet<>(user));
    }

    public WdbaDatabaseAccessService getWdbaDatabaseAccessService() {
        return db;
    }

    public Site getSite() {
        return site;
    }

    public Set<Principal> getUser() {
        return user;
    }
}
