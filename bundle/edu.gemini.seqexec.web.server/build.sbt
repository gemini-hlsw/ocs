import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.seqexec.web.server"

version := ocsVersion.value.toOsgiVersion

libraryDependencies ++= Seq(
  // NOTE this doesn't work on OSGI, http4s is not OSGi friendly
  "org.http4s" %% "http4s-dsl"          % "0.12.0",
  "org.http4s" %% "http4s-blaze-server" % "0.12.0",
  "org.scalaz"             %% "scalaz-core" % "7.1.6",
  "org.scalaz"             %% "scalaz-concurrent" % "7.1.6"
  )

Revolver.settings

osgiSettings

//Revolver.reStart <<= (Revolver.reStart dependsOn (crossTarget in (Compile)).value)

mainClass in Revolver.reStart := Some("edu.gemini.seqexec.web.server.WebServerLauncher")

unmanagedResources in (Compile, packageBin) += (classDirectory in Compile).value / "*.js"

resourceDirectories in (Compile, packageBin) += (classDirectory in Compile).value / "*.js"

//logLevel := Level.Debug

//crossTarget in (Compile, fastOptJS) := baseDirectory.value / "src/main/resources/sjs"

crossTarget in (Compile, fullOptJS) := file("js")

//crossTarget in packageExportedProductsJS := (crossTarget.value) / "main/public/javascripts"
crossTarget in (Compile, packageJSDependencies) := file("js")

artifactPath in (Compile, fastOptJS) := (resourceManaged in Compile).value /
  ((moduleName in fastOptJS).value + "-opt.js")

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.0",
  "com.github.japgolly.scalajs-react" %%% "core" % "0.10.4",
  "com.github.japgolly.scalajs-react" %%% "extra" % "0.10.4",
  "com.lihaoyi" %% "upickle" % "0.3.8"
)

// TODO get dependencies via webjars
jsDependencies += "org.webjars" % "react" % "0.14.3" / "react-with-addons.js" commonJSName "React"

skip in packageJSDependencies := false

ocsBundleSettings 

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.importPackage := Seq("!org.scalajs.jquery", "!org.scalajs.dom", "!scalatags.*", "*")