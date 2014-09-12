package edu.gemini.sp.vcs2

import edu.gemini.pot.sp._
import edu.gemini.spModel.data.ISPDataObject

case class NodeUpdate(key: SPNodeKey, dataObject: ISPDataObject, children: List[SPNodeKey]) {

  def createNode(p: ISPProgram, f: ISPFactory): Option[ISPNode] = {
    import SPComponentBroadType._
    import SPComponentType.{OBS_EXEC_LOG, OBS_QA_LOG, TEMPLATE_FOLDER, TEMPLATE_GROUP, TEMPLATE_PARAMETERS}
    val ct = dataObject.getType

    def obsComp = Some(f.createObsComponent(p, ct, key))
    def seqComp = Some(f.createSeqComponent(p, ct, key))
    def other(c: (ISPProgram, SPNodeKey) => ISPNode) = Some(c(p, key))

    val n = ct.broadType match {
      case AO          => obsComp
      case CONFLICT    => other(f.createConflictFolder)
      case DATA        => None
      case ENGINEERING => obsComp
      case GROUP       => other(f.createGroup)
      case INFO        => obsComp
      case INSTRUMENT  => obsComp
      case ITERATOR    => seqComp
      case OBSERVATION => other(f.createObservation) // TODO: obs #
      case OBSERVER    => seqComp
      case OBSLOG      => if (ct == OBS_EXEC_LOG) other(f.createObsExecLog)
                          else if (ct == OBS_QA_LOG) other(f.createObsQaLog)
                          else None
      case PLAN        => None
      case PROGRAM     => Some(f.createProgram(key, p.getProgramID))
      case SCHEDULING  => obsComp
      case TELESCOPE   => obsComp
      case TEMPLATE    => if (ct == TEMPLATE_FOLDER) other(f.createTemplateFolder)
                          else if (ct == TEMPLATE_GROUP) other(f.createTemplateGroup)
                          else if (ct == TEMPLATE_PARAMETERS) other(f.createTemplateParameters)
                          else None
      case UNKNOWN     => None
    }
    n.foreach(_.setDataObject(dataObject))
    n
  }
}