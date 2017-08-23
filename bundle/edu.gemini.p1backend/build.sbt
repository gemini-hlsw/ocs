import OcsKeys._

// note: inter-project dependencies are declared in projects/OcsBundle.scala

name := "edu.gemini.p1backend"

// version set in ThisBuild

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.0-MF"
  // The first one to use `cats-effects`. If not released yet, point to the
  // git repo/branch in `dependsOn` in OcsBundle.scala.
  // "org.http4s" %% "http4s-dsl" % "0.18.0-SNAPSHOT"
  )

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq("edu.gemini.p1backend")
