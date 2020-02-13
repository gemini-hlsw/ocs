import sbt.{ State => _, Configuration => _, Show => _, _ }
import Keys._
import scalaz._
import Scalaz.{ state => _, _}
import sbt.complete.DefaultParsers._
import sbt.complete._
import edu.gemini.osgi.tools.idea.{ IdeaModule, IdeaProject, IdeaProjectTask }
import edu.gemini.osgi.tools.app.{ Application, Configuration, AppBuilder }
import edu.gemini.osgi.tools.Version
import xml.PrettyPrinter

object OcsKeys // TODO: get rid of

trait OcsKey { this: OcsBundleSettings =>

  lazy val javaVersion = settingKey[String]("Java version to use for -source and -target, and IDEA projects.")
  lazy val ocsBootTime = settingKey[Long]("Time of last project reload, for caching.")
  lazy val ocsLibraryBundles = settingKey[List[File]]("List of all library bundles in the build.")
  lazy val ocsAllProjects = settingKey[List[ProjectRef]]("List of all projects in the build.")
  lazy val ocsAllBundleProjects = settingKey[List[ProjectRef]]("List of all bundle projects in the build.")

  lazy val ocsProjectDependencies = settingKey[Seq[ProjectRef]]("List of projects we depend on.")
  lazy val ocsProjectAggregate = settingKey[Seq[ProjectRef]]("List of projects we aggregate.")
  lazy val ocsDependencies = settingKey[Seq[ProjectRef]]("List of projects we depend on or aggregate.")
  lazy val ocsClosure = taskKey[Seq[ProjectRef]]("List of projects we depend on or aggregate, recursively.")
  lazy val ocsUsers = taskKey[Seq[ProjectRef]]("List of bundle projects that directly depend on us.")

  lazy val ocsBundleIdeaModuleAbstractPath = settingKey[File]("Abstract path of the [possibly non-existent] IDEA module for the current project.")
  lazy val ocsBundleIdeaModuleName = settingKey[String]("IDEA module name for the current project.")
  lazy val ocsBundleIdeaModule = taskKey[File]("Builds an IDEA module for the current project.")
  lazy val ocsBundleDependencies  = taskKey[Unit]("Display a full tree of bundle dependencies.")
  lazy val ocsBundleDependencies0 = taskKey[Unit]("Display a list of direct bundle dependencies.")
  lazy val ocsBundleUsers = taskKey[Unit]("Display a full tree of bundle users.")
  lazy val ocsBundleUsers0 = taskKey[Unit]("Display a list of direct bundle users.")
  lazy val ocsBundleInfo = taskKey[OcsBundleInfo]("Show bundle info.")

  lazy val ocsAppManifest = settingKey[Application]("App manifest.")
  lazy val ocsAppInfo = taskKey[Unit]("Show information about the current app.")
  lazy val ocsJreDir = settingKey[File]("Directory where distribution JREs are stored.")

  lazy val ocsVersion = settingKey[OcsVersion]("OCS version for non-PIT bundles and applications.")
  lazy val pitVersion = settingKey[OcsVersion]("Version for PIT and its [unshared] bundles.")

}

// Isomorphic with the real spModel.core.Version, but used at build time
case class OcsVersion(semester: String, test: Boolean, xmlCompatibility: Int, serialCompatibility: Int, minor: Int) {

  private val Pat = "(\\d{4})([AB])".r
  private val Pat(year, half) = semester
  private val halfDigit = if (half == "A") 0 else 1

  /** Convert to an OSGi-compatible version. */
  def toOsgiVersion: String =
    f"${year}%s${halfDigit}%d${xmlCompatibility}%02d.${serialCompatibility}%d.${minor}%d"

  def toBundleVersion: Version =
    Version.parse(toOsgiVersion)

  def sourceFileName = "CurrentVersion.java"

  def toClass(pkg: String): String = s"""
    |package $pkg;
    |
    |import java.text.ParseException;
    |import edu.gemini.spModel.core.Version;
    |import edu.gemini.spModel.core.Semester;
    |
    |// AUTO-GENERATED; DO NOT MODIFY
    |
    |class CurrentVersion {
    |
    |  /** Current version, as generated from the build. N.B. bundles at this version have OSGi version ${toOsgiVersion} */
    |  static Version get() { 
    |    try {
    |      return new Version(Semester.parse("$semester"), $test, $xmlCompatibility, $serialCompatibility, $minor);
    |    } catch (ParseException pe) {
    |      throw new Error("Bogus Version; check the build.", pe);
    |    }
    |  }
    |
    |}
    """.trim.stripMargin

  override def toString: String = {
    val testString = if (test) "-test" else ""
    s"${semester}${testString}.$xmlCompatibility.$serialCompatibility.$minor"
  }

}