import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.ictd"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/mysql-connector-java-5.1.46.jar")
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
  "edu.gemini.ictd")

initialCommands := """
  import edu.gemini.ictd._
  import edu.gemini.ictd.dao._
  import scalaz._, Scalaz._, scalaz.effect._
  import doobie.imports._
  import java.io.File
"""
