package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.spModel.template.SpBlueprint
import edu.gemini.phase2.template.factory.impl.GroupInitializer
import edu.gemini.spModel.gemini.gmos.GmosCommonType
import edu.gemini.pot.sp.ISPNode
import edu.gemini.spModel.core.SPProgramID

trait GmosBase[B <: SpBlueprint] extends GroupInitializer[B] {

  /** Select the filter with wavelength closest to specified lambda. */
  def closestFilter[A <: GmosCommonType.Filter](f: A, fs:A*)(lambda:Double) = {
    (f :: fs.toList).map(f => (f, Math.abs(f.getWavelength.toDouble - lambda))).sortBy(_._2).map(_._1).head
  }

  // SET "Custom Mask MDF" = G(N/S)YYYYS(Q/C/DD/SV/LP/FT)XXX-NN
  //     where:
  //     (N/S) is the site
  //     YYYYS is the semester, e.g. 2015A
  //     (Q/C/DD/SV/LP/FT) is the program type
  //     XXX is the program number, e.g. 001, or 012, or 123
  //     NN should be the string "NN" since the mask number is unknown
  def defaultCustomMaskName(pid: SPProgramID): String = {
    // or, in programmer-speak, delete the hyphens and add -NN
    pid.toString.filterNot(_ == '-') + "-NN"
  }

}
