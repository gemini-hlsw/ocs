import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.osgi.main"

version := "4.2.1" // same as the version of Felix that we're wrapping

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

// Explode and include the jars in our local lib/ directory.
OsgiKeys.additionalHeaders += ("Include-Resource", 
  Option(unmanagedBase.value.listFiles.filter(_.getName.endsWith(".jar")))
    .map(_.toList)
    .getOrElse(Nil)
    .map(f => "@" + f.getAbsolutePath)
    .mkString(", "))

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "org.osgi.resource",
  "org.osgi.framework.startlevel",
  "org.osgi.framework.wiring",
  "org.osgi.service.url",
  "org.osgi.framework.hooks.service",
  "org.osgi.framework.hooks.resolver",
  "org.osgi.service.startlevel",
  "org.osgi.framework.launch",
  "org.osgi.util.tracker",
  "org.osgi.framework.hooks.bundle",
  "org.osgi.service.packageadmin",
  "org.osgi.framework.namespace",
  "org.osgi.framework",
  "org.osgi.framework.hooks.weaving")

OsgiKeys.additionalHeaders +=
  ("Main-Class" -> "edu.gemini.osgi.main.Main")


