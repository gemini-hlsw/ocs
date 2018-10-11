package edu.gemini.spdb.cron

import java.util.logging.Logger
import java.security.Principal

import Storage._

package object osgi {

  type Env = java.util.Map[String, String]
  type Job = (Temp, Perm, Logger, Env, java.util.Set[Principal]) => Unit

}
