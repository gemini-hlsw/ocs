package edu.gemini.osgi.tools.app

import java.io.File

import scala.xml._

case class ApplicationMeta(
  id: String,
  name: String,
  useShortVersion: Boolean) {
  val versionRegex = """(\d\d\d\d.)\.(\d*)\.(\d*)\.(\d*)"""r

  def shortVersion(version: String) = version match {
    case versionRegex(y, s, m, _) if useShortVersion => s"$y.$s.$m"
    case x                                           => x
  }

  def executableName(version: String) = s"${id}_$version"
  def osxVisibleName(version: String) = s"Gemini ${name} ${shortVersion(version)}"
  def winVisibleName(version: String) = s"Gemini ${name}"
}

case class Application(
  id: String,
  name: String,
  label: Option[String] = None,
  version: String,
  configs: Seq[Configuration],
  useShortVersion: Boolean = false) {
  def meta = ApplicationMeta(id, name, useShortVersion)
}

