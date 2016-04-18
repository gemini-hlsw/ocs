import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.security"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/h2-1.3.170.jar")
)

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "2.0.0-M2",
  "org.scalaz.stream"      %% "scalaz-stream" % "0.8a" intransitive(),
  "org.scalaz"             %% "scalaz-core" % ScalaZVersion,
  "org.scalaz"             %% "scalaz-effect" % ScalaZVersion,
  "org.scalaz"             %% "scalaz-concurrent" % ScalaZVersion,
  "org.tpolecat"           %% "doobie-core" % "0.3.0-M1",
  "com.chuusai"            %% "shapeless" % "2.3.0",
  "org.scodec"             %% "scodec-bits" % "1.0.9",
  "org.scala-lang"         %  "scala-reflect" % "2.11.7",
  "org.scala-lang"         %  "scala-compiler" % "2.11.7"
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
