package edu.gemini.spdb.cron

import java.io.File

/**
 * Storage directories for cron jobs.  Temp files don't survive updates but
 * permanent files do.
 */
final case class CronStorage(tempDir: File, permDir: File) {

  def newTempFile(name: String): File =
    new File(tempDir, name)

  def newPermFile(name: String): File =
    new File(permDir, name)

}
