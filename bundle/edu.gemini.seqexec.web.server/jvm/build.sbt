import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.seqexec.web.server.jvm"

version := ocsVersion.value.toOsgiVersion

libraryDependencies ++= Seq(
  // NOTE this doesn't work on OSGI, http4s is not OSGi friendly
  "org.http4s" %% "http4s-dsl"          % "0.12.0",
  "org.http4s" %% "http4s-blaze-server" % "0.12.0",
  "org.scalaz"             %% "scalaz-core" % "7.1.6",
  "org.scalaz"             %% "scalaz-concurrent" % "7.1.6",
  "com.lihaoyi" %% "upickle" % "0.3.8"
  )

Revolver.settings

// Run FastOptJS on the JS project for reStart
Revolver.reStart <<= Revolver.reStart dependsOn (fastOptJS in (bundle_edu_gemini_seqexec_web_server_JS, Compile))

// Class to launch re-start with
mainClass in Revolver.reStart := Some("edu.gemini.seqexec.web.server.WebServerLauncher")

// This will make the generated js available to this bundle/server
(resources in(bundle_edu_gemini_seqexec_web_server_JVM, Compile, Revolver.reStart)) ++= Seq(
  (artifactPath in(bundle_edu_gemini_seqexec_web_server_JS, Compile, fastOptJS)).value
)

//unmanagedResources in (Compile, packageBin) += (classDirectory in Compile).value / "*.js"

//resourceDirectories in (Compile, packageBin) += (classDirectory in Compile).value / "*.js"

//logLevel := Level.Debug

//crossTarget in (Compile, fastOptJS) := baseDirectory.value / "src/main/resources/sjs"

//crossTarget in (Compile, fullOptJS) := file("js")

//crossTarget in packageExportedProductsJS := (crossTarget.value) / "main/public/javascripts"
//crossTarget in (Compile, packageJSDependencies) := file("js")

artifactPath in (Compile, fastOptJS) := (resourceManaged in Compile).value /
  ((moduleName in fastOptJS).value + "-opt.js")

skip in packageJSDependencies := false

osgiSettings

ocsBundleSettings 

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.importPackage := Seq("!org.scalajs.jquery", "!org.scalajs.dom", "!scalatags.*", "*")