import OcsKeys._

name := "ocs"

ocsVersion in ThisBuild := OcsVersion("2015A", true, 1, 1, 1)

pitVersion in ThisBuild := OcsVersion("2015A", false, 1, 2, 0)

// Bundles by default use the ocsVersion; this is overridden in bundles used only by the PIT
version in ThisBuild := ocsVersion.value.toOsgiVersion

scalaVersion in ThisBuild := "2.10.4"

// Note that this is not a standard setting; it's used for building IDEA modules.
javaVersion in ThisBuild := "1.7" 

scalacOptions in ThisBuild ++= Seq(
  // "-deprecation",
  "-encoding", "UTF-8",  // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:reflectiveCalls", // TODO: turn this off
  "-language:postfixOps",      // TODO: turn this off
  "-target:jvm-1.7",
  "-unchecked",
  // "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args", 
  "-Ywarn-all"
  // "-Ywarn-dead-code"        // N.B. doesn't work well with bottom
  // "-Ywarn-numeric-widen",   
  // "-Ywarn-value-discard"   
)

javacOptions in ThisBuild ++= Seq(
  "-source", "1.7",
  "-target", "1.7",
  "-Xlint:all,-serial,-path,-deprecation,-unchecked,-fallthrough" // TOOD: turn all on except maybe -serial and -path
)

// Use managed dependencies for tests; everyone gets JUnit, ScalaCheck, and Specs2
libraryDependencies in ThisBuild ++= Seq(
  "junit"           % "junit"           % "4.11"   % "test",
  "com.novocode"    % "junit-interface" % "0.9"    % "test",
  "org.scalacheck" %% "scalacheck"      % "1.10.1" % "test",
  "org.specs2"     %% "specs2"          % "1.12.3" % "test"
)

// Don't build scaladoc (for now)
publishArtifact in (ThisBuild, packageDoc) := false

// Don't build package source (for now)
publishArtifact in (ThisBuild, packageSrc) := false

// No poms
publishMavenStyle in ThisBuild := false


