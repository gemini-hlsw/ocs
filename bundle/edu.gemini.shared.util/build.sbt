import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.shared.util"

// version set in ThisBuild

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4")

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.shared.util",
  "edu.gemini.shared.util.astroServer",
  "edu.gemini.shared.util.bean",
  "edu.gemini.shared.util.exec",
  "edu.gemini.shared.util.test",
  "edu.gemini.shared.util.immutable")

