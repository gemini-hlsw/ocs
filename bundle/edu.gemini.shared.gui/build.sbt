import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.shared.gui"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/com-jgoodies-looks_2.10-2.4.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-jdesktop-swingx_2.10-1.6.4.jar"))

libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "2.0.0-M2"

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.shared",
  "edu.gemini.shared.gui",
  "edu.gemini.shared.gui.bean",
  "edu.gemini.shared.gui.calendar",
  "edu.gemini.shared.gui.textComponent",
  "edu.gemini.shared.gui.monthview",
  "edu.gemini.shared.gui.text")
