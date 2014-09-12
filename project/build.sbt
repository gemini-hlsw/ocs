
// Dependencies for the build itself.

libraryDependencies ++= Seq(
  "org.ow2.asm" % "asm" % "4.0",
  "org.eclipse.osgi" % "org.eclipse.osgi" % "3.6.0.v20100517",
  "org.scalaz" %% "scalaz-core" % "7.0.4",
  "org.scalaz" %% "scalaz-effect" % "7.0.4"
)


// TODO: remove this, it's for the inlined sbt-osgi plugin
libraryDependencies += "biz.aQute.bnd" % "bndlib" % "2.1.0"

