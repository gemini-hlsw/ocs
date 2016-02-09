import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.seqexec.web.client"

val jsName = SettingKey[String]("jsfilename")

jsName := "seqexec"

version := ocsVersion.value.toOsgiVersion

osgiSettings

artifactPath in (Compile, fastOptJS) := (resourceManaged in Compile).value /
  (jsName.value + "-opt.js")

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.0",
  "com.github.japgolly.scalajs-react" %%% "core" % "0.10.4",
  "com.github.japgolly.scalajs-react" %%% "extra" % "0.10.4",
  "com.lihaoyi" %%% "upickle" % "0.3.8"
)

// TODO get dependencies via webjars
jsDependencies += "org.webjars" % "react" % "0.14.3" / "react-with-addons.js" commonJSName "React"

skip in packageJSDependencies := false

ocsBundleSettings 

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.importPackage := Seq("!org.scalajs.jquery", "!org.scalajs.dom", "!scalatags.*", "*")