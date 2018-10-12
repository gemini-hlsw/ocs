package edu.gemini.spdb.cron

import java.util.logging.Logger
import java.security.Principal

package object osgi {

  type Env = java.util.Map[String, String]
  type Job = (CronStorage, Logger, Env, java.util.Set[Principal]) => Unit

}
