package edu.gemini.spModel.io.impl.migration.to2014A

import edu.gemini.pot.sp.{ISPTemplateFolder, ISPFactory, ISPObservation}
import edu.gemini.spModel.pio.{Container, Version}
import edu.gemini.spModel.rich.pot.sp.templateGroupWrapper
import edu.gemini.spModel.util.VersionTokenUtil

import scala.collection.JavaConverters._

/**
 * Handles parsing the new obs logs out of the old observing log if necessary.
 * This is the data migration for observing log for 2014A.
 */
object To2014A {
  private val Version_2014A = Version.`match`("2014A-1")

  def update(f: ISPFactory, obs: ISPObservation, c: Container): Unit =
    if (c.getVersion.compareTo(Version_2014A) < 0) {
      ObsLogUpdate.update(f, obs, c)
      GmosWarning.warnIfNecessary(obs)
    }

  // Update a template folder to remove duplicate template group numbers.
  def update(tf: ISPTemplateFolder, c: Container): Unit =
    if (c.getVersion.compareTo(Version_2014A) < 0) {
      val groups = tf.getTemplateGroups.asScala.toList
      val tokens = groups.map { tg => tg.versionToken -> tg.getNodeKey }
      VersionTokenUtil.normalize(tokens).foreach { case (token, key) =>
        groups.find(_.getNodeKey == key).foreach { _.versionToken = token }
      }
    }
}
