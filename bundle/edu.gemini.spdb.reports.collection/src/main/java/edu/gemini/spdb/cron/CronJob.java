package edu.gemini.spdb.cron;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public interface CronJob {

    String ALIAS = "edu.gemini.spdb.cron.alias";

    void run(CronStorage store, Logger log, Map<String, String> env, Set<Principal> user);

}
