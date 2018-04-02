package edu.gemini.spModel.gemini.ghost

import edu.gemini.pot.sp.{ISPObsComponent, SPComponentType}
import edu.gemini.spModel.config.IConfigBuilder
import edu.gemini.spModel.gemini.inst.DefaultInstNodeInitializer
import edu.gemini.spModel.obscomp.SPInstObsComp

/** Node initializer for GHOST.
  */
final class GhostNI extends DefaultInstNodeInitializer {
  override def getType: SPComponentType = {
    Ghost.SP_TYPE
  }

  override protected def createConfigBuilder(node: ISPObsComponent): IConfigBuilder = {
    new GhostCB(node)
  }

  override def createDataObject(): SPInstObsComp = {
    new Ghost
  }
}
