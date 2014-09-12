import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "jsky.elevation.plot"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-jfree_2.10-1.0.14.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.privatePackage := Seq("jsky.plot.*")

OsgiKeys.exportPackage := Seq(
  "jsky.plot",
  "jsky.plot.util",
  "jsky.plot.util.gui")

        
