import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.seqexec.web.server"

version := ocsVersion.value.toOsgiVersion

libraryDependencies ++= Seq(
  // NOTE this doesn't work on OSGI, http4s is not OSGi friendly
  // http4s
  "org.http4s"  %% "http4s-dsl"          % "0.12.0",
  "org.http4s"  %% "http4s-blaze-server" % "0.12.0",

  // Play
  "com.typesafe.play" %% "play" % "2.4.6",
  "com.typesafe.play" %% "play-netty-server" % "2.4.6",

  "org.scalaz"  %% "scalaz-core" % "7.1.6",
  "org.scalaz"  %% "scalaz-concurrent" % "7.1.6",
  "com.lihaoyi" %% "upickle" % "0.3.8"
  )

Revolver.settings

// Class to launch re-start with
mainClass in Revolver.reStart := Some("edu.gemini.seqexec.web.server.play.WebServerLauncher")

// Run FastOptJS on the JS project for reStart
Revolver.reStart <<= Revolver.reStart dependsOn (fastOptJS in (bundle_edu_gemini_seqexec_web_client, Compile))

// This will make the generated js available to this bundle/server
//(managedResources in Compile) += (artifactPath in(bundle_edu_gemini_seqexec_web_client, Compile, fastOptJS)).value

unmanagedResources in Compile ++= (unmanagedResources in(bundle_edu_gemini_seqexec_web_client, Compile)).value

//(unmanagedResourceDirectories in (Compile, Revolver.reStart)) += (sourceDirectory in(bundle_edu_gemini_seqexec_web_client, Compile, fastOptJS)).value

unmanagedResources in Compile += (fastOptJS in (bundle_edu_gemini_seqexec_web_client, Compile)).map(_.data).value

skip in packageJSDependencies := false

osgiSettings

ocsBundleSettings 

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.importPackage := Seq("!org.scalajs.jquery", "!org.scalajs.dom", "!scalatags.*", "*")