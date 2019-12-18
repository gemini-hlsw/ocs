package edu.gemini.itc.web.html

import java.io.PrintWriter

import edu.gemini.itc.shared.{ConfigCreator, GhostParameters, ItcParameters, PlottingDetails}
import edu.gemini.spModel.obscomp.ItcOverheadProvider

/**
 * Helper class for printing GHOST calculation results to an output stream.
 */
// TODO-GHOSTITC
final class GhostPrinter(p: ItcParameters,
                         instr: GhostParameters,
                         pdp: PlottingDetails,
                         out: PrintWriter) extends PrinterBase(out) with OverheadTablePrinter.PrinterWithOverhead {
  override def writeOutput(): Unit = ???

  override def createInstConfig(numberExposures: Int): ConfigCreator#ConfigCreatorResult = {
    val cc: ConfigCreator = new ConfigCreator(p)
    cc.createGhostConfig(instr, numberExposures)
  }

  override def getInst: ItcOverheadProvider = ???

  override def getReadoutTimePerCoadd: Double = ???
}
