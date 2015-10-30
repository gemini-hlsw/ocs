package edu.gemini.util.osgi

import java.io.File

import org.osgi.framework.BundleContext

object ExternalStorage extends ExternalStorage

trait ExternalStorage {

  /**
   * Directory containing the bundle's storage area.
   * This data will be lost every time the bundle / software is upgraded.
   */
  def getExternalDataRoot(context: BundleContext): File = {
    val root = (context.getDataFile(".") /: (1 to 4))((f, _) => f.getParentFile)
    val data = new File(root, "data")
    val bund = new File(data, context.getBundle.getSymbolicName)
    bund.mkdirs()
    bund
  }

  /**
   * File or directory within the external data root
   * This data will be lost every time the bundle / software is upgraded.
   */
  def getExternalDataFile(context: BundleContext, name: String): File =
    new File(getExternalDataRoot(context), name)

  /**
   * Gets the root directory for permanent user data for the given bundle. Data stored in this
   * directory will still be available after an upgrade of the related gemini bundle. Data is
   * segregated based on the `test` flag in order to facilitate testing without clobbering user
   * data (typically computed via `Version.current.isTest`).
   * @param context the osgi bundle context
   * @param test `true` if this is a test version
   * @return
   */
  def getPermanentDataRoot(context: BundleContext, test: Boolean): File = {
    val name = context.getBundle.getSymbolicName
    val version = context.getBundle.getVersion
    val bundleSegment = if (test) "bundle-test" else "bundle"
    val bundleRootDir = new File(permanentUserRoot, bundleSegment + File.separator + name)
    bundleRootDir.mkdirs()
    bundleRootDir
  }

  def getPermanentDataFile(context: BundleContext, test: Boolean, name: String, migrationSteps: List[MigrationStep]): File = {
    val root = getPermanentDataRoot(context, test)
    val file = new File(root, name)
    if (file.exists()) file else migrate(root, name, migrationSteps)
  }

  private def migrate(root: File, name: String, migrationSteps: List[MigrationStep]): File = {
    migrationSteps foreach { step =>
      val from = new File(root, step.fromName)
      val to = new File(root, step.toName)
      if (from.exists() && !to.exists()) step.migrate(from, to)
    }
    new File(root, name)
  }

  /**
   * Gets the root directory for permanent user data.
   * Note: The actual location for permanent user data is OS dependent.
   * Data stored in this directory will still be available after an upgrade of the related gemini software.
   * @return
   */
  lazy val permanentUserRoot: File = {
    // decide on root directory for this OS
    val rootDir =
      if (isMac) userHome + "/Library/Application Support/Gemini"
      else if (isWin) winAppData + "\\Gemini"
      else if (isLinux) userHome + "/.gemini"
      else defaultDir

    // check if directory exists and has read and write access, if not, return "<user.home>/.gemini" as fallback
    val userRoot = createDir(rootDir)
    if (userRoot.exists && userRoot.canRead && userRoot.canWrite) userRoot
    else createDir(defaultDir)
  }


  // == helper methods
  private val osName = System.getProperty("os.name")
  private val userHome = System.getProperty("user.home")
  private val winAppData = System.getenv("APPDATA")
  private val isMac = osName.contains("Mac")
  private val isWin = osName.contains("Win")
  private val isLinux = osName.contains("Linux")

  // as default we provide a "hidden" gemini folder in the user home directory
  private val defaultDir = userHome + File.separator + ".gemini"

  // add bundle name to given directory and create directories (as needed)
  private def createDir(dirName: String): File = {
    val directory = new File(dirName + File.separator)
    directory.mkdirs
    directory
  }

  /**
   * Definition of a migration step that takes an input file (which has to exist) and an output
   * file which will be used as the target of the migrated file and does not exist yet.
   * The migration method is expected to take the first file as the input and read and transform it as
   * needed and write the result to the second file.
   */
  case class MigrationStep(fromName: String, toName: String, migrate: (File, File) => Unit)
  
}
