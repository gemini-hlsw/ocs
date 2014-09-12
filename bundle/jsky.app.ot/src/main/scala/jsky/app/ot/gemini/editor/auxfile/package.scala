package jsky.app.ot.gemini.editor

import java.io.File
import jsky.util.Preferences

/**
 *
 */
package object auxfile {
  val DirectoryPref = "EdProgramAuxFile.Directory"
  val MaxFileSizeMb = 250
  val MaxFileSize   = MaxFileSizeMb * 1024 * 1024
  val FileChunkSize = 32 * 1024

  def dirPreference: Option[File] =
    Option(Preferences.getPreferences.getPreference(DirectoryPref)).map(new File(_))

  def dirPreference_=(dir: Option[File]) {
    Preferences.getPreferences.setPreference(DirectoryPref, dir.map(_.getAbsolutePath).orNull)
  }
}
