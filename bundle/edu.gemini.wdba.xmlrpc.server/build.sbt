import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.wdba.xmlrpc.server"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/argonaut_2.11-6.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-core_2.11-1.2.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-macro_2.11-1.2.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-xmlrpc_2.10-3.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.wdba.server.osgi.WDBAServerActivator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.wdba.exec",
  "edu.gemini.wdba.session",
  "edu.gemini.wdba.tcc",
  "edu.gemini.wdba.glue",
  "edu.gemini.wdba.glue.api")

OsgiKeys.privatePackage := Seq("edu.gemini.wdba.*")
