package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.spModel.template.SpBlueprint
import edu.gemini.phase2.template.factory.impl.GroupInitializer
import edu.gemini.spModel.gemini.gmos.GmosCommonType

trait GmosBase[B <: SpBlueprint] extends GroupInitializer[B] {

  /** Select the filter with wavelength closest to specified lambda. */
  def closestFilter[A <: GmosCommonType.Filter](f: A, fs:A*)(lambda:Double) = {
    (f :: fs.toList).map(f => (f, Math.abs(f.getWavelength.toDouble - lambda))).sortBy(_._2).map(_._1).head
  }

}
