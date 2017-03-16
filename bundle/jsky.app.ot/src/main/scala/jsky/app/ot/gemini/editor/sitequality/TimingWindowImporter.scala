package jsky.app.ot.gemini.editor.sitequality

import java.awt.Component
import java.io.File
import java.util.Collections

import edu.gemini.shared.gui.Chooser
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow

import scala.collection.JavaConverters._

// Prompt the user for a file and then parse it and return a list of TimingWindows.
class TimingWindowImporter(owner: Component) {
  // TODO: logging and system preferences to store directory.

  def promptImport(): java.util.List[TimingWindow] = {
    val fileChooser = new Chooser[TimingWindowImporter]("importer", owner)
    fileChooser.chooseOpen("Timing Windows (.tw)", "tw").fold(Collections.emptyList[TimingWindow]())(f => parseTimingWindows(f).asJava)
  }

  private def parseTimingWindows(twf: File): List[TimingWindow] = Nil
}

