import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.seqexec.web.server"

version := ocsVersion.value.toOsgiVersion

//unmanagedJars in Compile ++= Seq(
  //new File(baseDirectory.value, "../../lib/bundle/http4s-core_2.11-0.12.0.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/http4s-server_2.11-0.12.0.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/http4s-blazeserver_2.11-0.8.6.jar"),
//  new File(baseDirectory.value, "../../lib/bundle/http4s-server_2.10-0.9.3.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/http4s-blaze-server_2.10-0.9.3.jar")
  //new File(baseDirectory.value, "../../lib/bundle/http4s-dsl_2.10-0.9.3.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/scodec-bits_2.10-1.0.9.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/scalaz-stream_2.10-0.7.3a.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/scalaz-concurrent_2.10-7.1.3.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/scala-reflect-2.10.5.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/scalajs-library_2.10-0.6.5.jar"),
  // Runtime
  //new File(baseDirectory.value, "../../lib/bundle/blaze-core_2.11-0.11.0.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/slf4j-api-1.6.4.jar")
  //new File(baseDirectory.value, "../../lib/bundle/slf4j-jdk14-1.7.12.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/parboiled_2.11-2.1.0.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/blaze-core_2.10-0.8.2.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/blaze-http_2.10-0.8.2.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/http4s-blaze-core_2.10-0.9.3.jar"),
  //new File(baseDirectory.value, "../../lib/bundle/shapeless_2.10-2.2.5.jar")
//)

libraryDependencies ++= Seq(
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

//libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.8.1"

libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.5.3"

ocsBundleSettings 

OsgiKeys.bundleActivator := Some("edu.gemini.p1.backend.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq("edu.gemini.p1.backend")

OsgiKeys.importPackage := Seq("!org.scalajs.jquery", "!org.scalajs.dom", "!scalatags.*", "*")