import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.shared.util"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-library_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"))

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

        
