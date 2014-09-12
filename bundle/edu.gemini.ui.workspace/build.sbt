import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.ui.workspace"

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-jdesktop-swingx_2.10-1.6.4.jar"),
  new File(baseDirectory.value, "../../lib/bundle/com-jgoodies-looks_2.10-2.4.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.ui.workspace.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("*")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.ui.gface",
  "edu.gemini.ui.gface.util",
  "edu.gemini.ui.workspace",
  "edu.gemini.ui.workspace.impl",
  "edu.gemini.ui.workspace.osgi",
  "edu.gemini.ui.workspace.scala",
  "edu.gemini.ui.workspace.util")

        
