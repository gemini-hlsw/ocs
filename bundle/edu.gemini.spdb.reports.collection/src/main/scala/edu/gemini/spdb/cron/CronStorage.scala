package edu.gemini.spdb.cron

import java.io.File

/**
 * Storage directories for cron jobs.  Temp files don't survive updates but
 * permanent files do.
 */
final case class CronStorage(tempDir: File, permDir: File) {

  def getTempFile(name: String): File =
    new File(tempDir, name)

  def getPermFile(name: String): File =
    new File(permDir, name)

}
