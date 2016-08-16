import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "jsky.util.gui"

// version set in ThisBuild

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("*") // To load resources like icons, images, etc

OsgiKeys.exportPackage := Seq(
  "jsky.util.gui")

OsgiKeys.additionalHeaders +=
  ("Import-Package" -> "!com.sun.java.swing.plaf.windows,*")
