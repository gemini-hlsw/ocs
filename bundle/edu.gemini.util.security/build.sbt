import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.security"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/h2-1.3.170.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz"             %% "scalaz-core" % ScalaZVersion,
  "org.scalaz"             %% "scalaz-effect" % ScalaZVersion,
  "org.scalaz"             %% "scalaz-concurrent" % ScalaZVersion,
  "org.tpolecat"           %% "doobie-core" % "0.3.0a",
  "org.scala-lang"         %  "scala-reflect" % scalaVersion.value,
  "org.scala-lang"         %  "scala-compiler" % scalaVersion.value
  )

osgiSettings

ocsBundleSettings

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("*")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.util.security.permission",
  "edu.gemini.util.security.principal",
  "edu.gemini.util.security.auth",
  "edu.gemini.util.security.policy",
  "edu.gemini.util.security.auth.keychain")

initialCommands := """
  import edu.gemini.util.security.auth.keychain._
  import scalaz._, Scalaz._, scalaz.effect._
  import doobie.imports._
  import java.io.File
  import edu.gemini.util.security.principal._
"""
