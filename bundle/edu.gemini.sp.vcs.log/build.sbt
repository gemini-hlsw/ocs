import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.sp.vcs.log"

// version set in ThisBuild

libraryDependencies ++= Seq(
  "org.scalaz"     %% "scalaz-core"       % ScalaZVersion,
  "org.scalaz"     %% "scalaz-effect"     % ScalaZVersion,
  "org.scalaz"     %% "scalaz-concurrent" % ScalaZVersion,
  "org.tpolecat"   %% "doobie-core"       % "0.3.0a",
  "com.chuusai"    %% "shapeless"         % "2.3.2",
  "org.scala-lang" %  "scala-compiler"    % scalaVersion.value)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.sp.vcs.log.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.sp.vcs.log")

