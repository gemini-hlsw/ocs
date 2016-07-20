package edu.gemini.shared.gui

import java.awt.Desktop._
import java.net.{URI, URL}
import java.util.logging.{Level, Logger}

import edu.gemini.shared.Platform

/**
 * Utility for launching a browser on a URL, which is problematic on Linux.
 * This utility first tries to use the java.awt.Desktop method and then resorts
 * to low-level OS-specific exec if that doesn't work.
 */
object Browser {
  private val LOG = Logger.getLogger(Browser.getClass.getName)

  def open(url: URL) {
    open(url.toURI)
  }

  private lazy val desktopOk = isDesktopSupported && getDesktop.isSupported(Action.BROWSE)

  // Performs the side-effect of opening the URI and returns a boolean if we
  // think it worked
  private def openWithDesktop(uri: URI): Boolean =
    try {
      getDesktop.browse(uri)
      true
    } catch {
      case ex: Exception =>
        log(ex, uri)
        false
    }

  def open(uri: URI) {
    if (!(desktopOk && openWithDesktop(uri))) {
      try {
        Platform.displayURL(uri.toString)
      } catch {
        case ex: Exception => log(ex, uri)
      }
    }
  }

  private def log(ex: Exception, uri: URI) {
    LOG.log(Level.WARNING, "Could not open URI for browsing: " + uri, ex)
  }
}
