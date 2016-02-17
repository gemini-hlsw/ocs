import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.security"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/scala-reflect-2.10.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scala-swing_2.10-2.0.0-SNAPSHOT.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-concurrent_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-stream_2.10-0.7.2a.jar"),
  new File(baseDirectory.value, "../../lib/bundle/doobie-core_2.10-0.2.4-SNAPSHOT.jar"),
  new File(baseDirectory.value, "../../lib/bundle/shapeless_2.10-2.2.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scodec-bits_2.10-1.0.9.jar"),
  new File(baseDirectory.value, "../../lib/bundle/quasiquotes_2.10-2.0.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/h2-1.3.170.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.util.security.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("*")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.util.security.permission",
  "edu.gemini.util.security.principal",
  "edu.gemini.util.security.auth",
  "edu.gemini.util.security.auth.ui",
  "edu.gemini.util.security.policy",
  "edu.gemini.util.security.auth.keychain")

initialCommands := """
  import edu.gemini.util.security.auth.keychain._
  import scalaz._, Scalaz._, scalaz.effect._
  import doobie.imports._
  import java.io.File
  import edu.gemini.util.security.principal._
"""

// required for shapeless on 2.10.5
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
