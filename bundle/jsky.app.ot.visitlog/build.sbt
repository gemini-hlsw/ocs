import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "jsky.app.ot.visitlog"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-jdesktop-swingx_2.10-1.6.4.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-swing_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.0.5.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("jsky.app.ot.visitlog.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  )

        
